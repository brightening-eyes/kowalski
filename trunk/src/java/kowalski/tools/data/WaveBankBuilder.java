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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.bind.JAXBException;
import kowalski.tools.data.xml.AudioData;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.WaveBank;
import kowalski.tools.data.xml.WaveBankGroup;

/**
 * Handles creation of wave bank binaries.
 */
public class WaveBankBuilder extends DataBuilder
{
    /** The default suffix of files containing Kowalski wave banks. */
    public static final String WAVE_BANK_FILE_SUFFIX = ".kwb";
    /** The binary file identifier for engine data. */
    public static final byte[] WAVE_BANK_FILE_IDENTIFIER =
    {
        (byte)0xAB, 'K', 'W', 'B',
        (byte)0xBB, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A
    };

    /** 16 bit linear pcm*/
    private static final int KWL_ENCODING_PCM = 0;
    /** Ogg Vorbis. */
    private static final int KWL_ENCODING_OGG_VORBIS = 1;
    /** IMA ADPCM (aka DVI ADPCM). */
    private static final int KWL_ENCODING_IMA_ADPCM = 2;
    /** IMA ADPCM (aka DVI ADPCM). */
    private static final int KWL_ENCODING_AAC = 3;
    /** An unknown encoding.*/
    private static final int KWL_ENCODING_UNKNOWN = 4;


    public WaveBankBuilder()
    {
        
    }
    
    private void buildWaveBank(KowalskiProject project,
                               File projectFile,
                               WaveBank waveBank,
                              File waveBankFile,
                              String hierarchyPath)
            throws ProjectDataException, IOException
    {
        if (waveBank.getAudioDataList().size() == 0)
        {
            String msg = "The wave bank '" + hierarchyPath +
                                            "' is empty.";
            logError(msg);
            throw new ProjectDataException(msg);
        }

        File projectDir = projectFile.getParentFile();

        //check wave bank file timestamp
        long waveBankLastModified = 0;
        if (waveBankFile.exists())
        {
            waveBankLastModified = waveBankFile.lastModified();
        }

        //first gather the audio files
        List<AudioData> audioDataList = waveBank.getAudioDataList();
        final int numAudioDataItems = audioDataList.size();
        List<File> audioFiles = new ArrayList<File>();
        long newestAudioFileTimeStamp = 0;
        for (int i = 0; i < numAudioDataItems; i++)
        {
            AudioData adr = audioDataList.get(i);
            File filei =
                ProjectDataUtils.getAudioDataFile(project, projectDir, adr);

            if (!filei.exists())
            {
                String msg = "The audio file " + adr.getRelativePath() + " does not exist.";
                logError(msg);
                throw new ProjectDataException(msg);
            }
            else if (filei.isDirectory())
            {
                String msg = "The audio file " + adr.getRelativePath() + " is a directory.";
                logError(msg);
                throw new ProjectDataException(msg);
            }

            long lastModified = filei.lastModified();
            if (lastModified > newestAudioFileTimeStamp)
            {
                newestAudioFileTimeStamp = lastModified;
            }

            audioFiles.add(filei);
        }

        //if the wave bank target file is newer than the project file and all audio files in
        //this bank, it's up to date.
        long projectFileTimeStamp = projectFile.lastModified();
        if (waveBankLastModified > newestAudioFileTimeStamp &&
            waveBankLastModified > projectFileTimeStamp)
        {
            log("Wave bank file " + waveBankFile.getAbsolutePath() + " is up to date.");
            log("");
            return;
        }
        
        //TODO: serialize into memory and then to disk?
        //TODO: treat entries shared between wavebanks somehow. or don't allow that?
        FileOutputStream fileOutputStream = new FileOutputStream(waveBankFile);

        DataOutputStream dos = new DataOutputStream(fileOutputStream);

        //write the file identifier
        dos.write(WAVE_BANK_FILE_IDENTIFIER);

        //write the wave bank id
        dos.writeInt(hierarchyPath.length());
        dos.write(hierarchyPath.getBytes());

        log("Writing wave bank '" + hierarchyPath + "' (" + numAudioDataItems +
                " audio data " + (numAudioDataItems == 1 ? "entry" : "entries") +
                ")");
        log("to " + waveBankFile);

        //write the number of waves in the bank
        dos.writeInt(numAudioDataItems);
        for (int i = 0; i < numAudioDataItems; i++)
        {
            AudioData audioDatai = audioDataList.get(i);
            log("    " + audioDatai.getRelativePath());

            //get the file referenced by the audio data object
            File audioFile = audioFiles.get(i);
            byte[] audioDataBytes = null;
            AudioFileDescription.Encoding encoding;
            int numChannels = 0;

            /*
             * Possible scenarios:
             *
             *              stream from disk         dont stream from disk
             *           ---------------------------------------------------
             * PCM       |     B                | A
             *           |----------------------|---------------------------
             * non-PCM   |     B                | B
             *           ---------------------------------------------------
             *
             * A : convert to 16 bit pcm and store raw samples in wavebank
             * B : store entire file in wave bank
             *
             */

            //first assume the file is a linear PCM
            try
            {
                //TODO: bit depth and signedness and packing.
                //TODO: handle conversion to 16 bit signed interleaved.
                //TODO: decide on endianness.
                AudioInputStream audioInputStream =
                            AudioSystem.getAudioInputStream(audioFile);

                AudioFormat format = audioInputStream.getFormat();
                numChannels = format.getChannels();

                if (!audioDatai.isStreamFromDisk())
                {
                    //convert to 16 bit pcm if not streaming from disk
                    audioDataBytes = getAudioDataBytes(project, audioInputStream);
                }
                else
                {
                    //if streaming from disk, store the entire file
                    FileInputStream fis = new FileInputStream(audioFile);
                    int fileSize = (int)audioFile.length();
                    audioDataBytes = new byte[fileSize];
                    fis.read(audioDataBytes);
                    fis.close();

                    numChannels = 0;
                }
            
                encoding = kowalski.tools.data.AudioFileDescription.Encoding.PCM;
                
            }
            catch (UnsupportedAudioFileException e)
            {
                
                //see if we're dealing with an recognized compressed format
                kowalski.tools.data.AudioFileDescription f =
                        AudioFileParser.getAudioFileFormat(audioFile);

                FileInputStream fis = new FileInputStream(audioFile);
                int fileSize = (int)audioFile.length();
                audioDataBytes = new byte[fileSize];
                fis.read(audioDataBytes);
                fis.close();
                encoding = f.getEncoding();
                
            }

            //write the file name
            dos.writeInt(audioDatai.getRelativePath().length());
            dos.write(audioDatai.getRelativePath().getBytes());
            //write the encoding
            dos.writeInt(getEncodingInt(encoding));
            //write "stream from disk" flag. only relevant for non-pcm data
            if (audioDatai.isStreamFromDisk() && encoding.equals(AudioFileDescription.Encoding.PCM))
            {
                //throw new RuntimeException(audioDatai.getRelativePath() + " has stream flag set but is PCM.");
            }
            dos.writeInt(audioDatai.isStreamFromDisk() ? 1 : 0); //TODO
            //write the number of channels (set to 0 for non-pcm)
            dos.writeInt(numChannels);
            //write num audio data bytes
            dos.writeInt(audioDataBytes.length);
           
            dos.write(audioDataBytes);
            
            log("        Wrote " + audioDataBytes.length + " bytes of " +
                      encoding.toString() + " audio data.");
        }
        log("");
        fileOutputStream.close();
    }

    private int getEncodingInt(AudioFileDescription.Encoding enc)
    {
        if (enc.equals(AudioFileDescription.Encoding.PCM))
        {
            return KWL_ENCODING_PCM;
        }
        else if (enc.equals(AudioFileDescription.Encoding.Vorbis))
        {
            return KWL_ENCODING_OGG_VORBIS;
        }
        else if (enc.equals(AudioFileDescription.Encoding.IMAADPCM))
        {
            return KWL_ENCODING_IMA_ADPCM;
        }
        else if (enc.equals(AudioFileDescription.Encoding.AAC))
        {
            return KWL_ENCODING_AAC;
        }
        else
        {
            return KWL_ENCODING_UNKNOWN;
        }

    }

    private byte[] getAudioDataBytes(KowalskiProject project, AudioInputStream audioInputStream)
            throws IOException, ProjectDataException, UnsupportedAudioFileException
    {
        AudioFormat format = audioInputStream.getFormat();
        if (format.getChannels() != 1 && format.getChannels() != 2)
        {
            throw new UnsupportedAudioFileException("Only mono and stereo files are supported " +
                "in wave banks. Got " + format.getChannels() + " channels.");
        }
        if (format.getSampleSizeInBits() != 16 && format.getSampleSizeInBits() != 8)
        {
            throw new UnsupportedAudioFileException("Only 8 bit and 16 bit audio data is currently supported, got " +
                    format.getSampleSizeInBits() + " bits per sample");
        }

        boolean eightBit = format.getSampleSizeInBits() == 8;
        boolean bigEndian = format.isBigEndian();

        

        final int bytesPerFrame = format.getFrameSize();
        ByteArrayOutputStream audioByteStream = new ByteArrayOutputStream();
        
        byte[] frame = new byte[bytesPerFrame];
        while (audioInputStream.read(frame) != -1)
        {
            audioByteStream.write(frame);
        }

        byte[] audioDataBytes = audioByteStream.toByteArray();
        if (eightBit)
        {
            log("        Converting 8 bit samples to 16 bit");
            byte[] convertedAudioDataBytes = new byte[audioDataBytes.length * 2];
            for (int i = 0; i < audioDataBytes.length; i++)
            {
                int sample = audioDataBytes[i] << 8;
                convertedAudioDataBytes[2 * i] = (byte)(sample & 0xf);
                convertedAudioDataBytes[2 * i + 1] = (byte)((sample >> 8) & 0xf0);
            }
            audioDataBytes = convertedAudioDataBytes;
        }
        else if (bigEndian)
        {
            log("        Converting to little endian byte order");
            //System.out.println("        converting to little endian " + format);
            for (int i = 1; i < audioDataBytes.length; i += 2)
            {
                byte t = audioDataBytes[i - 1];
                audioDataBytes[i - 1] = audioDataBytes[i];
                audioDataBytes[i] = t;
            }
        }
        audioInputStream.close();
        audioByteStream.close();

        return audioDataBytes;
    }

    private void buildWaveBanksInGroup(KowalskiProject project,
                                       WaveBankGroup group,
                                       File projectFile,
                                       File currentDir,
                                       String currHierarchyPath,
                                       List<WaveBank> waveBanksToBuild)
                                       throws ProjectDataException, IOException
    {
        List<WaveBank> waveBanks = group.getWaveBanks();
        List<WaveBankGroup> subGroups = group.getSubGroups();
        currentDir.mkdir();
        for (int i = 0; i < waveBanks.size(); i++)
        {
            WaveBank waveBanki = waveBanks.get(i);
            if (!waveBanksToBuild.contains(waveBanki))
            {
                //skip any wavebanks we shouldnt build
                continue;
            }

            buildWaveBank(project, 
                          projectFile,
                          waveBanki,
                          new File(currentDir, waveBanki.getID() + WAVE_BANK_FILE_SUFFIX),
                          currHierarchyPath + waveBanki.getID());
        }

        for (int i = 0; i < subGroups.size(); i++)
        {
            WaveBankGroup groupi = subGroups.get(i);
            buildWaveBanksInGroup(project,
                                  groupi,
                                  projectFile,
                                  new File(currentDir, groupi.getID()),
                                  currHierarchyPath + groupi.getID() + "/",
                                  waveBanksToBuild);
        }
    }

    public void buildWavebanks(KowalskiProject project, File projectFile, String outputDir, List<WaveBank> waveBanksToBuild)
            throws ProjectDataException, IOException, UnsupportedAudioFileException
    {
        File outDir = new File(outputDir);
        outDir.mkdir();
        buildWaveBanksInGroup(project,
                              project.getWaveBankRootGroup(),
                              projectFile,
                              outDir,
                              "",
                              waveBanksToBuild);
        /*Map<String, WaveBank> waveBanksByPath = ProjectDataUtils.getWaveBanksByHierarchyPath(project);
        Iterator<String> it = waveBanksByPath.keySet().iterator();
        while (it.hasNext())
        {
            String path = it.next();
            WaveBank waveBanki = waveBanksByPath.get(path);
            File outputFile = new File(outputDir, path.replace('/', '_') + WAVE_BANK_FILE_SUFFIX);
            buildWaveBank(project, projectDir, waveBanki, outputFile, path);
        }*/
    }

    public void buildWavebanks(String projectPath, String outputDir)
            throws ProjectDataException, IOException, JAXBException, UnsupportedAudioFileException
    {
        File projectFile = new File(projectPath);
        KowalskiProject project = ProjectDataXMLSerializer.getInstance().deserializeKowalskiProject(projectPath);
        List<WaveBank> waveBanks = ProjectDataUtils.getWaveBankList(project);
        buildWavebanks(project, projectFile, outputDir, waveBanks);
    }
}
