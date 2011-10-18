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

package kowalski.editor;

import kowalski.util.IconManager;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultTreeCellRenderer;
import kowalski.tools.data.AudioFileParser;
import kowalski.tools.data.ProjectDataException;
import kowalski.tools.data.ProjectDataUtils;
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
import kowalski.tools.data.xml.Sound;
import kowalski.tools.data.xml.SoundGroup;
import kowalski.tools.data.xml.SoundReference;
import kowalski.tools.data.xml.WaveBank;
import kowalski.tools.data.xml.WaveBankGroup;

/**
 *
 */
class ProjectTreeCellRenderer extends DefaultTreeCellRenderer
{
    private Map<Class, Icon> iconsByClass = new Hashtable<Class, Icon>();
    private ImageIcon invalidWaveIcon = IconManager.getImageIcon("invalidwaveicon.png");
    private ImageIcon oggVorbisIcon = IconManager.getImageIcon("oggvorbisicon.png");
    private ImageIcon adpcmIcon = IconManager.getImageIcon("waveiconcompressed.png");
    private ImageIcon invalidWaveRefIcon = IconManager.getImageIcon("invalidwavereficon.png");
    private ImageIcon soundRootIcon = IconManager.getImageIcon("soundsicon.png");
    private ImageIcon eventRootIcon = IconManager.getImageIcon("eventsicon.png");
    private ImageIcon waveBankRootIcon = IconManager.getImageIcon("wavebanksicon.png");
    private ImageIcon mixPresetRootIcon = IconManager.getImageIcon("mixpresetsicon.png");
    private Border dropAcceptBorder = new LineBorder(new Color(0.0f, 0.7f, 0.0f, 0.3f), 1);
    private Border dropRejectBorder = null;

    ProjectTreeCellRenderer()
    {
        iconsByClass.put(Event.class, IconManager.getImageIcon("eventicon.png"));
        //iconsByClass.put(EventRootGroup.class, IconManager.getImageIcon("eventsicon.png"));
        iconsByClass.put(SoundGroup.class, IconManager.getImageIcon("groupicon.png"));
        iconsByClass.put(EventGroup.class, IconManager.getImageIcon("groupicon.png"));
        iconsByClass.put(MixPresetGroup.class, IconManager.getImageIcon("groupicon.png"));
        iconsByClass.put(WaveBankGroup.class, IconManager.getImageIcon("groupicon.png"));
        iconsByClass.put(MixBus.class, IconManager.getImageIcon("mixbusicon.png"));
        iconsByClass.put(Sound.class, IconManager.getImageIcon("soundicon.png"));
        iconsByClass.put(WaveBank.class, IconManager.getImageIcon("wavebankicon.png"));
        iconsByClass.put(AudioData.class, IconManager.getImageIcon("waveicon.png"));
        iconsByClass.put(AudioDataReference.class, IconManager.getImageIcon("wavereficon.png"));
        iconsByClass.put(KowalskiProject.class, IconManager.getImageIcon("logo.png"));
        //iconsByClass.put(SoundRootGroup.class, IconManager.getImageIcon("soundsicon.png"));
        //iconsByClass.put(MasterMixBus.class, IconManager.getImageIcon("mixbusesicon.png"));
        //iconsByClass.put(MixPresetRootGroup.class, IconManager.getImageIcon("mixpresetsicon.png"));
        iconsByClass.put(MixBusParameters.class, IconManager.getImageIcon("mixbusparametersicon.png"));
        iconsByClass.put(MixPreset.class, IconManager.getImageIcon("mixpreseticon.png"));
        //iconsByClass.put(WaveBankRootGroup.class, IconManager.getImageIcon("wavebanksicon.png"));
        iconsByClass.put(SoundReference.class, IconManager.getImageIcon("soundreficon.png"));
    }



    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus)
    {
        setBorder(dropRejectBorder);
        KowalskiProject project = KowalskiEditorFrame.getInstance().getCurrentProject();
        if (project == null)
        {
            return this;
        }

        if (value instanceof NodeWithIDAndComments)
        {
            setToolTipText(((NodeWithIDAndComments)value).getComments());
        }
        else
        {
            setToolTipText(null);
        }

        setOpaque(false);
        Color fg = null;
        JTree.DropLocation dropLocation = tree.getDropLocation();
        if (dropLocation != null
         && dropLocation.getChildIndex() == -1
         && tree.getRowForPath(dropLocation.getPath()) == row)
        {

           /*
           Color col = Color.RED;//DefaultLookup.getColor(this, ui, "Tree.dropCellForeground");
            if (col != null)
            {
                fg = col;
            }
            else
            {
                fg = getTextSelectionColor();
            }*/
            setBackground(new Color(0.2f, 0.8f, 0.0f, 0.2f));
            setOpaque(true);
        }
        else if (sel)
        {
            fg = getTextSelectionColor();
        }
        else
        {
            fg = getTextNonSelectionColor();
        }

        
        setForeground(fg);

        if (value == null)
        {
            setText("null");
            setIcon(null);
        }
        else
        {
        
            setIcon(iconsByClass.get(value.getClass()));

            if (value instanceof KowalskiProject)
            {
                KowalskiProject p = (KowalskiProject)value;
                setText("Kowalski Project");
            }
            else if (value instanceof AudioData)
            {
                AudioData audioData = (AudioData)value;
                
                File audioDataFile = null;

                try
                {
                    //TODO: dont do this every render?
                    audioDataFile = ProjectDataUtils.getAudioDataFile(
                        KowalskiEditorFrame.getInstance().getCurrentProject(),
                        KowalskiEditorFrame.getInstance().getCurrentProjectDir(),
                        audioData);

                    /*
                    if (AudioFileParser.isOggVorbis(audioDataFile))
                    {
                        setIcon(oggVorbisIcon);
                    }
                    else if (AudioFileParser.isIMAADPCMWav(audioDataFile))
                    {
                        setIcon(adpcmIcon);
                    }*/
                }
                catch (ProjectDataException e)
                {
                    setIcon(invalidWaveIcon);
                }

                setText(audioData.getRelativePath());
            }
            else if (value instanceof AudioDataReference)
            {
                AudioDataReference ref = (AudioDataReference)value;
                try
                {
                    //TODO: dont do this every render?
                    AudioData d = ProjectDataUtils.resolveAudioDataReference(
                        KowalskiEditorFrame.getInstance().getCurrentProject(), ref);
                    ProjectDataUtils.getAudioDataFile(
                        KowalskiEditorFrame.getInstance().getCurrentProject(),
                        KowalskiEditorFrame.getInstance().getCurrentProjectDir(),
                        d);
                }
                catch (ProjectDataException e)
                {
                    setIcon(invalidWaveRefIcon);
                }
                setText(ref.getRelativePath() + " (" + ref.getWaveBank() + ")");
            }
            else if (value instanceof MixPreset)
            {
                MixPreset w = (MixPreset)value;
                setText(w.getID());
            }
            else if (value instanceof MixBusParameters)
            {
                MixBusParameters w = (MixBusParameters)value;
                setText(w.getMixBus());
            }
            else if (value instanceof SoundReference)
            {
                SoundReference w = (SoundReference)value;
                setText(w.getReferenceString());
            }
            
            else if (value == project.getMasterMixBus())
            {
                setText("<html><b>Master Mix Bus</b></html>");
                //setIcon(mixPresetRootIcon);
            }
            else if (value == project.getMixPresetRootGroup())
            {
                setText("<html><b>Mix Presets</b></html>");
                setIcon(mixPresetRootIcon);
            }
            else if (value == project.getMasterMixBus())
            {
                setText("<html><b>Mix Buses</b></html>");
            }
            else if (value == project.getWaveBankRootGroup())
            {
                setText("<html><b>Wave Banks</b></html>");
                setIcon(waveBankRootIcon);
            }
            else if (value == project.getSoundRootGroup())
            {
                setText("<html><b>Sounds</b></html>");
                setIcon(soundRootIcon);
            }
            else if (value == project.getEventRootGroup())
            {
                setText("<html><b>Events</b></html>");
                setIcon(eventRootIcon);
            }
            else if (value instanceof NodeWithIDAndComments)
            {
                setText(((NodeWithIDAndComments)value).getID());
            }
            else
            {
                setText(value.getClass().getSimpleName());
            }
        }

        selected = sel;

    	return this;
    }
     
}
