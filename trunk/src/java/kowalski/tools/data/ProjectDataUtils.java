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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import kowalski.tools.data.xml.AudioData;
import kowalski.tools.data.xml.AudioDataReference;
import kowalski.tools.data.xml.Event;
import kowalski.tools.data.xml.EventGroup;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.MixBus;
import kowalski.tools.data.xml.MixBusParameters;
import kowalski.tools.data.xml.MixPreset;
import kowalski.tools.data.xml.MixPresetGroup;
import kowalski.tools.data.xml.NodeWithIDAndComments;
import kowalski.tools.data.xml.ObjectFactory;
import kowalski.tools.data.xml.Sound;
import kowalski.tools.data.xml.SoundGroup;
import kowalski.tools.data.xml.SoundPlaybackMode;
import kowalski.tools.data.xml.SoundReference;
import kowalski.tools.data.xml.WaveBank;
import kowalski.tools.data.xml.WaveBankGroup;


/**
 * A utility class for performing various operations and queries on
 * KowalskiProject instances.
 */
public abstract class ProjectDataUtils
{

    /**
     * A Comparator used to sort nodes by ID.
     */
    private static class NodeWithIDComparator implements Comparator<NodeWithIDAndComments>
    {
        @Override
        public int compare(NodeWithIDAndComments o1, NodeWithIDAndComments o2)
        {
            return o1.getID().compareTo(o2.getID());
        }
    }

    /**
     * A Comparator used to sort audio data items by path.
     */
    private static class AudioDataComparator implements Comparator<AudioData>
    {
        @Override
        public int compare(AudioData o1, AudioData o2)
        {
            return o1.getRelativePath().compareTo(o2.getRelativePath());
        }
    }

    private static NodeWithIDComparator nodeWithIDComparator = new NodeWithIDComparator();
    private static AudioDataComparator audioDataComparator = new AudioDataComparator();

    /**
     * Hidden constructor.
     */
    private ProjectDataUtils()
    {
        //nothing
    }

    /**
     * Returns a list of all groups (EventGroup, SoundGroup, MixPresetGroup, MixBus
     * or WaveBankGroup depending on the type of parentGroup) having a given
     * group as their parent.
     * @param parentGroup
     * @return
     */
    private static List<NodeWithIDAndComments> getChildGroups(Object parentGroup)
    {
        if (parentGroup == null)
        {
            throw new IllegalArgumentException();
        }

        List<NodeWithIDAndComments> childGroups =
                new ArrayList<NodeWithIDAndComments>();
        if (parentGroup instanceof EventGroup)
        {
            EventGroup eg = (EventGroup)parentGroup;
            for (int i = 0; i < eg.getSubGroups().size(); i++)
            {
                childGroups.add(eg.getSubGroups().get(i));
            }
        }
        else if (parentGroup instanceof MixBus)
        {
            MixBus eg = (MixBus)parentGroup;
            for (int i = 0; i < eg.getSubBuses().size(); i++)
            {
                childGroups.add(eg.getSubBuses().get(i));
            }
        }
        else if (parentGroup instanceof SoundGroup)
        {
            SoundGroup sg = (SoundGroup)parentGroup;
            for (int i = 0; i < sg.getSubGroups().size(); i++)
            {
                childGroups.add(sg.getSubGroups().get(i));
            }
        }
        else if (parentGroup instanceof MixPresetGroup)
        {
            MixPresetGroup mpg = (MixPresetGroup)parentGroup;
            for (int i = 0; i < mpg.getSubGroups().size(); i++)
            {
                childGroups.add(mpg.getSubGroups().get(i));
            }
        }
        else if (parentGroup instanceof WaveBankGroup)
        {
            WaveBankGroup wbg = (WaveBankGroup)parentGroup;
            for (int i = 0; i < wbg.getSubGroups().size(); i++)
            {
                childGroups.add(wbg.getSubGroups().get(i));
            }
        }

        return childGroups;
    }

    /**
     * Returns a list of all nodes (Event, Sound, MixPreset
     * or WaveBank depending on the type of parentGroup) having a given
     * group as their parent.
     * @param parentGroup
     * @return
     */
    private static List<NodeWithIDAndComments> getChildNodes(Object parentGroup)
    {
        if (parentGroup == null)
        {
            throw new IllegalArgumentException();
        }
        
        List<NodeWithIDAndComments> childNodes =
                new ArrayList<NodeWithIDAndComments>();
        if (parentGroup instanceof EventGroup)
        {
            EventGroup eg = (EventGroup)parentGroup;
            for (int i = 0; i < eg.getEvents().size(); i++)
            {
                childNodes.add(eg.getEvents().get(i));
            }
        }
        else if (parentGroup instanceof SoundGroup)
        {
            SoundGroup sg = (SoundGroup)parentGroup;
            for (int i = 0; i < sg.getSounds().size(); i++)
            {
                childNodes.add(sg.getSounds().get(i));
            }
        }
        else if (parentGroup instanceof MixPresetGroup)
        {
            MixPresetGroup mpg = (MixPresetGroup)parentGroup;
            for (int i = 0; i < mpg.getMixPresets().size(); i++)
            {
                childNodes.add(mpg.getMixPresets().get(i));
            }
        }
        else if (parentGroup instanceof WaveBankGroup)
        {
            WaveBankGroup wbg = (WaveBankGroup)parentGroup;
            for (int i = 0; i < wbg.getWaveBanks().size(); i++)
            {
                childNodes.add(wbg.getWaveBanks().get(i));
            }
        }
        return childNodes;
    }

    /**
     * Returns a list of all children of a given group.
     * @param topGroup
     * @return
     */
    private static List<NodeWithIDAndComments> getAllChildren(Object topGroup)
    {
        List<NodeWithIDAndComments> result = new ArrayList<NodeWithIDAndComments>();
        result.addAll(getChildGroups(topGroup));
        result.addAll(getChildNodes(topGroup));
        return result;
    }

    /**
     * Finds the node corresponding to a given path in a given group hierarchy.
     * Path elements are dilimited by "/".
     * @param parentGroup
     * @param path
     * @param entityName
     * @return
     */
    private static Object resolvePath(Object topGroup, String path, String entityName)
            throws ProjectDataException
    {
        //System.out.println("resolving reference " + path + " starting at " + topGroup);
        String[] referenceComponents = path.split("/");

        List<NodeWithIDAndComments> groups = getChildGroups(topGroup);
        List<NodeWithIDAndComments> nodes = getChildNodes(topGroup);

        NodeWithIDAndComments currentGroup = (NodeWithIDAndComments)topGroup;

        //traverse groups, assuming the last component of the reference
        //is not a group ID.
        for (int i = 0; i < referenceComponents.length - 1; i++)
        {
            final String componenti = referenceComponents[i];
            //find the subgroup to traverse
            final int numGroups = groups.size();
            boolean foundMatchingGroup = false;
            for (int j = 0; j < numGroups; j++)
            {
                NodeWithIDAndComments soundGroupj = groups.get(j);
                if (soundGroupj.getID().equals(componenti))
                {
                    //a mathing ID was found, keep traversing
                    foundMatchingGroup = true;
                    currentGroup = soundGroupj;
                    groups = getChildGroups(currentGroup);
                    break;
                }
            }

            if (!foundMatchingGroup)
            {
                throw new ProjectDataException("Error resolving reference '" + path +
                            "'. No group with ID '" + componenti + "' found.");
            }
        }

        String nodeID = referenceComponents[referenceComponents.length - 1];
        List<NodeWithIDAndComments> nodeList = getChildNodes(currentGroup);
        final int numChildNodes = nodeList.size();
        //System.out.println("num ch nodes = " + numChildNodes + " of " + currentGroup.getID());
        for (int i = 0; i < numChildNodes; i++)
        {
            NodeWithIDAndComments nodei = nodeList.get(i);
            //System.out.println("    id " + i + "=" + nodei.getID());
            if (nodei.getID().equals(nodeID))
            {
                return nodei;
            }
        }

        throw new ProjectDataException("The " + entityName + " reference '" +
                            path + "' is invalid, there is no " + entityName +
                              " with ID '" + nodeID + "'.");
    }

    /**
     * Returns a list of nodes representing the tree path from a given start node to
     * a given end node.
     * @param project
     * @param startGroup
     * @param nodeToFind
     * @return
     */
    public static List<Object> getTreePathTo(KowalskiProject project, Object startGroup, Object nodeToFind)
    {
        //System.out.println("Trying to find path from " +
        //        ((NodeWithIDAndComments)startGroup).getID() + " to " +
        //        ((NodeWithIDAndComments)nodeToFind).getID());
        List<Object> path = new ArrayList<Object>();
        findTreePath(path, (NodeWithIDAndComments)startGroup,
                 (NodeWithIDAndComments)nodeToFind, false);
        path.add(0, project);
        
        return path;
    }

    /**
     * Recursively finds a path between two nodes.
     * @param pathSoFar
     * @param currNode
     * @param nodeToFind
     * @param foundMatch
     * @return
     */
    private static boolean findTreePath(List<Object> pathSoFar, NodeWithIDAndComments currNode,
                                    NodeWithIDAndComments nodeToFind,
                                    boolean foundMatch)
    {
        if (foundMatch)
        {
            return true;
        }

        List<NodeWithIDAndComments> chGr = getChildGroups(currNode);
        List<NodeWithIDAndComments> chNo = getChildNodes(currNode);

        pathSoFar.add(currNode);
        //System.out.println("pushing " + currNode.getID());
        if (chNo.contains(nodeToFind) || chGr.contains(nodeToFind))
        {
            //System.out.println("pushing " + nodeToFind.getID());
            pathSoFar.add(nodeToFind);
            foundMatch = true;
        }

        for (int i = 0; i < chGr.size(); i++)
        {
            
            boolean b = findTreePath(pathSoFar, chGr.get(i), nodeToFind, foundMatch);
            if (b || foundMatch)
            {
                return true;
            }
            else
            {
                //System.out.println("popping " + chGr.get(i).getID() + ", " + foundMatch);
                pathSoFar.remove(pathSoFar.size() - 1);
            }
        }

        return foundMatch;
    }

    /**
     * Returns a string containing a "/" delimited path to a given node in a
     * given Kowalski project.
     * @param project
     * @param nodeToFind
     * @return
     */
    private static String getPathTo(KowalskiProject project, 
                                    NodeWithIDAndComments nodeToFind)
    {
        List<Object> pathObjects = getTreePathTo(project, getRootGroup(project, nodeToFind), nodeToFind);
        String path = "";
        for (int i = 2; i < pathObjects.size(); i++)//start at 2 to skip project and root
        {
            path += "/" + ((NodeWithIDAndComments)pathObjects.get(i)).getID();
        }

        return path.length() > 0 ? path.substring(1) : path; //skip leading '/'
    }
    


    /**
     * Does not include the ID of the top group in the path.
     * @param parentGroup
     * @return
     */
    private static void gatherHierarchyPathsByNode(Map<NodeWithIDAndComments, String> map,
                                                  NodeWithIDAndComments root,
                                                  String currentPath)
    {
        List<NodeWithIDAndComments> childGroups = getChildGroups(root);
        List<NodeWithIDAndComments> childNodes = getChildNodes(root);
        //System.out.println(childGroups.size() + " groups, " + childNodes.size() + " nodes.");
        for (int i = 0; i < childNodes.size(); i++)
        {
            NodeWithIDAndComments nodei = childNodes.get(i);
            String path = currentPath + "/" + nodei.getID();
            path = path.substring(1);//remove leading '/'
            //System.out.println("putting : " + path);
            map.put(nodei, path);
        }

        //traverse
        for (int i = 0; i < childGroups.size(); i++)
        {
            NodeWithIDAndComments groupi = childGroups.get(i);
            gatherHierarchyPathsByNode(map, groupi, currentPath + "/" + groupi.getID());
        }
    }

    /**
     *
     * @return
     */
    private static Map<NodeWithIDAndComments, String> getHierarchyPathsByNode(NodeWithIDAndComments root)

    {
        Map<NodeWithIDAndComments, String> map = new Hashtable<NodeWithIDAndComments, String>();
        gatherHierarchyPathsByNode(map, root, "");
        return map;
    }

    /**
     *
     * @return
     * @throws ProjectDataException if two or more nodes have the same path.
     */
    private static Map<String, NodeWithIDAndComments> getNodesByHierarchyPath(NodeWithIDAndComments root)
            throws ProjectDataException
    {
        Map<NodeWithIDAndComments, String> pathsByNode = getHierarchyPathsByNode(root);
        Map<String, NodeWithIDAndComments> nodesByPath = new Hashtable<String, NodeWithIDAndComments>();
        Iterator<NodeWithIDAndComments> it = pathsByNode.keySet().iterator();

        List<String> errorMessages = new ArrayList<String>();

        while (it.hasNext())
        {
            NodeWithIDAndComments node = it.next();
            String path = pathsByNode.get(node);

            if (nodesByPath.containsKey(path))
            {
                errorMessages.add(path + " is not unique");
            }
            nodesByPath.put(path, node);
        }

        if (errorMessages.size() > 0)
        {
            throw new ProjectDataException(errorMessages);
        }

        return nodesByPath;
    }

    private static void gatherNodeList(List<NodeWithIDAndComments> list, NodeWithIDAndComments root)
    {
        List<NodeWithIDAndComments> childGroups = getChildGroups(root);
        List<NodeWithIDAndComments> childNodes = getChildNodes(root);
        //System.out.println(childGroups.size() + " groups, " + childNodes.size() + " nodes.");
        for (int i = 0; i < childNodes.size(); i++)
        {
            NodeWithIDAndComments nodei = childNodes.get(i);

            if (list.contains(nodei))
            {
                throw new RuntimeException("node " + nodei.getID() + " already contained in list.");
            }
            //System.out.println("putting : " + path);
            list.add(nodei);
        }

        //traverse
        for (int i = 0; i < childGroups.size(); i++)
        {
            NodeWithIDAndComments groupi = childGroups.get(i);
            gatherNodeList(list, groupi);
        }
    }

    private static void gatherGroupList(List<NodeWithIDAndComments> list, NodeWithIDAndComments root)
    {
        List<NodeWithIDAndComments> childGroups = getChildGroups(root);

        list.add(root);
        //traverse
        for (int i = 0; i < childGroups.size(); i++)
        {
            NodeWithIDAndComments groupi = childGroups.get(i);
            gatherGroupList(list, groupi);
        }
    }
    
    /**
     * Creates a deep copy of a KowalskiProject instance by first marshaling
     * it and then unmarshaling it again.
     * @param project The project to be copied.
     * @return The deep copy.
     * @throws JAXBException
     */
    public static KowalskiProject cloneProject(KowalskiProject project)
            throws JAXBException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ProjectDataXMLSerializer.getInstance().serializeKowalskiProject(project, baos);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        KowalskiProject projectCopy = null;
        ProjectDataXMLSerializer.getInstance().deserializeKowalskiProject(bais);

        return projectCopy;
    }

    /**
     * Sorts instances of
     * @param project
     */
    public static void sortNodesWithID(KowalskiProject project)
    {
        sortNodesWithID(project.getEventRootGroup(), true);
        sortNodesWithID(project.getSoundRootGroup(), true);
        sortNodesWithID(project.getWaveBankRootGroup(), true);
        sortNodesWithID(project.getMixPresetRootGroup(), true);
    }

    /**
     * Sorts instances of
     * @param project
     */
    public static void sortAudioDataNodes(KowalskiProject project)
    {
        List<WaveBank> waveBanks = getWaveBankList(project);
        for (int i = 0; i < waveBanks.size(); i++)
        {
            List<AudioData> audioDataList = waveBanks.get(i).getAudioDataList();
            Collections.sort(audioDataList, audioDataComparator);
        }
    }

    /**
     * Returns the index at which a given node should be inserted to maintain
     * the lexicographical order of a given list.
     * @param nodes
     * @param nodeToInsert
     * @return
     */
    public static int getLexicographicalIndexOf(List<NodeWithIDAndComments> nodes,
                                                NodeWithIDAndComments nodeToInsert)
    {
        if (nodes.size() == 0)
        {
            return 0;
        }

        //sort the nodes (if they for some reason were not sorted
        Collections.sort(nodes, nodeWithIDComparator);

        for (int i = 0; i < nodes.size(); i++)
        {
            if (nodeToInsert.getID().compareTo(nodes.get(i).getID()) < 0)
            {
                return i;
            }
        }

        return nodes.size() ;
    }

    public static void sortNodesWithID(Object root, boolean recursive)
    {
        //sort children
        List<NodeWithIDAndComments> subGroups = getChildGroups(root);
        List<NodeWithIDAndComments> subNodes = getChildNodes(root);

        Collections.sort(subGroups, nodeWithIDComparator);
        Collections.sort(subNodes, nodeWithIDComparator);

        //TODO this could sure use some polymorphism....
        if (root instanceof SoundGroup)
        {
            SoundGroup sg = (SoundGroup)root;
            sg.getSubGroups().clear();
            sg.getSounds().clear();
            for (int i = 0; i < subGroups.size(); i++)
            {
                sg.getSubGroups().add((SoundGroup)subGroups.get(i));
            }
            for (int i = 0; i < subNodes.size(); i++)
            {
                sg.getSounds().add((Sound)subNodes.get(i));
            }
        }
        else if (root instanceof EventGroup)
        {
            EventGroup eg = (EventGroup)root;
            eg.getSubGroups().clear();
            eg.getEvents().clear();
            for (int i = 0; i < subGroups.size(); i++)
            {
                eg.getSubGroups().add((EventGroup)subGroups.get(i));
            }
            for (int i = 0; i < subNodes.size(); i++)
            {
                eg.getEvents().add((Event)subNodes.get(i));
            }
        }
        else if (root instanceof WaveBankGroup)
        {
            WaveBankGroup wbg = (WaveBankGroup)root;
            wbg.getSubGroups().clear();
            wbg.getWaveBanks().clear();
            for (int i = 0; i < subGroups.size(); i++)
            {
                wbg.getSubGroups().add((WaveBankGroup)subGroups.get(i));
            }
            for (int i = 0; i < subNodes.size(); i++)
            {
                wbg.getWaveBanks().add((WaveBank)subNodes.get(i));
            }
        }
        else if (root instanceof MixPresetGroup)
        {
            MixPresetGroup mpg = (MixPresetGroup)root;
            mpg.getSubGroups().clear();
            mpg.getMixPresets().clear();
            for (int i = 0; i < subGroups.size(); i++)
            {
                mpg.getSubGroups().add((MixPresetGroup)subGroups.get(i));
            }
            for (int i = 0; i < subNodes.size(); i++)
            {
                mpg.getMixPresets().add((MixPreset)subNodes.get(i));
            }
        }
        else
        {
            throw new IllegalArgumentException("Unknown group type:" + root);
        }


        //recurse if requested
        if (recursive)
        {
            for (int i = 0; i < subGroups.size(); i++)
            {
                sortNodesWithID(subGroups.get(i), recursive);
            }
        }
    }

    /**
     * Returns a list of Sound instances referencing a given AudioData instance.
     * @param project A KowalskiProject instance containing the data.
     * @param audioData The AudioData instance to look for references to.
     * @return A list of all Sound instances referencing audioData.
     */
    public static List<Sound> getSoundsReferencingAudioData(KowalskiProject project,
                                                            AudioData audioData)
    {
        List<Sound> projectSoundList = getSoundList(project);
        List<Sound> referencingSounds = new ArrayList<Sound>();
        final int numSounds = projectSoundList.size();
        for (int i = 0; i < numSounds; i++)
        {
            Sound soundi = projectSoundList.get(i);
            List<AudioDataReference> audioDataRefs = soundi.getAudioDataReferences();
            final int numAudioDataRefs = audioDataRefs.size();
            for (int j = 0; j < numAudioDataRefs; j++)
            {
                AudioDataReference audioDataRef = audioDataRefs.get(j);
                AudioData referencedData = null;
                try
                {
                    referencedData = resolveAudioDataReference(project, audioDataRef);
                }
                catch (ProjectDataException e)
                {
                    //TODO: something here?
                }

                if (referencedData == audioData)
                {
                    referencingSounds.add(soundi);
                    break;
                }
            }
        }

        return referencingSounds;
    }

    /**
     * Returns a list of all AudioData objects that are not referenced by
     * any Sound instance.
     * @param project A KowalskiProject instance containing the data.
     * @return A list of AudioData items that are not referenced by any Sound instance.
     */
    public static List<AudioData> getUnreferencedAudioDataList(KowalskiProject project)
    {

        List<Sound> soundList = getSoundList(project);
        Map<String, AudioData> audioDataByPath =
                getAudioDataByRelativePath(project);

        final int numSounds = soundList.size();
        for (int i = 0; i < numSounds; i++)
        {
            Sound soundi = soundList.get(i);
            List<AudioDataReference> audioDataRefs = soundi.getAudioDataReferences();
            final int numAudioDataRefs = audioDataRefs.size();
            for (int j = 0; j < numAudioDataRefs; j++)
            {
                AudioDataReference refj = audioDataRefs.get(j);
                if (audioDataByPath.containsKey(refj.getRelativePath()))
                {
                    audioDataByPath.remove(refj.getRelativePath());
                }
            }
        }

        //by now, audioDataByRef should only contain unreferenced audio data
        List<AudioData> unreferencedAudioDataList = 
                new ArrayList<AudioData>(audioDataByPath.values());

        return unreferencedAudioDataList;
    }

    /**
     * Returns a list of relative paths to all audio files that are
     * referenced by the project but cannot be found.
     * @param project A KowalskiProject instance containing the data.
     * @return A list of relative paths to non-existing audio files.
     */
    public static List<String> getRelativePathsOfNonExistingAudioFiles(KowalskiProject project,
                                                                       File projectDir)
    {
        Map<String, AudioData> audioDataByRef =
                getAudioDataByRelativePath(project);

        List<AudioData> audioDataList =
                new ArrayList<AudioData>(audioDataByRef.values());
        List<String> nonExistingFileNames = new ArrayList<String>();
        final int listSize = audioDataList.size();
        for (int i = 0; i < listSize; i++)
        {
            AudioData datai = audioDataList.get(i);
            try
            {
                getAudioDataFile(project, projectDir, datai);
            }
            catch (ProjectDataException e)
            {
                nonExistingFileNames.add(datai.getRelativePath());
            }
        }

        return nonExistingFileNames;
    }

    /**
     * Returns the Sound instance referenced by a given SoundReference instance.
     * @param project A KowalskiProject instance containing the data.
     * @param ref The SoundReference instance.
     * @return The Sound instance referenced by ref.
     * @throws ProjectDataException If the sound reference cannot be resolved.
     */
    
    public static Sound resolveSoundReference(KowalskiProject project,
                                              SoundReference ref)
        throws ProjectDataException
    {
        Object referencedSound = resolvePath(project.getSoundRootGroup(), ref.getReferenceString(), "sound");
        return (Sound)referencedSound;
    }


    /**
     * Returns the AudioData instance referenced by a given AudioDataReference instance.
     * @param project A KowalskiProject instance containing the data.
     * @param ref The AudioDataReference instance
     * @return The AudioData instance referenced by ref.
     * @throws ProjectDataException If the audio data reference cannot be resolved.
     */
    public static AudioData resolveAudioDataReference(KowalskiProject project,
                                                      AudioDataReference ref)
        throws ProjectDataException
    {
        //find the wave bank in the hierarchy
        WaveBank waveBank = (WaveBank)resolvePath(project.getWaveBankRootGroup(),
                                             ref.getWaveBank(), "wave bank");

        //find the audio data in the wavebank
        List<AudioData> audioDataList = waveBank.getAudioDataList();
        for (int i = 0; i < audioDataList.size(); i++)
        {
            AudioData audioData = audioDataList.get(i);
            if (audioData.getRelativePath().equals(ref.getRelativePath()))
            {
                return audioData;
            }
        }

        throw new ProjectDataException("Audio data '" + ref.getRelativePath()
                                               + "' not found in " +
                                               "bank '" + ref.getWaveBank() + "'.");
    }

    /**
     * Returns a File instance representing the audio file referenced
     * by a given AudioData instance.
     * @param project A KowalskiProject instance containing the data.
     * @param data The AudioData instance.
     * @return The file corresponding to the given AudioData instance.
     * @throws ProjectDataException If the audio file reference cannot be resolved.
     */
    public static File getAudioDataFile(KowalskiProject project,
                                        File projectDir,
                                        AudioData data)
        throws ProjectDataException
    {
        //TODO: support absolute paths properly
        File waveDirFile = new File(projectDir, project.getAudioFileRootDirectory());
        String[] relWavePathComponents = data.getRelativePath().split("/");
        
        if (!waveDirFile.exists())
        {
            throw new ProjectDataException("Specified audio data directory '" +
                    waveDirFile + "' does not exist.");
        }
        else if (!waveDirFile.isDirectory())
        {
            throw new ProjectDataException("Specified audio data directory '" +
                    waveDirFile + "' is not a directory.");
        }

        int i = 0;
        File waveFile = new File(waveDirFile.getAbsolutePath());
        while (i < relWavePathComponents.length)
        {
            waveFile = new File(waveFile, relWavePathComponents[i]);
            i++;
        }

        if (!waveFile.exists())
        {
            throw new ProjectDataException("Wave file " + waveFile.getAbsolutePath() +
                                               " does not exist.");
        }
        else if (!waveFile.exists())
        {
            throw new ProjectDataException(waveFile.getAbsolutePath() +
                                               " is a directory and not a file as expected");
        }

        return waveFile;
        
    }

    /**
     * Returns a map of events by event hierarcy path. The hierarchy path is of the form
     * eventgroupID1/.../eventID. The values of the map are Event references and the
     * map can contain both Event instances and StreamingEvent instances.
     * @param project A KowalskiProject instance containing the data.
     * @return A map of Event references by event hierarchy path.
     * @throws ProjectDataException If two or more events have the same path
     */
    
    public static Map<String, Event> getEventsByEventHierarchyPath(KowalskiProject project)
            throws ProjectDataException
    {
        Map<String, NodeWithIDAndComments> nodesByPath =
                getNodesByHierarchyPath(project.getEventRootGroup());

        Map<String, Event> eventsByPath =
                new Hashtable<String, Event>();
        Iterator<String> keyIterator = nodesByPath.keySet().iterator();
        while (keyIterator.hasNext())
        {
            String key = keyIterator.next();
            Event value = (Event)nodesByPath.get(key);
            eventsByPath.put(key, value);
        }

        return eventsByPath;
    }

    /**
     * Returns a map event hierarcy paths by event. The hierarchy path is of the form
     * eventgroupID1/.../eventID. The keys of the map are Event references and the
     * map can contain both Event instances and StreamingEvent instances.
     * @param project A KowalskiProject instance containing the data.
     * @return A map of event hierarchy paths by Event reference.
     */
    public static Map<Event, String> getEventHierarchyPathsByEvent(KowalskiProject project)
    {
        Map<NodeWithIDAndComments, String> pathsByNode = getHierarchyPathsByNode(project.getEventRootGroup());
        Map<Event, String> pathsByEvent = new Hashtable<Event, String>();
        Iterator<NodeWithIDAndComments> keyIterator = pathsByNode.keySet().iterator();
        while (keyIterator.hasNext())
        {
            NodeWithIDAndComments key = keyIterator.next();
            String value = pathsByNode.get(key);
            
            pathsByEvent.put((Event)key, value);
        }
        return pathsByEvent;
    }

    public static Map<MixPreset, String> getMixPresetHierarchyPathsByMixPreset(KowalskiProject project)
    {
        Map<NodeWithIDAndComments, String> pathsByNode = getHierarchyPathsByNode(project.getMixPresetRootGroup());
        Map<MixPreset, String> pathsByMixPreset = new Hashtable<MixPreset, String>();
        Iterator<NodeWithIDAndComments> keyIterator = pathsByNode.keySet().iterator();
        while (keyIterator.hasNext())
        {
            NodeWithIDAndComments key = keyIterator.next();
            String value = pathsByNode.get(key);

            pathsByMixPreset.put((MixPreset)key, value);
        }
        return pathsByMixPreset;
    }

     public static Map<String, WaveBank> getWaveBanksByHierarchyPath(KowalskiProject project)
            throws ProjectDataException
    {
        Map<String, NodeWithIDAndComments> wbNodesByPath = getNodesByHierarchyPath(project.getWaveBankRootGroup());
        Map<String, WaveBank> wbsByPath = new Hashtable<String, WaveBank>();
        Iterator<String> it = wbNodesByPath.keySet().iterator();
        while (it.hasNext())
        {
            String key = it.next();
            wbsByPath.put(key, (WaveBank)wbNodesByPath.get(key));
        }

        return wbsByPath;
    }

    public static Map<WaveBank, String> getHierarchyPathsByWaveBank(KowalskiProject project)
            throws ProjectDataException
    {
        Map<NodeWithIDAndComments, String> wbPathByNode = getHierarchyPathsByNode(project.getWaveBankRootGroup());
        Map<WaveBank, String> pathsByWaveBank = new Hashtable<WaveBank, String>();
        Iterator<NodeWithIDAndComments> it = wbPathByNode.keySet().iterator();
        while (it.hasNext())
        {
            WaveBank key = (WaveBank)it.next();
            pathsByWaveBank.put(key, wbPathByNode.get(key));
        }

        return pathsByWaveBank;
    }

    /**
     * Returns a map of sound hierarchy paths by Sound instance.
     * @see getSoundsBySoundHierarchyPath
     * @param project A KowalskiProject instance containing the data.
     * @return The map of sound hierarchy paths by Sound instance.
     */
    public static Map<Sound, String> getSoundHierarchyPathsBySound(KowalskiProject project)
    {
        Map<NodeWithIDAndComments, String> hierarchyPathsByNode = getHierarchyPathsByNode(project.getSoundRootGroup());
        Map<Sound, String> hierarchyPathsBySound = new Hashtable<Sound, String>();

        Iterator<NodeWithIDAndComments> keyIterator = hierarchyPathsByNode.keySet().iterator();
        while (keyIterator.hasNext())
        {
            NodeWithIDAndComments key = keyIterator.next();
            String value = hierarchyPathsByNode.get(key);
            hierarchyPathsBySound.put((Sound)key, value);
        }

        return hierarchyPathsBySound;

    }

    /**
     * Returns a map of Sound instances by hierarchy path.
     * @see getSoundHierarchyPathsBySound
     * @param project A KowalskiProject instance containing the data.
     * @return The map of Sound instances by sound hierarchy path..
     */
    public static Map<String, Sound> getSoundsBySoundHierarchyPath(KowalskiProject project)
            throws ProjectDataException
    {

        Map<String, NodeWithIDAndComments> soundNodesByPath =getNodesByHierarchyPath(project.getSoundRootGroup());
        Iterator<String> keyIterator = soundNodesByPath.keySet().iterator();
        Map<String, Sound> soundsByPath =
                new Hashtable<String, Sound>();
        while (keyIterator.hasNext())
        {
            String key = keyIterator.next();
            NodeWithIDAndComments value = soundNodesByPath.get(key);
            soundsByPath.put(key, (Sound)value);
        }

        return soundsByPath;
    }


    /**
     * Constructs a list of all events in a Kowalski project. The events
     * are represented by Event references, and the returned list can
     * contain both Event and StreamingEvent instances.
     * @param project A KowalskiProject instance containing the data.
     * @return A list of all events in project.
     */
    public static List<Event> getEventList(KowalskiProject project)
    {
        List<NodeWithIDAndComments> nodeList = new ArrayList<NodeWithIDAndComments>();
        gatherNodeList(nodeList, project.getEventRootGroup());
        List<Event> eventList = new ArrayList<Event>();
        for (int i = 0; i < nodeList.size(); i++)
        {
            eventList.add((Event)nodeList.get(i));
        }
        return eventList;
    }

    public static List<WaveBank> getWaveBankList(KowalskiProject project)
    {
        List<NodeWithIDAndComments> nodeList = new ArrayList<NodeWithIDAndComments>();
        gatherNodeList(nodeList, project.getWaveBankRootGroup());
        List<WaveBank> waveBankList = new ArrayList<WaveBank>();
        for (int i = 0; i < nodeList.size(); i++)
        {
            waveBankList.add((WaveBank)nodeList.get(i));
        }
        return waveBankList;
    }

    public static List<MixPreset> getMixPresetList(KowalskiProject project)
    {
        List<NodeWithIDAndComments> nodeList = new ArrayList<NodeWithIDAndComments>();
        gatherNodeList(nodeList, project.getMixPresetRootGroup());
        List<MixPreset> mixPresetList = new ArrayList<MixPreset>();
        for (int i = 0; i < nodeList.size(); i++)
        {
            mixPresetList.add((MixPreset)nodeList.get(i));
        }
        return mixPresetList;
    }

    public static List<Sound> getSoundList(KowalskiProject project)
    {
        List<NodeWithIDAndComments> nodeList = new ArrayList<NodeWithIDAndComments>();
        gatherNodeList(nodeList, project.getSoundRootGroup());
        List<Sound> soundList = new ArrayList<Sound>();
        for (int i = 0; i < nodeList.size(); i++)
        {
            soundList.add((Sound)nodeList.get(i));
        }
        return soundList;
    }

    public static List<SoundGroup> getSoundGroupList(KowalskiProject project)
    {
        List<NodeWithIDAndComments> nodeList = new ArrayList<NodeWithIDAndComments>();
        gatherGroupList(nodeList, project.getSoundRootGroup());
        List<SoundGroup> soundGroupList = new ArrayList<SoundGroup>();
        for (int i = 0; i < nodeList.size(); i++)
        {
            soundGroupList.add((SoundGroup)nodeList.get(i));
        }
        return soundGroupList;
    }

    public static List<EventGroup> getEventGroupList(KowalskiProject project)
    {
        List<NodeWithIDAndComments> nodeList = new ArrayList<NodeWithIDAndComments>();
        gatherGroupList(nodeList, project.getEventRootGroup());
        List<EventGroup> eventGroupList = new ArrayList<EventGroup>();
        for (int i = 0; i < nodeList.size(); i++)
        {
            eventGroupList.add((EventGroup)nodeList.get(i));
        }
        return eventGroupList;
    }

    /**
     * Returns a list of all AudioData instance in a given project.
     * @param project A KowalskiProject instance containing the data.
     * @param waveList The list of all AudioData instances in project.
     */
    public static List<AudioData> getAudioDataList(KowalskiProject project)
    {
        List<AudioData> audioDataList = new ArrayList<AudioData>();
        final List<WaveBank> waveBankList = getWaveBankList(project);
        final int numWaveBanks = waveBankList.size();

        for (int i = 0; i < numWaveBanks; i++)
        {
            final WaveBank waveBanki = waveBankList.get(i);
            final List<AudioData> audioDataListi = waveBanki.getAudioDataList();
            final int numWavesInBank = audioDataListi.size();
            for (int j = 0; j < numWavesInBank; j++)
            {
                AudioData audioDataj = audioDataListi.get(j);
                audioDataList.add(audioDataj);
            }
        }

        return audioDataList;
    }

    public static List<AudioDataReference> getAudioDataReferenceList(KowalskiProject project)
    {
        //look for audio data references in sounds and events
        List<Event> events = getEventList(project);
        List<Sound> sounds = getSoundList(project);
        List<AudioDataReference> audioDataReferences =
                new ArrayList<AudioDataReference>();

        for (int i = 0; i < events.size(); i++)
        {
            Event eventi = events.get(i);
            if (eventi.getAudioDataReferenceOrSoundReference() instanceof AudioDataReference)
            {
                audioDataReferences.add(((AudioDataReference)eventi.getAudioDataReferenceOrSoundReference()));
            }
        }

        for (int i = 0; i < sounds.size(); i++)
        {
            Sound soundi = sounds.get(i);
            List<AudioDataReference> refs = soundi.getAudioDataReferences();
            for (int j = 0; j < refs.size(); j++)
            {
                audioDataReferences.add(refs.get(j));
            }
        }

        return audioDataReferences;
    }

    /**
     * Returns a list of all MixBus instances in a given project.
     * @param project A KowalskiProject instance containing the data.
     * @return The list of MixBus instances in project.
     */
    public static List<MixBus> getMixBusList(KowalskiProject project)
    {
        List<MixBus> mixBusList = new ArrayList<MixBus>();
        gatherMixBusList(mixBusList, project.getMasterMixBus());

        return mixBusList;
    }

    private static void gatherMixBusList(List<MixBus> list, MixBus root)
    {
        list.add(root);
        for (int i = 0; i < root.getSubBuses().size(); i++)
        {
            MixBus subBusi = root.getSubBuses().get(i);
            gatherMixBusList(list, subBusi);
        }
    }
   

    /**
     * Returns the WaveBank instance containing a given AudioData instance, or
     * null if no wave bank contains the instance.
     * @param project A KowalskiProject instance containing the data.
     * @param audioData The AudioData instance to find the containing wave bank of.
     * @return The WaveBank instance containing the audioData, or null if
     * audioData is not contained in any wave bank of project.
     */
    public static WaveBank getWaveBankContainingAudioData(KowalskiProject project,
                                                          AudioData audioData)
    {
        List<WaveBank> waveBankList = getWaveBankList(project);
        final int numWaveBanks = waveBankList.size();

        for (int i = 0; i < numWaveBanks; i++)
        {
            WaveBank waveBanki = waveBankList.get(i);
            if (waveBanki.getAudioDataList().contains(audioData))
            {
                return waveBanki;
            }
        }

        return null;
    }

    public static List<WaveBank> getWaveBanksReferencedByEvent(KowalskiProject project,
                                                               Event event)
    {
        List<WaveBank> referencedWaveBanks = new ArrayList<WaveBank>();
        List<AudioDataReference> audioDataRefs = null;
        if (event.getAudioDataReferenceOrSoundReference() instanceof AudioDataReference)
        {
            audioDataRefs = new ArrayList<AudioDataReference>();
            audioDataRefs.add((AudioDataReference)event.getAudioDataReferenceOrSoundReference());
        }
        else if (event.getAudioDataReferenceOrSoundReference() instanceof SoundReference)
        {
            SoundReference soundRef = (SoundReference)event.getAudioDataReferenceOrSoundReference();
            Sound sound = null;
            try
            {
                sound = resolveSoundReference(project, soundRef);
            }
            catch (ProjectDataException e)
            {
                return referencedWaveBanks;
            }
            audioDataRefs = sound.getAudioDataReferences();
        }
        else
        {
            throw new RuntimeException();
        }

        for (int i = 0; i < audioDataRefs.size(); i++)
        {
            AudioDataReference ref = audioDataRefs.get(i);
            AudioData ad = null;
            try
            {
                ad = resolveAudioDataReference(project, ref);
            }
            catch (ProjectDataException e)
            {
                return referencedWaveBanks;
            }
            WaveBank waveBank = getWaveBankContainingAudioData(project, ad);
            if (!referencedWaveBanks.contains(waveBank))
            {
                referencedWaveBanks.add(waveBank);
            }
        }

        return referencedWaveBanks;
    }

    /**
     * Constructs a map of AudioData references by relative audio file path.
     * @param project A KowalskiProject instance containing the data.
     * @param wavesByRefrenceString The map of AudioData references by relative audio file path.
     */
    public static Map<String, AudioData> getAudioDataByRelativePath(KowalskiProject project)
    {
        Map<String, AudioData> wavesByRefrenceString = new Hashtable<String, AudioData>();
        
        final List<WaveBank> waveBankList = getWaveBankList(project);
        final int numWaveBanks = waveBankList.size();

        for (int i = 0; i < numWaveBanks; i++)
        {
            final WaveBank waveBanki = waveBankList.get(i);
            final List<AudioData> audioDataList = waveBanki.getAudioDataList();
            final int numWavesInBank = audioDataList.size();
            for (int j = 0; j < numWavesInBank; j++)
            {
                AudioData waveDataj = audioDataList.get(j);
                wavesByRefrenceString.put(waveDataj.getRelativePath(),
                                          waveDataj);
            }
        }

        return wavesByRefrenceString;
    }
    
    

    /**
     * Creates a minimal valid KowalskiProject instance using a given
     * audio file root directory.
     * @param audioFileRootDir The audio file root directory of the created project.
     * @return A minimal blank KowalskiProject instance.
     */
    public static KowalskiProject createBlankProject(String audioFileRootDir)
    {
        if (audioFileRootDir == null)
        {
            throw new IllegalArgumentException("audioFileRootDir is null");
        }

        ObjectFactory of = new ObjectFactory();
        KowalskiProject project = of.createKowalskiProject();
        project.setVersion("1.0");
        project.setAudioFileRootDirectory(audioFileRootDir);

        WaveBankGroup waveBankRoot = of.createWaveBankGroup();
        waveBankRoot.setID("root");
        SoundGroup soundRoot = of.createSoundGroup();
        soundRoot.setID("root");
        EventGroup eventRoot = of.createEventGroup();
        eventRoot.setID("root");
        MixBus masterBus = of.createMixBus();
        masterBus.setID("master");

        project.setEventRootGroup(eventRoot);
        project.setSoundRootGroup(soundRoot);
        project.setMasterMixBus(masterBus);        
        project.setWaveBankRootGroup(waveBankRoot);

        MixPresetGroup mixPresetRoot = of.createMixPresetGroup();
        mixPresetRoot.setID("root");
        List<MixPreset> presetList = mixPresetRoot.getMixPresets();
        MixPreset defaultPreset = createDefaultMixPreset(project, "defaultpreset");
        defaultPreset.setDefault(true);
        presetList.add(defaultPreset);
        project.setMixPresetRootGroup(mixPresetRoot);

        //project structure sanity check.
        try
        {
            ProjectDataValidator.validateProjectData(project);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

        return project;
    }

    public static Sound createDefaultSound(String id)
    {
        Sound ret = new Sound();
        ret.setPlaybackMode(SoundPlaybackMode.SEQUENTIAL);
        ret.setID(id);
        return ret;
    }

    public static MixBus createDefaultMixBus(String id)
    {
        MixBus ret = new MixBus();
        ret.setID(id);
        return ret;
    }

    public static WaveBank createDefaultWaveBank(String id)
    {
        WaveBank ret = new WaveBank();
        ret.setID(id);
        return ret;
    }


    public static MixBusParameters createDefaultMixBusParameters(MixBus bus)
    {
        MixBusParameters p = new MixBusParameters();
        p.setMixBus(bus.getID());
        p.setLeftGain(1.0f);
        p.setRightGain(1.0f);
        p.setPitch(1.0f);
        return p;
    }

    public static MixPreset createDefaultMixPreset(KowalskiProject project, String id)
    {
        MixPreset defaultPreset = new MixPreset();
        defaultPreset.setID(id);
        defaultPreset.setDefault(false);
        //add one parameter set per mix bus
        List<MixBus> mixBusList = getMixBusList(project);
        for (int i = 0; i < mixBusList.size(); i++)
        {
            MixBus busi = mixBusList.get(i);
            MixBusParameters params = new MixBusParameters();
            params.setLeftGain(1.0f);
            params.setRightGain(1.0f);
            params.setPitch(1.0f);
            params.setMixBus(busi.getID());
            defaultPreset.getMixBusParameterList().add(params);
        }
        
        defaultPreset.setID(id);
        return defaultPreset;
    }

    

    /**
     * Returns a list of all SoundReferences in a project.
     * @param project
     * @return
     */
    public static List<SoundReference> getSoundReferenceList(KowalskiProject project)
    {
        List<SoundReference> list = new ArrayList<SoundReference>();
        List<Event> eventList = getEventList(project);
        final int numEvents = eventList.size();

        for (int i = 0; i < numEvents; i++)
        {
            Event eventi = eventList.get(i);
            if (eventi.getAudioDataReferenceOrSoundReference() instanceof SoundReference)
            {
                list.add((SoundReference)eventi.getAudioDataReferenceOrSoundReference());
            }
        }

        return list;
        
    }


    /**
     * Returns the EventGroup containing a given Event, or null if the event
     * is not contained in any group.
     * @param soundGroup
     * @param sound
     * @return
     */
    /*
    public static NodeWithIDAndComments getGroupContainingNode(NodeWithIDAndComments root,
                                                               NodeWithIDAndComments node)
    {
        List<NodeWithIDAndComments> groupList = new ArrayList<NodeWithIDAndComments>();
        gatherGroupList(groupList, root);

        final int numEventGroups = groupList.size();

        for (int i = 0; i < numEventGroups; i++)
        {
            NodeWithIDAndComments groupi = groupList.get(i);
            List<NodeWithIDAndComments> nodes = getChildNodes(groupi);
            final int numEvents = nodes.size();
            for (int j = 0; j < numEvents; j++)
            {
                if (nodes.get(j) == node)
                {
                    return groupi;
                }
            }
        }

        return null;

    }*/

    private static NodeWithIDAndComments getRootGroup(KowalskiProject project, NodeWithIDAndComments node)
    {
        NodeWithIDAndComments root = null;
        
        if (node instanceof EventGroup ||
            node instanceof Event)
        {
            root = project.getEventRootGroup();
        }
        else if (node instanceof SoundGroup ||
                 node instanceof Sound)
        {
            root = project.getSoundRootGroup();
        }
        else if (node instanceof MixPresetGroup ||
                 node instanceof MixPreset)
        {
            root = project.getMixPresetRootGroup();
        }
        else if (node instanceof WaveBankGroup ||
                 node instanceof WaveBank)
        {
            root = project.getWaveBankRootGroup();
        }
        else if (node instanceof MixBus)
        {
            root = project.getMasterMixBus();
        }
        else
        {
            throw new IllegalArgumentException();
        }

        return root;
    }

    public static NodeWithIDAndComments getParentGroup(KowalskiProject project,
                                                       NodeWithIDAndComments child)
    {
        NodeWithIDAndComments root = null;
        if (child instanceof WaveBank || child instanceof WaveBankGroup)
        {
            root = project.getWaveBankRootGroup();
        }
        else if (child instanceof Sound || child instanceof SoundGroup)
        {
            root = project.getSoundRootGroup();
        }
        else if (child instanceof Event || child instanceof EventGroup)
        {
            root = project.getEventRootGroup();
        }
        else if (child instanceof MixPreset || child instanceof MixPresetGroup)
        {
            root = project.getMixPresetRootGroup();
        }
        else if (child instanceof MixBus)
        {
            root = project.getMasterMixBus();
        }
        else
        {
            throw new RuntimeException();
        }

        List<NodeWithIDAndComments> groupList = new ArrayList<NodeWithIDAndComments>();
        gatherGroupList(groupList, root);

        final int numEventGroups = groupList.size();

        for (int i = 0; i < numEventGroups; i++)
        {
            NodeWithIDAndComments groupi = groupList.get(i);
            List<NodeWithIDAndComments> groups = getAllChildren(groupi);
            final int numEvents = groups.size();
            for (int j = 0; j < numEvents; j++)
            {
                if (groups.get(j) == child)
                {
                    return groupi;
                }
            }
        }

        return null;

    }

    /**
     * Changes the ID of a given Sound.
     * @param soundToChange
     * @param newID
     * @throws ProjectDataException If the ID is already taken by another sound.
     */
    public static void setSoundID(KowalskiProject project, Sound soundToChange, String newID)
            throws ProjectDataException

    {
        //check if the sound group containing the sound contains another sound
        //with the given id
        SoundGroup group = (SoundGroup)getParentGroup(project, soundToChange);
        List<Sound> soundsInSameGroup =
                group.getSounds();

        final int numSoundsInGroup = soundsInSameGroup.size();
        for (int i = 0; i < numSoundsInGroup; i++)
        {
            Sound soundi = soundsInSameGroup.get(i);
            if (soundi == soundToChange)
            {
                continue;
            }

            if (soundi.getID().equals(newID))
            {
                throw new ProjectDataException("The ID '" + newID + "' is " +
                        "already taken by a sound in group '" + group.getID() + "'.");
            }
        }

        //check that the ID matches the xsd restrictions
        String oldID = soundToChange.getID();
        soundToChange.setID(newID);
        try
        {
            ProjectDataXMLSerializer.getInstance().validateNodeWithIDAndComments(soundToChange);
        }
        catch (JAXBException e)
        {
            soundToChange.setID(oldID);
            throw new ProjectDataException(e.getLinkedException().getMessage());
        }

        //if we made it here, the new id has been set. now update sound references
        //accordingly
        List<SoundReference> soundReferences = getSoundReferenceList(project);
        final int numSoundReferences = soundReferences.size();
        for (int i = 0; i < numSoundReferences; i++)
        {

            SoundReference soundRef = soundReferences.get(i);
            String refString = soundRef.getReferenceString();
            String[] refStringComponents =
                    refString.split("/");
            if (refStringComponents.length < 2)
            {
                throw new ProjectDataException("Malformed sound reference '" +
                        refString + "'");
            }

            
            if (oldID.equals(refStringComponents[refStringComponents.length - 1]))
            {
                refStringComponents[refStringComponents.length - 1] = newID;
                String newReferenceString = "";
                for (int j = 0; j < refStringComponents.length; j++)
                {
                    newReferenceString += refStringComponents[j];
                    if (j < refStringComponents.length - 1)
                    {
                        newReferenceString += "/";
                    }
                }

                soundRef.setReferenceString(newReferenceString);
            }
        }

    }

    

    private static boolean doesNodeHaveChildWithID(NodeWithIDAndComments group,
                                                   String subgroupID)
    {
        List<NodeWithIDAndComments> subGroups = getChildGroups(group);
        List<NodeWithIDAndComments> childNodes = getChildNodes(group);
        
        for (int i = 0; i < subGroups.size(); i++)
        {
            if (subGroups.get(i).getID().equals(subgroupID))
            {
                return true;
            }
        }

        for (int i = 0; i < childNodes.size(); i++)
        {
            if (childNodes.get(i).getID().equals(subgroupID))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Changes the ID of a given Event.
     * @param eventToChange
     * @param newID
     * @throws ProjectDataException If the ID is already taken by another Event.
     */
    /*
    public static void setEventID(KowalskiProject project, Event eventToChange, String newID)
            throws ProjectDataException
    {
        //check if the event group containing the event contains another event
        //with the given id
        EventGroup group = (EventGroup)getGroupContainingNode(project.getEventRootGroup(), eventToChange);
        List<Event> eventsInSameGroup = group.getEvents();

        final int numSoundsInGroup = eventsInSameGroup.size();
        for (int i = 0; i < numSoundsInGroup; i++)
        {
            Event eventi = eventsInSameGroup.get(i);
            if (eventi == eventToChange)
            {
                continue;
            }

            if (eventi.getID().equals(newID))
            {
                throw new ProjectDataException("The ID '" + newID + "' is " +
                        "already taken by an event in group '" + group.getID() + "'.");
            }
        }

        //check that the ID matches the xsd restrictions
        String oldID = eventToChange.getID();
        eventToChange.setID(newID);
        try
        {
            ProjectDataXMLSerializer.getInstance().validateNodeWithIDAndComments(eventToChange);
        }
        catch (JAXBException e)
        {
            eventToChange.setID(oldID);
            //TODO: fix up
            throw new ProjectDataException(e.getLinkedException().getMessage());
        }
    }*/

    /**
     * Changes the ID of a given MixBus.
     * @param busToChange
     * @param newID
     * @throws ProjectDataException If the ID is already taken by another MixBus or
     *      if the master bus is passed.
     */
    /*
    public static void setMixBusID(KowalskiProject project, MixBus busToChange, String newId)
            throws ProjectDataException
    {
        if (busToChange.getID().equals("master"))
        {
            throw new ProjectDataException("cannot change the constant ID of the master bus");
        }
        
        List<MixBus> mixBusList = getMixBusList(project);
        final int numMixBuses = mixBusList.size();
        for (int i = 0; i < numMixBuses; i++)
        {
            if (mixBusList.get(i) == busToChange)
            {
                continue;
            }
            
            String idi = mixBusList.get(i).getID();
            if (idi.equals(newId))
            {
                throw new ProjectDataException("there is already a mix bus with ID '" +
                        newId + "'");
            }
        }

        String oldId = busToChange.getID();
        busToChange.setID(oldId);
        try
        {
            ProjectDataXMLSerializer.getInstance().validateNodeWithIDAndComments(busToChange);
        }
        catch (JAXBException e)
        {
            busToChange.setID(newId);
            throw new ProjectDataException(e.getLinkedException().getMessage());
        }
    }*/

    public static void setNodeWithIDAndCommentsID(KowalskiProject project, 
                                                  NodeWithIDAndComments node,
                                                  String newID)
        throws ProjectDataException
    {
        NodeWithIDAndComments parent = getParentGroup(project, node);
        List<NodeWithIDAndComments> siblings = getAllChildren(parent);
        for (int i = 0; i < siblings.size(); i++)
        {
            NodeWithIDAndComments siblingi = siblings.get(i);
            String idi = siblingi.getID();
            if (idi.equals(newID) && siblingi != node)
            {
                throw new ProjectDataException("There is already a node with ID '" + idi +
                        "' in the group '" + parent.getID() + "'");
            }
        }

        String oldID = node.getID();
        try
        {
            System.out.println("old id, new id : " + oldID + ", " + newID);
            node.setID(newID);
            ProjectDataXMLSerializer.getInstance().validateNodeWithIDAndComments(node);
        }
        catch (JAXBException e)
        {
            node.setID(oldID);
            throw new ProjectDataException("");
        }
    }

    public static void moveEvent(KowalskiProject project,
                                             Event event,
                                             EventGroup eventGroup)
         throws ProjectDataException
    {
        //do nothing if the event is already in the target group
        if (eventGroup.getEvents().contains(event))
        {
            return;
        }

        //check that there is no event with the same id in the target group
        List<Event> targetGroupEvents = eventGroup.getEvents();
        final int numEvents = targetGroupEvents.size();
        for (int i = 0; i < numEvents; i++)
        {
            Event eventi = targetGroupEvents.get(i);
            if (eventi.getID().equals(event.getID()))
            {
                throw new ProjectDataException("There is already an event with ID '" +
                        event.getID() + "'" +
                    " in event group '" + eventGroup.getID() + "'");
            }
        }

        //double check that the event to be moved belongs to a group
        EventGroup startGroup = (EventGroup)getParentGroup(project, event);
        if (startGroup == null)
        {
            throw new IllegalArgumentException("The event '" + event.getID() + "'" +
                    " is not contained in any group");
        }

        //move the event
        startGroup.getEvents().remove(event);
        List<Event> eventListToAddTo = eventGroup.getEvents();
        eventListToAddTo.add(getLexicographicalIndexOf(getChildNodes(eventGroup), event), event);
    }

    public static MixPreset getDefaultMixPreset(KowalskiProject project)
            throws ProjectDataException
    {
        List<MixPreset> presets = getMixPresetList(project);
        for (int i = 0; i < presets.size(); i++)
        {
            if (presets.get(i).isDefault())
            {
                return presets.get(i);
            }
        }

        throw new ProjectDataException("No default mix preset found in project.");
    }

    public static void moveMixPreset(KowalskiProject project, MixPreset presetToMove, MixPresetGroup target)
            throws ProjectDataException
    {
        if (doesNodeHaveChildWithID(target, presetToMove.getID()))
        {
            throw new ProjectDataException("There is already an mix preset with ID '" + presetToMove.getID() +
                                            "' in the target group.");
        }

        MixPresetGroup oldParent = (MixPresetGroup)getParentGroup(project, presetToMove);

        int idx = getLexicographicalIndexOf(getChildNodes(target), presetToMove);
        target.getMixPresets().add(idx, presetToMove);
        oldParent.getMixPresets().remove(presetToMove);
    }

    public static void moveWaveBank(KowalskiProject project, WaveBank waveBankToMove, WaveBankGroup target)
            throws ProjectDataException
    {
        if (doesNodeHaveChildWithID(target, waveBankToMove.getID()))
        {
            throw new ProjectDataException("There is already a wave bank with ID '" + waveBankToMove.getID() +
                                            "' in the target group.");
        }

        String oldPath = getPathTo(project, waveBankToMove);
        String pathToNewTarget = getPathTo(project, target);
        String newWaveBankPath = pathToNewTarget.length() > 0 ? pathToNewTarget + "/" + waveBankToMove.getID() : waveBankToMove.getID();
        System.out.println("ProjectDataUtils.moveWaveBank: old path " + oldPath + ", path to new target " + pathToNewTarget);

        WaveBankGroup oldParent = (WaveBankGroup)getParentGroup(project, waveBankToMove);
        int idx = getLexicographicalIndexOf(getChildNodes(target), waveBankToMove);
        target.getWaveBanks().add(idx, waveBankToMove);
        oldParent.getWaveBanks().remove(waveBankToMove);

        //update any audio data references
        List<AudioDataReference> audioDataRefs = getAudioDataReferenceList(project);
        for (int i = 0; i < audioDataRefs.size(); i++)
        {
            AudioDataReference refi = audioDataRefs.get(i);
            if (refi.getWaveBank().equals(oldPath))
            {

                System.out.println("ProjectDataUtils.moveWaveBank: changing wave bank reference from");
                System.out.println("ProjectDataUtils.moveWaveBank: " + refi.getWaveBank() + " to " + newWaveBankPath);
                refi.setWaveBank(newWaveBankPath);
            }
        }
    }

    public static void moveSound(KowalskiProject project, Sound sound, SoundGroup soundGroup)
         throws ProjectDataException
    {
        //do nothing if the sound is already in the target group
        if (soundGroup.getSounds().contains(sound))
        {
            return;
        }

        //check that there is no sound with the same id in the target group
        List<Sound> targetGroupSounds = soundGroup.getSounds();
        final int numSounds = targetGroupSounds.size();
        for (int i = 0; i < numSounds; i++)
        {
            Sound soundi = targetGroupSounds.get(i);
            if (soundi.getID().equals(sound.getID()))
            {
                throw new ProjectDataException("There is already a sound with ID '" +
                        sound.getID() + "'" +
                    " in sound group '" + soundGroup.getID() + "'");
            }
        }

        //double check that the sound to be moved belongs to a group
        SoundGroup startGroup = (SoundGroup)getParentGroup(project, sound);
        if (startGroup == null)
        {
            throw new IllegalArgumentException("The sound '" + sound.getID() + "'" +
                    " is not contained in any group");
        }

        //if we made it here, moving the sound is allowed, so we need to
        //update all events referencing the moved sound
        String newSoundPath = getPathTo(project, soundGroup) + "/" + sound.getID();
        newSoundPath = newSoundPath.startsWith("/") ? newSoundPath.substring(1) : newSoundPath;
        String oldSoundPath = getPathTo(project, sound);

        List<SoundReference> soundRefs = getSoundReferenceList(project);
        for (int i = 0; i < soundRefs.size(); i++)
        {
            SoundReference sr = soundRefs.get(i);
            
            if (sr.getReferenceString().equals(oldSoundPath))
            {
                System.out.println("updating sound reference " + sr.getReferenceString() +
                                   " to " + newSoundPath);
                sr.setReferenceString(newSoundPath);
            }
        }

        //move the event
        startGroup.getSounds().remove(sound);
        soundGroup.getSounds().add(sound);
    }

    
    private static boolean isGroupDecendant(NodeWithIDAndComments group,
                                            NodeWithIDAndComments potentialDecendant,
                                            boolean foundMatch)
    {
        List<NodeWithIDAndComments> subGroups = getChildGroups(group);

        if (subGroups.contains(potentialDecendant))
        {
            return true;
        }

        for (int i = 0; i < subGroups.size(); i++)
        {
            NodeWithIDAndComments subGroupi = subGroups.get(i);
            return foundMatch || 
                   isGroupDecendant(subGroupi, potentialDecendant, foundMatch);
        }

        return false;
    }

    
    public static void moveGroup(KowalskiProject project,
                                 NodeWithIDAndComments groupToMove,
                                 NodeWithIDAndComments targetGroup)
      throws ProjectDataException
    {
        NodeWithIDAndComments parentGroup = getParentGroup(project, groupToMove);

        //do nothing if the group to move is already in its target location
        //or if attempting to move a group to itself
        if (parentGroup == targetGroup || groupToMove == targetGroup)
        {
            return;
        }

        //check if we're trying to move the group to one of its decendants
        if (isGroupDecendant(groupToMove, targetGroup, false))
        {
            throw new ProjectDataException("Trying to move a group '" +
                    groupToMove.getID() + "' to one of its decendants.");

        }

        
        if (doesNodeHaveChildWithID(targetGroup, groupToMove.getID()))
        {
            throw new ProjectDataException("There is already a group with " +
                    "ID '" + groupToMove.getID() + "' at the specified hierachy level.");
        }

        //move the group
        addGroup(project, targetGroup, groupToMove);//add first, because this statement may
                                                    //cause an exception

        //if we made it here, moving the group went well.
        //check if any references need to be updated
        if (groupToMove instanceof SoundGroup)
        {
            List<NodeWithIDAndComments> movedSounds =
                    new ArrayList<NodeWithIDAndComments>();
            gatherNodeList(movedSounds, groupToMove);

            List<SoundReference> soundReferences = getSoundReferenceList(project);
            Map<String, Sound> soundsByPath = getSoundsBySoundHierarchyPath(project);
            
            
        }
        else if (groupToMove instanceof WaveBankGroup)
        {

        }

        //remove the group from its old parent
        removeGroup(project, parentGroup, groupToMove);
        
    }

    public static void addAudioDataToWaveBank(KowalskiProject project,
                                              WaveBank waveBank,
                                              AudioData ad)
    {
        waveBank.getAudioDataList().add(ad);
        Collections.sort(waveBank.getAudioDataList(), audioDataComparator);
    }

    public static AudioData addAudioFileToWaveBank(KowalskiProject project,
                                                   WaveBank waveBank,
                                                   File file)
                                       throws ProjectDataException
    {
        AudioData ad = new AudioData();
        File baseDir = new File(project.getAudioFileRootDirectory());
        String relativePath = baseDir.toURI().relativize(file.toURI()).getPath();

        //check if the bank already contains a reference to the file
        List<AudioData> audioDataList = waveBank.getAudioDataList();
        for (int i = 0; i < audioDataList.size(); i++)
        {
            if (audioDataList.get(i).getRelativePath().equals(relativePath))
            {
                throw new ProjectDataException("The wave bank '" + waveBank.getID() +
                                               "' already contains a reference to " +
                                               relativePath + ".");
            }
        }

        ad.setRelativePath(relativePath);
        if (waveBank.getAudioDataList().contains(ad))
        {
            throw new IllegalArgumentException();
        }
        addAudioDataToWaveBank(project, waveBank, ad);
        return ad;
    }

    public static void removeAudioDataFromWaveBank(KowalskiProject project,
                                                   WaveBank waveBank,
                                                   AudioData ad)
    {
        if (!waveBank.getAudioDataList().contains(ad))
        {
            throw new IllegalArgumentException();
        }
        waveBank.getAudioDataList().remove(ad);
    }

    public static void addAudioDataReferenceToSound(KowalskiProject project,
                                                    AudioData audioData,
                                                    Sound sound)
    {
        WaveBank wb = getWaveBankContainingAudioData(project, audioData);
        AudioDataReference adr = new AudioDataReference();
        adr.setProbabilityWeight(1.0f);
        adr.setWaveBank(wb.getID());
        adr.setRelativePath(audioData.getRelativePath());
        sound.getAudioDataReferences().add(adr);
    }

    public static void moveAudioDataReferenceToSound(KowalskiProject project,
                                                     AudioDataReference audioDataRef,
                                                     Sound oldSound,
                                                     Sound newSound)
    {
        if (oldSound == newSound)
        {
            return;
        }

        if (!oldSound.getAudioDataReferences().contains(audioDataRef) ||
            newSound.getAudioDataReferences().contains(audioDataRef))
        {
            throw new RuntimeException();
        }

        //move the reference
        oldSound.getAudioDataReferences().remove(audioDataRef);
        newSound.getAudioDataReferences().add(audioDataRef);
    }

    public static void moveAudioDataToWaveBank(KowalskiProject project,
                                               AudioData audioData,
                                               WaveBank waveBank)
                                               throws ProjectDataException
    {
        throw new UnsupportedOperationException();
        /*
        if (waveBank.getAudioDataList().contains(audioData))
        {
            return;
        }

        WaveBank oldBank = getWaveBankContainingAudioData(project, audioData);
        if (oldBank == null)
        {
            throw new RuntimeException();
        }

        if (isAudioDataWithRelativePathInWaveBank(project, waveBank, audioData))
        {
            throw new ProjectDataException("Wave bank '" + waveBank.getID() + "' already " +
                    "contains a reference to " + audioData.getRelativePath());
        }

        //update audio data references in StreamingEvents and Sounds
        List<Sound> sounds = getSoundList(project);        
        for (int i = 0; i < sounds.size(); i++)
        {
            Sound soundi = sounds.get(i);
            List<AudioDataReference> audioRefs = soundi.getAudioDataReferences();
            for (int j = 0; j < audioRefs.size(); j++)
            {
                AudioDataReference audioRefj = audioRefs.get(j);
                if (audioRefj.getWaveBank().equals(oldBank.getID()) &&
                    audioRefj.getRelativePath().equals(audioData.getRelativePath()))
                {
                    //this audio data reference points to the same bank and file,
                    //point it to the new wave bank
                    audioRefj.setWaveBankID(waveBank.getID());
                }

            }
        }

        List<Event> events = getEventList(project);
        for (int i = 0; i < events.size(); i++)
        {
            if (!(events.get(i) instanceof StreamingEvent))
            {
                continue;
            }
            StreamingEvent eventi = (StreamingEvent)events.get(i);
            AudioDataReference adr = eventi.getAudioDataReference();
            if (adr.getWaveBank().equals(oldBank.getID()) &&
                adr.getRelativePath().equals(audioData.getRelativePath()))
            {
                //this audio data reference points to the same bank and file,
                //point it to the new wave bank
                adr.setWaveBankID(waveBank.getID());
            }
        }

        //move the audio data
        oldBank.getAudioDataList().remove(audioData);
        waveBank.getAudioDataList().add(audioData);
         *
         */
    }

    private static boolean isAudioDataWithRelativePathInWaveBank(KowalskiProject project,
            WaveBank waveBank, AudioData audioData)
    {
        List<AudioData> audioDataItems = waveBank.getAudioDataList();
        for (int i = 0; i < audioDataItems.size(); i++)
        {
            if (audioDataItems.get(i).getRelativePath().equals(audioData.getRelativePath()))
            {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Creates a new EventGroup with a given event group as parent. If the parent is null,
     * the new group gets created under the Events node.
     * @param project
     * @param node
     * @param soundID
     */
    public static NodeWithIDAndComments createAndAddGroup(KowalskiProject project,
                                                          NodeWithIDAndComments parent,
                                                          String groupID)
                                                          throws ProjectDataException
    {
        //the ID that the group will end up having. will change if the desired ID is already taken.
        String actualID = groupID;
        int i = 2;
        while (doesNodeHaveChildWithID(parent, actualID))
        {
            actualID = groupID + "_" + i;
            i++;
        }
        //System.out.println("createAndAddGroup");
        NodeWithIDAndComments newGroup = null;
        if (parent instanceof EventGroup)
        {
            newGroup = new EventGroup();
            newGroup.setID(actualID);
        }
        else if (parent instanceof SoundGroup)
        {
            newGroup = new SoundGroup();
            newGroup.setID(actualID);
        }
        else if (parent instanceof MixPresetGroup)
        {
            newGroup = new MixPresetGroup();
            newGroup.setID(actualID);
        }
        else if (parent instanceof WaveBankGroup)
        {
            newGroup = new WaveBankGroup();
            newGroup.setID(actualID);
        }
        else
        {
            throw new IllegalArgumentException();
        }

        addGroup(project, parent, newGroup);

        return newGroup;
    }


    public static void addGroup(KowalskiProject project, NodeWithIDAndComments parent, NodeWithIDAndComments group)
            throws ProjectDataException
    {

        if (doesNodeHaveChildWithID(parent, group.getID()))
        {
            throw new ProjectDataException("A group with ID '" + group.getID() + "' already exits.");
        }
        
        //work out where to insert the group so as to maintain the alphabetical order
        int idx = getLexicographicalIndexOf(getChildGroups(parent), group);

        //insert the group
        if (group instanceof EventGroup)
        {
            ((EventGroup)parent).getSubGroups().add(idx, (EventGroup)group);
        }
        else if (group instanceof SoundGroup)
        {
            ((SoundGroup)parent).getSubGroups().add(idx, (SoundGroup)group);
        }
        else if (group instanceof MixPresetGroup)
        {
            ((MixPresetGroup)parent).getSubGroups().add(idx, (MixPresetGroup)group);
        }
        else if (group instanceof WaveBankGroup)
        {
            ((WaveBankGroup)parent).getSubGroups().add(idx, (WaveBankGroup)group);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    public static Event createAndAddEvent(KowalskiProject project, EventGroup group, String eventID)
    {
        //the ID that the event will end up having. will change if the desired ID is already taken.
        String actualID = eventID;
        int i = 2;
        while (doesNodeHaveChildWithID(group, actualID))
        {
            actualID = eventID + "_" + i;
            i++;
        }

        Event newEvent = new Event();
        newEvent.setID(actualID);

        //associate the event with the master bus by default
        newEvent.setBus(project.getMasterMixBus().getID());

        group.getEvents().add(newEvent);
        return newEvent;
    }

    public static void removeEvent(KowalskiProject project, EventGroup group, Event event)
    {
        //the ID that the event will end up having. will change if the desired ID is already taken.
        if (!group.getEvents().contains(event))
        {
            throw new IllegalArgumentException();
        }

        group.getEvents().remove(event);
    }

    public static Sound createAndAddSound(KowalskiProject project, SoundGroup group, String soundID)
    {
        //the ID that the event will end up having. will change if the desired ID is already taken.
        String actualID = soundID;
        int i = 2;
        while (doesNodeHaveChildWithID(group, actualID))
        {
            actualID = soundID + "_" + i;
            i++;
        }

        Sound newSound = createDefaultSound(actualID);
        group.getSounds().add(newSound);
        return newSound;
    }

    public static void removeSound(KowalskiProject project, SoundGroup group, Sound sound)
            throws ProjectDataException
    {
        if (!group.getSounds().contains(sound))
        {
            throw new IllegalArgumentException();
        }

        //dont allow removal if one or more events are referencing the sound
        Map<String, Event> allEvents = getEventsByEventHierarchyPath(project);
        List<String> referencingEventIDs = new ArrayList<String>();
        Iterator<String> it = allEvents.keySet().iterator();
        while (it.hasNext())
        {
            String id = it.next();
            Event eventi = allEvents.get(id);
            if (eventi.getAudioDataReferenceOrSoundReference() instanceof SoundReference)
            {
                Sound s = resolveSoundReference(project,
                                      (SoundReference)eventi.getAudioDataReferenceOrSoundReference());
                if (s == sound)
                {
                    referencingEventIDs.add(id);
                }
            }
        }
        
        if (referencingEventIDs.size() > 0)
        {
            List<String> errorMessages = new ArrayList<String>();
            errorMessages.add("Cannot remove sound '" + sound.getID() + "' because it is referenced " +
                              "by the following event(s):");
            errorMessages.addAll(referencingEventIDs);
            throw new ProjectDataException(errorMessages);
        }

        group.getSounds().remove(sound);
    }

    public static MixPreset createAndAddMixPreset(KowalskiProject project, MixPresetGroup parent, String presetID)
    {
        //the ID that the event will end up having. will change if the desired ID is already taken.
        String actualID = presetID;
        int n = 2;
        while (doesNodeHaveChildWithID(parent, actualID))
        {
            actualID = presetID + "_" + n;
            n++;
        }

        MixPreset newPreset = createDefaultMixPreset(project, actualID);
        int idx = getLexicographicalIndexOf(getChildNodes(parent), newPreset);
        parent.getMixPresets().add(idx, newPreset);

        return newPreset;
    }

    public static MixBus createAndAddMixBus(KowalskiProject project, MixBus parent, String busID)
    {
        //the ID that the event will end up having. will change if the desired ID is already taken.
        String actualID = busID;
        int i = 2;
        while (isMixBusIDTaken(project, actualID))
        {
            actualID = busID + "_" + i;
            i++;
        }

        MixBus newBus = createDefaultMixBus(actualID);
        addMixBus(project, parent, newBus);
        
        return newBus;
    }

    public static void addMixBus(KowalskiProject project, MixBus parent, MixBus bus)
    {
        int idx = getLexicographicalIndexOf(getChildGroups(parent), bus);
        System.out.println("new bus idx = " + idx);
        parent.getSubBuses().add(idx, bus);

        //add this mix bus to mix presets
        String addedBusID = bus.getID();
        List<MixPreset> mixPresets = ProjectDataUtils.getMixPresetList(project);
        for (int i = 0; i < mixPresets.size(); i++)
        {
            List<MixBusParameters> paramList = mixPresets.get(i).getMixBusParameterList();
            MixBusParameters newParams = new MixBusParameters();
            newParams.setLeftGain(1.0f);
            newParams.setRightGain(1.0f);
            newParams.setPitch(1.0f);
            newParams.setMixBus(addedBusID);
            //TODO: alphabetical order
            paramList.add(newParams);
        }
    }
    
    public static void removeMixBus(KowalskiProject project, MixBus parent, MixBus bus)
            throws ProjectDataException
    {
        //the ID that the event will end up having. will change if the desired ID is already taken.
        if (!parent.getSubBuses().contains(bus))
        {
            throw new IllegalArgumentException();
        }

        //dont allow removal of a bus that:
        //-is currently referenced by events
        //-have children currently referenced by events
        List<NodeWithIDAndComments> childBuses = new ArrayList<NodeWithIDAndComments>();
        gatherGroupList(childBuses, bus);
        //dont allow removal of buses that are referenced by events for now
        //make any events referencing the removed mix bus
        List<Event> eventList = getEventList(project);
        for (int i = 0; i < eventList.size(); i++)
        {
            Event eventi = eventList.get(i);
            if (eventi.getBus().equals(bus.getID()))
            {
                throw new ProjectDataException("Cannot remove mix bus '" + bus.getID() +
                                               "' because it is referenced by one or more events");
            }

            for (int j = 0; j < childBuses.size(); j++)
            {
                String subBusId = ((MixBus)childBuses.get(j)).getID();
                if (eventi.getBus().equals(subBusId))
                {
                    throw new ProjectDataException("Cannot remove mix bus '" + bus.getID() +
                                               "' because at least one of its sub buses ('" +
                                               subBusId + "') is referenced by one or more events");
                }
            }
        }



        parent.getSubBuses().remove(bus);

        //remove parameter sets referencing the removed bus
        List<MixPreset> mixPresets = getMixPresetList(project);
        for (int i = 0; i < mixPresets.size(); i++)
        {
            List<MixBusParameters> params = mixPresets.get(i).getMixBusParameterList();
            List<MixBusParameters> paramsToRemove = new LinkedList<MixBusParameters>();
            for (int j = 0; j < params.size(); j++)
            {
                MixBusParameters p = params.get(j);
                if (p.getMixBus().equals(bus.getID()))
                {
                    paramsToRemove.add(p);
                }
            }

            for (int j = 0; j < paramsToRemove.size(); j++)
            {
                System.out.println("removing parameter set for " +
                                   paramsToRemove.get(j).getMixBus() + " from preset " +
                                   mixPresets.get(i));
                params.remove(paramsToRemove.get(j));
            }
        }
    }

    public static WaveBank createAndAddWaveBank(KowalskiProject project,
                                                WaveBankGroup parent,
                                                String id)
    {
        //the ID that the event will end up having. will change if the desired ID is already taken.
        String actualID = id;
        int n = 2;
        while (doesNodeHaveChildWithID(parent, actualID))
        {
            actualID = id + "_" + n;
            n++;
        }

        WaveBank wb = createDefaultWaveBank(actualID);
        parent.getWaveBanks().add(wb);
        return wb;
    }

    public static void removeWaveBank(KowalskiProject project, WaveBankGroup parent,
                                        WaveBank waveBank)
        throws ProjectDataException
    {
        if (!parent.getWaveBanks().contains(waveBank))
        {
            throw new RuntimeException();
        }

        List<String> referencingEventIDs = getIDsOfEventsReferencingWaveBank(project, waveBank);
        List<String> referencingSoundIDs = getIDsOfSoundsReferencingWaveBank(project, waveBank);
        List<String> errorMessages = new ArrayList<String>();
        
        if (referencingEventIDs.size() > 0 || referencingSoundIDs.size() > 0)
        {
            errorMessages.add("Cannot remove wave bank '" + waveBank.getID() +
                              "' because it is referenced by the following events and sounds:");
            errorMessages.addAll(referencingEventIDs);
            errorMessages.addAll(referencingSoundIDs);
            throw new ProjectDataException(errorMessages);
        }
        
        parent.getWaveBanks().remove(waveBank);
    }

    private static List<String> getIDsOfEventsReferencingWaveBank(KowalskiProject project, WaveBank waveBank)
            throws ProjectDataException
    {
        List<String> referencingEventIDs = new ArrayList<String>();
        Map<String, Event> allEvents = getEventsByEventHierarchyPath(project);
        Iterator<String> it = allEvents.keySet().iterator();

        while (it.hasNext())
        {
            String id = it.next();
            Event eventi = allEvents.get(id);
            Object audioDataReference = eventi.getAudioDataReferenceOrSoundReference();
            if (audioDataReference instanceof AudioDataReference)
            {
                AudioDataReference ref = (AudioDataReference)audioDataReference;
                try
                {
                    Object wb = resolvePath(project.getWaveBankRootGroup(),
                                            ref.getWaveBank(),
                                            "WaveBank");
                    if (wb == waveBank)
                    {
                        referencingEventIDs.add(id);
                    }
                }
                catch (ProjectDataException e)
                {
                    throw new RuntimeException();
                }
            }
        }

        return referencingEventIDs;
    }

    private static List<String> getIDsOfSoundsReferencingWaveBank(KowalskiProject project, WaveBank waveBank)
            throws ProjectDataException
    {
        List<String> referencingSoundIds = new ArrayList<String>();
        Map<String, Sound> allSounds = getSoundsBySoundHierarchyPath(project);
        Iterator<String> it = allSounds.keySet().iterator();
        
        while(it.hasNext())
        {
            String id = it.next();
            Sound soundi = allSounds.get(id);
            List<AudioDataReference> audioRefs = soundi.getAudioDataReferences();
            for (int j = 0; j < audioRefs.size(); j++)
            {
                try
                {
                    Object wb = resolvePath(project.getWaveBankRootGroup(),
                                            audioRefs.get(j).getWaveBank(),
                                            "WaveBank");
                    if (wb == waveBank)
                    {
                        referencingSoundIds.add(id);
                        break;
                    }
                }
                catch (ProjectDataException e)
                {
                    throw new RuntimeException();
                }
            }
        }

        return referencingSoundIds;
    }

    public static void removeGroup(KowalskiProject project, NodeWithIDAndComments parent, 
                                   NodeWithIDAndComments group)

    {
        
        List<NodeWithIDAndComments> siblingGroups = getChildGroups(parent);

        if (!siblingGroups.contains(group))
        {
            throw new RuntimeException("Attempting to remove event group '" + group.getID() + "'" +
                    " from a group that is not its parent");
        }

        if (parent instanceof EventGroup)
        {
            ((EventGroup)parent).getSubGroups().remove((EventGroup)group);
        }
        else if (parent instanceof SoundGroup)
        {
            ((SoundGroup)parent).getSubGroups().remove((SoundGroup)group);
        }
        else if (parent instanceof WaveBankGroup)
        {
            ((WaveBankGroup)parent).getSubGroups().remove((WaveBankGroup)group);
        }
        else if (parent instanceof MixPresetGroup)
        {
            ((MixPresetGroup)parent).getSubGroups().remove((MixPresetGroup)group);
        }
        else
        {
            throw new RuntimeException();
        }

    }

    public static void createSound(KowalskiProject project, SoundGroup group, String eventID)
    {
        //the ID that the event will end up having. will change if the desired ID is already taken.
        String actualID = eventID;
        int i = 2;
        while (doesNodeHaveChildWithID(group, actualID))
        {
            actualID = eventID + "_" + i;
            i++;
        }

        Sound newSound = new Sound();
        newSound.setPlaybackMode(SoundPlaybackMode.SEQUENTIAL);
        newSound.setID(actualID);
        group.getSounds().add(newSound);

    }

    public static void moveMixBus(KowalskiProject project, MixBus busToMove, MixBus targetBus)
            throws ProjectDataException
    {
        if (busToMove == targetBus)
        {
            return;
        }
        if (targetBus.getSubBuses().contains(busToMove))
        {
            return;
        }

        if (busToMove.getID().equals("master"))
        {
            throw new ProjectDataException("Attempting to move the 'master' bus.");
        }
        if (isMixBusDecendant(busToMove, targetBus, false))
        {
            throw new ProjectDataException("Attempting to move a mix bus to one of its sub-buses.");
        }

        //find the bus containing the bus to move
        MixBus parentBus = getMixBusParent(project, busToMove);
        if (parentBus == null)
        {
            throw new RuntimeException("The parent of a non-master bus should not be null.");
        }
        //and move the bus
        parentBus.getSubBuses().remove(busToMove);
        int idx = getLexicographicalIndexOf(getChildGroups(targetBus), busToMove);
        targetBus.getSubBuses().add(idx, busToMove);

    }

    private static boolean isMixBusDecendant(MixBus topBus,
                                             MixBus potentialDecendant,
                                             boolean foundMatch)
    {
        List<MixBus> subBuses = topBus.getSubBuses();

        if (subBuses.contains(potentialDecendant))
        {
            return true;
        }

        for (int i = 0; i < subBuses.size(); i++)
        {
            return foundMatch | isMixBusDecendant(subBuses.get(i), potentialDecendant, false);
        }

        return false;
    }

    private static MixBus getMixBusParent(KowalskiProject project, MixBus mixBusToFind)
    {
        List<MixBus> mixBuses = getMixBusList(project);

        for (int i = 0; i < mixBuses.size(); i++)
        {
            if (mixBuses.get(i).getSubBuses().contains(mixBusToFind))
            {
                return mixBuses.get(i);
            }
        }

        return null;
    }

    private static boolean isMixBusIDTaken(KowalskiProject project, String busID)
    {
        List<MixBus> mixBuses = getMixBusList(project);

        for (int i = 0; i < mixBuses.size(); i++)
        {
            if (mixBuses.get(i).getID().equals(busID))
            {
                return true;
            }
        }

        return false;
    }
}
