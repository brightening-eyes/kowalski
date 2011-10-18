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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import kowalski.tools.data.xml.AudioData;
import kowalski.tools.data.xml.AudioDataReference;
import kowalski.tools.data.xml.Event;
import kowalski.tools.data.xml.EventRetriggerMode;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.MixBus;
import kowalski.tools.data.xml.MixBusParameters;
import kowalski.tools.data.xml.MixPreset;
import kowalski.tools.data.xml.Sound;
import kowalski.tools.data.xml.SoundPlaybackMode;
import kowalski.tools.data.xml.SoundReference;
import kowalski.tools.data.xml.WaveBank;

/**
 * Class responsible for the conversion of Kowalski XML data
 * to a binary representation read by the Kowalski engine.
 */
public class EngineDataBuilder extends DataBuilder
{
    /** The default suffix of files containing binary Kowalski engine data. */
    public static final String ENGINE_DATA_FILE_SUFFIX = ".kwl";
    
    /** The binary file identifier for engine data. */
    public static final byte[] ENGINE_DATA_FILE_IDENTIFIER =
        {(byte)0xAB, 'K', 'W', 'L',
         (byte)0xBB, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A};

    /** The event chunk identifier (evts)*/
    public static final int EVENTS_CHUNK_ID = 0x73747665;
    /** The sonds chunk identifier (snds)*/
    public static final int SOUNDS_CHUNK_ID = 0x73646e73;
    /** The mix buses chunk identifier (mxbs) */
    public static final int MIX_BUSES_CHUNK_ID = 0x7362786d;
    /** The mix preset chunk identifier (mxpr)*/
    public static final int MIX_PRESETS_CHUNK_ID = 0x7270786d;
    /** The wave bank chunk identifier (wbks)*/
    public static final int WAVE_BANKS_CHUNK_ID = 0x736b6277;
    /**The directory containing the project XML file.*/
    private File projectDirectory;
    /** A map of mix presets by hierarchy path.*/
    private Map<MixPreset, String> getHierarchyPathsByMixPreset;
    /** A list of all mix buses.*/
    private List<MixBus> mixBusList;
    /** A list of all sound definitions. */
    private List<Sound> soundList;
    /**
     * A dictionary of created sounds (ie sounds replacing references to PCM audio data)
     * by referencing event.
     */
    Map<Event, Sound> additionalSoundsByEvent;
    /**  */
    private Map<String, Sound> soundsBySoundHierarchyPath;
    /** */
    private List<Event> eventList;
    private List<WaveBank> waveBankList;
    Map<String, WaveBank> waveBanksByHierarchyPath;
    /** */
    private List<Event> nonPCMEvents = new ArrayList<Event>();
    /** */
    private Map<Event, String> eventHierarcyPathsByEvent;
    /** A list of wave data objects (excluding audio data). */
    //private List<AudioData> audioDataList;

    /**
     * A hashtable of wave data objects by reference address, ie
     * bankName/wave.
     */
    private Map<String, AudioData> wavesByRelativePathString;
    /**
     * A hashtable of first wave index-bank size pairs by wave bank.
     */
    //private Map<WaveBank, IndexRange> waveBankRangesByWaveBank;
    /** */
    //private ProjectDataXMLSerializer xmlDeserializer;
    
    /** */
    private static final int RANDOM = 0;
    private static final int RANDOM_NO_REPEAT = 1;
    /** */
    private static final int SEQUENTIAL = 2;
    private static final int SEQUENTIAL_NO_RESET = 3;
    private static final int IN_RANDOM_OUT = 4;
    private static final int IN_RANDOM_NO_REPEAT_OUT = 5;
    /** */
    private static final int IN_SEQUENTIAL_OUT = 6;
    /** */
    private static final int RETRIGGER = 0;
    /** */
    private static final int NO_RETRIGGER = 1;

    /**
     * Constructor.
     */
    public EngineDataBuilder()
    {
        
        
    }

    public void buildEngineData(String projectPath,
                                String binPath)
        throws IOException, JAXBException, ProjectDataException
    {

        //open and validate the project file to build.
        File projectFile = new File(projectPath);
        if (!projectFile.exists())
        {
            throw new IllegalArgumentException("Input file " + projectPath + " does not exist.");
        }
        else if (projectFile.isDirectory())
        {
            throw new IllegalArgumentException("Input file " + projectPath + " is a directory.");
        }
        File projectDir = projectFile.getParentFile();


        //append suffix if not already present
        if (!binPath.endsWith(ENGINE_DATA_FILE_SUFFIX))
        {
            binPath += ENGINE_DATA_FILE_SUFFIX;
        }

        projectDirectory = projectDir;
        
        //don't build if files are up to date
        File binFile = new File(binPath);

        if (binFile.exists())
        {
            if (binFile.lastModified() > projectFile.lastModified())
            {
                //the engine data file is up to date.
                log("Kowalski Engine data " + binFile + " is up to date.");
                log("");
                return;
            }
        }

        //deserialize and validate the project data
        KowalskiProject project = ProjectDataXMLSerializer.getInstance().deserializeKowalskiProject(projectPath);
        ProjectDataValidator.validateProjectDataForSerialization(project, projectFile);

        waveBanksByHierarchyPath = ProjectDataUtils.getWaveBanksByHierarchyPath(project);
        waveBankList = new ArrayList<WaveBank>();
        Iterator<String> it = waveBanksByHierarchyPath.keySet().iterator();
        while (it.hasNext())
        {
            waveBankList.add(waveBanksByHierarchyPath.get(it.next()));
        }

        eventHierarcyPathsByEvent = ProjectDataUtils.getEventHierarchyPathsByEvent(project);
        eventList = ProjectDataUtils.getEventList(project);

        //prepare sound data
        soundList = ProjectDataUtils.getSoundList(project);
        //adds one sound to the sound list per event referencing a piece of PCM audio data.
        createAdditionalSounds(project);
        soundsBySoundHierarchyPath = ProjectDataUtils.getSoundsBySoundHierarchyPath(project);

        

        wavesByRelativePathString = ProjectDataUtils.getAudioDataByRelativePath(project);

        //prepare mix bus data
        mixBusList = ProjectDataUtils.getMixBusList(project);

        //prepare mix preset data
        getHierarchyPathsByMixPreset = ProjectDataUtils.getMixPresetHierarchyPathsByMixPreset(project);

        //do binary serialization
        try
        {
            binFile.createNewFile();
        }
        catch (IOException e)
        {
            throw new IOException("Error creating file " + binPath + ": " + e.getMessage());
        }

        log("Writing Kowalski engine data binary");
        log("to " + binFile);

        FileOutputStream fos = new FileOutputStream(binFile);
        DataOutputStream binaryFileOutputStream = new DataOutputStream(fos);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream tempOutputStream = new DataOutputStream(byteStream);
        log("    Wrote file identifier, " + ENGINE_DATA_FILE_IDENTIFIER.length + " bytes.");
        binaryFileOutputStream.write(ENGINE_DATA_FILE_IDENTIFIER);

        serializeWaveBankData(project, tempOutputStream);
        binaryFileOutputStream.writeInt(WAVE_BANKS_CHUNK_ID);
        binaryFileOutputStream.writeInt(byteStream.size());
        log("    Wrote wave bank chunk, " + byteStream.size() + " bytes.");
        binaryFileOutputStream.write(byteStream.toByteArray());
        byteStream.reset();

        serializeMixBusData(tempOutputStream);
        binaryFileOutputStream.writeInt(MIX_BUSES_CHUNK_ID);
        binaryFileOutputStream.writeInt(byteStream.size());
        binaryFileOutputStream.write(byteStream.toByteArray());
        log("    Wrote mix bus chunk, " + byteStream.size() + " bytes.");
        byteStream.reset();

        serializeMixPresetData(tempOutputStream);
        binaryFileOutputStream.writeInt(MIX_PRESETS_CHUNK_ID);
        binaryFileOutputStream.writeInt(byteStream.size());
        binaryFileOutputStream.write(byteStream.toByteArray());
        log("    Wrote mix preset chunk, " + byteStream.size() + " bytes.");
        byteStream.reset();

        serializeSoundData(tempOutputStream, project);
        binaryFileOutputStream.writeInt(SOUNDS_CHUNK_ID);
        binaryFileOutputStream.writeInt(byteStream.size());
        binaryFileOutputStream.write(byteStream.toByteArray());
        log("    Wrote sounds chunk, " + byteStream.size() + " bytes.");
        byteStream.reset();

        serializeEventData(project, tempOutputStream);
        binaryFileOutputStream.writeInt(EVENTS_CHUNK_ID);
        //System.out.println(byteStream.size());
        binaryFileOutputStream.writeInt(byteStream.size());
        binaryFileOutputStream.write(byteStream.toByteArray());
        log("    Wrote events chunk, " + byteStream.size() + " bytes.");
        byteStream.reset();

        log("    Total size: " + binaryFileOutputStream.size() + " bytes.");
        log("");
        tempOutputStream.close();
    }

    /**
     * Creates sounds for all events referencing PCM audio data directly.
     * @param project
     * @throws ProjectDataException
     */
    private void createAdditionalSounds(KowalskiProject project)
            throws ProjectDataException
    {
        additionalSoundsByEvent = new HashMap<Event, Sound>();

        for (int i = 0; i < eventList.size(); i++)
        {
            Event eventi = eventList.get(i);

            if (eventi.getAudioDataReferenceOrSoundReference() instanceof AudioDataReference)
            {
                AudioDataReference ref = (AudioDataReference)eventi.getAudioDataReferenceOrSoundReference();
                AudioData audioData = ProjectDataUtils.resolveAudioDataReference(project, ref);
                AudioFileDescription format =
                        AudioFileParser.getAudioFileFormat(ProjectDataUtils.getAudioDataFile(project, projectDirectory, audioData));

                if (format.getEncoding().equals(AudioFileDescription.Encoding.PCM))
                {
                    Sound sound = new Sound();
                    sound.setGain(1.0f);
                    sound.setPitch(1.0f);
                    sound.setPitchVariationPercent(0.0f);
                    sound.setGainVariationPercent(0.0f);
                    sound.setPlaybackCount(1);
                    sound.setPlaybackMode(SoundPlaybackMode.SEQUENTIAL);
                    sound.getAudioDataReferences().add(ref);

                    soundList.add(sound);
                    additionalSoundsByEvent.put(eventi, sound);
                }
                else
                {
                    nonPCMEvents.add(eventi);
                }
            }
        }
    }

    /**
     * Converts a specified Kowalski project xml file to a binary format for
     * use in the Kowalski runtime.
     * @param xmlPath
     * @param binPath
     * @throws IOException
     * @throws JAXBException
     */
    /*
    public void buildEngineData(String xmlPath, String binPath)
        throws IOException, JAXBException, ProjectDataException
    {
        //Deserialize xml data
        FileInputStream fis = new FileInputStream(xmlPath);
        KowalskiProject project = xmlDeserializer.deserializeKowalskiProject(fis);

        
        File xmlFile = new File(xmlPath);
        buildEngineData(project, xmlFile.getParentFile(), binPath);
    }*/

    private void serializeMixPresetData(DataOutputStream dos)
            throws IOException
    {
        final int numMixPresets = getHierarchyPathsByMixPreset.size();
        //write the number of mix presets
        dos.writeInt(numMixPresets);
        final int numParamSetsPerPreset = mixBusList.size();


        Iterator<MixPreset> it = getHierarchyPathsByMixPreset.keySet().iterator();
        while (it.hasNext())
        {
            MixPreset preseti = it.next();
            String presetID = getHierarchyPathsByMixPreset.get(preseti);
            //write mix preset id
            dos.writeInt(presetID.length());
            dos.write(presetID.getBytes());

            //write default flag
            dos.writeInt(preseti.isDefault() ? 1 : 0);

            List<MixBusParameters> parameterSets =
                    preseti.getMixBusParameterList();
            final int numParameterSets = parameterSets.size();

            if (numParamSetsPerPreset != numParameterSets)
            {
                throw new RuntimeException();
            }

            for (int j = 0; j < numParameterSets; j++)
            {
                MixBusParameters paramsj = parameterSets.get(j);
                final int mixBusIndex = getMixBusIndex(paramsj.getMixBus());
                //write mix bus index
                dos.writeInt(mixBusIndex);
                
                //write gain
                dos.writeFloat(paramsj.getLeftGain());
                dos.writeFloat(paramsj.getRightGain());

                //write pitch
                dos.writeFloat(paramsj.getPitch());
            }
        }
    }

    private int getMixBusIndex(String busID)
    {
        final int numMixBuses = mixBusList.size();

        for (int i = 0; i < numMixBuses; i++)
        {
            MixBus busi = mixBusList.get(i);
            if (busi.getID().equals(busID))
            {
                return i;
            }
        }

        throw new RuntimeException("no mix bus with ID '" + busID + "' found");
    }

    private int[] getWaveBankAndAudioDataIndex(AudioDataReference ref)
    {
        String waveBankId = ref.getWaveBank();
        String fileName = ref.getRelativePath();

        if (!waveBanksByHierarchyPath.containsKey(waveBankId))
        {
            throw new RuntimeException();
        }

        WaveBank wb = waveBanksByHierarchyPath.get(waveBankId);
        if (!waveBankList.contains(wb))
        {
            throw new RuntimeException();
        }
        int waveBankIndex = waveBankList.indexOf(wb);

        int audioDataIndex = -1;
        List<AudioData> items = wb.getAudioDataList();
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).getRelativePath().equals(fileName))
            {
                audioDataIndex = i;
                break;
            }
        }
        
        if (audioDataIndex < 0)
        {
            throw new RuntimeException();
        }
        
        return new int[] {waveBankIndex, audioDataIndex};




    }

    /**
     * 
     * @param dos
     * @throws IOException
     */
    private void serializeMixBusData(DataOutputStream dos)
            throws IOException
    {
        final int numMixBuses = mixBusList.size();

        dos.writeInt(numMixBuses);
        
        for (int i = 0; i < numMixBuses; i++)
        {
            MixBus busi = mixBusList.get(i);
            dos.writeInt(busi.getID().length());
            dos.write(busi.getID().getBytes());

            List<MixBus> subBuses = busi.getSubBuses();
            final int numSubBuses = subBuses.size();
            dos.writeInt(numSubBuses);
            for (int j = 0; j < numSubBuses; j++)
            {
                MixBus subBusj = subBuses.get(j);
                final int subBusIndex = mixBusList.indexOf(subBusj);
                dos.writeInt(subBusIndex);
            }
        }
    }

    /**
     * 
     * @param dos
     * @throws IOException
     */
    private void serializeSoundData(DataOutputStream dos, KowalskiProject project)
                                    throws IOException, ProjectDataException
    {
        Map<String, WaveBank> waveBanksByPath = ProjectDataUtils.getWaveBanksByHierarchyPath(project);
        //write the number of sound definitions
        final int numSounds = soundList.size();
        ensureNonNegativeInt(numSounds);
        dos.writeInt(numSounds);

        //write the sound definitions
        for (int i = 0; i < numSounds; i++)
        {
            final Sound soundi = soundList.get(i);
            
            dos.writeInt(soundi.getPlaybackCount());
            dos.writeInt(soundi.isDeferStop() ? 1 : 0);
            dos.writeFloat(soundi.getGain());
            dos.writeFloat(soundi.getGainVariationPercent());
            dos.writeFloat(soundi.getPitch());
            dos.writeFloat(soundi.getPitchVariationPercent());
            final String playbackMode = soundi.getPlaybackMode().value();
            
            if (playbackMode.equals(SoundPlaybackMode.SEQUENTIAL.value()))
            {
                dos.writeInt(SEQUENTIAL);
            }
            else if (playbackMode.equals(SoundPlaybackMode.SEQUENTIAL_NO_RESET.value()))
            {
                dos.writeInt(SEQUENTIAL_NO_RESET);
            }
            else if (playbackMode.equals(SoundPlaybackMode.RANDOM.value()))
            {
                dos.writeInt(RANDOM);
            }
            else if (playbackMode.equals(SoundPlaybackMode.RANDOM_NO_REPEAT.value()))
            {
                dos.writeInt(RANDOM_NO_REPEAT);
            }
            else if (playbackMode.equals(SoundPlaybackMode.IN_SEQUENTIAL_OUT.value()))
            {
                dos.writeInt(IN_SEQUENTIAL_OUT);
            }
            else if (playbackMode.equals(SoundPlaybackMode.IN_RANDOM_OUT.value()))
            {
                dos.writeInt(IN_RANDOM_OUT);
            }
            else if (playbackMode.equals(SoundPlaybackMode.IN_RANDOM_NO_REPEAT_OUT.value()))
            {
                dos.writeInt(IN_RANDOM_NO_REPEAT_OUT);
            }
            else
            {
                throw new IllegalArgumentException("unknown playback mode");
            }

            final List<AudioDataReference> audioDataReferences =
                    soundi.getAudioDataReferences();
            final int numWaveReferences = audioDataReferences.size();
            dos.writeInt(numWaveReferences);
            
            for (int j = 0; j < numWaveReferences; j++)
            {
               final AudioDataReference audioReferencej = audioDataReferences.get(j);
               String waveBankId = audioReferencej.getWaveBank();
               if (!waveBanksByPath.containsKey(waveBankId))
               {
                   throw new RuntimeException();
               }

               //if this is a sound defined in project data (as opposed to a temporary sound
               //created to handle events referencing audio data directly)
               if (soundi.getID() != null)
               {
                   AudioData referencedAudioData = ProjectDataUtils.resolveAudioDataReference(project, audioReferencej);

                   if (referencedAudioData.isStreamFromDisk())
                   {
                        throw new ProjectDataException("Sound " + soundi.getID() + " contains reference " +
                                "to audio data " + referencedAudioData.getRelativePath() + " with stream from disk flag set.");
                   }
               }

               int indices[] = getWaveBankAndAudioDataIndex(audioReferencej);

               final int waveBankIndex = indices[0];
               ensureNonNegativeInt(waveBankIndex);
               dos.writeInt(waveBankIndex);

               final int audioDataIndex = indices[1];
               ensureNonNegativeInt(audioDataIndex);
               dos.writeInt(audioDataIndex);
            }
        }
    }

    /**
     * 
     * @param dos
     * @throws IOException
     */
    private void serializeEventData(KowalskiProject project, DataOutputStream dos)
            throws IOException, ProjectDataException
    {
        //The serialization of event data works as follows:
        //All events referencing PCM audio data (as opposed to sounds) in the project get the
        //audio data reference replaced by a simple sound in the engine data,
        //to make all PCM events work in the same way in the engine.
        //Streaming events, ie events using non-PCM data, reference the audio
        //data directly in the engine, just like in the project data.
        final int numEventsDefinitions = eventList.size();

        //write the total number of event definitions
        dos.writeInt(numEventsDefinitions);

        //write the event definitions
        for (int i = 0; i < numEventsDefinitions; i++)
        {
            final Event eventi = eventList.get(i);
            String eventHierarchyPath = eventHierarcyPathsByEvent.get(eventi);

            final String mixBus = eventi.getBus();
            final int numMixBuses = mixBusList.size();
            int mixBusIndex = -1;
            for (int j = 0; j < numMixBuses; j++)
            {
                if (mixBusList.get(j).getID().equals(mixBus))
                {
                    mixBusIndex = j;
                    break;
                }
            }
            if (mixBusIndex < 0)
            {
                throw new RuntimeException("mix bus " + mixBus +
                        " referenced from event " + eventi.getID() +
                        " could not be found");
            }

            final boolean nonPCM = nonPCMEvents.contains(eventi);
            final boolean isPositional = eventi.isPositional();
            
            int instanceCount = 1; //Event only
            int soundReferenceIndex = -1; //Event only
            int audioDataIndex = -1; //streaming events only
            int waveBankIndex = -1; //streaming events only
            int loopIfStreaming = 0; //Streaming events only
            int retriggerMode = getEventRetriggerModeInt(eventi.getRetriggerMode());
            
            AudioData audioData = null;
            AudioDataReference audioDataRef = null;

            if (eventi.getAudioDataReferenceOrSoundReference() instanceof AudioDataReference)
            {
                audioDataRef =
                    ((AudioDataReference)eventi.getAudioDataReferenceOrSoundReference());
                //if (audioDataRef != null)
                {
                    loopIfStreaming = audioDataRef.isLoop() ? 1 : 0;
                    audioData = ProjectDataUtils.resolveAudioDataReference(project, audioDataRef);
                }
            }

            boolean streamFromDisk = audioData == null ? false : audioData.isStreamFromDisk();

            if (nonPCM || streamFromDisk)
            {
                int[] indices = getWaveBankAndAudioDataIndex(audioDataRef);
                waveBankIndex = indices[0];
                audioDataIndex = indices[1];
            }
            else
            {
                Event se = (Event)eventi;
                instanceCount = se.getInstanceCount();
                Object ref = se.getAudioDataReferenceOrSoundReference();

                Sound referencedSound = null;
                if (additionalSoundsByEvent.containsKey(eventi))
                {
                    referencedSound = additionalSoundsByEvent.get(eventi);
                }
                else
                {
                    referencedSound = soundsBySoundHierarchyPath.get(((SoundReference)ref).getReferenceString());
                }
                 
                if (referencedSound == null)
                {
                    throw new RuntimeException();
                }
                final int soundIndex = soundList.indexOf(referencedSound);
                if (soundIndex < 0)
                {
                    throw new RuntimeException();
                }
                soundReferenceIndex = soundIndex;
            }

            List<WaveBank> referencedWaveBanks =
                    ProjectDataUtils.getWaveBanksReferencedByEvent(project, eventi);
            int numReferencedWaveBanks = referencedWaveBanks.size();
            
            //write the data for this event definition
            dos.writeInt(eventHierarchyPath.length());
            dos.write(eventHierarchyPath.getBytes());
            dos.writeInt(instanceCount);
            
            dos.writeFloat(eventi.getGain());
            dos.writeFloat(eventi.getPitch());
            dos.writeFloat(eventi.getInnerConeAngle());
            dos.writeFloat(eventi.getOuterConeAngle());
            dos.writeFloat(eventi.getOuterConeGain());
            dos.writeInt(mixBusIndex);
            dos.writeInt(isPositional ? 1 : 0);
            //these two fields only apply to sound-based events
            dos.writeInt(soundReferenceIndex);
            dos.writeInt(retriggerMode);
            //these three fields only apply to streaming events
            dos.writeInt(waveBankIndex);
            dos.writeInt(audioDataIndex);
            dos.writeInt(loopIfStreaming);
            
            
            dos.writeInt(numReferencedWaveBanks);
            for (int j = 0; j < numReferencedWaveBanks; j++)
            {
                dos.writeInt(waveBankList.indexOf(referencedWaveBanks.get(j)));
            }
        }
    }

    private int getEventRetriggerModeInt(EventRetriggerMode mode)
    {
        final String retriggerMode = mode.value();
        int retriggerModeInt = -1;
        if (retriggerMode.equals(EventRetriggerMode.RETRIGGER.value()))
        {
            retriggerModeInt = RETRIGGER;
        }
        else if (retriggerMode.equals(EventRetriggerMode.NO_RETRIGGER.value()))
        {
            retriggerModeInt = NO_RETRIGGER;
        }
        else
        {
            throw new RuntimeException("Invalid retrigger mode: " + mode);
        }

        return retriggerModeInt;
    }

    /**
     * Writes the wave bank data chunk.
     * @param project
     * @param dos
     * @throws IOException
     * @throws ProjectDataException
     */
    private void serializeWaveBankData(KowalskiProject project,
                                       DataOutputStream dos)
                                       throws IOException, ProjectDataException

    {
        List<AudioData> audioDataList = ProjectDataUtils.getAudioDataList(project);

        final int numAudioDataEntriesTotal = audioDataList.size();
        dos.writeInt(numAudioDataEntriesTotal);

        final int numWavebanks = waveBanksByHierarchyPath.size();
        dos.writeInt(numWavebanks);

        Iterator<String> it = waveBanksByHierarchyPath.keySet().iterator();
        while (it.hasNext())
        {
            String path = it.next();
            WaveBank waveBanki = waveBanksByHierarchyPath.get(path);
            dos.writeInt(path.length());
            dos.write(path.getBytes());

            List<AudioData> audioDataItems = waveBanki.getAudioDataList();
            dos.writeInt(audioDataItems.size());
            for (int i = 0; i < audioDataItems.size(); i++)
            {
                AudioData audioDatai = audioDataItems.get(i);
                dos.writeInt(audioDatai.getRelativePath().length());
                dos.write(audioDatai.getRelativePath().getBytes());
            }
        }
    }

    private void ensureNonNegativeInt(int i)
    {
        if (i < 0)
        {
            throw new RuntimeException();
        }
    }

    private void ensurePositiveInt(int i)
    {
        if (i < 1)
        {
            throw new RuntimeException();
        }
    }
}
