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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.xml.bind.JAXBException;
import kowalski.tools.data.xml.AudioData;
import kowalski.tools.data.xml.AudioDataReference;
import kowalski.tools.data.xml.Event;
import kowalski.tools.data.xml.EventGroup;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.MixBus;
import kowalski.tools.data.xml.MixBusParameters;
import kowalski.tools.data.xml.MixPreset;
import kowalski.tools.data.xml.Sound;
import kowalski.tools.data.xml.SoundGroup;
import kowalski.tools.data.xml.SoundReference;
import kowalski.tools.data.xml.WaveBank;

/**
 * This class handles validation of Kowalski project data.
 */
public abstract class ProjectDataValidator
{
    /**
     * Checks that all mix bus names are unique and that there is exactly one
     * bus with id 'master'.
     * @param project The project instance to check.
     * @param errorMessages A list to append any error messages to.
     */
    private static void validateMixBuses(KowalskiProject project, List<String> errorMessages)
    {
        List<MixBus> mixBusList = ProjectDataUtils.getMixBusList(project);

        final int numMixBuses = mixBusList.size();
        List<String> mixBusIds = new Vector<String>();
        for (int i = 0; i < numMixBuses; i++)
        {
            final String mixBusiId = mixBusList.get(i).getID();
            if (mixBusIds.contains(mixBusiId))
            {
                errorMessages.add("Mix bus ID '" + mixBusiId + "' is not unique.");
            }
            mixBusIds.add(mixBusiId);
        }
    }

    /**
     * Checks that all mix preset ids are unique, that there is exactly one
     * parameter set per bus per preset and that exactly one preset
     * is the default.
     * @param project The project to check.
     * @param errorMessages A list to append any error messages to.
     */
    private static final void validateMixPresets(KowalskiProject project, List<String> errorMessages)        
    {
        final List<MixBus> mixBusList = ProjectDataUtils.getMixBusList(project);
        final int numMixBuses = mixBusList.size();
        
        final List<String> mixPresetNames = new ArrayList<String>();
        final List<MixPreset> mixPresetList = ProjectDataUtils.getMixPresetList(project);
        final int numMixPresets = mixPresetList.size();
        
        boolean foundDefault = false;

        for (int i = 0; i < numMixPresets; i++)
        {
            final MixPreset mixPreseti = mixPresetList.get(i);

            if (mixPreseti.isDefault())
            {
                if (foundDefault)
                {
                    errorMessages.add("Multiple default mix presets found.");
                }
                foundDefault = true;
            }
            final String mixPresetId = mixPreseti.getID();

            if (mixPresetNames.contains(mixPresetId))
            {
                errorMessages.add("Mix preset id '" + mixPresetId + "' is not unique.");
            }
            mixPresetNames.add(mixPresetId);

            final List<MixBusParameters> parametersList =
                    mixPreseti.getMixBusParameterList();
            final int numMixBusParameterSets = parametersList.size();
            if (numMixBusParameterSets != numMixBuses)
            {
                errorMessages.add("The number of parameter sets does not equal the " +
                                 "number of mix buses for mix preset '" + mixPresetId + "'.");
                continue;
            }

            //check that each bus is referenced exactly once
            final List<String> mixBusReferences = new Vector<String>();
            for (int j = 0; j < numMixBusParameterSets; j++)
            {
                final MixBusParameters paramsj = parametersList.get(j);
                final String mixBusId = paramsj.getMixBus();
                if (mixBusReferences.contains(mixBusId))
                {
                    errorMessages.add("Mix preset '" + mixPresetId + "'" +
                            " contains multiple parameter sets for mix bus '" + mixBusId + "'.");
                }
                mixBusReferences.add(mixBusId);
            }

            for (int j = 0; j < numMixBusParameterSets; j++)
            {
                final String mixBusIdj = mixBusList.get(j).getID();
                if (!mixBusReferences.contains(mixBusIdj))
                {
                    errorMessages.add("Mix preset '" + mixPresetId + "'" +
                            " does not contain a parameter set for mix bus '" + mixBusIdj + "'.");
                }
            }
        }

        if (!foundDefault)
        {
            errorMessages.add("No default mix preset found.");
        }
    }

    /**
     * Checks that all wave bank IDs are unique and that the wave banks do not contain
     * multiple references to the same audio file.
     * @param project The project instance to check.
     * @param errorMessages A list to append any error messages to.
     */
    private static void validateWaveBanks(KowalskiProject project, List<String> errorMessages, File optionalProjectDir)
    {
        List<WaveBank> waveBanks = project.getWaveBankRootGroup().getWaveBanks();
        List<String> waveBankIds = new ArrayList<String>();
        final int numWaveBanks = waveBanks.size();
        for (int i = 0; i < numWaveBanks; i++)
        {
            String bankID = waveBanks.get(i).getID();
            if (waveBankIds.contains(bankID))
            {
                errorMessages.add("Wave bank ID '" +
                           bankID + "' is not unique.");
            }
            waveBankIds.add(bankID);
        }

        for (int i = 0; i < numWaveBanks; i++)
        {
            List<AudioData> audioDataItems = waveBanks.get(i).getAudioDataList();
            List<String> paths = new ArrayList<String>();
            for (int j = 0; j < audioDataItems.size(); j++)
            {
                AudioData audioDataj = audioDataItems.get(j);
                String path = audioDataj.getRelativePath();
                if (paths.contains(path))
                {
                    errorMessages.add("Wave bank '" + waveBanks.get(i).getID() + "' contains " +
                                      "multiple references to audio file " + audioDataj.getRelativePath());
                }
                else
                {
                    if (optionalProjectDir != null)
                    {
                        try
                        {
                            File audioFile = ProjectDataUtils.getAudioDataFile(project, optionalProjectDir, audioDataj);
                        }
                        catch (ProjectDataException e)
                        {
                            errorMessages.add("Wave bank '" + waveBanks.get(i).getID() + "' references" +
                                      "non-existing file " + audioDataj.getRelativePath());
                        }
                    }

                    paths.add(path);
                }
            }
        }
    }

    /**
     * Checks the integrity of the sound data of a given project instance.
     * @param project The project instance to check.
     * @param errorMessages A list to append any error messages to.
     */
    private static void validateSounds(KowalskiProject project, List<String> errorMessages, File optionalProjectDir)
    {
        //check that all audio data references resolve to an audio data item in a wave bank.
        Map<Sound, String> soundsHierarchyPathsBySound = ProjectDataUtils.getSoundHierarchyPathsBySound(project);
        Iterator<Sound> keysetIterator = soundsHierarchyPathsBySound.keySet().iterator();
        while (keysetIterator.hasNext())
        {
            final Sound sound = keysetIterator.next();
            final String refString = soundsHierarchyPathsBySound.get(sound);

            //loop over the audio data references of the current sound
            List<AudioDataReference> audioDataRefs = sound.getAudioDataReferences();
            final int numAudioDataRefs = audioDataRefs.size();
            for (int j = 0; j < numAudioDataRefs; j++)
            {
                AudioDataReference audioDataRef = audioDataRefs.get(j);
                AudioData refedData = null;

                //try to resolve the audio data reference. generate an error message if it fails.
                try
                {
                    refedData = ProjectDataUtils.resolveAudioDataReference(project, audioDataRef);
                }
                catch (ProjectDataException e)
                {
                    //TODO: report invalid wave bank ref separately.
                    errorMessages.add("The sound '" + refString + "' references audio data '" +
                             audioDataRef.getRelativePath() + "' which is not present in wave bank '" +
                             audioDataRef.getWaveBank() + "'.");
                    //errorMessages.add(e.toString());
                }

                //sounds must not reference audio data with stream from disk
                //flag set
                if (refedData.isStreamFromDisk())
                {
                    errorMessages.add("Sound '" + sound.getID() + "' references" +
                        " audio data '" + refedData.getRelativePath() + "' with stream from disk flag set.");
                }
                
                //sounds must not reference non-pcm audio files
                if (optionalProjectDir != null)
                {
                    File audioFile = null;

                    try
                    {
                        audioFile = ProjectDataUtils.getAudioDataFile(project, optionalProjectDir, refedData);
                        boolean isNonPCM = AudioFileParser.getPCMFormat(audioFile) == null;

                        if (isNonPCM)
                        {
                            //sounds must not reference non-pcm audio
                            errorMessages.add("Sound '" + sound.getID() + "' references" +
                                " non-PCM audio data '" + refedData.getRelativePath() + "'.");
                        }
                    }
                    catch (ProjectDataException e)
                    {
                        //audio file does not exist. output this error in wave bank validation
                    }
                }
            }
        }

        //check that all sounds have a unique path
        checkPaths(soundsHierarchyPathsBySound.values().iterator(), "sound", errorMessages);

        //check that all sound groups are unique in the scope of the parent
        validateSoundGroups(project, project.getSoundRootGroup().getSubGroups(), "", errorMessages);
    }

    /**
     * Recursively checks that all sound group IDs are unique in the scope of the
     * parent group.
     * @param project
     * @param currentChildren
     * @param currentPath
     * @param errorMessages
     */
    private static void validateSoundGroups(KowalskiProject project,
                                                  List<SoundGroup> currentChildren,
                                                  String currentPath,
                                                  List<String> errorMessages)
    {
        final int numGroups = currentChildren.size();

        //verify unique IDs
        List<String> ids = new ArrayList<String>();
        for (int i = 0; i < numGroups; i++)
        {
            String currID = currentChildren.get(i).getID();
            if (ids.contains(currID))
            {
                if (currentPath.length() == 0)
                {
                    errorMessages.add("The sound group ID '" + currID + "' is not unique in " +
                            "the scope of the sound data top node.");
                }
                else
                {
                    errorMessages.add("The sound group ID '" + currID + "' is not unique in " +
                            "the scope of sound group '" + currentPath.substring(1) + "'.");
                }
            }
            else
            {
                ids.add(currID);
            }
        }

        //traverse
        for (int i = 0; i < numGroups; i++)
        {
            SoundGroup groupi = currentChildren.get(i);
            validateSoundGroups(project, groupi.getSubGroups(), currentPath + "/" + groupi.getID(), errorMessages);
        }
    }

    /**
     * Checks the integrity of the event data of a given project instance.
     * @param project The project instance to check.
     * @param errorMessages A list to append any error messages to.
     */
    private static void validateEvents(KowalskiProject project, List<String> errorMessages)
    {
        List<Event> events = ProjectDataUtils.getEventList(project);
        Map<Event, String> pathsByEvent =
                ProjectDataUtils.getEventHierarchyPathsByEvent(project);
        
        //check that all events reference an existing mix bus
        final int numEvents = events.size();
        List<MixBus> mixBuses = ProjectDataUtils.getMixBusList(project);
        for (int i = 0; i < numEvents; i++)
        {
            Event eventi = events.get(i);
            MixBus referencedBus = null;
            for (int j = 0; j < mixBuses.size(); j++)
            {
                if (eventi.getBus().equals(mixBuses.get(j).getID()))
                {
                    referencedBus = mixBuses.get(j);
                }
            }
            if (referencedBus == null)
            {
                errorMessages.add("The event '" + pathsByEvent.get(eventi) +
                        "' references a mix bus with ID '" + eventi.getBus() +
                        "' that does not exist.");
            }

            if (eventi instanceof Event)
            {
                Event event = (Event)eventi;
                Object obj = event.getAudioDataReferenceOrSoundReference();
                if (obj == null)
                {
                    //this is allowed. do nothing
                }
                else if (obj instanceof SoundReference)
                {
                    SoundReference soundRef = (SoundReference)obj;
                    try
                    {
                        ProjectDataUtils.resolveSoundReference(project, soundRef);
                    }
                    catch (ProjectDataException e)
                    {
                        System.out.println(e);
                        errorMessages.add("The event '" + pathsByEvent.get(event) +
                                "' contains the invalid sound reference '" +
                                soundRef.getReferenceString() + "'");
                    }
                }
                else
                {
                    AudioDataReference audioRef = (AudioDataReference)obj;
                    try
                    {
                        ProjectDataUtils.resolveAudioDataReference(project, audioRef);
                    }
                    catch (ProjectDataException e)
                    {
                        errorMessages.add("The streaming event '" + pathsByEvent.get(event) +
                                "' contains an invalid audio data reference.");
                    }
                }
            }
        }

        //check uniqueness of event and event group IDs.
        checkPaths(pathsByEvent.values().iterator(), "event", errorMessages);
        validateEventGroups(project, project.getEventRootGroup().getSubGroups(), "", errorMessages);
    }

    /**
     * Recursively checks that all sound group IDs are unique in the scope of the
     * parent group.
     * @param project
     * @param currentChildren
     * @param currentPath
     * @param errorMessages
     */
    private static void validateEventGroups(KowalskiProject project,
                                                  List<EventGroup> currentChildren,
                                                  String currentPath,
                                                  List<String> errorMessages)
    {
        final int numGroups = currentChildren.size();

        //verify unique IDs
        List<String> ids = new ArrayList<String>();
        for (int i = 0; i < numGroups; i++)
        {
            String currID = currentChildren.get(i).getID();
            if (ids.contains(currID))
            {
                if (currentPath.length() == 0)
                {
                    errorMessages.add("The event group ID '" + currID + "' is not unique in " +
                            "the scope of the event data top node.");
                }
                else
                {
                    errorMessages.add("The event group ID '" + currID + "' is not unique in " +
                            "the scope of event group '" + currentPath.substring(1) + "'.");
                }
            }
            else
            {
                ids.add(currID);
            }
        }

        //traverse
        for (int i = 0; i < numGroups; i++)
        {
            EventGroup groupi = currentChildren.get(i);
            validateEventGroups(project, groupi.getSubGroups(), currentPath + "/" + groupi.getID(), errorMessages);
        }
    }

    /**
     * Check that a set of paths of the form a/b/c.. define a hierarchy
     * where the leaf nodes have unique IDs in the scope of the containing
     * node.
     * @param paths
     * @param entityName
     * @param errorMessages
     */
    private static void checkPaths(Iterator<String> paths, String entityName, List<String> errorMessages)
    {
        List<String> encounteredPaths = new ArrayList<String>();
        List<String> duplicates = new ArrayList<String>();
        
        while (paths.hasNext())
        {
            String currPath = paths.next();
            if (encounteredPaths.contains(currPath))
            {
                if (!duplicates.contains(currPath))
                {
                    duplicates.add(currPath);
                    errorMessages.add("The ID of the " + entityName + " '" + currPath + "' is not " +
                            "unique in the scope of the containing " + entityName + " group.");
                }
            }
            else
            {
                encounteredPaths.add(currPath);
            }

            //TODO: check group ID uniqueness.
        }
    }

    /**
     * Checks that a KowalskiProject instance is valid for binary serialization.
     * This validation is stricter than that performed by validateProjectData. For
     * example, the number of events is not allowed to be zero.
     * @see validateProjectData
     */
    public static void validateProjectDataForSerialization(KowalskiProject project, File projectFile)
            throws ProjectDataException, JAXBException
    {
        List<String> errorMessages = new ArrayList<String>();
        //do usual validation...
        try
        {
            File projectDir = projectFile.getParentFile();
            if (!projectDir.isDirectory())
            {
                throw new IllegalArgumentException("projectDir must be a directory");
            }

            validateProjectData(project, projectDir);
        }
        catch (ProjectDataException e)
        {
            errorMessages = e.getMessages();
        }

        //... plus some extra stuff
        List<Event> eventList = ProjectDataUtils.getEventList(project);
        if (eventList.size() == 0)
        {
            errorMessages.add("Project with no events cannot be serialized.");
        }

        List<WaveBank> waveBankList = ProjectDataUtils.getWaveBankList(project);
        if (waveBankList.size() == 0)
        {
            errorMessages.add("Project with no wave banks cannot be serialized.");
        }

        if (errorMessages.size() > 0)
        {
            throw new ProjectDataException(errorMessages);
        }
    }

    private static void validateProjectData(KowalskiProject project, File optionalProjectDir)
            throws ProjectDataException, JAXBException
    {
        //validate against XSD schema
        ProjectDataXMLSerializer xmlSerializer = ProjectDataXMLSerializer.getInstance();
        xmlSerializer.validateKowalskiProject(project);

        //check additional data structure constraints
        List<String> errorMessages = new ArrayList<String>();
        validateMixBuses(project, errorMessages);
        validateMixPresets(project, errorMessages);
        validateWaveBanks(project, errorMessages, optionalProjectDir);
        validateSounds(project, errorMessages, optionalProjectDir);
        validateEvents(project, errorMessages);

        if (errorMessages.size() > 0)
        {
            throw new ProjectDataException("Error validating project data", errorMessages);
        }
    }

    /**
     * Checks that a KowalskiProject instance is valid with respect to the xml schema and
     * some additional structure rules.
     * @param project The project instance to check.
     * @throws ProjectDataException If the project instance violates any of the structure constraints
     * not described by the XSD schema (e.g non-unique wave bank IDs).
     * @throws JAXBException If the project instance violates any of the structure constraints
     * defined by the XSD schema.
     */
    public static void validateProjectData(KowalskiProject project)
            throws ProjectDataException, JAXBException

    {
        validateProjectData(project, null);
    }
}
