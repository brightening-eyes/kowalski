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
#ifndef KWL__SOUND_ENGINE_H
#define KWL__SOUND_ENGINE_H

/*! \file */ 

#include "kowalski.h"
#include "kwl_audiodata.h"
#include "kwl_dspunit.h"
#include "kwl_event.h"
#include "kwl_inputstream.h"
#include "kwl_synchronization.h"
#include "kwl_mixpreset.h"
#include "kwl_positionalaudiolistener.h"
#include "kwl_positionalaudiosettings.h"
#include "kwl_softwaremixer.h"
#include "kwl_sound.h"
#include "kwl_wavebank.h"

#ifdef __cplusplus
extern "C"
{
#endif /* __cplusplus */

/***********************************************************************
 * Sound engine constants.
 ***********************************************************************/
/** 
 * The file identifier for wave bank binaries, ie the sequence of bytes
 * that all wave bank binary files start with.
 */
static const char KWL_WAVE_BANK_BINARY_FILE_IDENTIFIER[9] =
{
    0xAB, 'K', 'W', 'B', 0xBB, 0x0D, 0x0A, 0x1A, 0x0A
};
    
/** The number of bytes in the wave bank binary identifier. */
static const int KWL_WAVE_BANK_BINARY_FILE_IDENTIFIER_LENGTH = 9;
    
/** 
 * The file identifier for engine binaries, ie the sequence of bytes
 * that all engine data binary files start with.
 */
static const char KWL_ENGINE_DATA_BINARY_FILE_IDENTIFIER[9] =
{
    0xAB, 'K', 'W', 'L', 0xBB, 0x0D, 0x0A, 0x1A, 0x0A
};
    
/** The number of bytes in the engine data identifier.*/
static const int KWL_ENGINE_DATA_BINARY_FILE_IDENTIFIER_LENGTH = 9;
    
/** The ID of the event data chunk in an engine data binary file. */
static const int KWL_EVENTS_CHUNK_ID = 0x73747665;
    
/** The ID of the sound data chunk in an engine data binary file. */
static const int KWL_SOUNDS_CHUNK_ID = 0x73646e73;
    
/** The ID of the mix bus data chunk in an engine data binary file. */
static const int KWL_MIX_BUSES_CHUNK_ID = 0x7362786d;
    
/** The ID of the mix preset data chunk in an engine data binary file. */
static const int KWL_MIX_PRESETS_CHUNK_ID = 0x7270786d;
    
/** The ID of the wave bank data chunk in an engine data binary file. */
static const int KWL_WAVE_BANKS_CHUNK_ID = 0x736b6277;

/***********************************************************************
 * Sound engine struct.
 ***********************************************************************/
/** A struct representing the singleton Kowalski sound engine. */
typedef struct kwlSoundEngine
{
    /** Zero if no engine data is loaded, non-zero otherwise. */
    int engineDataIsLoaded;
    
    /** The software mixer responsible for generating the final output buffers. */
    kwlSoftwareMixer* mixer;

    /** The number of mix buses.*/
    int numMixBuses;
    /** */
    kwlMixBus* mixBuses;
    /** */
    kwlMixBus* masterBus;
    
    /** A message queue that incoming messages from the mixer thread get copied to and then processed. */
    kwlMessageQueue fromMixerQueue;
    /** A message queue for outgoing messages to the mixer thread. */
    kwlMessageQueue toMixerQueue;
    /** A message queue that outgoing messages get copied to and then grabbed from the mixer thread. 
     * NOTE: this queue is accessed from both the engine thread and the mixer thread so the main lock
     * must be acquired prior to any manipulation to protect the data.
     */
    kwlMessageQueue toMixerQueueShared;
    /** A struct containing information about the current 3D audio listener. */
    kwlPositionalAudioListener listener;

    /** The number of wave banks */
    int numWaveBanks;
    /** An array of wave banks */
    kwlWaveBank* waveBanks;

    /** The number of mix presets. */
    int numMixPresets;
    /** An array of mix presets. */
    kwlMixPreset* mixPresets;
    /** The number of seconds it takes to fade between mix presets.*/
    float mixPresetFadeTime;
    
    /** The total number of audio data entries. */
    int totalNumAudioDataEntries;
    /** An array of audio data entries that may or may not contain loaded audio data. */
    kwlAudioData* audioDataEntries;

    /** The current number of event definitions read from engine data.*/
    int numEventDefinitions;
    /** An array of arrays of definitions read from data. */
    struct kwlEventDefinition* eventDefinitions;
    /** An array of arrays of event instances read from data. Instance i of event definition j is at [j][i]. */
    struct kwlEvent** events;
    /** The current size of the array of freeform events, i.e events created in code.*/
    int freeformEventArraySize;
    /** An array of freeform events, i.e events created in code. This array is dynamically resized and may contain null entries. */
    struct kwlEvent** freeformEvents;

    /** The number of sound definitions currently loaded from engine data.*/
    int numSoundDefinitions;
    /** An array of sound definitions. */
    struct kwlSound* sounds;

    int isInputEnabled;
    
    long long lastNumFramesMixed;
    /** */
    int numDecoders;
    /** */
    struct kwlDecoder* decoders;
    
    /** 
     * A linked list of currently playing events, ie events for which a 'start event' message has been sent and
     * an 'event stopped' message has not yet been received. 
     */
    struct kwlEvent* playingEventList;
    /** A collection of positional audio parameters.*/
    kwlPositionalAudioSettings positionalAudioSettings;
    /** */
    kwlMutexLock mainMutexLock;

} kwlSoundEngine; 
    
/** Initializes a newly allocated sound engine instance. */
void kwlSoundEngine_init(kwlSoundEngine* engine);
    
/** Deletes a given sound engine instance. */
void kwlSoundEngine_free(kwlSoundEngine* engine);

/***********************************************************************
 * Event methods
 ***********************************************************************/
/** */
kwlError kwlSoundEngine_eventGetHandle(kwlSoundEngine* engine, const char* const eventId, int* handle);

/** */
kwlError kwlSoundEngine_eventDefinitionGetHandle(kwlSoundEngine* engine, 
                                                 const char* const eventDefinitionID, 
                                                 kwlEventDefinitionHandle* handle);

/** */
kwlError kwlSoundEngine_eventCreateWithAudioData(kwlSoundEngine* engine, kwlAudioData* audioData, 
                                                 kwlEventHandle* handle, kwlEventType type,
                                                 const char* const eventId);

/** */
kwlError kwlSoundEngine_eventCreateWithBuffer(kwlSoundEngine* engine, kwlPCMBuffer* buffer, 
                                              kwlEventHandle* handle, kwlEventType type);
    
/** */
kwlError kwlSoundEngine_eventCreateWithFile(kwlSoundEngine* engine, const char* const audioFilePath, 
                                            kwlEventHandle* handle, kwlEventType type, int streamFromDisk);
    
/** */
kwlError kwlSoundEngine_eventRelease(kwlSoundEngine* engine, kwlEventHandle handle);

/** */
kwlError kwlSoundEngine_unloadFreeformEvent(kwlSoundEngine* engine, struct kwlEvent* event);

/** */
kwlError kwlSoundEngine_eventStart(kwlSoundEngine* engine, const int handle, float fadeInTimeSec);
    
/** */
kwlError kwlSoundEngine_startEventInstance(kwlSoundEngine* engine, struct kwlEvent* event, float fadeInTimeSec);
    
/** */
kwlError kwlSoundEngine_eventStop(kwlSoundEngine* engine, const int handle, float fadeOutTimeSec);

/** */
kwlError kwlSoundEngine_eventStartOneShot(kwlSoundEngine* engine, 
                                          kwlEventDefinitionHandle handle, 
                                          float x, float y, float z, 
                                          int startAtPosition,
                                          kwlEventStoppedCallack stoppedCallback,
                                          void* stoppedCallbackUserData);

kwlError kwlSoundEngine_eventSetStoppedCallback(kwlSoundEngine* engine, const int handle, 
                                                kwlEventStoppedCallack stoppedCallback,
                                                void* stoppedCallbackUserData);
    
/** */
kwlError kwlSoundEngine_eventPause(kwlSoundEngine* engine, const int handle);
    
/** */
kwlError kwlSoundEngine_eventResume(kwlSoundEngine* engine, const int handle);    
    
/** */
kwlError kwlSoundEngine_eventIsPlaying(kwlSoundEngine* engine, const int handle, int* isPlaying);
    
/** */
kwlError kwlSoundEngine_eventSetPitch(kwlSoundEngine* engine, kwlEventHandle event, float pitchPercent);
    
/** */
kwlError kwlSoundEngine_eventSetPosition(kwlSoundEngine* engine, kwlEventHandle handle, float posX, float posY, float posZ);
    
/** */
kwlError kwlSoundEngine_eventSetOrientation(kwlSoundEngine* engine, kwlEventHandle handle, float directionX, float directionY, float directionZ);
    
/** */
kwlError kwlSoundEngine_eventSetVelocity(kwlSoundEngine* engine, kwlEventHandle handle, float velX, float velY, float velZ);
    
/** */
kwlError kwlSoundEngine_eventSetBalance(kwlSoundEngine* engine, kwlEventHandle handle, float balance);
    
/** */
kwlError kwlSoundEngine_eventSetGain(kwlSoundEngine* engine, kwlEventHandle eventHandle, float gain, int isLinearGain);
    
/** Adds a given event to the linked list of currently playing events. */
void kwlSoundEngine_addEventToPlayingList(kwlSoundEngine* engine, struct kwlEvent* eventToAdd);
    
/** Removes a given event from the linked list of currently playing events. */
void kwlSoundEngine_removeEventFromPlayingList(kwlSoundEngine* engine, struct kwlEvent* eventToRemove);
    
/** Returns the event corresponding to a given handle or NULL if the handle is invalid.*/
struct kwlEvent* kwlSoundEngine_getEventFromHandle(kwlSoundEngine* engine, kwlEventHandle handle);
    
/** Returns non-zero if the given handle corresponds to a freeform event, zero otherwise.*/
int kwlSoundEngine_isFreeformEventHandle(kwlSoundEngine* engine, kwlEventHandle handle);   

/** */
int kwlSoundEngine_getFreeformIndexFromHandle(kwlSoundEngine* engine, kwlEventHandle handle);
    
/** */
void kwlSoundEngine_updateEvents(kwlSoundEngine* engine);

/** */
float kwlSoundEngine_getConeGain(kwlSoundEngine* engine, float cosAngle, float cosInner, float cosOuter, float outerGain);    

/***********************************************************************
 * DSP units
 ***********************************************************************/    
kwlError kwlSoundEngine_attachDSPUnitToEvent(kwlSoundEngine* engine, kwlEventHandle eventHandle, kwlDSPUnit* dspUnit);
kwlError kwlSoundEngine_attachDSPUnitToMixBus(kwlSoundEngine* engine, kwlMixBusHandle mixBusHandle, kwlDSPUnit* dspUnit);
kwlError kwlSoundEngine_attachDSPUnitToInput(kwlSoundEngine* engine, kwlDSPUnit* dspUnit);
kwlError kwlSoundEngine_attachDSPUnitToOutput(kwlSoundEngine* engine, kwlDSPUnit* dspUnit);
/***********************************************************************
 * Mix buses/presets
 ***********************************************************************/    
/** */
kwlError kwlSoundEngine_mixBusGetHandle(kwlSoundEngine* engine, const char* const busId, kwlMixBusHandle* handle);

/** Returns the mix bus corresponding to a given handle or NULL if the handle is invalid.*/
struct kwlMixBus* kwlSoundEngine_getMixBusFromHandle(kwlSoundEngine* engine, kwlMixBusHandle handle);
    
/** */
kwlError kwlSoundEngine_mixBusSetGain(kwlSoundEngine* engine, kwlMixBusHandle handle, float gain, int isLinearGain);
    
/** */
kwlError kwlSoundEngine_mixBusSetPitch(kwlSoundEngine* engine, kwlMixBusHandle handle, float pitch);

/** */
kwlError kwlSoundEngine_mixPresetGetHandle(kwlSoundEngine* engine, const char* const busId, kwlMixBusHandle* handle);
    
/** */
kwlError kwlSoundEngine_mixPresetSetActive(kwlSoundEngine* engine, kwlMixPresetHandle handle, int doFade);
    
/** */
void kwlSoundEngine_updateMixPresets(kwlSoundEngine* engine, float timeStepSec);

/***********************************************************************
 * Positional audio
 ***********************************************************************/
/** */
kwlError kwlSoundEngine_setListenerPosition(kwlSoundEngine* engine, float posX, float posY, float posZ);
    
/** */
kwlError kwlSoundEngine_setListenerVelocity(kwlSoundEngine* engine, float velX, float velY, float velZ);
    
/** */
kwlError kwlSoundEngine_setListenerOrientation(kwlSoundEngine* engine, float dirX, float dirY, float dirZ,
                                               float upX, float upY, float upZ);

/** */
kwlError kwlSoundEngine_setDistanceAttenuationModel(kwlSoundEngine* engine,
                                                    kwlDistanceAttenuationModel type, 
                                                    int clamp,
                                                    float maxDistance,
                                                    float rolloffFactor,
                                                    float referenceDistance);

/** */
kwlError kwlSoundEngine_setDopplerShiftParameters(kwlSoundEngine* engine, float speedOfSound, float dopplerScale);

/** */
kwlError kwlSoundEngine_setConeAttenuationEnabled(kwlSoundEngine* engine, int listenerCone, int eventCones);
    
/** */
kwlError kwlSoundEngine_setListenerConeParameters(kwlSoundEngine* engine, 
                                                  float innerAngle, 
                                                  float outerAngle, 
                                                  float outerGain);
    
/** Returns the positional audio distance gain using the currently set distance attenuation model.*/
float kwlSoundEngine_getDistanceGain(kwlSoundEngine* engine, float distanceInv);
/***********************************************************************
 * Basic engine functionality
 ***********************************************************************/
/** */
kwlError kwlSoundEngine_resume(kwlSoundEngine* engine);
    
/** */
kwlError kwlSoundEngine_pause(kwlSoundEngine* engine);
    
/** */
kwlError kwlSoundEngine_update(kwlSoundEngine* engine, float timeStep);
    
/** */
kwlError kwlSoundEngine_initialize(kwlSoundEngine* engine, int sampleRate, int numOutChannels, int numInChannels, int bufferSize);
    
/** */
kwlError kwlSoundEngine_engineDataIsLoaded(kwlSoundEngine* engine, int* ret);
    
/** */
kwlError kwlSoundEngine_engineDataLoad(kwlSoundEngine* engine, const char* const dataFile);
    
/** */
kwlError kwlSoundEngine_engineDataUnload(kwlSoundEngine* engine);

/** */
kwlError kwlSoundEngine_unloadEngineDataBlocking(kwlSoundEngine* engine);
    
/** Releases all loaded audio and engine data and shuts down the underlying sound system. */
void kwlSoundEngine_deinitialize(kwlSoundEngine* engine);

/***********************************************************************
 * Data loading/unloading methods
 ***********************************************************************/
/** Loads non-audio data (ie engine data) from a given stream. */
kwlError kwlSoundEngine_loadNonAudioData(kwlSoundEngine* engine, kwlInputStream* stream);
    
/** Moves the read pointer of a given stream to the first data byte of a chunk with a given id */
void kwlSoundEngine_seekToEngineDataChunk(kwlSoundEngine* engine, kwlInputStream* stream, int chunkId);

/** Loads mix bus data from a given file stream.*/
void kwlSoundEngine_loadMixBusData(kwlSoundEngine* engine, kwlInputStream* stream);

/** Loads mix preset data from a given file stream.*/
void kwlSoundEngine_loadMixPresetData(kwlSoundEngine* engine, kwlInputStream* stream);
    
/** Releases any currently loaded mix preset data. */
void kwlSoundEngine_freeMixPresetData(kwlSoundEngine* engine);
        
/** Releases any currently loaded mix bus data. */
void kwlSoundEngine_freeMixBusData(kwlSoundEngine* engine);

/** Loads event bus data from a given file stream.*/
void kwlSoundEngine_loadEventData(kwlSoundEngine* engine, kwlInputStream* stream);

/** Releases any currently loaded event data. */
void kwlSoundEngine_freeEventData(kwlSoundEngine* engine);
    
/** Loads sound data from a given file stream.*/
void kwlSoundEngine_loadSoundData(kwlSoundEngine* engine, kwlInputStream* stream);

/** Releases any currently loaded sound data. */
void kwlSoundEngine_freeSoundData(kwlSoundEngine* engine);

/** Loads wave bank data (not including audio data) from a given file stream.*/
void kwlSoundEngine_loadWaveBankData(kwlSoundEngine* engine, kwlInputStream* stream);
    
/** Releases any currently loaded wave bank data. */
void kwlSoundEngine_freeWaveBankData(kwlSoundEngine* engine);
    
/** Loads the audio data entries in Kowalski wave bank binary file. */
kwlError kwlSoundEngine_loadWaveBank(kwlSoundEngine* engine, const char* const waveBankFile, kwlWaveBankHandle* handle);

/** */
kwlError kwlSoundEngine_waveBankIsLoaded(kwlSoundEngine* engine, kwlWaveBankHandle handle, int* isLoaded);

/** */
kwlError kwlSoundEngine_waveBankIsReferencedByPlayingEvent(kwlSoundEngine* engine, kwlWaveBankHandle handle, int* isReferenced);

/** */
kwlError kwlSoundEngine_requestUnloadWaveBank(kwlSoundEngine* engine, kwlWaveBankHandle waveBankHandle, int blockUntilUnloaded);

/** */
kwlError kwlSoundEngine_unloadWaveBank(kwlSoundEngine* engine, kwlWaveBank* waveBank);

/** */
kwlError kwlSoundEngine_getNumFramesMixed(kwlSoundEngine* engine, unsigned int* numFrames);
    
/** */
kwlError kwlSoundEngine_getOutLevels(kwlSoundEngine* engine, float* leftLevel, float* rightLevel);
    
/** */
kwlError kwlSoundEngine_hasClipped(kwlSoundEngine* engine, int* hasClipped);
    
/***********************************************************************
 * Engine methods to be implemented per target host.
 ***********************************************************************/
/** 
 * Carries out host specific initialization of the underlying sound system 
 * and hooks it up to the Kowalski engine callbacks.
 */
kwlError kwlSoundEngine_hostSpecificInitialize(kwlSoundEngine* engine, int sampleRate, int numOutChannels, int numInChannels, int bufferSize);

/** 
 * Performs host specific deinitialization of the underlying sound system.
 */
kwlError kwlSoundEngine_hostSpecificDeinitialize(kwlSoundEngine* engine);
    
#ifdef __cplusplus
}
#endif /* __cplusplus */    
    
#endif /*KWL__SOUND_ENGINE_H*/
