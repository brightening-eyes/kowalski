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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioFileParser
{
    //TODO: clean this up.
    public static AudioFileDescription getAudioFileFormat(File file)
    {

        AudioFileDescription f = tryParseOggVorbis(file);
        if (f != null)
        {
            return f;
        }

        f = getIMAADPCMFormat(file);
        if (f != null)
        {
            return f;
        }

        f = getPCMFormat(file);
        if (f != null)
        {
            return f;
        }

        f = tryParseCAF(file);
        if (f != null)
        {
            return f;
        }

        AudioFileDescription fallback = 
                new AudioFileDescription(file, AudioFileDescription.Encoding.UNKNOWN, 0, 0, 0);
        return fallback;
    }

    public static AudioFileDescription getAudioFileFormat(String path)
    {
        return getAudioFileFormat(new File(path));
    }

    private static AudioFileDescription tryParseCAF(File f)
    {
        DataInputStream dis = null;
        try
        {
            FileInputStream fis = new FileInputStream(f);
            dis = new DataInputStream(fis);
        }
        catch (FileNotFoundException e)
        {

        }

        try
        {
            byte b1 = dis.readByte();
            byte b2 = dis.readByte();
            byte b3 = dis.readByte();
            byte b4 = dis.readByte();

            boolean isCaff = b1 == 'c' && b2 == 'a' && b3 == 'f' && b4 == 'f';
            if (!isCaff)
            {
                return null;
            }

            int version = dis.readShort();
            int flags = dis.readShort();


            byte c1 = dis.readByte();
            byte c2 = dis.readByte();
            byte c3 = dis.readByte();
            byte c4 = dis.readByte();

            boolean isDesc = c1 == 'd' && c2 == 'e' && c3 == 's' && c4 == 'c';
            if (!isDesc)
            {
                return null;
            }

            long chunkSize = dis.readLong();
            

            double fs = dis.readDouble();

            byte d1 = dis.readByte();
            byte d2 = dis.readByte();
            byte d3 = dis.readByte();
            byte d4 = dis.readByte();

            boolean isLpcm = d1 == 'l' && d2 == 'p' && d3 == 'c' && d4 == 'm';
            boolean isIma4 = d1 == 'i' && d2 == 'm' && d3 == 'a' && d4 == '4';
            boolean isAAC = d1 == 'a' && d2 == 'a' && d3 == 'c' && d4 == ' ';
            AudioFileDescription.Encoding enc;
            if (isLpcm)
            {
                enc = AudioFileDescription.Encoding.PCM;
            }
            else if (isIma4)
            {
                enc = AudioFileDescription.Encoding.IMAADPCM;
            }
            else if (isAAC)
            {
                enc = AudioFileDescription.Encoding.AAC;
            }
            else
            {
                return null;
            }
            int formatFlags = dis.readInt();
            int bytesPerPacket = dis.readInt();
            int framesPerPacket = dis.readInt();
            int channelsPerFrame = dis.readInt();
            int bitsPerChannel = dis.readInt();

            return new AudioFileDescription(f, enc, channelsPerFrame, (float)fs, bitsPerChannel);
            //System.out.println("fs " + fs + ", lpcm " + isLpcm + ", ima4 " + isIma4 + ", format flags " + formatFlags +
            //                   ", bytes per packet " + bytesPerPacket + ", frames per packet " + framesPerPacket +
            //                   ", ch per frame " + channelsPerFrame + ", bits per ch " + bitsPerChannel);
        }
        catch (IOException e)
        {
            return null;
        }



    }

    private static javax.sound.sampled.AudioFormat getJavaSoundFormat(File file)
    {
        try
        {
            AudioInputStream audioInputStream =
                AudioSystem.getAudioInputStream (file);
            javax.sound.sampled.AudioFormat format = audioInputStream.getFormat();
            return format;
        }
        catch (IOException e)
        {
            return null;
        }
        catch (UnsupportedAudioFileException e)
        {
            return null;
        }
    }

    static AudioFileDescription getPCMFormat(File file)
    {
        javax.sound.sampled.AudioFormat javaSoundFormat =
                                        getJavaSoundFormat(file);
        if (javaSoundFormat == null)
        {
            return null;
        }
        
        return new AudioFileDescription(file, AudioFileDescription.Encoding.PCM,
                                        javaSoundFormat.getChannels(),
                                        javaSoundFormat.getFrameRate(),
                                        javaSoundFormat.getSampleSizeInBits());
    }

    private static AudioFileDescription tryParseOggVorbis(File file)
    {
        

        try
        {
            int numChannels = 0;
            float sampleRate = 0;
            int bitsPerSample = 0;

            //
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);

            //read ogg header
            {
                //capture pattern (4 bytes)
                byte cp1 = dis.readByte();
                byte cp2 = dis.readByte();
                byte cp3 = dis.readByte();
                byte cp4 = dis.readByte();
            
                boolean isOgg = cp1 == 'O' && cp2 == 'g' && cp3 == 'g' && cp4 == 'S';

                if (!isOgg)
                {
                    return null;
                }

                byte version = dis.readByte();
                byte headerTyper = dis.readByte();
                long granulePosition = dis.readLong();
                int serialNumber = dis.readInt();
                int pageSequenceNumber = dis.readInt();
                int checkSum = dis.readInt();
                byte numPageSegments = dis.readByte();
                byte[] segmentTable = new byte[numPageSegments];
                dis.read(segmentTable);
            }
            
            //read vorbis header
            {
                //packet type should be 1 for identification header
                byte packetType = dis.readByte();

                boolean isVorbis = 
                              dis.readByte() == 'v' &&
                              dis.readByte() == 'o' &&
                              dis.readByte() == 'r' &&
                              dis.readByte() == 'b' &&
                              dis.readByte() == 'i' &&
                              dis.readByte() == 's';

                if (!isVorbis)
                {
                    return null;
                }

                int vorbisVersion = dis.readInt();
                numChannels = dis.readByte();
                sampleRate = endianSwapInt(dis.readInt());
                int maxBitRate = dis.readInt();
                int minBitRate = dis.readInt();
                int nominalBitRate = dis.readInt();
                //etc
            }

            dis.close();

            return new AudioFileDescription(file, AudioFileDescription.Encoding.Vorbis, numChannels, sampleRate, bitsPerSample);
        }
        catch (IOException e)
        {
            return null;
        }
    }


    private static int endianSwapInt(int x)
    {
        return  ((x << 24) & 0xff000000) |
                ((x << 8) & 0x00ff0000) |
                ((x >> 8) & 0x0000ff00) |
                ((x >> 24) & 0x000000ff);
    }

    private static short endianSwapShort(short x)
    {
        return (short)((x << 8) | (x >> 8));
    }


    private static AudioFileDescription getIMAADPCMFormat(File file)
    {
        try
        {
            //
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);
            //read RIFF chunk
            {
                byte b1 = dis.readByte();
                byte b2 = dis.readByte();
                byte b3 = dis.readByte();
                byte b4 = dis.readByte();
                if (b1 != 'R' || b2 != 'I' || b3 != 'F' || b4 != 'F')
                {
                    return null;
                }
            }

            dis.readInt();

            {
                byte b1 = dis.readByte();
                byte b2 = dis.readByte();
                byte b3 = dis.readByte();
                byte b4 = dis.readByte();
                if (b1 != 'W' || b2 != 'A' || b3 != 'V' || b4 != 'E')
                {
                    return null;
                }
            }
        

            boolean fmtChunkFound = false;
            short numChannels = 0;
            float sampleRate = 0;
            int bitsPerSample = 0;
            short nBlockAlign = 0;

            while (!fmtChunkFound)
            {
                byte c1 = dis.readByte();
                byte c2 = dis.readByte();
                byte c3 = dis.readByte();
                byte c4 = dis.readByte();

                if (c1 == 'f' && c2 == 'm' && c3 == 't' && c4 ==' ')
                {
                    //printf("reading %c%c%c%c (fmt ) chunk", c1, c2, c3, c4);
                    int chunkSize = endianSwapInt(dis.readInt());
                    short audioFormat = endianSwapShort(dis.readShort());
                    numChannels = endianSwapShort(dis.readShort());
                    sampleRate = endianSwapInt(dis.readInt());
                    int  byteRate = endianSwapInt(dis.readInt());
                    nBlockAlign = endianSwapShort(dis.readShort());
                    bitsPerSample = endianSwapShort(dis.readShort());
                    int cbSize = endianSwapInt(dis.readInt());
                    //dis.readShort();
                    if (audioFormat != 0x11 || bitsPerSample != 4)
                    {
                        return null;
                    }



                    fmtChunkFound = true;
                }
                else
                {

                    int chunkSize = endianSwapInt(dis.readInt());
                    dis.skip(chunkSize);
                }
            }

            return new AudioFileDescription(file,
                AudioFileDescription.Encoding.IMAADPCM, numChannels, sampleRate, bitsPerSample);
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
