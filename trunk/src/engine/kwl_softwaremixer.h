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
#ifndef KWL__SOFTWARE_MIXER_H
#define KWL__SOFTWARE_MIXER_H

/*! \file */ 

#include "kwl_decoder.h"
#include "kwl_event.h"
#include "kwl_messagequeue.h"
#include "kwl_mixbus.h"
#include "kwl_wavebank.h"

#ifdef __cplusplus
extern "C"
{
#endif /* __cplusplus */

/*Ideally, the temp buffers should be bigger than the output buffers.*/
#define KWL_TEMP_BUFFER_SIZE_IN_FRAMES 1024

/*forward declarations*/
struct kwlEvent;

/** 
 * Pitch values within this epsilon of 1 are considered to be unit pitch
 * and pitch values below this epsilon are treated as zero.
 */
static const float PITCH_EPSILON = 0.001f;
    
/** A struct encapsulating the */
typedef struct kwlSoftwareMixer
{
    /** Non-zero if the mixer is paused, zero otherwise*/
    kwlSharedChar isPaused;
    /** The number of frames mixed since the mixer was initialized. */
    kwlSharedLongLong numFramesMixed;
    /** 
     * The mix bus freeform events are mixed through. This bus exists in parallel with 
     * the data driven mix bus hierarchy and the final mix is the sum of the outputs
     * from this bus and the data driven master bus (if any).
     */
    kwlMixBus freeformEventsBus;
    /** The master mix bus.*/
    kwlMixBus* masterBus;
    /** The number of mix buses in the mixer. */
    int numMixBuses;
    /** An array of the mix buses in the mixer. */
    kwlMixBus* mixBuses;
    /** A message queue that incoming messages from the engine thread get copied to and then processed. */
    kwlMessageQueue fromEngineQueue;
    /** A message queue for outgoing messages to the engine thread. This queue is exclusive to the mixer thread */
    kwlMessageQueue toEngineQueue;
    /** 
     * A message queue that outgoing messages get copied to and then grabbed from the engine thread. 
     * NOTE: this queue is accessed from both the engine thread and the mixer thread so \c kwlAcquireMainLock()
     * must be called proior to any manipulation to protect the data.
     */
    kwlMessageQueue toEngineQueueShared;
    /** */
    kwlSharedFloat latestBufferAbsPeakLeft;
    /** */
    kwlSharedFloat latestBufferAbsPeakRight;
    /** Non-zero if clipping occured, zero otherwise.*/
    kwlSharedInt clipFlag;
    /** Non-zero if level metering is enabled, zero otherwise.*/
    kwlSharedChar isLevelMeteringEnabled;
    /** The dsp unit that input audio is passed through. Can be null.*/
    kwlSharedVoidPointer inputDSPUnit;
    /** The dsp unit that the master output is passed through. Can be null.*/
    kwlSharedVoidPointer outputDSPUnit;
    /** A pointer to the audio engine.*/
    struct kwlSoundEngine* engine;
    /** The sample rate in Hz.*/
    float sampleRate;
    /** The number of channels output audio. 1 or 2*/
    int numOutChannels;
    /** The number of channels for input audio. 0, 1, or 2.*/
    int numInChannels;
    /** A temporary buffer used to store input samples.*/
    float* inBuffer;
    /** A temporary buffer used to store output samples.*/
    float* outBuffer;
    /** A temporary buffer to mix the output of events into.*/
    float* tempEventBuffer;
    /** A temporary buffer to mix the output of mix buses into.*/
    float* tempMixBusBuffer;
    /** Non-zero if the mix bus hierarchy should be reset, zero otherwise.*/
    int resetMixBusesRequested;
    /** */
    kwlMutexLock* mixerEngineMutexLock;
} kwlSoftwareMixer;

kwlSoftwareMixer* kwlSoftwareMixer_new();

void kwlSoftwareMixer_free(kwlSoftwareMixer* mixer);

/** Sends a message to the engine thread, notifying it that the given event just stopped. */
void kwlSoftwareMixer_sendEventStoppedMessage(kwlSoftwareMixer* mixer, struct kwlEvent* event);
/** */
void kwlSoftwareMixer_stopAllEventsReferencingWaveBank(kwlSoftwareMixer* mixer, kwlWaveBank* waveBank);
/** */
void kwlSoftwareMixer_stopAllDataDrivenEvents(kwlSoftwareMixer* mixer);
/** */
void kwlSoftwareMixer_stopAllEvents(kwlSoftwareMixer* mixer);
/** */    
void kwlSoftwareMixer_setMixBusArray(kwlSoftwareMixer* mixer, kwlMixBus* buses, int numBuses);
/** */
void kwlSoftwareMixer_resetMixBuses(kwlSoftwareMixer* mixer);
/** Processes any enqueued incoming messages from the engine thread. */
void kwlSoftwareMixer_processMessages(kwlSoftwareMixer* mixer);
void kwlSoftwareMixer_updateOutput(kwlSoftwareMixer* mixer);
void kwlSoftwareMixer_updateInput(kwlSoftwareMixer* mixer);
void kwlSoftwareMixer_allocateTempBuffers(kwlSoftwareMixer* mixer);

/**
 * Performs mixing into an output buffer of a given size.
 * @param mixer The mixer responsible for the mixing.
 * @param outBuffer The buffer to mix into
 * @numFrames The buffer size in frames.
 */
void kwlSoftwareMixer_render(kwlSoftwareMixer* mixer, float* outBuffer, int numFrames);

/**
 * This method passes an input buffer of a given size to the input dsp unit, if any.
 * @param mixer The mixer.
 * @param inBuffer The input samples.
 * @numFrames The buffer size in frames.
 */
void kwlSoftwareMixer_processInputBuffer(kwlSoftwareMixer* mixer, const float* inBuffer, int numFrames);

#ifdef __cplusplus
}
#endif /* __cplusplus */
    
#endif /*KWL__SOFTWARE_MIXER_H*/
