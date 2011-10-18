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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.bind.JAXBException;
import kowalski.tools.data.ProjectDataException;
import kowalski.tools.data.ProjectDataUtils;
import kowalski.tools.data.ProjectDataXMLSerializer;
import kowalski.editor.actions.AbstractProjectDataAction;
import kowalski.editor.actions.ChangeSoundParameterAction;
import kowalski.tools.data.xml.Event;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.MixPreset;
import kowalski.tools.data.xml.NodeWithIDAndComments;
import kowalski.tools.data.xml.Sound;
import kowalski.tools.data.xml.WaveBank;

/**
 *
 */
/**
 * A panel for editing a number of named properties
 */
class NodeWithIDAndCommentsEditorPanel extends AbstractPanel
{
    protected KowalskiProject project;
    protected NodeWithIDAndComments node;
    private JTextArea commentsTextArea;
    private JTextField idTextField;
    protected List<JLabel> labelList = new ArrayList<JLabel>();
    protected List<JComponent> componentList = new ArrayList<JComponent>();

    NodeWithIDAndCommentsEditorPanel(KowalskiProject p, NodeWithIDAndComments n)
    {
        if (n == null || p == null)
        {
            throw new IllegalArgumentException();
        }

        project = p;

        node = n;
        
        //ID field at the top
        labelList.add(new JLabel("ID"));
        idTextField = new JTextField(node.getID());
        idTextField.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
                
            }

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown())
                {
                    onChange();
                }
            }

            public void keyReleased(KeyEvent e) {

            }
        });
        componentList.add(idTextField);

        //then node type specific stuff
        addComponents();

        //and finally the comment field at the bottom
        labelList.add(new JLabel("Comments"));
        commentsTextArea = new JTextArea(node.getComments());
        commentsTextArea.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
                
            }

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown())
                {
                    onChange();
                }
            }

            public void keyReleased(KeyEvent e) {
                
            }
        });
        JScrollPane sp = new JScrollPane(commentsTextArea);
        sp.setPreferredSize(new Dimension(100, 100));
        componentList.add(sp);


        double maxWidth = 0;
        Dimension maxDim = null;
        for (int i = 0; i < labelList.size(); i++)
        {
            JLabel label = labelList.get(i);
            double preferredWidth = label.getPreferredSize().getWidth();
            if (preferredWidth > maxWidth)
            {
                maxWidth = preferredWidth;
                maxDim = label.getPreferredSize();
            }
        }

        //make all labels as wide as the widest
        for (int i = 0; i < labelList.size(); i++)
        {
            labelList.get(i).setMaximumSize(maxDim);
            labelList.get(i).setMinimumSize(maxDim);
            labelList.get(i).setPreferredSize(maxDim);
        }

        //add the label-component pairs to the panel
        JPanel mainPanel = new AbstractPanel(); //TODO: rename... not abstract...
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        for (int i = 0; i < labelList.size(); i++)
        {
            JPanel paneli = new AbstractPanel(new BorderLayout());
            JPanel labelPanel = new AbstractPanel(new BorderLayout());
            labelPanel.add(labelList.get(i), BorderLayout.NORTH);
            paneli.add(labelPanel, BorderLayout.WEST);

            paneli.add(componentList.get(i), BorderLayout.CENTER);
            mainPanel.add(paneli);
        }

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.NORTH);

    }

    void addComponents()
    {
        //
    }

    private void onChange()
    {
        
    }

}

class MixPresetEditorPanel extends NodeWithIDAndCommentsEditorPanel
{

    MixPresetEditorPanel(KowalskiProject p, MixPreset m)
    {
        super(p, m);
    }

    @Override
    void addComponents()
    {
        super.addComponents();
        labelList.add(new JLabel("Parameter sets"));
        componentList.add(new ParameterSetTable((MixPreset)node));
    }
}

class EventEditorPanel extends NodeWithIDAndCommentsEditorPanel
{
    private ConeEditorPanel coneEditorPanel;

    EventEditorPanel(KowalskiProject p, Event e)
    {
        super(p, e);
    }

    @Override
    void addComponents()
    {
        super.addComponents();

        //Bus
        labelList.add(new JLabel("Mix Bus"));
        JComboBox busCombo = new JComboBox();
        componentList.add(busCombo);

        //Retrigger mode
        labelList.add(new JLabel("Retrigger Mode"));
        JComboBox retriggerCombo = new JComboBox();
        componentList.add(retriggerCombo);

        //Instance count
        labelList.add(new JLabel("Instance Count"));
        JTextField countField = new JTextField();
        componentList.add(countField);

        //gain
        labelList.add(new JLabel("Gain"));
        GainSlider gainSlider = new GainSlider();
        componentList.add(gainSlider);

        //pitch
        labelList.add(new JLabel("Pitch"));
        JSlider pitchSlider = new PercentSlider(200);
        componentList.add(pitchSlider);

        //3D
        labelList.add(new JLabel("Positional"));
        JCheckBox checkBox3D = new JCheckBox("");
        checkBox3D.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e)
            {
                coneEditorPanel.setComponentsEnabled(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        componentList.add(checkBox3D);

        //inner cone
        coneEditorPanel = new ConeEditorPanel();
        labelList.add(new JLabel("Cone"));
        componentList.add(coneEditorPanel);

        
    }
}

class WaveBankEditorPanel extends NodeWithIDAndCommentsEditorPanel
{
    WaveBankEditorPanel(KowalskiProject p, WaveBank w)
    {
        super(p, w);
    }

    @Override
    void addComponents()
    {
        super.addComponents();
        labelList.add(new JLabel("Audio files"));
        componentList.add(new AudioDataTable((WaveBank)node));
    }
}

class SoundEditorPanel extends NodeWithIDAndCommentsEditorPanel
{
    JTextField loopCountTextField;

    SoundEditorPanel(KowalskiProject p, Sound s)
    {
        super(p, s);
    }

    @Override
    void addComponents()
    {
        super.addComponents();
        Sound sound = (Sound)node;

        loopCountTextField = new JTextField(sound.getPlaybackCount() + "");
        
        loopCountTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                AbstractProjectDataAction action =
                        new ChangeSoundParameterAction(project,
                                (Sound)node,
                                ChangeSoundParameterAction.LOOP_COUNT,
                                loopCountTextField.getText());
                KowalskiEditorFrame.getInstance().pushAndPerformAction(action);
            }
        });
        
        componentList.add(loopCountTextField);
        labelList.add(new JLabel("Playback count"));
        
        /*
        JComboBox playbackModeCombo = new JComboBox(SoundPlaybackMode.values());
        playbackModeCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                    ((Sound)node).setPlaybackMode(
                            (SoundPlaybackMode)playbackModeCombo.getSelectedItem());
                }
            }
        };


        pitchSlider = new FloatJSlider("pitch", -12, 12);
        pitchSlider.
                addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (currentSound != null)
                {
                    currentSound.setPitch(pitchSlider.getFloatValue());
                }
            }
        });

        pitchVariationPercentSlider = new FloatJSlider("pitch variation",
                0,
                100);
        pitchVariationPercentSlider.
                addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (currentSound != null)
                {
                    currentSound.setPitchVariationPercent(pitchVariationPercentSlider.getFloatValue());
                }
            }
        });

        gainSlider = new FloatJSlider("gain", 0, 2);
        gainSlider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (currentSound != null)
                {
                    currentSound.setGain(gainSlider.getFloatValue());
                }
            }
        });

        gainVariationPercentSlider = new FloatJSlider("gain variation",
                0,
                100);
        gainVariationPercentSlider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (currentSound != null)
                {
                    currentSound.setGainVariationPercent(gainVariationPercentSlider.getFloatValue());
                }
            }
        });
        */
        //gain
        labelList.add(new JLabel("Gain"));
        JSlider gainSlider = new GainSlider();
        componentList.add(gainSlider);

        labelList.add(new JLabel("Gainvariation"));
        JSlider gainVarSlider = new PercentSlider(100);
        componentList.add(gainVarSlider);

        //pitch
        labelList.add(new JLabel("Pitch"));
        JSlider pitchSlider = new PercentSlider(200);
        componentList.add(pitchSlider);

        labelList.add(new JLabel("Pitch variation"));
        JSlider pitchVarSlider = new PercentSlider(100);
        componentList.add(pitchVarSlider);

        labelList.add(new JLabel("Audio files"));
        componentList.add(new AudioDataReferenceTable(sound));
    }
}