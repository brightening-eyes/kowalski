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

package kowalski.tools.binaryfileviewer;

import java.awt.BorderLayout;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import kowalski.tools.data.EngineDataBuilder;
import kowalski.tools.data.WaveBankBuilder;
import kowalski.util.JFrameWithKowalskiIcon;



/**
 * A frame displaying the contents of a wave bank or project data
 * binary file in a JTree component.
 */
public class BinaryFileViewerFrame extends JFrameWithKowalskiIcon
{
    /** The JTree responsible for viewing the binary file. */
    private JTree viewTree;
    /** A label used to display some extra info, e.g file size.*/
    private JLabel infoLabel;
    /** The default width of the viewer frame.*/
    private static final int INITIAL_WIDTH = 500;
    /** The default height of the viewer frame.*/
    private static final int INITIAL_HEIGHT = 500;
    /** The total number of bytes of streaming audio. Wave banks only.*/
    private int numStreamingBytes = -1;
    /** The total number of bytes of non-streaming audio. Wave banks only.*/
    private int numNonStreamingBytes = -1;

    /**
     * Creates a new viewer frame.
     * @param binaryFile The file to view
     * @throws IOException if there was an error reading the input file.
     */
    public BinaryFileViewerFrame(File binaryFile)
            throws IOException
    {
        super();
        
        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
       
        JPanel contentPanel = new JPanel(new BorderLayout());
        
        //create binary data viewer tree...
        viewTree = createTree(binaryFile);
        viewTree.setEditable(false);
        viewTree.setCellRenderer(new BinaryFileViewerCellRenderer());
        viewTree.setShowsRootHandles(true);

        //...and expand all nodes
        for (int i = 0; i < viewTree.getRowCount(); i++)
        {
            viewTree.expandRow(i);
        }

        //add the tree and the info label to the frame.
        JScrollPane treeScrollPane = new JScrollPane(viewTree);
        contentPanel.add(treeScrollPane, BorderLayout.CENTER);
        infoLabel = new JLabel();
        contentPanel.add(infoLabel, BorderLayout.SOUTH);
        if (numStreamingBytes < 0 && numNonStreamingBytes < 0)
        {
            infoLabel.setText("File size: " + binaryFile.length() + " bytes");
        }
        else
        {
            infoLabel.setText("File size: " + binaryFile.length() + " bytes (" +
                    numNonStreamingBytes + " non-streaming, " + numStreamingBytes + " streaming)");
        }
        add(contentPanel);
    }

    /**
     * Creates a JTree visualizing the contents of a given wave bank or engine data file.
     * @param binaryFile The file to view.
     * @return A JTree visualizing the file.
     */
    private JTree createTree(File binaryFile)
        throws IOException
    {
        //open the file
        FileInputStream fis = new FileInputStream(binaryFile);
        DataInputStream dis = new DataInputStream(fis);

        //check if it's a wave bank or engine data binary
        boolean isEngineData = true;
        boolean isWaveBankData = true;
        
        byte[] fid = EngineDataBuilder.ENGINE_DATA_FILE_IDENTIFIER;
        for (int i = 0; i < fid.length; i++)
        {
            byte b = dis.readByte();
            if (fid[i] != b)
            {
                isEngineData = false;
                break;
            }
        }
        dis.close();

        fis = new FileInputStream(binaryFile);
        dis = new DataInputStream(fis);

        fid = WaveBankBuilder.WAVE_BANK_FILE_IDENTIFIER;
        for (int i = 0; i < fid.length; i++)
        {
            byte b = dis.readByte();
            if (fid[i] != b)
            {
                isWaveBankData = false;
                break;
            }
        }
        dis.close();
        
        fis = new FileInputStream(binaryFile);
        dis = new DataInputStream(fis);

        //create a wave bank or engine data viewer tree
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(binaryFile);
        if (isEngineData)
        {
            populateEngineDataTree(dis, rootNode);
            dis.close();
            return new JTree(rootNode);
        }
        else if (isWaveBankData)
        {
            populateWaveBankDataTree(dis, rootNode);
            dis.close();
            return new JTree(rootNode);
        }

        dis.close();
        throw new IOException("Unknown file format.");
    }

    private void populateWaveBankDataTree(DataInputStream dis, DefaultMutableTreeNode rootNode)
            throws IOException
    {
        numStreamingBytes = 0;
        numNonStreamingBytes = 0;

        byte[] identifier = new byte[EngineDataBuilder.ENGINE_DATA_FILE_IDENTIFIER.length];
        dis.read(identifier);
        rootNode.add(new BinaryFileViewerTreeNode("File identifier", identifier));

        rootNode.add(new BinaryFileViewerTreeNode("Wave bank ID", readASCIIString(dis)));
        int numEntries = dis.readInt();
        rootNode.add(new BinaryFileViewerTreeNode("Audio data entry count", numEntries));

        for (int i = 0; i < numEntries; i++)
        {
            String filename = readASCIIString(dis);

            DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode(toHTMLBold("Audio data entry (" + i + ")"));
            entryNode.add(new BinaryFileViewerTreeNode("Filename", filename));
            rootNode.add(entryNode);
            int encoding  = dis.readInt();
            entryNode.add(new BinaryFileViewerTreeNode("Encoding", encoding));
            int stream = dis.readInt();
            entryNode.add(new BinaryFileViewerTreeNode("Stream from disk", stream));
            int numChannels = dis.readInt();
            entryNode.add(new BinaryFileViewerTreeNode("Num channels", numChannels));
            int numBytes = dis.readInt();
            entryNode.add(new BinaryFileViewerTreeNode("Audio data size", numBytes));
            if (stream != 0)
            {
                numStreamingBytes += numBytes;
            }
            else
            {
                numNonStreamingBytes += numBytes;
            }
            dis.skip(numBytes);
            entryNode.add(new DefaultMutableTreeNode("Audio data"));
        }
    }

    private void populateEngineDataTree(DataInputStream dis, DefaultMutableTreeNode rootNode)
            throws IOException
    {

        byte[] identifier = new byte[EngineDataBuilder.ENGINE_DATA_FILE_IDENTIFIER.length];
        dis.read(identifier);
        rootNode.add(new BinaryFileViewerTreeNode("File identifier", identifier));

        //Wave banks chunk
        DefaultMutableTreeNode waveBanksNode =
                new DefaultMutableTreeNode(toHTMLBold("Wave banks chunk"));
        rootNode.add(waveBanksNode);
        populateWaveBanksSubTree(dis, waveBanksNode);

        // mix buses chunk
        DefaultMutableTreeNode mixBusesNode =
                new DefaultMutableTreeNode(toHTMLBold("Mix buses chunk"));
        rootNode.add(mixBusesNode);
        int numMixBuses = populateMixBusesSubTree(dis, mixBusesNode);

        // mix presets chunk
        DefaultMutableTreeNode mixPresetsNode =
                new DefaultMutableTreeNode(toHTMLBold("Mix presets chunk"));
        rootNode.add(mixPresetsNode);
        populateMixPresetsSubTree(dis, mixPresetsNode, numMixBuses);

        // sounds chunk
        DefaultMutableTreeNode soundsNode =
                new DefaultMutableTreeNode(toHTMLBold("Sounds chunk"));
        rootNode.add(soundsNode);
        populateSoundsSubTree(dis, soundsNode);

        //events chunk
        DefaultMutableTreeNode eventsNode =
                new DefaultMutableTreeNode(toHTMLBold("Events chunk"));
        rootNode.add(eventsNode);
        populateEventsSubTree(dis, eventsNode);
    }

    private void populateWaveBanksSubTree(DataInputStream dis, DefaultMutableTreeNode waveBanksNode)
            throws IOException
    {
        int chunkId = dis.readInt();

        if (chunkId != EngineDataBuilder.WAVE_BANKS_CHUNK_ID)
        {
            throw new IOException("Expected wave bank chunk identifier, got " + chunkId);
        }

        waveBanksNode.add(new BinaryFileViewerTreeNode("Chunk ID", chunkId));
        waveBanksNode.add(new BinaryFileViewerTreeNode("Chunk size", dis.readInt()));

        final int numAudioDataItems = dis.readInt();

        waveBanksNode.add(new BinaryFileViewerTreeNode("Audio data item count (total)", numAudioDataItems));
        final int numWaveBanks = dis.readInt();

        waveBanksNode.add(new BinaryFileViewerTreeNode("Wave bank count", numWaveBanks));

        for (int i = 0; i < numWaveBanks; i++)
        {
            DefaultMutableTreeNode waveBankNode = new DefaultMutableTreeNode(toHTMLBold("Wave bank (" + i + ")"));
            waveBanksNode.add(waveBankNode);
            String id = readASCIIString(dis);
            waveBankNode.add(new BinaryFileViewerTreeNode("ID", id));
            int itemCount = dis.readInt();
            waveBankNode.add(new BinaryFileViewerTreeNode("Audio data item count", itemCount));
            DefaultMutableTreeNode itemsNode =
                new DefaultMutableTreeNode(toHTMLBold("Audio data items"));
            waveBankNode.add(itemsNode);

            for (int j = 0; j < itemCount; j++)
            {

                String fileName = readASCIIString(dis);

                itemsNode.add(new BinaryFileViewerTreeNode("File name", fileName));
            }
        }
    }

    private int populateMixBusesSubTree(DataInputStream dis, DefaultMutableTreeNode mixBusesNode)
            throws IOException
    {
        int chunkId = dis.readInt();

        if (chunkId != EngineDataBuilder.MIX_BUSES_CHUNK_ID)
        {
            throw new IOException("Expected mix buses chunk identifier, got " + chunkId);
        }

        mixBusesNode.add(new BinaryFileViewerTreeNode("Chunk ID", chunkId));
        mixBusesNode.add(new BinaryFileViewerTreeNode("Chunk size", dis.readInt()));
        final int numMixBuses = dis.readInt();
        mixBusesNode.add(new BinaryFileViewerTreeNode("Mix bus count", numMixBuses));
        for (int i = 0; i < numMixBuses; i++)
        {
            DefaultMutableTreeNode mixBusNode = new DefaultMutableTreeNode(toHTMLBold("Mix bus (" + i + ")"));
            mixBusesNode.add(mixBusNode);
            mixBusNode.add(new BinaryFileViewerTreeNode("ID", readASCIIString(dis)));
            int numSubBuses = dis.readInt();
            mixBusNode.add(new BinaryFileViewerTreeNode("Sub bus count", numSubBuses));
            DefaultMutableTreeNode subBusesNode = new DefaultMutableTreeNode(toHTMLBold("Sub bus indices"));
            if (numSubBuses > 0)
            {
                mixBusNode.add(subBusesNode);
                for (int j = 0; j < numSubBuses; j++)
                {
                    subBusesNode.add(new BinaryFileViewerTreeNode("Bus index", dis.readInt()));
                }
            }
        }

        return numMixBuses;
    }

    private void populateMixPresetsSubTree(DataInputStream dis, DefaultMutableTreeNode mixPresetsNode,
                                           int numMixBuses)
            throws IOException
    {
        int chunkId = dis.readInt();
        if (chunkId != EngineDataBuilder.MIX_PRESETS_CHUNK_ID)
        {
            throw new IOException("Expected mix presets chunk identifier, got " + chunkId);
        }

        mixPresetsNode.add(new BinaryFileViewerTreeNode("Chunk ID", chunkId));

        mixPresetsNode.add(new BinaryFileViewerTreeNode("Chunk size", dis.readInt()));
        int numMixPresets = dis.readInt();
        mixPresetsNode.add(new BinaryFileViewerTreeNode("Mix preset count", numMixPresets));
        for (int i = 0; i < numMixPresets; i++)
        {
            DefaultMutableTreeNode mixPresetNode =
                    new DefaultMutableTreeNode(toHTMLBold("Mix preset (" + i + ")"));
            mixPresetsNode.add(mixPresetNode);
            mixPresetNode.add(new BinaryFileViewerTreeNode("ID", readASCIIString(dis)));
            mixPresetNode.add(new BinaryFileViewerTreeNode("Is default", dis.readInt()));

            for (int j = 0; j < numMixBuses; j++)
            {
                DefaultMutableTreeNode parameterSetNode =
                    new DefaultMutableTreeNode(toHTMLBold("Parameter set (" + j + ")"));
                mixPresetNode.add(parameterSetNode);
                parameterSetNode.add(new BinaryFileViewerTreeNode("Mix bus index", dis.readInt()));
                parameterSetNode.add(new BinaryFileViewerTreeNode("Left gain", dis.readFloat()));
                parameterSetNode.add(new BinaryFileViewerTreeNode("Right gain", dis.readFloat()));
                parameterSetNode.add(new BinaryFileViewerTreeNode("Pitch", dis.readFloat()));
            }
        }
    }

    private void populateSoundsSubTree(DataInputStream dis, DefaultMutableTreeNode soundsNode)
            throws IOException
    {
        int chunkId = dis.readInt();
        if (chunkId != EngineDataBuilder.SOUNDS_CHUNK_ID)
        {
            throw new IOException("Expected sounds chunk identifier, got " + chunkId);
        }
        soundsNode.add(new BinaryFileViewerTreeNode("Chunk ID", chunkId));

        soundsNode.add(new BinaryFileViewerTreeNode("Chunk size", dis.readInt()));
        int numSoundDefs = dis.readInt();
        soundsNode.add(new BinaryFileViewerTreeNode("Sound definition count", numSoundDefs));
        for (int i = 0; i < numSoundDefs; i++)
        {
            DefaultMutableTreeNode soundNode =
                new DefaultMutableTreeNode(toHTMLBold("Sound (" + i + ")"));
            soundsNode.add(soundNode);
            soundNode.add(new BinaryFileViewerTreeNode("Playback count", dis.readInt()));
            soundNode.add(new BinaryFileViewerTreeNode("Defer stop", dis.readInt()));
            soundNode.add(new BinaryFileViewerTreeNode("Gain", dis.readFloat()));
            soundNode.add(new BinaryFileViewerTreeNode("Gain variation", dis.readFloat()));
            soundNode.add(new BinaryFileViewerTreeNode("Pitch", dis.readFloat()));
            soundNode.add(new BinaryFileViewerTreeNode("Pitch variation", dis.readFloat()));
            soundNode.add(new BinaryFileViewerTreeNode("Playback mode", dis.readInt()));
            int audioReferenceCount = dis.readInt();
            soundNode.add(new BinaryFileViewerTreeNode("Audio data reference count", audioReferenceCount));
            DefaultMutableTreeNode audioRefsNode =
                new DefaultMutableTreeNode(toHTMLBold("Audio data references"));
            soundNode.add(audioRefsNode);
            for (int j = 0; j < audioReferenceCount; j++)
            {
                audioRefsNode.add(new BinaryFileViewerTreeNode("Wave bank index", dis.readInt()));
                audioRefsNode.add(new BinaryFileViewerTreeNode("Audio data index", dis.readInt()));
            }
        }
    }

    private void populateEventsSubTree(DataInputStream dis, DefaultMutableTreeNode eventsNode)
            throws IOException
    {
        int chunkId = dis.readInt();
        if (chunkId != EngineDataBuilder.EVENTS_CHUNK_ID)
        {
            throw new IOException("Expected events chunk identifier, got " + chunkId);
        }
        eventsNode.add(new BinaryFileViewerTreeNode("Chunk ID", chunkId));


        eventsNode.add(new BinaryFileViewerTreeNode("Chunk size", dis.readInt()));

        int eventDefCount = dis.readInt();
        eventsNode.add(new BinaryFileViewerTreeNode("Event definition count", eventDefCount));
        for (int i = 0; i < eventDefCount; i++)
        {
            DefaultMutableTreeNode eventNode = new DefaultMutableTreeNode(toHTMLBold("Event (" + i + ")"));
            eventsNode.add(eventNode);
            eventNode.add(new BinaryFileViewerTreeNode("ID", readASCIIString(dis)));
            eventNode.add(new BinaryFileViewerTreeNode("Instance count", dis.readInt()));
            eventNode.add(new BinaryFileViewerTreeNode("Gain", dis.readFloat()));
            eventNode.add(new BinaryFileViewerTreeNode("Pitch", dis.readFloat()));
            eventNode.add(new BinaryFileViewerTreeNode("Inner cone angle", dis.readFloat()));
            eventNode.add(new BinaryFileViewerTreeNode("Outer cone angle", dis.readFloat()));
            eventNode.add(new BinaryFileViewerTreeNode("Outer cone gain", dis.readFloat()));
            eventNode.add(new BinaryFileViewerTreeNode("Mix bus index", dis.readInt()));
            eventNode.add(new BinaryFileViewerTreeNode("Is postitional", dis.readInt()));

            int numSoundRefs = 1;//for now... dis.readInt();
            for (int j = 0; j < numSoundRefs; j++)
            {
                eventNode.add(new BinaryFileViewerTreeNode("Sound index (non-streaming events only)",
                                                           dis.readInt()));
            }
            eventNode.add(new BinaryFileViewerTreeNode("Retrigger mode (non-streaming events only)",
                                                       dis.readInt()));

            eventNode.add(new BinaryFileViewerTreeNode("Wave bank index (streaming events only)",
                                                       dis.readInt()));
            eventNode.add(new BinaryFileViewerTreeNode("Audio data index (streaming events only)",
                                                       dis.readInt()));
            eventNode.add(new BinaryFileViewerTreeNode("Loop (streaming events only)",
                                                       dis.readInt()));

            
            int numReferencedWaveBanks = dis.readInt();
            DefaultMutableTreeNode waveBankRefsNode =
                new DefaultMutableTreeNode(toHTMLBold("Referenced wave bank indices"));
            eventNode.add(waveBankRefsNode);
            for (int j = 0; j < numReferencedWaveBanks; j++)
            {
                waveBankRefsNode.add(new BinaryFileViewerTreeNode("Index", dis.readInt()));
            }
        }
    }
    
    /**
     * Helper method that adds HTML bold formatting tags.
     * @param str
     * @return
     */
    private String toHTMLBold(String str)
    {
        return "<html><b>" + str + "</b></html>";
    }

    /**
     * Reads an ASCII string from a stream.
     * @param dis
     * @return
     * @throws Exception
     */
    private String readASCIIString(DataInputStream dis)
            throws IOException
    {
        int numChars = dis.readInt();
        byte[] bytes = new byte[numChars];
        dis.read(bytes);

        String ret = new String(bytes);
        return ret;
    }

}
