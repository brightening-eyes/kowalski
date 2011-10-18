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
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import kowalski.tools.data.xml.Event;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.MixPreset;
import kowalski.tools.data.xml.NodeWithIDAndComments;
import kowalski.tools.data.xml.Sound;
import kowalski.tools.data.xml.WaveBank;


/**
 *
 */
class EditPanel extends AbstractPanel implements ProjectDataChangeListener
{
    private static final int TREE_WIDTH = 250;
    private JPanel currentNodeEditPanel;
    private JSplitPane waveTreeSplitPane;

    EditPanel(ProjectTree projectTree, WaveDirectoryTree waveDirectoryTree)
    {
        setLayout(new BorderLayout());
        JScrollPane projTreeScrollPane = new JScrollPane(projectTree);
        
        JSplitPane projTreeSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        projTreeScrollPane.setPreferredSize(new Dimension(TREE_WIDTH, 100));
        projTreeSplitPane.setResizeWeight(0);
        projTreeSplitPane.setBorder(null);
        projTreeSplitPane.setOpaque(false);

        JScrollPane waveTreeScrollPane = new JScrollPane(waveDirectoryTree);
        waveTreeScrollPane.setPreferredSize(new Dimension(TREE_WIDTH, 100));
        waveTreeSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        waveTreeSplitPane.setResizeWeight(1);
        waveTreeSplitPane.setBorder(null);
        waveTreeSplitPane.setRightComponent(waveTreeScrollPane);
        waveTreeSplitPane.setOpaque(false);
        currentNodeEditPanel = new JPanel();
        waveTreeSplitPane.setLeftComponent(currentNodeEditPanel);

        projTreeSplitPane.setLeftComponent(projTreeScrollPane);
        projTreeSplitPane.setRightComponent(waveTreeSplitPane);

        add(projTreeSplitPane, BorderLayout.CENTER);

        projectTree.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {
                onProjectTreeSelectionChanged(e.getPath().getLastPathComponent());
            }
        });

        currentNodeEditPanel = new JPanel(new GridLayout(1, 1));
        currentNodeEditPanel.setOpaque(false);
        currentNodeEditPanel.setLayout(new BorderLayout());
        currentNodeEditPanel.setMinimumSize(new Dimension(100, 100));

        //soundEditorPanel = new SoundEditorPanel();
//        /eventEditorPanel = new EventEditorPanel();
    }

    private void onProjectTreeSelectionChanged(Object userObject)
    {
        if (userObject == null)
        {
            return;
        }

        if (!(userObject instanceof NodeWithIDAndComments))
        {
            return;
        }
        
        KowalskiProject project =
                KowalskiEditorFrame.getInstance().getCurrentProject();

        if (userObject == project.getEventRootGroup() ||
            userObject == project.getSoundRootGroup() ||
            userObject == project.getMixPresetRootGroup() ||
            userObject == project.getMasterMixBus() ||
            userObject == project.getWaveBankRootGroup())
        {
            return;
        }

        if (userObject instanceof Sound)
        {
            currentNodeEditPanel = new SoundEditorPanel(project, (Sound)userObject);
        }
        else if (userObject instanceof Event)
        {
            currentNodeEditPanel = new EventEditorPanel(project, (Event)userObject);
        }
        else if (userObject instanceof MixPreset)
        {
            currentNodeEditPanel = new MixPresetEditorPanel(project, (MixPreset)userObject);
        }
        else if (userObject instanceof WaveBank)
        {
            currentNodeEditPanel = new WaveBankEditorPanel(project, (WaveBank)userObject);
        }
        else if (userObject instanceof NodeWithIDAndComments)
        {
            currentNodeEditPanel = new NodeWithIDAndCommentsEditorPanel(project, (NodeWithIDAndComments)userObject);
        }

        int divLocation = waveTreeSplitPane.getDividerLocation();
        waveTreeSplitPane.setLeftComponent(currentNodeEditPanel);
        waveTreeSplitPane.setDividerLocation(divLocation);
        repaint();
         
    }

    public void onProjectDataChanged(KowalskiProject project)
    {
        
    }
}
