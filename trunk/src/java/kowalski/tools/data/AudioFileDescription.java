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

package kowalski.tools.data;

import java.io.File;

/**
 *
 */
public class AudioFileDescription
{
    public enum Encoding
    {
        PCM,
        Vorbis,
        IMAADPCM,
        AAC,
        UNKNOWN
    }

    private Encoding encoding;
    private int numChannels;
    private int bitsPerSample;
    private float sampleRate;
    private String fileName;
    private long fileSize;

    public AudioFileDescription(File f, Encoding enc, int nChannels, float sRate, int sampleSizeInBits)
    {
        fileSize = f.length();
        fileName = f.getName();
        encoding = enc;
        numChannels = nChannels;
        sampleRate = sRate;
        bitsPerSample = sampleSizeInBits;
    }

    public Encoding getEncoding()
    {
        return encoding;
    }

    public String getFileName()
    {
        return fileName;
    }

    public int getNumChannels()
    {
        return numChannels;
    }
    public int getBitsPerSample()
    {
        return bitsPerSample;
    }

    public float getSampleRate()
    {
        return sampleRate;
    }

    public long getFileSize()
    {
        return fileSize;
    }
}
