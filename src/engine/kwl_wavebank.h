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

#ifndef KWL__WAVE_BANK_H
#define KWL__WAVE_BANK_H

/*! \file */ 

#ifdef __cplusplus
extern "C"
{
#endif /* __cplusplus */
    
/** 
 * A named collection of pieces of audio data.
 */
typedef struct kwlWaveBank
{
    /** The ID of the wave bank. */
    const char* id;
    /* Non-zero if the wave bank is loaded, zero otherwise*/
    char isLoaded;
    /** The path to the wave bank file. Empty if the wave bank is not loaded.*/
    char *waveBankFilePath;
    /** An array of audio data entries for the wave bank. */
    struct kwlAudioData* audioDataItems;
    /** The number of audio data entries in the wave bank. */
    int numAudioDataEntries;
} kwlWaveBank;

#ifdef __cplusplus
}
#endif /* __cplusplus */    
    
#endif /*KWL__WAVE_BANK_H*/
