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
#include "kwl_event.h"
#include "kwl_synchronization.h"
#include "kwl_sound.h"

#include "kwl_assert.h"
#include <math.h>
#include <stdlib.h>

/** */
void kwlEvent_init(kwlEvent* event)
{
    kwlMemset(event, 0, sizeof(kwlEvent));
    
    event->definition_mixer = NULL;
    event->definition_engine = NULL;
    
    event->positionX = 0.0f;
    event->positionY = 0.0f;
    event->positionZ = 0.0f;
    
    event->velocityX = 0.0f;
    event->velocityY = 0.0f;
    event->velocityZ = 0.0f;
    
    event->directionX = 0.0f;
    event->directionY = 0.0f;
    event->directionZ = 0.0f;
    
    event->userGain = 1.0f;
    event->userPitch = 1.0f;    
    event->balance = 0.0f;
    
    event->numBuffersPlayed = 0;
    event->currentAudioDataIndex = 0;
    event->pitchAccumulator = 0.0f;
    
    event->fadeGainIncrPerFrame = 0.0f;
    event->fadeGain = 1.0f;
    event->soundPitch = 1.0f;
    event->playbackState = KWL_STOPPED;
}

void kwlEvent_start(kwlEvent* event)
{
    event->numBuffersPlayed = 0;
    event->pitchAccumulator = 0.0f;
    event->currentPCMFrameIndex = 0;
    event->playbackState = KWL_PLAYING;
    event->soundPitch = 1.0f;
    event->prevEffectiveGain[0] = -1.0f;
    event->prevEffectiveGain[1] = -1.0f;
}

static int isUnitPitch(float pitch)
{
    return pitch - 1.0f < PITCH_EPSILON && 
           pitch - 1.0f > -PITCH_EPSILON ? 
                1 : 0;
}

int kwlEvent_getNumRemainingOutFrames(kwlEvent* event, float pitch)
{
    KWL_ASSERT(pitch > 0);
    if (isUnitPitch(pitch) != 0)
    {
        return event->currentPCMBufferSize - event->currentPCMFrameIndex;
    }
    else
    {
        int numRemaining = (int)((event->currentPCMBufferSize - event->currentPCMFrameIndex - event->pitchAccumulator) / pitch); 
        KWL_ASSERT(numRemaining >= 0 && "kwlEvent_getNumRemainingOutFrames: negative num frames");
        return numRemaining;
    }
}

int kwlEvent_render(kwlEvent* event, 
                    float* outBuffer,
                    const int numOutChannels,
                    const int numFrames,
                    const float accumulatedBusPitch)
{
    /* initial playback logic checks */
    {
        if (event->playbackState == KWL_STOP_AND_UNLOAD_REQUESTED)
        {
            return 1;
        }
        else if (event->playbackState == KWL_STOP_REQUESTED)
        {
            /*if the event has been requested to stop and if the
              sound (if any) permits stopping mid-buffer, return 1 to indicate
              that the event should be removed from the mixer.*/
            int allowsImmediateStop = 
                event->definition_mixer->sound != NULL ? 
                event->definition_mixer->sound->deferStop == 0 : 1;
            if (allowsImmediateStop != 0)
            {
                return 1;
            }
        }
        else if (event->playbackState == KWL_PLAY_LAST_BUFFER_AND_STOP_REQUESTED)
        {
            int allowsImmediateStop = 
                event->definition_mixer->sound != NULL ? 
                event->definition_mixer->sound->deferStop == 0 : 1;
            if (allowsImmediateStop != 0)
            {
                kwlSound_pickNextBufferForEvent(event->definition_mixer->sound, 
                                                event, 0);
            }
        }        
        else if (event->isPaused != 0)
        {
            kwlClearFloatBuffer(outBuffer, numFrames * numOutChannels);
            return 0;
        }
        else if (event->pitch.valueMixer < PITCH_EPSILON)
        {
            /*Don't allow too low pitch values.*/
            event->pitch.valueMixer = PITCH_EPSILON;
        }
    }
    
    /*Update fade progress*/
    {
        event->fadeGain += event->fadeGainIncrPerFrame * numFrames;
        if (event->fadeGain > 1.0f)
        {
            event->fadeGain = 1.0f;
        }
        else if (event->fadeGain < 0.0f)
        {
            event->fadeGain = 0.0f;
            /** The fade out just finished, signal that the event should be stopped.*/
            return 1;
        }
    }
    
    /*gets set to a non-zero value when the out buffer has been completely filled*/
    int endOfOutBufferReached = 0;
    /*the index of the current frame in the out buffer*/
    int outFrameIdx = 0;
    /*gets set to a non-zero value when the end of the current source buffer is reached*/
    int endOfSourceBufferReached = 0;
    
    /* 
       During this loop, the output buffer is filled with samples from 
       either a sound or a decoder.     
     */
    int donePlaying = 0;
    while (!endOfOutBufferReached)
    {
        /*if the event pitch is close enough to 1, pitch shifting is not applied.*/
        float effectivePitch = event->pitch.valueMixer * event->soundPitch * accumulatedBusPitch;
        if (effectivePitch < PITCH_EPSILON)
        {
            effectivePitch = PITCH_EPSILON;
        }
        
        int unitPitch = isUnitPitch(effectivePitch);
        
        /*Check if we have enough source frames to fill the output buffer. */
        int numOutFramesLeft = kwlEvent_getNumRemainingOutFrames(event, effectivePitch);
        int maxOutFrameIdx = numFrames;
        if (numOutFramesLeft < numFrames - outFrameIdx) 
        {
            maxOutFrameIdx = outFrameIdx + numOutFramesLeft + (unitPitch == 0 ? 1 : 0);/*TODO: ugly*/
            endOfSourceBufferReached = 1;
        }
        
        int outSampleIdx = 0;
        int srcSampleIdx = 0;
        float pitchAccumulator = 0;
        
        const float soundGain = event->definition_mixer->sound != NULL ? 
                                event->definition_mixer->sound->gain : 1.0f;
        
        /*This loop is where the actual mixing takes place.*/
        //printf("about to mix event buffer, event->currentPCMFrameIndex %d, ep %f\n", event->currentPCMFrameIndex, effectivePitch);
        int ch;
        for (ch = 0; ch < numOutChannels; ch++)
        { 
            outSampleIdx = outFrameIdx * numOutChannels + ch;
            const int maxOutSampleIdx = maxOutFrameIdx * numOutChannels + ch;
            srcSampleIdx = event->currentPCMFrameIndex * event->currentNumChannels + ch;
            pitchAccumulator = event->pitchAccumulator;
            
            if (unitPitch)
            {
                /*a simplified mix loop without pitch shifting*/
                kwlInt16ToFloatWithGain(event->currentPCMBuffer, 
                                        outBuffer,
                                        maxOutSampleIdx,                    
                                        &srcSampleIdx,
                                        event->currentNumChannels,
                                        &outSampleIdx, 
                                        numOutChannels, 
                                        soundGain);
                KWL_ASSERT(srcSampleIdx >= 0);
            }
            else
            {
                kwlInt16ToFloatWithGainAndPitch(event->currentPCMBuffer, 
                                                outBuffer,
                                                maxOutSampleIdx,                    
                                                &srcSampleIdx,
                                                event->currentNumChannels,
                                                &outSampleIdx, 
                                                numOutChannels, 
                                                soundGain,
                                                effectivePitch,
                                                &pitchAccumulator);
            }
            
            /*There are 4 possible combinations of input and output channel counts to consider:*/

            /*1. mono in, stereo out: copy left out to right out and break the loop after the first of two channels*/
            if (event->currentNumChannels == 1 && numOutChannels == 2)
            {
                int i;
                for (i = outFrameIdx * numOutChannels + ch; i < maxOutSampleIdx;)
                {
                    outBuffer[i + 1] = outBuffer[i];
                    i += 2;
                }
                break;
            }
                        
            /*2. stereo in, mono out*/
            /*If the input is stereo, its right channel gets ignored.*/
            
            /*3. mono in, mono out*/
            /*Requires no special handling.*/
            
            /*4. stereo in, stereo out*/
            /*Requires no special handling.*/
        }
        
        KWL_ASSERT(srcSampleIdx >= 0);
        outFrameIdx = outSampleIdx / numOutChannels;
        event->pitchAccumulator = pitchAccumulator;
        event->currentPCMFrameIndex = srcSampleIdx / event->currentNumChannels;
        
        /* Perform playback logic checks if the end of the current source buffer was reached.*/ 
        if (endOfSourceBufferReached != 0)
        {
            event->numBuffersPlayed++;
            donePlaying = 0;
            if (event->definition_mixer == NULL && event->decoder == NULL)
            {
                /*this is a PCM event created in code. we're done playing.*/
                donePlaying = 1;
            }
            else if (event->decoder != NULL)
            {
                /*decode the next buffer*/
                donePlaying = kwlDecoder_decodeNewBufferForEvent(event->decoder, event);
            }
            else
            {
                /*get another pcm buffer from the event's sound*/
                donePlaying = kwlSound_pickNextBufferForEvent(event->definition_mixer->sound, event, 0);
            }
            
            if (donePlaying != 0)
            {
                /*the event finished playing, fill the remainder of the out buffer with zeros*/
                kwlClearFloatBuffer(&outBuffer[outFrameIdx * numOutChannels], 
                                    (numFrames - outFrameIdx) * numOutChannels);
                
                break;
            }
            else
            {
                /*A new src buffer was just picked.*/
                endOfSourceBufferReached = 0;
            }
        }
        else
        {
            /* if we made it here the end of the out buffer must have been reached. */
            KWL_ASSERT(outFrameIdx == numFrames);
            endOfOutBufferReached = 1;
        }
    }
    
    /*Feed final event output through the event DSP unit, if any.*/
    kwlDSPUnit* dspUnit = (kwlDSPUnit*)event->dspUnit.valueMixer;
    if (dspUnit != NULL)
    {
        (*dspUnit->dspCallback)(outBuffer,
                                numOutChannels,
                                numFrames, 
                                dspUnit->data);
    }
    
    /* Apply per buffer gain with ramps if necessary*/
    {
        float effectiveGain[2] = 
        {
            event->fadeGain * event->gainLeft.valueMixer,
            event->fadeGain * event->gainRight.valueMixer
        };
        
        if (event->prevEffectiveGain[0] < 0.0f)
        {
            event->prevEffectiveGain[0] = effectiveGain[0];
            event->prevEffectiveGain[1] = effectiveGain[1];
        }
        
        kwlApplyGainRamp(outBuffer, 
                         numOutChannels, 
                         numFrames, 
                         event->prevEffectiveGain, 
                         effectiveGain);
        
        event->prevEffectiveGain[0] = effectiveGain[0];
        event->prevEffectiveGain[1] = effectiveGain[1];
    }
    
    return donePlaying;
}
