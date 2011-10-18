/*
Copyright (c) 2010-2011 Per Gantelius

This software is provided 'as-is', without any express or implied
warranty. In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

   1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required.

   2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.

   3. This notice may not be removed or altered from any source
   distribution.
*/
#include "kwl_asm.h"
#include "kwl_audiodata.h"
#include "kwl_audiofileutil.h"
#include "kwl_synchronization.h"
#include "kwl_decoder.h"
#include "kwl_event.h"
#include "kwl_eventdefinition.h"
#include "kwl_memory.h"
#include "kwl_messagequeue.h"
#include "kwl_positionalaudiolistener.h"
#include "kwl_softwaremixer.h"
#include "kwl_sound.h"
#include "kwl_soundengine.h"
#include "kwl_wavebank.h"

#include "kwl_assert.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>

int kwlSoundEngine_isFreeformEventHandle(kwlSoundEngine* engine, kwlEventHandle handle)
{
    int mask = 1 << 31;
    int isFreeForm = handle & mask;
    return isFreeForm;
}

int kwlSoundEngine_getFreeformIndexFromHandle(kwlSoundEngine* engine, kwlEventHandle handle)
{
    return handle & 0xffff;
}

kwlEvent* kwlSoundEngine_getEventFromHandle(kwlSoundEngine* engine, kwlEventHandle handle)
{
    kwlEvent* event = NULL;
    
    if (kwlSoundEngine_isFreeformEventHandle(engine, handle))
    {
        int freeformEventIndex = handle & 0xffff;
        if (freeformEventIndex >= 0 &&
            freeformEventIndex < engine->freeformEventArraySize)
        {
            event = engine->freeformEvents[freeformEventIndex];
        }
    }
    else
    {
        int eventDefinitionIndex = handle & 0xffff;
        int eventInstanceIndex = (handle & 0x7fff) >> 16;
        
        if (eventDefinitionIndex >= 0 && 
            eventDefinitionIndex < engine->numEventDefinitions)
        {
            if (eventInstanceIndex >= 0 && 
                eventInstanceIndex < engine->eventDefinitions[eventDefinitionIndex].instanceCount)
            {
                event = &engine->events[eventDefinitionIndex][eventInstanceIndex];
            }
        }
    }    
    /* A return value of NULL is valid and means that the event handle was invalid.*/
    return event;
}

/** */
kwlEventHandle computeEventHandle(int eventDefinitionOrFreeformIndex, int eventInstanceIndex, int isFreeForm)
{
    /*
     The event handle uniquely indentifies an event instance that could either 
     be loaded from data or created in code as a freeform event. A data event
     is uniquely defined by an event definition index and an event instance index
     and a freeform event by its index into the array of freeform events.
     
     An event handle is a 32 bit int encoded as follows:
     
     bit number |        32             |  31  |  ...  |  17  |  16  |  ...  |  1  |
                |-----------------------|-------------------------------------------
                |0 for data events      |these 15 bits encode |these 16 bits encode the 
                |1 for freeform events  |the event instance   |event definition index for data
                |                       |index and are zero   |events and the array index for 
                |                       |for freeform events  |freeform events.
     
     This encoding scheme means that the maximum number of event instances per definition is 32767
     and the maximum number of event definitions and freeform events is 65535.
     
     KWL_INVALID_HANDLE = 0xffffffff is used to represent invalid handles.
     */
    
    KWL_ASSERT(eventDefinitionOrFreeformIndex >= 0);
    KWL_ASSERT(eventInstanceIndex >= 0);
    KWL_ASSERT(eventDefinitionOrFreeformIndex < (1 << 16));
    KWL_ASSERT(eventInstanceIndex < (1 << 15));
    
    int bit32 = (isFreeForm != 0 ? 1 : 0) << 31;
    int bits31To17 = eventInstanceIndex << 16;
    int bits1To16 = eventDefinitionOrFreeformIndex;
    
    int handle = bit32 | bits31To17 | bits1To16;
    /*printf("computing event handle: dIdx=%d, iIdx=%d, handle=%d\n", eventDefinitionIndex, eventInstanceIndex, handle);*/
    return handle;
}

/** */
void kwlSoundEngine_init(kwlSoundEngine* engine)
{
    /*create message queues*/
    kwlMessageQueue_init(&engine->toMixerQueue);
    kwlMessageQueue_init(&engine->toMixerQueueShared);
    kwlMessageQueue_init(&engine->fromMixerQueue);
    
    /*create the software mixer*/
    engine->mixer = kwlSoftwareMixer_new();
    engine->mixer->engine = engine;
    
    /*init positional audio listener and settings */
    kwlPositionalAudioListener_init(&engine->listener);
    kwlPositionalAudioSettings_init(&engine->positionalAudioSettings);
    
    engine->engineDataIsLoaded = 0;
    engine->playingEventList = NULL;
    engine->numMixBuses = 0;
    engine->mixBuses = NULL;    
    engine->masterBus = NULL;
    
    engine->freeformEventArraySize = 0;
    engine->freeformEvents = NULL;
    
    int KWL_NUM_DECODERS = 10;
    engine->numDecoders = KWL_NUM_DECODERS;
    engine->decoders = (kwlDecoder*)KWL_MALLOC(sizeof(kwlDecoder) * KWL_NUM_DECODERS, "decoders");
    kwlMemset(engine->decoders, 0, sizeof(kwlDecoder) * KWL_NUM_DECODERS);
    
    //set up main mutex lock
    kwlMutexLockInit(&engine->mainMutexLock);
    engine->mixer->mainMutexLock = &engine->mainMutexLock;
}

void kwlSoundEngine_free(kwlSoundEngine* engine)
{
    KWL_ASSERT(engine != NULL);
    KWL_FREE(engine->decoders);
}

kwlError kwlSoundEngine_loadWaveBank(kwlSoundEngine* engine, const char* const waveBankPath, kwlWaveBankHandle* handle)
{
    /*TODO: handle the case of more than one wave bank sharing a piece of audio data.*/
    *handle = KWL_INVALID_HANDLE;
    
    if (!engine->engineDataIsLoaded)
    {
        return KWL_ENGINE_DATA_NOT_LOADED;
    }
    
    /*Open the file...*/
    kwlInputStream stream;
    kwlError result = kwlInputStream_initWithFile(&stream, waveBankPath);
    if (result != KWL_NO_ERROR)
    {
        kwlInputStream_close(&stream);
        return result;
    }
    /*... and check the wave bank file identifier.*/
    int i;
    for (i = 0; i < KWL_WAVE_BANK_BINARY_FILE_IDENTIFIER_LENGTH; i++)
    {
        const char identifierChari = kwlInputStream_readChar(&stream);
        if (identifierChari != KWL_WAVE_BANK_BINARY_FILE_IDENTIFIER[i])
        {
            /* Not the file identifier we expected. */
            kwlInputStream_close(&stream);
            return KWL_UNKNOWN_FILE_FORMAT;
        }
    }
    
    /*Read the ID from the wave bank binary file and find a matching wave bank struct.*/
    const char* waveBankToLoadId = kwlInputStream_readASCIIString(&stream);
    const int waveBankToLoadnumAudioDataEntries = kwlInputStream_readIntBE(&stream);
    const int numWaveBanks = engine->numWaveBanks;
    kwlWaveBank* matchingWaveBank = NULL;
    int matchingWaveBankIndex = -1;
    for (i = 0; i < numWaveBanks; i++)
    {
        if (strcmp(waveBankToLoadId, engine->waveBanks[i].id) == 0)
        {
            matchingWaveBank = &engine->waveBanks[i];
            matchingWaveBankIndex = i;
        }
    }
    
    KWL_FREE((void*)waveBankToLoadId);
    if (matchingWaveBank == NULL)
    {
        /*No matching bank was found. Close the file stream and return an error.*/
        kwlInputStream_close(&stream);
        return KWL_NO_MATCHING_WAVE_BANK;
    }
    else if (waveBankToLoadnumAudioDataEntries != matchingWaveBank->numAudioDataEntries)
    {
        /*A matching wave bank was found but the number of audio data entries
          does not match the binary wave bank data.*/
        kwlInputStream_close(&stream);
        return KWL_WAVE_BANK_ENTRY_MISMATCH;
    }
    else if (matchingWaveBank->isLoaded != 0)
    {
        /*The wave bank is already loaded, just set the handle and do nothing.*/
        *handle = matchingWaveBankIndex;
        kwlInputStream_close(&stream);
        return KWL_NO_ERROR;
    }
    
    /*Store the path the wave bank was loaded from (used when streaming from disk).*/
    int pathLen = strlen(waveBankPath);
    matchingWaveBank->waveBankFilePath = (char*)KWL_MALLOC((pathLen + 1) * sizeof(char), "wave bank path string");
    strcpy(matchingWaveBank->waveBankFilePath, waveBankPath);
    
    /*Make sure that the entries of the wave bank to load and the wave bank struct line up.*/
    for (i = 0; i < waveBankToLoadnumAudioDataEntries; i++)
    {
        const char* filePathi = kwlInputStream_readASCIIString(&stream);
        
        int matchingEntryIndex = -1;
        int j = 0;
        for (j = 0; j < waveBankToLoadnumAudioDataEntries; j++)
        {
            if (strcmp(matchingWaveBank->audioDataItems[j].filePath, filePathi) == 0)
            {
                matchingEntryIndex = j;
                break;
            }
        }
        
        KWL_FREE((void*)filePathi);
        
        if (matchingEntryIndex < 0)
        {
            /* This wave bank entry has no corresponding waveform slot. Abort loading.*/
            kwlInputStream_close(&stream);
            return KWL_WAVE_BANK_ENTRY_MISMATCH;
        }
        
        /*skip to the next wave data entry*/
        /*const int encoding = */kwlInputStream_readIntBE(&stream);
        /*const int streamFromDisk = */kwlInputStream_readIntBE(&stream);
        const int numChannels = kwlInputStream_readIntBE(&stream);
        KWL_ASSERT((numChannels == 0 || numChannels == 1 || numChannels == 2) && "invalid num channels");
        const int numBytes = kwlInputStream_readIntBE(&stream);
        KWL_ASSERT(numBytes > 0);
        kwlInputStream_skip(&stream, numBytes);
    }
    
    /*Move the stream read position to the first audio data entry.*/
    kwlInputStream_reset(&stream);
    kwlInputStream_skip(&stream, KWL_WAVE_BANK_BINARY_FILE_IDENTIFIER_LENGTH);
    int strLen = kwlInputStream_readIntBE(&stream);
    kwlInputStream_skip(&stream, strLen); 
    /*int numEntries = */kwlInputStream_readIntBE(&stream);
    
    /*If we made it this far, the wave bank binary data lines up with a wave
      bank structure of the engine so we're ready to load audio data.*/
    for (i = 0; i < waveBankToLoadnumAudioDataEntries; i++)
    {
        char* const waveEntryIdi = kwlInputStream_readASCIIString(&stream);
        
        kwlAudioData* matchingAudioData = NULL;
        int j;
        for (j = 0; j < waveBankToLoadnumAudioDataEntries; j++)
        {
            kwlAudioData* entryj = &matchingWaveBank->audioDataItems[j];
            if (strcmp(entryj->filePath, waveEntryIdi) == 0)
            {
                matchingAudioData = entryj;
                break;
            }
        }
        
        KWL_FREE(waveEntryIdi);
        
        const kwlAudioEncoding encoding = (kwlAudioEncoding)kwlInputStream_readIntBE(&stream);
        const int streamFromDisk = kwlInputStream_readIntBE(&stream);
        const int numChannels = kwlInputStream_readIntBE(&stream);
        const int numBytes = kwlInputStream_readIntBE(&stream);
        const int numFrames = numBytes / 2 * numChannels;
        KWL_ASSERT(numBytes > 0 && "the number of audio data bytes must be positive");
        KWL_ASSERT(matchingAudioData != NULL && "no matching wave bank entry");
        KWL_ASSERT(numChannels == 0 || numChannels == 1 || numChannels == 2 && "invalid number of channels");

        /*free any old data*/
        kwlAudioData_free(matchingAudioData);
        
        /*Store audio meta data.*/
        matchingAudioData->numFrames = numFrames;
        matchingAudioData->numChannels = numChannels;
        matchingAudioData->numBytes = numBytes;
        matchingAudioData->encoding = (kwlAudioEncoding)encoding;
        matchingAudioData->streamFromDisk = streamFromDisk;
        matchingAudioData->isLoaded = 1;
        matchingAudioData->bytes = NULL;
        
        if (streamFromDisk == 0)
        {
            /*This entry should not be streamed, so allocate audio data up front.*/
            matchingAudioData->bytes = KWL_MALLOC(numBytes, "kwlSoundEngine_loadWaveBank");

            int bytesRead = kwlInputStream_read(&stream, 
                                                (signed char*)matchingAudioData->bytes, 
                                                numBytes);
            KWL_ASSERT(bytesRead == numBytes);
        }
        else
        {
            /*Store the offset into the wave bank binary files for streaming entries.*/
            matchingAudioData->fileOffset = kwlInputStream_tell(&stream);
            kwlInputStream_skip(&stream, numBytes);
        }
    }
    
    kwlInputStream_close(&stream);
    *handle = matchingWaveBankIndex;
    matchingWaveBank->isLoaded = 1;
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_waveBankIsLoaded(kwlSoundEngine* engine, kwlWaveBankHandle handle, int* isLoaded)
{
    if (engine->engineDataIsLoaded == 0)
    {
        return KWL_ENGINE_DATA_NOT_LOADED;
    }
    
    if (handle < 0 || handle >= engine->numWaveBanks || handle == KWL_INVALID_HANDLE)
    {
        return KWL_INVALID_WAVE_BANK_HANDLE;
    }
    
    *isLoaded = engine->waveBanks[handle].isLoaded;
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_waveBankIsReferencedByPlayingEvent(kwlSoundEngine* engine, kwlWaveBankHandle handle, int* isReferenced)
{
    if (engine->engineDataIsLoaded == 0)
    {
        return KWL_ENGINE_DATA_NOT_LOADED;
    }
    
    if (handle < 0 || handle >= engine->numWaveBanks || handle == KWL_INVALID_HANDLE)
    {
        return KWL_INVALID_WAVE_BANK_HANDLE;
    }
    
    kwlWaveBank* waveBank = &engine->waveBanks[handle];
    *isReferenced = 0;
    
    /*early exit if the wave bank is not loaded*/
    if (waveBank->isLoaded == 0)
    {
        return KWL_NO_ERROR;
    }
    
    /*loop over all currently playing events and see if any of them 
     references the wavebank.*/
    kwlEvent* e = engine->playingEventList;
    while (e != NULL)
    {
        if (e->definition_engine->referencedWaveBanks == NULL ||
            e->definition_engine->mixBus == NULL)
        {
            /*dont check freeform event.*/
        }
        else
        {    
            const int numBanks = e->definition_engine->numReferencedWaveBanks;
            kwlWaveBank** banks = e->definition_engine->referencedWaveBanks;
            
            for (int i = 0; i < numBanks; i++)
            {
                if (banks[i] == waveBank)
                {
                    *isReferenced = 1;
                    return KWL_NO_ERROR;
                }
            }
        }
        
        e = e->nextEvent_engine;
    }

    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_requestUnloadWaveBank(kwlSoundEngine* engine, kwlWaveBankHandle handle, int blockUntilUnloaded)
{   
    if (engine->engineDataIsLoaded == 0)
    {
        return KWL_ENGINE_DATA_NOT_LOADED;
    }
    
    if (handle < 0 || handle >= engine->numWaveBanks || handle == KWL_INVALID_HANDLE)
    {
        return KWL_INVALID_WAVE_BANK_HANDLE;
    }
    
    /*Send a message to the mixer to stop all playing events referencing audio data from the wave bank.
      Once the events are stopped, the mixer sends a message back to the engine indicating that it
      is safe to unload the wavebank.*/
    kwlWaveBank* waveBankToUnload = &engine->waveBanks[handle];
    
    if (waveBankToUnload->isLoaded == 0)
    {
        return KWL_NO_ERROR;
    }
    
    int result = kwlMessageQueue_addMessage(&engine->toMixerQueue, KWL_STOP_ALL_EVENTS_REFERENCING_WAVE_BANK, waveBankToUnload);
    if (result == 0)
    {
        return KWL_MESSAGE_QUEUE_FULL;
    }
    
    if (blockUntilUnloaded != 0)
    {
        //TODO: do this in a better way. this will cause a freeze if the audio thread is not running
        /*If a blocking unload was requested, wait for the wavebank to get unloaded before
         returning.*/
        while (waveBankToUnload->isLoaded != 0)
        {
            kwlUpdate(0);
        }
    }
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_unloadWaveBank(kwlSoundEngine* engine, kwlWaveBank* waveBank)
{
    if (waveBank->isLoaded == 0)
    {
        return KWL_NO_ERROR;
    }
    
    /* Free all allocated audio data in the wave bank*/
    const int numAudioDataEntriesInBank = waveBank->numAudioDataEntries;
    int i;
    for (i = 0; i < numAudioDataEntriesInBank; i++)
    {
        kwlAudioData* wavei = &waveBank->audioDataItems[i];
        kwlAudioData_free(wavei);
    }
    waveBank->isLoaded = 0;
    KWL_FREE(waveBank->waveBankFilePath);
    
    return KWL_NO_ERROR;
}


kwlError kwlSoundEngine_eventGetHandle(kwlSoundEngine* engine, const char* const eventID, kwlEventHandle* handle)
{
    *handle = KWL_INVALID_HANDLE;
    
    if (!engine->engineDataIsLoaded)
    {
        return KWL_ENGINE_DATA_NOT_LOADED;
    }
    
    KWL_ASSERT(engine->events != NULL);
    KWL_ASSERT(engine->eventDefinitions != NULL);
    
    const int numEventDefinitions = engine->numEventDefinitions;
    int i;
    for (i = 0; i < numEventDefinitions; i++)
    {
        if (strcmp(eventID, engine->eventDefinitions[i].id) == 0)
        {
            const int numInstances = engine->eventDefinitions[i].instanceCount;
            int j;
            for (j = 0; j < numInstances; j++)
            {
                kwlEvent* const eventj = &engine->events[i][j];
                if (eventj->isAssociatedWithHandle == 0)
                {
                    *handle = computeEventHandle(i, j, 0);
                    eventj->isAssociatedWithHandle = 1;
                    return KWL_NO_ERROR;
                }
            }
            
            return KWL_NO_FREE_EVENT_INSTANCES;
        }
    }
    
    return KWL_UNKNOWN_EVENT_DEFINITION_ID;
}

kwlError kwlSoundEngine_eventDefinitionGetHandle(kwlSoundEngine* engine, 
                                                 const char* const eventDefinitionID, 
                                                 kwlEventDefinitionHandle* handle)
{
    *handle = KWL_INVALID_HANDLE;
    
    if (!engine->engineDataIsLoaded)
    {
        return KWL_ENGINE_DATA_NOT_LOADED;
    }
    
    int i;
    for (i = 0; i < engine->numEventDefinitions; i++)
    {
        if (strcmp(eventDefinitionID, engine->eventDefinitions[i].id) == 0)
        {
            *handle = i;
            return KWL_NO_ERROR;
        }
    }
    
    return KWL_UNKNOWN_EVENT_DEFINITION_ID;
}

kwlError kwlSoundEngine_eventCreateWithAudioData(kwlSoundEngine* engine, kwlAudioData* audioData, 
                                                 kwlEventHandle* handle, kwlEventType type,
                                                 const char* const eventId)
{
    /*create the event. as opposed to a data driven event, a freeform event does
     not reference sounds and event definitions in the engine, but own its local data
     that is freed when the event is released.*/
    kwlEvent* createdEvent = (kwlEvent*)KWL_MALLOC(sizeof(kwlEvent), "freeform event instance");
    kwlEvent_init(createdEvent);
    
    kwlSound* sound = NULL;
    kwlAudioData* streamAudioData = NULL;
    
    /*create a sound if we loaded a PCM file.*/
    if (audioData->encoding == KWL_ENCODING_SIGNED_16BIT_PCM)
    {
        sound = (kwlSound*)KWL_MALLOC(sizeof(kwlSound), "freeform event: sound");
        kwlSound_init(sound);
        sound->audioDataEntries = (kwlAudioData**)KWL_MALLOC(sizeof(kwlAudioData*), 
                                                  "freeform event: sound audio data array list");
        sound->audioDataEntries[0] = audioData;
        sound->numAudioDataEntries = 1;
        sound->playbackMode = KWL_SEQUENTIAL;
        sound->playbackCount = 1;
        sound->deferStop = 0;
        sound->gain = 1.0f;
        sound->pitch = 1.0f;
        sound->pitchVariation = 0.0f;
        sound->gainVariation = 0.0f;
    }
    else
    {
        KWL_ASSERT(0 && "TODO: support creating non-pcm events");
    }
    
    /*create an event definition*/
    kwlEventDefinition* eventDefinition = 
    (kwlEventDefinition*)KWL_MALLOC(sizeof(kwlEventDefinition), 
                                    "freeform event definition");
    kwlEventDefinition_init(eventDefinition);
    
    eventDefinition->id = eventId;
    eventDefinition->instanceCount = 1;
    eventDefinition->isPositional = type == KWL_POSITIONAL ? 1 : 0;
    eventDefinition->gain = 1.0f;
    eventDefinition->pitch = 1.0f;
    eventDefinition->innerConeCosAngle = 1.0f;
    eventDefinition->outerConeCosAngle = -1.0f;
    eventDefinition->outerConeGain = 1.0f;
    eventDefinition->retriggerMode = KWL_RETRIGGER;
    eventDefinition->stealingMode = KWL_DONT_STEAL;
    eventDefinition->streamAudioData = streamAudioData;
    eventDefinition->sound = sound;
    eventDefinition->numReferencedWaveBanks = 0;
    eventDefinition->referencedWaveBanks = NULL;
    /*Set the mix bus to NULL. This is how the mixer knows this is a freeform event.
     TODO: solve this in some better way?*/
    eventDefinition->mixBus = NULL;
    
    createdEvent->definition_mixer = eventDefinition;
    createdEvent->definition_engine = eventDefinition;
    
    /*find a free slot in the freeform event array. reallocate the array if needed */
    int slotIdx = -1;
    int i;
    for (i = 0; i < engine->freeformEventArraySize; i++)
    {
        if (engine->freeformEvents[i] == NULL)
        {
            slotIdx = i;
            break;
        }
    }
    
    if (slotIdx < 0)
    {
        /*no free slots, reallocate the array 
         and pick the newly allocated slot*/
        engine->freeformEventArraySize++;
        engine->freeformEvents = (kwlEvent**)realloc(engine->freeformEvents, 
                                                     engine->freeformEventArraySize * sizeof(kwlEvent*));
        engine->freeformEvents[engine->freeformEventArraySize - 1] = NULL;
        
        slotIdx = engine->freeformEventArraySize - 1;
    }
    
    *handle = computeEventHandle(slotIdx, 0, 1);
    //printf("slot idx %d, handle %d, event %d\n", slotIdx, *handle, createdEvent);
    
    engine->freeformEvents[slotIdx] = createdEvent;
    return KWL_NO_ERROR;
    
}

kwlError kwlSoundEngine_eventCreateWithBuffer(kwlSoundEngine* engine, kwlPCMBuffer* buffer, 
                                              kwlEventHandle* handle, kwlEventType type)
{
    if (buffer->numFrames < 1 ||
        buffer->numChannels < 1 || 
        buffer->numChannels > 2 ||
        buffer->pcmData == NULL)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    kwlAudioData* audioData = (kwlAudioData*)KWL_MALLOC(sizeof(kwlAudioData), 
                                                        "freeform event audio data struct");
    kwlMemset(audioData, 0, sizeof(kwlAudioData));
    
    audioData->numChannels = buffer->numChannels;
    audioData->numFrames = buffer->numFrames;
    audioData->numBytes = buffer->numFrames * buffer->numChannels * 2;/*2 bytes per 16 bit sample*/
    audioData->bytes = buffer->pcmData;
    audioData->encoding = KWL_ENCODING_SIGNED_16BIT_PCM;
    
    return kwlSoundEngine_eventCreateWithAudioData(engine, audioData, handle, type, "freeform buffer event");
}

kwlError kwlSoundEngine_eventCreateWithFile(kwlSoundEngine* engine, const char* const audioFilePath, 
                                            kwlEventHandle* handle, kwlEventType type, int streamFromDisk)
{
    *handle = KWL_INVALID_HANDLE;
    
    KWL_ASSERT(streamFromDisk == 0 && "stream flag not supported yet");
    
    /*try to load the audio file data*/
    kwlAudioData* audioData = (kwlAudioData*)KWL_MALLOC(sizeof(kwlAudioData), 
                                                        "freeform event audio data struct");
    kwlMemset(audioData, 0, sizeof(kwlAudioData));
    
    kwlError error = kwlLoadAudioFile(audioFilePath, audioData, KWL_CONVERT_TO_INT16_OR_FAIL);
    if (error != KWL_NO_ERROR)
    {
        KWL_FREE(audioData);
        return error;
    }
    
    if (type == KWL_POSITIONAL &&
        audioData->numChannels != 1)
    {
        kwlAudioData_free(audioData);
        KWL_FREE(audioData);
        return KWL_POSITIONAL_EVENT_MUST_BE_MONO;
    }
        
    
    return kwlSoundEngine_eventCreateWithAudioData(engine, audioData, handle, type, (char*)audioFilePath);
}

kwlError kwlSoundEngine_unloadFreeformEvent(kwlSoundEngine* engine, kwlEvent* event)
{
    /*printf("kwlSoundEngine_unloadFreeformEvent: %s\n", event->definition_engine->id);*/
    KWL_ASSERT(event != NULL);
    KWL_ASSERT(event->isPlaying == 0);
    
    int matchFound = 0;
    int eventIndex;
    for (eventIndex = 0; eventIndex < engine->freeformEventArraySize; eventIndex++)
    {
        if (engine->freeformEvents[eventIndex] == event)
        {
            matchFound = 1;
            break;
        }
    }
    
    KWL_ASSERT(matchFound != 0 && "trying to free a freeform event that is not in the engine's list");
    
    /*Free all data associate with the freeform event.*/
    kwlEventDefinition* eventDefinition = event->definition_engine;
    if (eventDefinition->streamAudioData != NULL)
    {
        KWL_ASSERT(0); /* double check this*/
        kwlAudioData_free(eventDefinition->streamAudioData);
        KWL_FREE(eventDefinition->streamAudioData);
    }
    else if (eventDefinition->sound != NULL)
    {
        /*TODO: this check could be more robust. it will cause a memory
                leak for freeform events created from files with the name
                "freeform buffer event"*/
        if (strcmp(eventDefinition->id, "freeform buffer event") == 0)
        {
            /*don't release audio data buffer for freeform buffer events.*/
            eventDefinition->sound->audioDataEntries[0]->bytes = NULL;
        }
        /* Free loaded audio data */
        kwlAudioData_free(eventDefinition->sound->audioDataEntries[0]);
        /* Free allocated audio data and sound structs */
        KWL_FREE(eventDefinition->sound->audioDataEntries[0]);
        KWL_FREE(eventDefinition->sound->audioDataEntries);
        KWL_FREE(eventDefinition->sound);
    }
    
    /* Finally, free the event instance and the event definition. */
    KWL_FREE(eventDefinition);
    KWL_FREE(event);
    
    /*Clear the event slot so it can be reused. */
    engine->freeformEvents[eventIndex] = NULL;
    
    return KWL_NO_ERROR;
}


kwlError kwlSoundEngine_eventRelease(kwlSoundEngine* engine, kwlEventHandle handle)
{
    kwlEvent* eventToRelease = kwlSoundEngine_getEventFromHandle(engine, handle);
    if (eventToRelease == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    /*If this is a freeform event, dispose of any data allocated for it.*/
    if (kwlSoundEngine_isFreeformEventHandle(engine, handle))
    {   
        if (eventToRelease->isPlaying == 0)
        {
            kwlSoundEngine_unloadFreeformEvent(engine, eventToRelease);
        }
        else
        {
            /*Send a message to the mixer instructing it to stop the event we want to unload.
              Once the event is stopped, a KWL_UNLOAD_FREEFORM_EVENT message triggering the actual
              unloading will be send back to the engine.*/
            int result = kwlMessageQueue_addMessage(&engine->toMixerQueue, 
                                                    KWL_FREEFORM_EVENT_STOP, 
                                                    eventToRelease);
            if (result == 0)
            {
                KWL_ASSERT(0);
                return KWL_MESSAGE_QUEUE_FULL;
            }
        }
    }
    /*For data driven events, just mark the event as not associated with a handle.*/
    else
    {
        /*TODO: reset other stuff here?*/
        eventToRelease->userGain = 1.0f;
        eventToRelease->userPitch = 1.0f;
        eventToRelease->dspUnit.valueEngine = NULL;
        eventToRelease->isAssociatedWithHandle = 0;
    }
    
    /*Clear callbacks*/
    eventToRelease->stoppedCallback = NULL;
    eventToRelease->stoppedCallbackUserData = NULL;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_mixBusGetHandle(kwlSoundEngine* engine, const char* const busId, kwlMixBusHandle* handle)
{
    *handle = KWL_INVALID_HANDLE;
    if (!engine->engineDataIsLoaded)
    {
        return KWL_ENGINE_DATA_NOT_LOADED;
    }
    
    KWL_ASSERT(engine->mixBuses != NULL);
    
    const int numMixBuses = engine->numMixBuses;
    
    int i;
    for (i = 0; i < numMixBuses; i++)
    {
        if (strcmp(busId, engine->mixBuses[i].id) == 0)
        {
            /*the mix bus handle is just the index into the mix bus array*/
            *handle = i;
            return KWL_NO_ERROR;
        }
    }
    
    return KWL_UNKNOWN_MIX_BUS_ID;
}

kwlMixBus* kwlSoundEngine_getMixBusFromHandle(kwlSoundEngine* engine, kwlMixBusHandle handle)
{
    if (handle < 0 || handle >= engine->numMixBuses || handle == KWL_INVALID_HANDLE) 
    {
        return NULL;
    }
    
    return &engine->mixBuses[handle];
}

kwlError kwlSoundEngine_mixBusSetGain(kwlSoundEngine* engine, kwlMixBusHandle handle, float gain, int isLinearGain)
{
    /*TODO: thread safe access to numMixBuses??*/
    /*TODO: return no engine data error?*/
    kwlMixBus* const mixBus = kwlSoundEngine_getMixBusFromHandle(engine, handle);
    if (mixBus == NULL)
    {
        return KWL_INVALID_MIX_BUS_HANDLE;
    }
    if (gain < 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    /*TODO: support left and right gain separately?*/
    float linGain = isLinearGain == 1 ? gain : logGainToLinGain(gain);
    mixBus->userGainLeft = linGain;
    mixBus->userGainRight = linGain;
        
    return KWL_NO_ERROR;
}


kwlError kwlSoundEngine_mixBusSetPitch(kwlSoundEngine* engine, kwlMixBusHandle handle, float pitch)
{
    /*TODO: thread safe access to numMixBuses??*/
    /*TODO: return no engine data error?*/
    kwlMixBus* const mixBus = kwlSoundEngine_getMixBusFromHandle(engine, handle);
    if (mixBus == NULL)
    {
        return KWL_INVALID_MIX_BUS_HANDLE;
    }
    if (pitch < 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    mixBus->userPitch = pitch;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_mixPresetGetHandle(kwlSoundEngine* engine, const char* const presetId, kwlMixBusHandle* handle)
{
    int i;
    for (i = 0; i < engine->numMixPresets; i++)
    {
        if (strcmp(presetId, engine->mixPresets[i].id) == 0)
        {
            *handle = i;
            return KWL_NO_ERROR;
        }
    }
    
    *handle = -1;
    return KWL_UNKNOWN_MIX_PRESET_ID;
}

kwlError kwlSoundEngine_mixPresetSetActive(kwlSoundEngine* engine, kwlMixPresetHandle handle, int doFade)
{
    if (handle < 0 || handle >= engine->numMixPresets || handle == KWL_INVALID_HANDLE)
    {
        return KWL_INVALID_MIX_PRESET_HANDLE;
    }
    
    int i;
    for (i = 0; i < engine->numMixPresets; i++)
    {
        float w = (i == handle ? 1.0f : 0.0f);
        engine->mixPresets[i].targetWeight = w;
        
        /*if we're not doing a fade, just set the weight straight away*/
        if (doFade == 0)
        {
            engine->mixPresets[i].weight = w;
        }
    }
    
    return KWL_NO_ERROR;
}


kwlError kwlSoundEngine_setListenerPosition(kwlSoundEngine* engine, float posX, float posY, float posZ)
{
    engine->listener.positionX = posX;
    engine->listener.positionY = posY;
    engine->listener.positionZ = posZ;
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_setListenerVelocity(kwlSoundEngine* engine, float velX, float velY, float velZ)
{
    engine->listener.velocityX = velX;
    engine->listener.velocityY = velY;
    engine->listener.velocityZ = velZ;
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_setListenerOrientation(kwlSoundEngine* engine, float dirX, float dirY, float dirZ,
                                               float upX, float upY, float upZ)
{
    if ((dirX == 0.0f && dirY == 0.0f && dirZ == 0.0f) ||
        (upX == 0.0f && upY == 0.0f && upZ == 0.0f))
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    float upLengthInv = kwlFastInverseSqrt(upX * upX + upY * upY + upZ * upZ);
    engine->listener.upX = upX * upLengthInv;
    engine->listener.upY = upY * upLengthInv;
    engine->listener.upZ = upZ * upLengthInv;
    
    float dirLengthInv = kwlFastInverseSqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
    engine->listener.directionX = dirX * dirLengthInv;
    engine->listener.directionY = dirY * dirLengthInv;
    engine->listener.directionZ = dirZ * dirLengthInv;
    
    engine->listener.rightX = engine->listener.directionY * engine->listener.upZ -
                              engine->listener.directionZ * engine->listener.upY;
    engine->listener.rightY = engine->listener.directionZ * engine->listener.upX -
                              engine->listener.directionX * engine->listener.upZ;
    engine->listener.rightZ = engine->listener.directionX * engine->listener.upY -
                              engine->listener.directionY * engine->listener.upX;
    
    return KWL_NO_ERROR;    
}

kwlError kwlSoundEngine_setDistanceAttenuationModel(kwlSoundEngine* engine,
                                                    kwlDistanceAttenuationModel type, 
                                                    int clamp,
                                                    float maxDistance,
                                                    float rolloffFactor,
                                                    float referenceDistance)
{
    if (rolloffFactor < 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    if (referenceDistance <= 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    if (referenceDistance >= maxDistance && maxDistance > 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    engine->positionalAudioSettings.clamp = clamp;
    engine->positionalAudioSettings.referenceDistance = referenceDistance;
    engine->positionalAudioSettings.rolloffFactor = rolloffFactor;
    engine->positionalAudioSettings.referenceDistance = referenceDistance;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_setDopplerShiftParameters(kwlSoundEngine* engine, float speedOfSound, float dopplerScale)
{
    if (speedOfSound <= 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    if (dopplerScale > 1.0f || dopplerScale < 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    engine->positionalAudioSettings.speedOfSound = speedOfSound;
    engine->positionalAudioSettings.dopplerScale = dopplerScale;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_setConeAttenuationEnabled(kwlSoundEngine* engine, int listenerCone, int eventCones)
{
    engine->positionalAudioSettings.isEventConeAttenuationEnabled = eventCones;
    engine->positionalAudioSettings.isListenerConeAttenuationEnabled = listenerCone;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_setListenerConeParameters(kwlSoundEngine* engine, 
                                                  float innerAngle, 
                                                  float outerAngle, 
                                                  float outerGain)
{
    if (innerAngle < 0 || innerAngle > 360)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    if (outerAngle < 0 || outerAngle > 360)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    if (outerAngle < innerAngle)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    if (outerGain < 0)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    const float degToRad = 0.017453292f;
    float cosInner = cosf(degToRad * innerAngle / 2.0f);
    float cosOuter = cosf(degToRad * outerAngle / 2.0f);
    
    engine->listener.outerConeGain = outerGain;
    engine->listener.innerConeCosAngle = cosInner;
    engine->listener.outerConeCosAngle = cosOuter;
    
    return KWL_NO_ERROR;
}

float kwlSoundEngine_getDistanceGain(kwlSoundEngine* engine, float distanceInv)
{
    KWL_ASSERT(distanceInv >= 0.0f);
    kwlDistanceAttenuationModel model = engine->positionalAudioSettings.distanceModel;
    int clamp = engine->positionalAudioSettings.clamp;
    float distance = distanceInv = 1 / distanceInv; /*TODO: user distanceInv directly?*/
    float refDist = engine->positionalAudioSettings.referenceDistance;
    float rolloff = engine->positionalAudioSettings.rolloffFactor;
    float maxDist = engine->positionalAudioSettings.maxDistance;

    if (maxDist > 0.0f && distance > maxDist)
    {
        /*The event is too far away.*/
        return 0.0f;
    }
    
    switch (model)
    {
        case KWL_CONSTANT:
            return 1.0f;
        case KWL_INV_DISTANCE:
        {
            float gain = refDist / (refDist + rolloff * (distance - refDist));
            if (gain > 1.0f && clamp != 0)
            {
                gain = 1.0f;
            }
            return gain;
        }
        case KWL_LINEAR:
        {
            float gain = (1 - rolloff * (distance - refDist) / (maxDist - refDist));
            if (gain < 0.0f)
            {
                gain = 0.0f;
            }
            else if (gain > 1.0f && clamp != 0)
            {
                gain = 1.0f;
            }
            return gain;
        }
    }
    
    KWL_ASSERT(0 && "unknown distance attenuation model");
    return 1.0f;
}

void kwlSoundEngine_updateMixPresets(kwlSoundEngine* engine, float timeStepSec)
{
    /*TODO: read from project data?*/
    engine->mixPresetFadeTime = 1.0f;
    
    /*update mix preset weights towards the target values*/
    float dWeight = engine->mixPresetFadeTime > 0.0f ? timeStepSec / engine->mixPresetFadeTime : 1.0f;
    int i;
    for (i = 0; i < engine->numMixPresets; i++)
    {
        kwlMixPreset* preseti = &engine->mixPresets[i];
        //printf("updating %s to weight ", preseti->id);
        
        float delta = preseti->weight < preseti->targetWeight ? dWeight : - dWeight;
        if (preseti->weight != preseti->targetWeight)
        {
            preseti->weight += delta;
            if (preseti->weight < 0)
            {
                preseti->weight = 0;
            }
            else if (preseti->weight > 1)
            {
                preseti->weight = 1;
            }
        }
        //printf("%f by %f\n", preseti->weight, delta);
    }
    
    if (0)
    {
        for (i = 0; i < engine->numMixPresets; i++)
        {
            kwlMixPreset* preseti = &engine->mixPresets[i];
            
            printf("%s weight %f        ", preseti->id, preseti->weight);
        }
        printf("\n");
    }
    
    /* Blend mix preset parameter sets.  
       First reset all mix bus parameters...*/
    int mixBusIndex;
    for (mixBusIndex = 0; mixBusIndex < engine->numMixBuses; mixBusIndex++)
    {
        engine->mixBuses[mixBusIndex].mixPresetGainLeft = 0.0f;
        engine->mixBuses[mixBusIndex].mixPresetGainRight = 0.0f;
        engine->mixBuses[mixBusIndex].mixPresetPitch = 0.0f;
    }
    
    /*...then accumulate the values from each mix preset.*/
    float debugWeightSum[7] = {0, 0, 0, 0, 0, 0, 0};
    int j;
    for (j = 0; j < engine->numMixPresets; j++)
    {
        float presetweight = engine->mixPresets[j].weight;
        for (int paramSetIndex = 0; paramSetIndex < engine->numMixBuses; paramSetIndex++)
        {
            kwlMixBusParameters* params = &engine->mixPresets[j].parameterSets[paramSetIndex];
            int busIndex = params->mixBusIndex;
            KWL_ASSERT(busIndex < engine->numMixBuses && busIndex >= 0);
            engine->mixBuses[busIndex].mixPresetGainLeft += params->logGainLeft * presetweight;
            engine->mixBuses[busIndex].mixPresetGainRight += params->logGainRight * presetweight;
            engine->mixBuses[busIndex].mixPresetPitch += params->pitch * presetweight;
            debugWeightSum[busIndex] += presetweight;
        }
    }
    /*
    printf("debug weight sums: ");
    for (int b = 0; b < 7; b++)sfsdf
    {
        df
        printf("%f, ", debugWeightSum[b]);
    }
    printf("\n");*/
    
    /*Finally, convert from adjusted gain to linear gain.*/
    for (mixBusIndex = 0; mixBusIndex < engine->numMixBuses; mixBusIndex++)
    {
        engine->mixBuses[mixBusIndex].mixPresetGainLeft = 
            logGainToLinGain(engine->mixBuses[mixBusIndex].mixPresetGainLeft);
        engine->mixBuses[mixBusIndex].mixPresetGainRight = 
            logGainToLinGain(engine->mixBuses[mixBusIndex].mixPresetGainRight);
        
        /*printf("setting gain of bus %s to %f\n", 
               engine->mixBuses[mixBusIndex].id,
               engine->mixBuses[mixBusIndex].mixPresetGainLeft);*/
    }
}

float kwlSoundEngine_getConeGain(kwlSoundEngine* engine, float cosAngle, float cosInner, float cosOuter, float outerGain)
{
    float coneGain = 1.0f;
    if (cosAngle < cosOuter)
    {
        coneGain = outerGain; 
    }
    else if (cosAngle >= cosOuter &&
             cosAngle < cosInner)   
    {
        const float delta = cosInner - cosOuter;
        float param = 1.0f;
        
        if (delta != 0)
        {
            param = (cosAngle - cosOuter) / delta;
        }
        coneGain = outerGain + param * (1 - outerGain);
    }
    
    return coneGain;
}

void kwlSoundEngine_updateEvents(kwlSoundEngine* engine)
{
    /*cache some listener variables*/
    const float posXListener = engine->listener.positionX;
    const float posYListener = engine->listener.positionY;
    const float posZListener = engine->listener.positionZ;
    
    const float dirXListener = engine->listener.directionX;
    const float dirYListener = engine->listener.directionY;
    const float dirZListener = engine->listener.directionZ;
    
    const float rightXListener = engine->listener.rightX;
    const float rightYListener = engine->listener.rightY;
    const float rightZListener = engine->listener.rightZ;
    
    const float velXListener = engine->listener.velocityX;
    const float velYListener = engine->listener.velocityY;
    const float velZListener = engine->listener.velocityZ;
    
    const float cosInnerListener = engine->listener.innerConeCosAngle;
    const float cosOuterListener = engine->listener.outerConeCosAngle;
    const float outerGainListener = engine->listener.outerConeGain;
    
    const float speedOfSound = engine->positionalAudioSettings.speedOfSound;
    const float dopplerScale = engine->positionalAudioSettings.dopplerScale;
    
    const int eventConesEnabled = engine->positionalAudioSettings.isEventConeAttenuationEnabled;
    const int isDirectionalListener = engine->positionalAudioSettings.isListenerConeAttenuationEnabled &&
                                      engine->listener.outerConeGain != 1.0f; 
    
    /*recalculate positional gain and pitch of currently playing events*/
    kwlEvent* eventList = engine->playingEventList;
    while (eventList != NULL)
    {   
        kwlEventDefinition* definition = eventList->definition_engine;
        if (definition->isPositional)
        {
            /*compute a a normalized vector from the listener to the event*/
            float dx = posXListener - eventList->positionX;
            float dy = posYListener - eventList->positionY;
            float dz = posZListener - eventList->positionZ;
            const float distInv = kwlFastInverseSqrt(dx * dx + dy * dy + dz * dz);
            dx *= distInv;
            dy *= distInv;
            dz *= distInv;
            
            const float distanceAttenuation = kwlSoundEngine_getDistanceGain(engine, distInv);
            
            /*pan. TODO: equal enery pan?*/
            float dot = -dx * rightXListener +
                        -dy * rightYListener +
                        -dz * rightZListener;
            float panLeft = 0.2f + (-dot > 0 ? -dot : 0);
            float panRight = 0.2f + (-dot < 0 ? dot : 0);
            
            /*cone attenuation*/
            int isDirectionalEvent = definition->outerConeGain != 1.0f;
            float coneGain = 1.0f;
            if (isDirectionalEvent && eventConesEnabled)
            {
                const float cosInner = definition->innerConeCosAngle;
                const float cosOuter = definition->outerConeCosAngle;
                const float outerGain = definition->outerConeGain;
                
                /*There are three ange intervals to consider:
                 - 0-inner cone angle: apply unit gain.
                 - inner cone angle - outer cone angle: 
                   interpolate between unit gain and outer cone gain
                 - outer cone angle - 180: apply outer cone gain
                 */
                float dotProd = eventList->directionX * dx +
                                eventList->directionY * dy +
                                eventList->directionZ * dz;
                
                coneGain = kwlSoundEngine_getConeGain(engine, dotProd, cosInner, cosOuter, outerGain);
            }
            
            if (isDirectionalListener)
            {
                float dotProd = -dirXListener * dx +
                                -dirYListener * dy +
                                -dirZListener * dz;
                
                float listenerConeGain = 
                                kwlSoundEngine_getConeGain(engine, 
                                                           dotProd, 
                                                           cosInnerListener, 
                                                           cosOuterListener, 
                                                           outerGainListener);
                coneGain *= listenerConeGain;
            }
            
            /*doppler shift:
             project velocities onto the unit vector 
             pointing from the listener to the event*/
            float vListener = velXListener * dx +    
                              velYListener * dy + 
                              velZListener * dz;
            float vEvent = eventList->velocityX * dx +    
            eventList->velocityY * dy + 
            eventList->velocityZ * dz;
            
            float dopplerShift = (1 - dopplerScale) + dopplerScale * (speedOfSound - vListener) / (speedOfSound - vEvent);
            if (dopplerShift < 0)
            {
                dopplerShift = 0.0001f;/*TODO: handle this properly*/
            }
            
            float positionalGainLeft = coneGain * distanceAttenuation * panLeft;
            float positionalGainRight = coneGain * distanceAttenuation * panRight;

            eventList->gainLeft.valueEngine = 
                eventList->definition_engine->gain * eventList->userGain * positionalGainLeft;
            eventList->gainRight.valueEngine = 
                eventList->definition_engine->gain * eventList->userGain * positionalGainRight;
            eventList->pitch.valueEngine = 
                eventList->definition_engine->pitch * eventList->userPitch * dopplerShift;
        }
        else 
        {
            float balanceGainLeft = 1 - eventList->balance;
            float balanceGainRight = 1 + eventList->balance;
            
            eventList->gainLeft.valueEngine = 
                eventList->definition_engine->gain * eventList->userGain * balanceGainLeft;
            eventList->gainRight.valueEngine = 
                eventList->definition_engine->gain * eventList->userGain * balanceGainRight;
            eventList->pitch.valueEngine = 
                eventList->definition_engine->pitch * eventList->userPitch;
        }
        
        if (eventList->dspUnit.valueMixer != NULL)
        {
            kwlDSPUnit* dspUnit = (kwlDSPUnit*)eventList->dspUnit.valueMixer;
            dspUnit->updateDSPEngineCallback(dspUnit->data);
        }
        
        eventList = eventList->nextEvent_engine;
    }
}


kwlError kwlSoundEngine_update(kwlSoundEngine* engine, float timeStepSec)
{
    kwlSoundEngine_updateEvents(engine);        
    kwlSoundEngine_updateMixPresets(engine, timeStepSec);
        
    kwlDSPUnit* inputDspUnit = (kwlDSPUnit*)engine->mixer->inputDSPUnit.valueEngine;
    if (inputDspUnit != NULL)
    {
        inputDspUnit->updateDSPEngineCallback(inputDspUnit->data);
    }
    
    kwlDSPUnit* outputDspUnit = (kwlDSPUnit*)engine->mixer->outputDSPUnit.valueEngine;
    if (outputDspUnit != NULL)
    {
        outputDspUnit->updateDSPEngineCallback(outputDspUnit->data);
    }
    
    /*************************************************************************
      The following section of code manipulates variables that are accessed from 
      both the engine thread and the mixer thread, so a lock is required to 
      protect them. A minimum amount of work should be done in this section.
     **************************************************************************/
    kwlMutexLockAcquire(&engine->mainMutexLock);
    
    /*Flush any locally buffered messages to the outgoing queue that is shared
      between the mixer thread and the engine thread.*/
    kwlMessageQueue_flushTo(&engine->toMixerQueue, &engine->toMixerQueueShared);
    
    /*copy any pending messages from the mixer*/
    kwlMessageQueue_flushTo(&engine->mixer->toEngineQueueShared, &engine->fromMixerQueue);
    
    /*update the mixer parameters of currently playing events */
    kwlEvent* eventList = engine->playingEventList;
    while (eventList != NULL)
    {
        eventList->gainLeft.valueShared = eventList->gainLeft.valueEngine;
        eventList->gainRight.valueShared = eventList->gainRight.valueEngine;
        eventList->pitch.valueShared = eventList->pitch.valueEngine;
        eventList->dspUnit.valueShared = eventList->dspUnit.valueEngine;
        
        eventList = eventList->nextEvent_engine;
    }
    
    const int numMixBuses = engine->numMixBuses;
    int i;
    for (i = 0; i < numMixBuses; i++)
    {
        kwlMixBus* busi = &engine->mixBuses[i];
        busi->totalGainLeft.valueShared = busi->mixPresetGainLeft * busi->userGainLeft;
        busi->totalGainRight.valueShared = busi->mixPresetGainRight * busi->userGainRight;
        busi->totalPitch.valueShared = busi->mixPresetPitch * busi->userPitch;
        busi->dspUnit.valueShared = busi->dspUnit.valueEngine;
    }
    
    engine->mixer->inputDSPUnit.valueShared = 
        engine->mixer->inputDSPUnit.valueEngine; 
    
    engine->mixer->outputDSPUnit.valueShared = 
        engine->mixer->outputDSPUnit.valueEngine; 
    
    engine->mixer->isLevelMeteringEnabled.valueShared = 
        engine->mixer->isLevelMeteringEnabled.valueEngine;
    
    engine->mixer->latestBufferAbsPeakLeft.valueEngine = 
        engine->mixer->latestBufferAbsPeakLeft.valueShared;
    engine->mixer->latestBufferAbsPeakRight.valueEngine = 
        engine->mixer->latestBufferAbsPeakRight.valueShared;
    engine->mixer->clipFlag.valueEngine = 
        engine->mixer->clipFlag.valueShared;
    engine->mixer->isPaused.valueShared = engine->mixer->isPaused.valueEngine;
    
    engine->mixer->numFramesMixed.valueEngine = engine->mixer->numFramesMixed.valueShared;
    
    /**************************************************************************
     done manipulating shared data. release the lock
     **************************************************************************/
    kwlMutexLockRelease(&engine->mainMutexLock);
    
    /*process messages from the mixer*/
    int numMessages = engine->fromMixerQueue.numMessages;
    
    int unloadEngineDataRequested = 0;
    for (i = 0; i < numMessages; i++)
    {
        kwlMessage* message = &engine->fromMixerQueue.messages[i];
        kwlMessageType type = message->type;
        void* messageData = message->data;
        /*printf("engine: processing incoming message %d/%d of type %d\n", i + 1, numMessages, type);*/
        
        if (type == KWL_EVENT_STOPPED ||
            type == KWL_UNLOAD_FREEFORM_EVENT)
        {
            kwlEvent* event = (kwlEvent*)messageData;
            event->isPlaying = 0;
            if (event->decoder != NULL)
            {
                kwlDecoder_deinit(event->decoder);
            }
            /*printf("    %s: %s\n", type == KWL_EVENT_STOPPED ? "event stopped" : "unload freeform event", 
                   event->definition_engine->id);*/
            kwlSoundEngine_removeEventFromPlayingList(engine, event);
            
            if (type == KWL_UNLOAD_FREEFORM_EVENT)
            {
                kwlSoundEngine_unloadFreeformEvent(engine, event);
            }
            
            if (event->stoppedCallback != NULL)
            {
                event->stoppedCallback(event->stoppedCallbackUserData);
            }
        }
        else if (type == KWL_UNLOAD_WAVEBANK)
        {
            kwlWaveBank* waveBank = (kwlWaveBank*)messageData;
            //printf("    unload wave bank: %s\n", waveBank->id);
            kwlSoundEngine_unloadWaveBank(engine, waveBank);
        }
        else if (type == KWL_UNLOAD_ENGINE_DATA)
        {
            /*Unload engine data after all messages have been processed.*/
            //printf("received KWL_UNLOAD_ENGINE_DATA\n");
            unloadEngineDataRequested = 1;
        }
    }
    
    if (unloadEngineDataRequested != 0)
    {
        kwlSoundEngine_engineDataUnload(engine);
    }
    
    engine->fromMixerQueue.numMessages = 0;

    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_resume(kwlSoundEngine* engine)
{
    engine->mixer->isPaused.valueEngine = 0;
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_pause(kwlSoundEngine* engine)
{
    engine->mixer->isPaused.valueEngine = 1;
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_startEventInstance(kwlSoundEngine* engine, 
                                           kwlEvent* eventToPlay, 
                                           float fadeInTimeSec)
{
    /* If the event is not playing. */
    if (eventToPlay->isPlaying == 0)
    {
        /* If this is a streaming event...*/
        if (eventToPlay->definition_engine->streamAudioData != NULL)
        {
            if (eventToPlay->definition_engine->streamAudioData->isLoaded == 0)
            {
                return KWL_NO_ERROR; /*TODO: return some other error here?*/
            }
            /*...find a decoder and initialise it*/
            int freeDecoderIdx = -1;
            int i;
            for (i = 0; i < engine->numDecoders; i++)
            {
                if (engine->decoders[i].codecData == NULL)
                {
                    freeDecoderIdx = i;
                    break;
                }
            }
            if (freeDecoderIdx < 0)
            {
                return KWL_NO_FREE_DECODERS;
            }
            eventToPlay->decoder = &engine->decoders[freeDecoderIdx];
            kwlError initResult = kwlDecoder_init(eventToPlay->decoder, 
                                                  eventToPlay);

            if (initResult != KWL_NO_ERROR)
            {
                return initResult;
            }
        }
            
        /*mark the event as playing and send a start message to the mixer.*/
        eventToPlay->isPlaying = 1;
        kwlSoundEngine_addEventToPlayingList(engine, eventToPlay);
        int result = kwlMessageQueue_addMessageWithParam(&engine->toMixerQueue, 
                                                         KWL_EVENT_START, 
                                                         eventToPlay, 
                                                         fadeInTimeSec);
        
        if (result == 0)
        {
            return KWL_MESSAGE_QUEUE_FULL;
        }
    }
    /* If the event is playing and is not a streaming event, retrigger it. */
    else if (eventToPlay->definition_engine->streamAudioData == NULL)
    {
        /*the event is already flagged as playing. this means that it is either:
         1. playing in the mixer (the most likely case)
         2. not playing in the mixer but the stopped notification has not yet reached the engine. */
        
        /*mark the event as playing and send a retrigger message to the mixer.*/
        eventToPlay->isPlaying = 1;
        //kwlSoundEngine_addEventToPlayingList(engine, eventToPlay);
        int result = kwlMessageQueue_addMessageWithParam(&engine->toMixerQueue, 
                                                         KWL_EVENT_RETRIGGER, 
                                                         eventToPlay, 
                                                         fadeInTimeSec);
        
        if (result == 0)
        {
            return KWL_MESSAGE_QUEUE_FULL;
        }
    }
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_eventStart(kwlSoundEngine* engine, const int handle, float fadeInTimeSec)
{
    kwlEvent* eventToPlay = kwlSoundEngine_getEventFromHandle(engine, handle);
    
    if (eventToPlay == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    if (fadeInTimeSec < 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    return kwlSoundEngine_startEventInstance(engine, eventToPlay, fadeInTimeSec);
    
}

kwlError kwlSoundEngine_eventStartOneShot(kwlSoundEngine* engine, 
                                          kwlEventDefinitionHandle handle, 
                                          float x, float y, float z, 
                                          int startAtPosition,
                                          kwlEventStoppedCallack stoppedCallback,
                                          void* stoppedCallbackUserData)
{
    /* Check handle*/
    if (handle == KWL_INVALID_HANDLE ||
        handle < 0 ||
        handle >= engine->numEventDefinitions)
    {
        return KWL_INVALID_EVENT_DEFINITION_HANDLE;
    }
    
    kwlEventDefinition* definition = &engine->eventDefinitions[handle];
    
    if (startAtPosition != 0 && definition->isPositional == 0)
    {
        return KWL_EVENT_IS_NOT_POSITIONAL;
    }
    else if (startAtPosition == 0 && definition->isPositional != 0)
    {
        /*If positional events are played "non-positionally", put them at the origin.*/
        x = 0.0f;
        y = 0.0f;
        z = 0.0f;
    }
    
    /* Look for a free event instance that is not currently playing.*/
    int instanceCount = definition->instanceCount;
    kwlEvent* instanceToStart = NULL;
    int numStealableInstances = 0;
    int i;
    for (i = 0; i < instanceCount; i++)
    {
        if (engine->events[handle][i].isAssociatedWithHandle == 0)
        {
            numStealableInstances++;
            if (engine->events[handle][i].isPlaying == 0)
            {
                instanceToStart = &engine->events[handle][i];
            }
        }
    }
    
    /*printf("instance to start: %d, n stealable=%d\n", 
             instanceToStart, 
             numStealableInstances);*/
    
    if (numStealableInstances == 0)
    {
        /*All instances of the event definition are currently associated with
          handles.*/
        return KWL_NO_FREE_EVENT_INSTANCES;
    }
    
    if (instanceToStart == NULL)
    {
        /*No free instance was found, but there are instances to steal.*/
        kwlEventInstanceStealingMode stealingMode = definition->stealingMode;
        if (stealingMode == KWL_DONT_STEAL)
        {
            /*Fail silently if instance stealing is not allowed.*/
            return KWL_NO_ERROR;
        }
        else if (stealingMode == KWL_STEAL_RANDOM)
        {
            /*Steal a randomly selected instance.*/
            int stealIndex = rand() % numStealableInstances;
            int stealableIndex = 0;

            for (i = 0; i < instanceCount; i++)
            {
                if (engine->events[handle][i].isAssociatedWithHandle == 0 &&
                    engine->events[handle][i].isPlaying == 1)
                {
                    if (stealableIndex == stealIndex)
                    {
                        instanceToStart = &engine->events[handle][i];
                        break;
                    }
                    stealableIndex++;
                }
            }
        }
        else if (stealingMode == KWL_STEAL_QUIETEST)
        {
            /*Find the instance with the lowest gain and steal it.*/
            float minGain = -1;

            for (i = 0; i < instanceCount; i++)
            {
                if (engine->events[handle][i].isAssociatedWithHandle == 0 &&
                    engine->events[handle][i].isPlaying == 1)
                {
                    /*Compare against the average channel gain of the instance.*/
                    float gain = engine->events[handle][i].gainLeft.valueEngine +
                                 engine->events[handle][i].gainRight.valueEngine;
                    if (minGain < 0 || gain < minGain)
                    {
                        instanceToStart = &engine->events[handle][i];
                    }
                }
            }
        }
        
        //since this instance is about to be stolen and thus stopped,
        //fire the stopped callback
        if (instanceToStart->stoppedCallback != NULL)
        {
            instanceToStart->stoppedCallback(instanceToStart->stoppedCallbackUserData);
        }
    }
    
    KWL_ASSERT(instanceToStart != NULL && "no one-shot instance found");
    if (startAtPosition)
    {
        instanceToStart->positionX = x;
        instanceToStart->positionY = y;
        instanceToStart->positionZ = z;
        
        instanceToStart->velocityX = 0.0f;
        instanceToStart->velocityY = 0.0f;
        instanceToStart->velocityZ = 0.0f;
    }
    
    instanceToStart->stoppedCallback = stoppedCallback;
    instanceToStart->stoppedCallbackUserData = stoppedCallbackUserData;
    
    return kwlSoundEngine_startEventInstance(engine, instanceToStart, 0.0f);
}

kwlError kwlSoundEngine_eventSetStoppedCallback(kwlSoundEngine* engine, const int handle, 
                                                kwlEventStoppedCallack stoppedCallback,
                                                void* stoppedCallbackUserData)
{
    kwlEvent* event = kwlSoundEngine_getEventFromHandle(engine, handle);
    if (event == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    if (stoppedCallback == NULL)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    event->stoppedCallback = stoppedCallback;
    event->stoppedCallbackUserData = stoppedCallbackUserData;
    
    return KWL_NO_ERROR;
}

/** */
kwlError kwlSoundEngine_eventStop(kwlSoundEngine* engine, const int handle, float fadeOutTimeSec)
{
    kwlEvent* eventToStop = kwlSoundEngine_getEventFromHandle(engine, handle);
    if (eventToStop == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    if (fadeOutTimeSec < 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    int result = kwlMessageQueue_addMessageWithParam(&engine->toMixerQueue, KWL_EVENT_STOP, eventToStop, fadeOutTimeSec);
    if (result == 0)
    {
        return KWL_MESSAGE_QUEUE_FULL;
    }
    
    return KWL_NO_ERROR;
}

/** */
kwlError kwlSoundEngine_eventPause(kwlSoundEngine* engine, const int handle)
{
    kwlEvent* eventToPause = kwlSoundEngine_getEventFromHandle(engine, handle);
    if (eventToPause == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    int result = kwlMessageQueue_addMessage(&engine->toMixerQueue, KWL_EVENT_PAUSE, eventToPause);
    if (result == 0)
    {
        return KWL_MESSAGE_QUEUE_FULL;
    }
    
    return KWL_NO_ERROR;
}

/** */
kwlError kwlSoundEngine_eventResume(kwlSoundEngine* engine, const int handle)
{
    kwlEvent* eventToResume = kwlSoundEngine_getEventFromHandle(engine, handle);
    if (eventToResume == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    int result = kwlMessageQueue_addMessage(&engine->toMixerQueue, KWL_EVENT_RESUME, eventToResume);
    if (result == 0)
    {
        return KWL_MESSAGE_QUEUE_FULL;
    }
    
    return KWL_NO_ERROR;
}

/** */
kwlError kwlSoundEngine_eventIsPlaying(kwlSoundEngine* engine, const int handle, int* isPlaying)
{
    kwlEvent* event = kwlSoundEngine_getEventFromHandle(engine, handle);
    if (event == NULL)
    {
        *isPlaying = 0;
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    *isPlaying = event->isPlaying;
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_eventSetPitch(kwlSoundEngine* engine, kwlEventHandle eventHandle, float pitch)
{
    kwlEvent* event = kwlSoundEngine_getEventFromHandle(engine, eventHandle);
    if (event == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    if (pitch < 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    event->userPitch = pitch;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_eventSetPosition(kwlSoundEngine* engine, kwlEventHandle handle, float posX, float posY, float posZ)
{
    kwlEvent* event = kwlSoundEngine_getEventFromHandle(engine, handle);
    if (event == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    if (event->definition_engine->isPositional == 0)
    {
        return KWL_EVENT_IS_NOT_POSITIONAL;
    }
    
    event->positionX = posX;
    event->positionY = posY;
    event->positionZ = posZ;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_eventSetOrientation(kwlSoundEngine* engine, kwlEventHandle handle, float directionX, float directionY, float directionZ)
{
    kwlEvent* event = kwlSoundEngine_getEventFromHandle(engine, handle);
    if (event == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    if (event->definition_engine->isPositional == 0)
    {
        return KWL_EVENT_IS_NOT_POSITIONAL;
    }
    
    if (directionX == 0.0f && directionY == 0.0f && directionZ == 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    float length = kwlFastInverseSqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
    
    event->directionX = directionX / length;
    event->directionY = directionY / length;
    event->directionZ = directionZ / length;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_eventSetVelocity(kwlSoundEngine* engine, kwlEventHandle handle, float velX, float velY, float velZ)
{
    kwlEvent* event = kwlSoundEngine_getEventFromHandle(engine, handle);
    if (event == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    if (event->definition_engine->isPositional == 0)
    {
        return KWL_EVENT_IS_NOT_POSITIONAL;
    }
    
    event->velocityX = velX;
    event->velocityY = velY;
    event->velocityZ = velZ;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_eventSetBalance(kwlSoundEngine* engine, kwlEventHandle handle, float balance)
{
    kwlEvent* event = kwlSoundEngine_getEventFromHandle(engine, handle);
    if (event == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    if (event->definition_engine->isPositional != 0)
    {
        return KWL_EVENT_IS_NOT_NONPOSITIONAL;
    }
    
    if (balance > 1.0f || balance < -1.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    event->balance = balance;
    
    return KWL_NO_ERROR;
}
    
kwlError kwlSoundEngine_eventSetGain(kwlSoundEngine* engine, kwlEventHandle eventHandle, float gain, int isLinearGain)
{
    kwlEvent* event = kwlSoundEngine_getEventFromHandle(engine, eventHandle);
    if (event == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    if (gain < 0.0f)
    {
        return KWL_INVALID_PARAMETER_VALUE;
    }
    
    event->userGain = isLinearGain == 1 ? gain : logGainToLinGain(gain);
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_attachDSPUnitToEvent(kwlSoundEngine* engine, kwlEventHandle eventHandle, kwlDSPUnit* dspUnit)
{
    kwlEvent* event = kwlSoundEngine_getEventFromHandle(engine, eventHandle);
    if (event == NULL)
    {
        return KWL_INVALID_EVENT_INSTANCE_HANDLE;
    }
    
    event->dspUnit.valueEngine = dspUnit;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_attachDSPUnitToMixBus(kwlSoundEngine* engine, kwlMixBusHandle mixBusHandle, kwlDSPUnit* dspUnit)
{
    kwlMixBus* bus = kwlSoundEngine_getMixBusFromHandle(engine, mixBusHandle);
    if (bus == NULL)
    {
        return KWL_INVALID_MIX_BUS_HANDLE;
    }
    
    bus->dspUnit.valueEngine = dspUnit;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_attachDSPUnitToInput(kwlSoundEngine* engine, kwlDSPUnit* dspUnit)
{
    engine->mixer->inputDSPUnit.valueEngine = dspUnit;
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_attachDSPUnitToOutput(kwlSoundEngine* engine, kwlDSPUnit* dspUnit)
{
    engine->mixer->outputDSPUnit.valueEngine = dspUnit;
    return KWL_NO_ERROR;
}


void debugPrintEventList(kwlEvent* event)
{
    kwlEvent* temp = event;
    while (temp != NULL)
    {
        printf("    %s\n", temp->definition_engine->id);
        temp  = temp->nextEvent_engine;
    }
}

/** */
void kwlSoundEngine_addEventToPlayingList(kwlSoundEngine* engine, kwlEvent* eventToAdd)
{
    //printf("about to add %s to list:\n", eventToAdd->definition_engine->id);
    //debugPrintEventList(engine->playingEventList);
    
    /*verify that the event is not already in the list
    kwlEvent* temp = engine->playingEventList;
    if (temp != NULL)
    {
        while (temp != NULL)
        {
            KWL_ASSERT(temp != eventToAdd && "event is already in the 'playing' list");
            temp = temp->nextEvent_engine;
        }
    }*/
    
    /*add the event to the linked list of playing events*/
    KWL_ASSERT(eventToAdd->nextEvent_mixer == NULL);
    if (engine->playingEventList == NULL)        
    {
        engine->playingEventList = eventToAdd;
    }
    else
    {
        kwlEvent* e = engine->playingEventList;
        while (e->nextEvent_engine != NULL)
        {
            e = e->nextEvent_engine;
        }
        e->nextEvent_engine = eventToAdd;
    }
    
    eventToAdd->nextEvent_engine = NULL;
    
    //printf("added %s to list:\n", eventToAdd->definition_engine->id);
    //debugPrintEventList(engine->playingEventList);
}

/** */
void kwlSoundEngine_removeEventFromPlayingList(kwlSoundEngine* engine, kwlEvent* event)
{
    kwlEvent* prevEvent = NULL;
    kwlEvent* eventi = engine->playingEventList;
    
    while (eventi != event)
    {
        prevEvent = eventi;
        eventi = eventi->nextEvent_engine;
    }
    
    if (prevEvent == NULL)
    {
        engine->playingEventList = eventi->nextEvent_engine;
    }
    else
    {
        prevEvent->nextEvent_engine = eventi->nextEvent_engine;
    }
    
    event->nextEvent_engine = NULL;
    /*
    printf("about to remove %s from list:\n", eventToRemove->definition_engine->id);
    debugPrintEventList(engine->playingEventList);
    
    //verify that the event is already in the list
    kwlEvent* temp = engine->playingEventList;
    KWL_ASSERT(temp != NULL && "trying to remove event from empty list");
    
    int foundMatch = 0;
    if (temp != NULL)
    {
        while (temp != NULL)
        {
            if (temp == eventToRemove)
            {
                foundMatch = 1;
                break;
            }
            temp = temp->nextEvent_engine;
        }
    }
    KWL_ASSERT(foundMatch && "event to be removed is not in the 'playing' list");
    
    //remove the event from the linked list of playing events.
    kwlEvent* event = engine->playingEventList;
    if (event == eventToRemove)
    {
        engine->playingEventList = event->nextEvent_engine;
    }
    else 
    {
        while (event->nextEvent_engine != eventToRemove)
        {
            event = event->nextEvent_engine;
        }
        KWL_ASSERT(event->nextEvent_engine == eventToRemove);
        event->nextEvent_engine = eventToRemove->nextEvent_engine;
    }
    
    printf("removed %s from list:\n", eventToRemove->definition_engine->id);
    debugPrintEventList(engine->playingEventList);*/
}

/*****************************************************************************
 * Data loading/unloading methods 
 *****************************************************************************/

kwlError kwlSoundEngine_loadNonAudioData(kwlSoundEngine* engine, kwlInputStream* stream)
{
    if (engine->engineDataIsLoaded)
    {
        KWL_ASSERT(0 && "TODO: free current data");
    }
    
    /*check file identifier*/
    int i;
    for (i = 0; i < KWL_ENGINE_DATA_BINARY_FILE_IDENTIFIER_LENGTH; i++)
    {
        const char identifierChari = kwlInputStream_readChar(stream);
        if (identifierChari != KWL_ENGINE_DATA_BINARY_FILE_IDENTIFIER[i])
        {
            return KWL_UNKNOWN_FILE_FORMAT;
        }
    }
    
    /*Load chunks*/
    kwlSoundEngine_loadMixBusData(engine, stream);
    kwlSoundEngine_loadMixPresetData(engine, stream);
    kwlSoundEngine_loadWaveBankData(engine, stream);
    /*must happen after wave bank loading*/
    kwlSoundEngine_loadSoundData(engine, stream);
    /*must happen after sound, wave bank and mix bus loading.*/
    kwlSoundEngine_loadEventData(engine, stream);
    
    engine->engineDataIsLoaded = 1;
    return KWL_NO_ERROR;
}

void kwlSoundEngine_seekToEngineDataChunk(kwlSoundEngine* engine, kwlInputStream* stream, int chunkId)
{
    /*move to the start of the stream*/
    kwlInputStream_reset(stream);
    /*move to first chunk*/
    kwlInputStream_skip(stream, KWL_ENGINE_DATA_BINARY_FILE_IDENTIFIER_LENGTH);
    
    while (!kwlInputStream_isAtEndOfStream(stream))
    {
        const int currentChunkId = kwlInputStream_readIntBE(stream);
        const int chunkSize = kwlInputStream_readIntBE(stream);
        if (currentChunkId == chunkId)
        {
            return;
        }
        else
        {
            kwlInputStream_skip(stream, chunkSize);
        }
    }
    
    KWL_ASSERT(0 && "no matching chunk id found");
}

void kwlSoundEngine_loadMixBusData(kwlSoundEngine* engine, kwlInputStream* stream)
{
    kwlSoundEngine_seekToEngineDataChunk(engine, stream, KWL_MIX_BUSES_CHUNK_ID);
    KWL_ASSERT(engine->mixBuses == NULL);

    /*allocate memory for the mix bus data*/
    const int numMixBuses = kwlInputStream_readIntBE(stream);
    KWL_ASSERT(numMixBuses > 0);
    engine->numMixBuses = numMixBuses;
    engine->numMixBuses = numMixBuses;
    engine->mixBuses = 
        (kwlMixBus*)KWL_MALLOC(numMixBuses * sizeof(kwlMixBus), 
                               "kwlSoundEngine_loadMixBusData: mixer bus array");
    engine->mixBuses = engine->mixBuses;
    engine->numMixBuses = engine->numMixBuses;
    kwlMemset(engine->mixBuses, 0, numMixBuses * sizeof(kwlMixBus));
    
    /*read mix bus data*/
    int i;
    for (i = 0; i < numMixBuses; i++)
    {
        kwlMixBus* const mixBusi = &engine->mixBuses[i];
        kwlMixBus_init(mixBusi);
    
        mixBusi->id = kwlInputStream_readASCIIString(stream);
        if (strcmp(mixBusi->id, "master") == 0)
        {
            KWL_ASSERT(engine->masterBus == NULL && "multiple master buses found");
            engine->masterBus = mixBusi;
            engine->masterBus->isMaster = 1;
        }
        
        const int numSubBuses = kwlInputStream_readIntBE(stream);
        KWL_ASSERT(numSubBuses >= 0);
        mixBusi->numSubBuses = numSubBuses;
        mixBusi->subBuses = NULL;
        if (numSubBuses > 0)
        {
            mixBusi->subBuses = (kwlMixBus**)KWL_MALLOC(numSubBuses * sizeof(kwlMixBus*), 
                                                        "kwlSoundEngine_loadMixBusData: sub buses");
            int j;
            for (j = 0; j < numSubBuses; j++)
            {
                const int subBusIndexj = kwlInputStream_readIntBE(stream);
                KWL_ASSERT(subBusIndexj >= 0 && subBusIndexj < numMixBuses);
                mixBusi->subBuses[j] = &engine->mixBuses[subBusIndexj];
            }
        }
    }
    
    KWL_ASSERT(engine->masterBus != NULL);
}

void kwlSoundEngine_freeMixBusData(kwlSoundEngine* engine)
{   
    if (engine->mixBuses == NULL)
    {
        return;
    }
    
    /*free the mix bus IDs*/
    const int numMixBuses = engine->numMixBuses;
    int i;
    for (i = 0; i < numMixBuses; i++)
    {
        if (engine->mixBuses[i].subBuses != NULL)
        {
            KWL_FREE(engine->mixBuses[i].subBuses);
        }
        KWL_FREE(engine->mixBuses[i].id);
    }
    
    /*free the mix bus array*/
    KWL_FREE(engine->mixBuses);
    engine->mixBuses = NULL;
    engine->masterBus = NULL;
    engine->mixBuses = NULL;;
    engine->numMixBuses = 0;;
    engine->masterBus = NULL;
}

void kwlSoundEngine_loadMixPresetData(kwlSoundEngine* engine, kwlInputStream* stream)
{
    kwlSoundEngine_seekToEngineDataChunk(engine, stream, KWL_MIX_PRESETS_CHUNK_ID);
    KWL_ASSERT(engine->mixBuses != 0); /*needed for mix bus lookup per param set*/
    
    /*allocate memory for the mix preset data*/
    const int numMixPresets = kwlInputStream_readIntBE(stream);
    KWL_ASSERT(numMixPresets > 0);
    engine->numMixPresets = numMixPresets;
    const int numParameterSets = engine->numMixBuses;
    int defaultPresetIndex = -1;
    engine->mixPresets = (kwlMixPreset*)KWL_MALLOC(sizeof(kwlMixPreset) * numMixPresets,
                                                   "kwlSoundEngine_loadMixPresetData");
    
    /*read data*/
    int i;
    for (i = 0; i < numMixPresets; i++)
    {
        engine->mixPresets[i].id = kwlInputStream_readASCIIString(stream);
        const int isDefault = kwlInputStream_readIntBE(stream);
        if (isDefault != 0)
        {
            KWL_ASSERT(defaultPresetIndex == -1 && "multiple default presets found");
            defaultPresetIndex = i;
            engine->mixPresets[i].weight = 1.0f;
            engine->mixPresets[i].targetWeight = 1.0f;
        }
        else
        {
            engine->mixPresets[i].weight = 0.0f;
            engine->mixPresets[i].targetWeight = 0.0f;
        }
        
        engine->mixPresets[i].numParameterSets = numParameterSets;
        engine->mixPresets[i].parameterSets = 
            (kwlMixBusParameters*)KWL_MALLOC(sizeof(kwlMixBusParameters) * numParameterSets, 
                                             "kwlSoundEngine_loadMixPresetData");
        int j;
        for (j = 0; j < numParameterSets; j++)
        {
            const int mixBusIndex = kwlInputStream_readIntBE(stream);
            KWL_ASSERT(mixBusIndex >= 0 &&  mixBusIndex < numParameterSets);
            engine->mixPresets[i].parameterSets[j].mixBusIndex = mixBusIndex;
            engine->mixPresets[i].parameterSets[j].logGainLeft = kwlInputStream_readFloatBE(stream);
            engine->mixPresets[i].parameterSets[j].logGainRight = kwlInputStream_readFloatBE(stream);
            engine->mixPresets[i].parameterSets[j].pitch = kwlInputStream_readFloatBE(stream);
        }
    }
    KWL_ASSERT(defaultPresetIndex >= 0);
    
}

void kwlSoundEngine_freeMixPresetData(kwlSoundEngine* engine)
{
    if (engine->mixPresets == NULL)
    {
        return;
    }

    const int numMixPresets = engine->numMixPresets;
    
    /*free any memory allocated per mix preset*/
    int i;
    for (i = 0; i < numMixPresets; i++)
    {
        KWL_FREE(engine->mixPresets[i].id);
        KWL_FREE(engine->mixPresets[i].parameterSets);
    }
    
    /*free the mix preset array*/
    KWL_FREE(engine->mixPresets);
    engine->mixPresets = NULL;
    engine->numMixPresets = 0;
}

/** */
void kwlSoundEngine_loadEventData(kwlSoundEngine* engine, kwlInputStream* stream)
{
    kwlSoundEngine_seekToEngineDataChunk(engine, stream, KWL_EVENTS_CHUNK_ID);
    KWL_ASSERT(engine->sounds != NULL);
    KWL_ASSERT(engine->events == NULL);
    KWL_ASSERT(engine->eventDefinitions == NULL);
    
    /*read the total number of event definitions*/
    const int numEventDefinitions = kwlInputStream_readIntBE(stream);
    KWL_ASSERT(numEventDefinitions > 0);
    engine->numEventDefinitions = numEventDefinitions;
    engine->events = 
        (kwlEvent**)KWL_MALLOC(numEventDefinitions * sizeof(kwlEvent*), "kwlSoundEngine_loadEventData");
    kwlMemset(engine->events, 0, numEventDefinitions * sizeof(kwlEvent*));
    engine->eventDefinitions = 
        (kwlEventDefinition*)KWL_MALLOC(numEventDefinitions * sizeof(kwlEventDefinition), "kwlSoundEngine_loadEventData");
    kwlMemset(engine->eventDefinitions, 0, numEventDefinitions * sizeof(kwlEventDefinition));
    
    int i;
    for (i = 0; i < numEventDefinitions; i++)
    {
        kwlEventDefinition* definitioni = &engine->eventDefinitions[i];
        /*read the id of this event definition*/
        definitioni->id = kwlInputStream_readASCIIString(stream);
        
        const int instanceCount = kwlInputStream_readIntBE(stream);
        KWL_ASSERT(instanceCount >= -1);
        definitioni->instanceCount = instanceCount;
        const int numInstancesToAllocate = instanceCount < 1 ? 1 : instanceCount;
        engine->events[i] = 
            (kwlEvent*)KWL_MALLOC(numInstancesToAllocate * sizeof(kwlEvent), "kwlSoundEngine_loadEventData");
        kwlMemset(engine->events[i], 0, numInstancesToAllocate * sizeof(kwlEvent));
        
        definitioni->gain = kwlInputStream_readFloatBE(stream);
        definitioni->pitch = kwlInputStream_readFloatBE(stream);
        const float degToRad = 0.0174532925199433;
        float innerConeAngleRad = degToRad * kwlInputStream_readFloatBE(stream);
        float outerConeAngleRad = degToRad * kwlInputStream_readFloatBE(stream);
        definitioni->innerConeCosAngle = cosf(innerConeAngleRad / 2.0f);
        definitioni->outerConeCosAngle = cosf(outerConeAngleRad / 2.0f);
        definitioni->outerConeGain = kwlInputStream_readFloatBE(stream);
        
        kwlEvent_init(&engine->events[i][0]);
        engine->events[i][0].definition_engine = definitioni;
        engine->events[i][0].definition_mixer = definitioni;
        
        /*read the index of the mix bus that this event belongs to*/
        const int mixBusIndex = kwlInputStream_readIntBE(stream);
        KWL_ASSERT(mixBusIndex >= 0 && mixBusIndex < engine->numMixBuses);
        definitioni->mixBus = &engine->mixBuses[mixBusIndex];
        definitioni->isPositional = kwlInputStream_readIntBE(stream);
        KWL_ASSERT(definitioni->isPositional == 0 || 
               definitioni->isPositional == 1);
        
        /*read the index of the sound referenced by this event (ignored for streaming events)*/
        const int soundIndex = kwlInputStream_readIntBE(stream);
        if (soundIndex == -1)
        {
            /*streaming event, no sound.*/
            definitioni->sound = NULL;
        }
        else
        {
            KWL_ASSERT(soundIndex >= 0 && soundIndex < engine->numSoundDefinitions);
            definitioni->sound = &engine->sounds[soundIndex];
        }
        
        /*read the event retrigger mode (ignored for streaming events)*/
        definitioni->retriggerMode = (kwlEventRetriggerMode)kwlInputStream_readIntBE(stream); /*TODO: assert*/
        KWL_ASSERT(definitioni->retriggerMode < 10 && definitioni->retriggerMode >= 0);
        
        /*read the index of the audio data referenced by this event (only used for streaming events)*/
        const int waveBankIndex = kwlInputStream_readIntBE(stream);
        const int audioDataIndex = kwlInputStream_readIntBE(stream);
        if (audioDataIndex >= 0 && audioDataIndex < engine->totalNumAudioDataEntries &&
            waveBankIndex >=0 && waveBankIndex < engine->numWaveBanks)
        {
            definitioni->streamAudioData = &engine->waveBanks[waveBankIndex].audioDataItems[audioDataIndex];
        }
        else
        {
            definitioni->streamAudioData = NULL;
        }
        
        /*read loop flag (ignored for non-streaming events)*/
        const int loopIfStreaming = kwlInputStream_readIntBE(stream);
        definitioni->loopIfStreaming = loopIfStreaming;
        
        /*read referenced wave banks*/
        definitioni->numReferencedWaveBanks = kwlInputStream_readIntBE(stream);
        KWL_ASSERT(definitioni->numReferencedWaveBanks < 10000 && definitioni->numReferencedWaveBanks >= 0);
        definitioni->referencedWaveBanks = 
            (kwlWaveBank**)KWL_MALLOC(definitioni->numReferencedWaveBanks * sizeof(kwlWaveBank*), "wave bank refs");
        int j;
        for (j = 0; j < definitioni->numReferencedWaveBanks; j++)
        {
            int waveBankIndex = kwlInputStream_readIntBE(stream);
            KWL_ASSERT(waveBankIndex >= 0 && waveBankIndex < engine->numWaveBanks);
            definitioni->referencedWaveBanks[j] = &engine->waveBanks[waveBankIndex];
        }
        
        /*copy the first event instance to the other slots.*/
        for (j = 1; j < numInstancesToAllocate; j++)
        {
            kwlMemcpy(&engine->events[i][j], &engine->events[i][0], sizeof(kwlEvent));
        }
    }
}

void kwlSoundEngine_freeEventData(kwlSoundEngine* engine)
{
    if (engine->events == NULL && 
        engine->eventDefinitions == NULL)
    {
        return;
    }
    
    const int numEventDefinitions = engine->numEventDefinitions;
    
    int i;
    for (i = 0; i < numEventDefinitions; i++)
    {
        kwlEventDefinition* defi = &engine->eventDefinitions[i];
        KWL_FREE(engine->events[i]);
        KWL_FREE(defi->referencedWaveBanks);
        KWL_FREE(defi->id);
    }
    
    KWL_FREE(engine->events);
    engine->events = NULL;
    KWL_FREE(engine->eventDefinitions);
    engine->eventDefinitions = NULL;
    engine->numEventDefinitions = 0;
}

/** */
void kwlSoundEngine_loadSoundData(kwlSoundEngine* engine, kwlInputStream* stream)
{
    kwlSoundEngine_seekToEngineDataChunk(engine, stream, KWL_SOUNDS_CHUNK_ID);
    
    /*allocate memory for sound definitions*/
    const int numSoundDefinitions = kwlInputStream_readIntBE(stream);
    KWL_ASSERT(numSoundDefinitions >= 0 && "the number of sound definitions must be non-negative");
    engine->numSoundDefinitions = numSoundDefinitions;
    engine->sounds = (kwlSound*)KWL_MALLOC(numSoundDefinitions * sizeof(kwlSound), "sound definitions");
    kwlMemset(engine->sounds, 0, numSoundDefinitions * sizeof(kwlSound));
    
    /*read sound definitions*/
    int i;
    for (i = 0; i < numSoundDefinitions; i++)
    {
        kwlSound_init(&engine->sounds[i]);
        engine->sounds[i].playbackCount = kwlInputStream_readIntBE(stream);
        engine->sounds[i].deferStop = kwlInputStream_readIntBE(stream);
        engine->sounds[i].gain = kwlInputStream_readFloatBE(stream);
        engine->sounds[i].gainVariation = kwlInputStream_readFloatBE(stream);
        engine->sounds[i].pitch = kwlInputStream_readFloatBE(stream);
        engine->sounds[i].pitchVariation = kwlInputStream_readFloatBE(stream);
        engine->sounds[i].playbackMode = (kwlSoundPlaybackMode)kwlInputStream_readIntBE(stream);
        
        const int numWaveReferences = kwlInputStream_readIntBE(stream);
        KWL_ASSERT(numWaveReferences > 0);
        engine->sounds[i].audioDataEntries = (kwlAudioData**)KWL_MALLOC(numWaveReferences * sizeof(kwlAudioData*), 
                                                             "kwlSoundEngine_loadSoundData: wave list");
        engine->sounds[i].numAudioDataEntries = numWaveReferences;

        int j;
        for (j = 0; j < numWaveReferences; j++)
        {
            const int waveBankIndex = kwlInputStream_readIntBE(stream);
            KWL_ASSERT(waveBankIndex >= 0 && waveBankIndex < engine->numWaveBanks);
            kwlWaveBank* waveBank = &engine->waveBanks[waveBankIndex];
        
            const int audioDataIndex = kwlInputStream_readIntBE(stream);
            KWL_ASSERT(audioDataIndex >= 0 && audioDataIndex < waveBank->numAudioDataEntries);
            
            KWL_ASSERT(engine->audioDataEntries != NULL);
            engine->sounds[i].audioDataEntries[j] = &waveBank->audioDataItems[audioDataIndex];
        }
    }
}

/** */
void kwlSoundEngine_freeSoundData(kwlSoundEngine* engine)
{
    if (engine->sounds == NULL)
    {
        return;
    }
    
    int i;
    for (i = 0; i < engine->numSoundDefinitions; i++)
    {
        KWL_FREE(engine->sounds[i].audioDataEntries);
    }
    KWL_FREE(engine->sounds);    
    engine->sounds = NULL;
    engine->numSoundDefinitions = 0;
}

/** non-audio data */
void kwlSoundEngine_loadWaveBankData(kwlSoundEngine* engine, kwlInputStream* stream)
{
    kwlSoundEngine_seekToEngineDataChunk(engine, stream, KWL_WAVE_BANKS_CHUNK_ID);
    
    /*deserialize wave bank structures*/
    const int totalnumAudioDataEntries = kwlInputStream_readIntBE(stream);
    KWL_ASSERT(totalnumAudioDataEntries > 0);
    const int numWaveBanks = kwlInputStream_readIntBE(stream);
    KWL_ASSERT(numWaveBanks > 0);
    
    engine->totalNumAudioDataEntries = totalnumAudioDataEntries;
    engine->audioDataEntries = (kwlAudioData*)KWL_MALLOC(totalnumAudioDataEntries * sizeof(kwlAudioData), "kwlSoundEngine_loadWaveBankData");
    kwlMemset(engine->audioDataEntries, 0, totalnumAudioDataEntries * sizeof(kwlAudioData)); 
    
    engine->numWaveBanks = numWaveBanks;
    engine->waveBanks = (kwlWaveBank*)KWL_MALLOC(numWaveBanks * sizeof(kwlWaveBank), "kwlSoundEngine_loadWaveBankData");
    kwlMemset(engine->waveBanks, 0, numWaveBanks * sizeof(kwlWaveBank)); 

    int i;
    int audioDataItemIdx = 0;
    for (i = 0; i < numWaveBanks; i++)
    {
        kwlWaveBank* waveBanki = &engine->waveBanks[i];
        waveBanki->id = kwlInputStream_readASCIIString(stream);
        const int numAudioDataEntries = kwlInputStream_readIntBE(stream);
        KWL_ASSERT(numAudioDataEntries > 0);
        waveBanki->numAudioDataEntries = numAudioDataEntries;
        waveBanki->audioDataItems = &engine->audioDataEntries[audioDataItemIdx];
        int j;
        for (j = 0; j < numAudioDataEntries; j++)
        {
            engine->audioDataEntries[audioDataItemIdx].filePath = kwlInputStream_readASCIIString(stream);
            engine->audioDataEntries[audioDataItemIdx].waveBank = waveBanki;
            audioDataItemIdx++;
        }
    }
}

/** */
void kwlSoundEngine_freeWaveBankData(kwlSoundEngine* engine)
{
    if (engine->waveBanks != NULL)
    {
        
        const int numWaveBanks = engine->numWaveBanks;
        int i;
        for (i = 0; i < numWaveBanks; i++)
        {
            KWL_FREE((void*)engine->waveBanks[i].id);
        }
        KWL_FREE(engine->waveBanks);
        engine->waveBanks = NULL;
    }
    
    if (engine->audioDataEntries != NULL)
    {
        const int numAudioDataEntries = engine->totalNumAudioDataEntries;
        int i;
        for (i = 0; i < numAudioDataEntries; i++)
        {
            KWL_FREE((void*)engine->audioDataEntries[i].filePath);
        }
        KWL_FREE(engine->audioDataEntries);
        engine->audioDataEntries = NULL;
    }
}


/** */
kwlError kwlSoundEngine_initialize(kwlSoundEngine* engine, 
                                   int sampleRate, 
                                   int numOutChannels,
                                   int numInChannels,
                                   int bufferSize)
{    
    //TODO: set this elsewhere?
    engine->mixer->sampleRate = sampleRate;
    engine->mixer->numOutChannels = numOutChannels;
    engine->mixer->numInChannels = numInChannels;
    engine->isInputEnabled = numInChannels > 0 ? 1 : 0;
    
    kwlSoftwareMixer_allocateTempBuffers(engine->mixer);
    
    kwlError result = kwlSoundEngine_hostSpecificInitialize(engine, sampleRate, numOutChannels, numInChannels, bufferSize);
    
    return result;
}

kwlError kwlSoundEngine_engineDataIsLoaded(kwlSoundEngine* engine, int* ret)
{
    *ret = engine->engineDataIsLoaded;
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_engineDataLoad(kwlSoundEngine* engine, const char* const dataFile)
{
    if (engine->engineDataIsLoaded != 0)
    {
        return KWL_ENGINE_ALREADY_LOADED;
    }
    
    kwlInputStream stream;
    kwlError result = kwlInputStream_initWithFile(&stream, dataFile);

    if (result != KWL_NO_ERROR)
    {
        kwlInputStream_close(&stream);
        return result;
    }
    
    result = kwlSoundEngine_loadNonAudioData(engine, &stream);
    
    kwlInputStream_close(&stream);
    
    if (result != KWL_NO_ERROR)
    {
        return result;
    }
    
    /*If we made it here, loading went well. Notify the mixer that the mix 
      bus hierarchy has been loaded.*/
    int success = kwlMessageQueue_addMessageWithParam(&engine->toMixerQueue, 
                                                      KWL_SET_MASTER_BUS, 
                                                      engine->mixBuses, 
                                                      engine->numMixBuses);
    KWL_ASSERT(success != 0);
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_unloadEngineDataBlocking(kwlSoundEngine* engine)
{
    if (engine->engineDataIsLoaded == 0)
    {
        return KWL_NO_ERROR;
    }
    
    /*Send a message to the mixer requesting it to: 
     - stop any playing data driven events
     - change its mix bus array to only contain the always valid master bus.
      The mixer then sends a KWL_UNLOAD_ENGINE_DATA message back to the engine thread
      that triggers the actual unloading.*/
    int result = kwlMessageQueue_addMessage(&engine->toMixerQueue, KWL_PREPARE_ENGINE_DATA_UNLOAD, NULL);
    //printf("posted KWL_PREPARE_ENGINE_DATA_UNLOAD\n");
    KWL_ASSERT(result != 0);
    
    /*Block until the engine data has been unloaded.*/
    while (engine->engineDataIsLoaded != 0)
    {
        /*printf("waiting for mixer to stop data driven events and clear mix buses\n");*/
        kwlUpdate(0);
    }
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_engineDataUnload(kwlSoundEngine* engine)
{
    /* Unload wave banks*/
    int i;
    for (i = 0; i < engine->numWaveBanks; i++)
    {
        kwlSoundEngine_unloadWaveBank(engine, &engine->waveBanks[i]);
    }
    
    /* Free non-audio data*/
    kwlSoundEngine_freeEventData(engine);
    kwlSoundEngine_freeSoundData(engine);
    kwlSoundEngine_freeMixPresetData(engine);
    kwlSoundEngine_freeMixBusData(engine);
    kwlSoundEngine_freeWaveBankData(engine);
        
    engine->engineDataIsLoaded = 0;
    //engine->playingEventList = NULL;
    
    return KWL_NO_ERROR;
}

/** */
void kwlSoundEngine_deinitialize(kwlSoundEngine* engine)
{
    /* Unload any engine data and wave banks*/
    kwlEngineDataUnload();
    /* Shut down the sound system.*/
    kwlSoundEngine_hostSpecificDeinitialize(engine);
}

kwlError kwlSoundEngine_getNumFramesMixed(kwlSoundEngine* engine, unsigned int* numFrames)
{
    if (engine->lastNumFramesMixed == 0)
    {
        *numFrames = 0;
    }
    else
    {
        *numFrames = engine->mixer->numFramesMixed.valueEngine - engine->lastNumFramesMixed;
    }
    
    engine->lastNumFramesMixed = engine->mixer->numFramesMixed.valueEngine;
    
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_hasClipped(kwlSoundEngine* engine, int* hasClipped)
{
    if (engine->mixer->isLevelMeteringEnabled.valueEngine == 0)  
    {
        *hasClipped = 0;
        return KWL_LEVEL_METERING_DISABLED;
    }
    
    *hasClipped = engine->mixer->clipFlag.valueEngine;
    return KWL_NO_ERROR;
}

kwlError kwlSoundEngine_getOutLevels(kwlSoundEngine* engine, float* leftLevel, float* rightLevel)
{    
    if (engine->mixer->isLevelMeteringEnabled.valueEngine == 0)  
    {
        *rightLevel = 0.0f;
        *leftLevel = 0.0f;
        return KWL_LEVEL_METERING_DISABLED;
    }
    
    *leftLevel = engine->mixer->latestBufferAbsPeakLeft.valueEngine;
    *rightLevel = engine->mixer->latestBufferAbsPeakRight.valueEngine;
    
    return KWL_NO_ERROR;
}
