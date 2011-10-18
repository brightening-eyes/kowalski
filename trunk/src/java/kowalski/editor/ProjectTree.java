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

import kowalski.editor.actions.MoveEventAction;
import kowalski.editor.actions.MoveGroupAction;
import kowalski.editor.actions.AbstractProjectDataAction;
import java.awt.datatransfer.Transferable;
import kowalski.tools.data.ProjectDataUtils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DropMode;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import kowalski.editor.actions.CreateEventAction;
import kowalski.editor.actions.CreateGroupAction;
import kowalski.editor.actions.CreateMixBusAction;
import kowalski.editor.actions.CreateMixPresetAction;
import kowalski.editor.actions.CreateSoundAction;
import kowalski.editor.actions.CreateWaveBankAction;
import kowalski.editor.actions.MoveMixBusAction;
import kowalski.editor.actions.MoveMixPresetAction;
import kowalski.editor.actions.MoveSoundAction;
import kowalski.editor.actions.MoveWaveBankAction;
import kowalski.editor.actions.RemoveEventAction;
import kowalski.editor.actions.RemoveGroupAction;
import kowalski.editor.actions.RemoveMixBusAction;
import kowalski.editor.actions.RemoveMixPresetAction;
import kowalski.editor.actions.RemoveSoundAction;
import kowalski.editor.actions.RemoveWaveBankAction;
import kowalski.tools.data.xml.AudioData;
import kowalski.tools.data.xml.AudioDataReference;
import kowalski.tools.data.xml.Event;
import kowalski.tools.data.xml.EventGroup;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.MixBus;
import kowalski.tools.data.xml.MixPreset;
import kowalski.tools.data.xml.MixPresetGroup;
import kowalski.tools.data.xml.NodeWithExpansionState;
import kowalski.tools.data.xml.NodeWithIDAndComments;
import kowalski.tools.data.xml.Sound;
import kowalski.tools.data.xml.SoundGroup;
import kowalski.tools.data.xml.WaveBank;
import kowalski.tools.data.xml.WaveBankGroup;

class ProjectTree extends JTree
                  implements ActionListener, KeyListener, TreeExpansionListener, ProjectDataChangeListener
{

    private JPopupMenu popupMenu;
    private JMenuItem newGroupItem;
    private JMenuItem removeGroupItem;

    //sound operations
    private JMenuItem newSoundItem;
    private JMenuItem removeSoundItem;
    //event and eventgroup operations
    private JMenuItem newEventItem;
    private JMenuItem removeEventItem;
    //mix bus operations
    private JMenuItem newMixBusItem;
    private JMenuItem removeMixBusItem;
    //wave bank and wave bank groups operations
    private JMenuItem newWaveBankItem;
    private JMenuItem removeWaveBankItem;
    private JMenuItem removeAudioDataItem;
    //mix preset and mix preset groups operations
    private JMenuItem newMixPresetItem;
    private JMenuItem removeMixPresetItem;

    private Object popupMenuObject;

    ProjectTree(KowalskiProject project)
    {
        super();
        setTransferHandler(new MainTransferHandler());
        setDragEnabled(true);
        setDropMode(DropMode.ON);
        DefaultTreeModel d;
        ToolTipManager.sharedInstance().registerComponent(this);
        addKeyListener(this);
        addTreeExpansionListener(this);
        treeModel = new ProjectTreeModel(project);
        setModel(treeModel);
        setCellRenderer(new ProjectTreeCellRenderer());
        
        setShowsRootHandles(false);
        //create a mouse listener that listens for popup menu triggers
        addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger())
                {
                    onPopupMenuTriggered(e);
                }
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                {
                    onPopupMenuTriggered(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                {
                    onPopupMenuTriggered(e);
                }
            }

            public void mouseEntered(MouseEvent e) {
                
            }

            public void mouseExited(MouseEvent e) {
                
            }
        });
        
        initPopupMenu();
    }


    /*
    void setEventsVisible(boolean visible)
    {
        isEventsNodeVisible = visible;
        setProject(xmlProjectObject);
    }

    void setSoundsVisible(boolean visible)
    {
        isSoundsNodeVisible = visible;
        setProject(xmlProjectObject);
    }

    void setMixBusesVisible(boolean visible)
    {
        isMixBusesNodeVisible = visible;
        setProject(xmlProjectObject);
    }

    void setMixPresetsVisible(boolean visible)
    {
        isMixPresetsNodeVisible = visible;
        setProject(xmlProjectObject);
    }

    void setWaveBanksVisible(boolean visible)
    {
        isWaveBanksNodeVisible = visible;
        setProject(xmlProjectObject);
    }*/

    void reloadExpansionState()
    {
        for (int i = 0; i < getRowCount(); i++)
        {
            Object obj = getPathForRow(i).getLastPathComponent();
            if (!(obj instanceof NodeWithExpansionState))
            {
                continue;
            }
            boolean isExpanded = ((NodeWithExpansionState)obj).isExpanded();
            //System.out.println("tree row " + i + " expanded=" + isExpanded);
            if (isExpanded)
            {
                expandPath(getPathForRow(i));
            }
        }
    }

    
    void onNodeDropped(Object droppedNode,
                       Object targetNode)
    {
        System.out.println(droppedNode + " dropped on " + targetNode);
        if (targetNode == null)
        {
            //a node was dropped on nothing, do nothing
            return;
        }
        
        AbstractProjectDataAction action = createActionFromDrop(droppedNode, targetNode);
        
        if (action != null)
        {
            KowalskiEditorFrame frame = KowalskiEditorFrame.getInstance();
            frame.pushAndPerformAction(action);
        }
    }

    Transferable createTransferable()
    {
        Object node = getSelectionPath().getLastPathComponent();
        if (node instanceof KowalskiProject)
        {
            return null;
        }
        KowalskiProject currProj = KowalskiEditorFrame.getInstance().getCurrentProject();
        if (node == currProj.getEventRootGroup() ||
            node == currProj.getSoundRootGroup() ||
            node == currProj.getWaveBankRootGroup() ||
            node == currProj.getMixPresetRootGroup() ||
            node == currProj.getMasterMixBus())
        {
            return null;
        }

        return new ProjectObjectTransferable(node);
    }

    boolean canAcceptDrop(Object droppedNode, Object targetNode)
    {
        //System.out.println("ProjectTree.canAcceptDrop: " + droppedNode + " on " + targetNode);
        if (targetNode == droppedNode)
        {
            return false;
        }
        return (droppedNode instanceof EventGroup && targetNode instanceof EventGroup) ||
               (droppedNode instanceof Event && targetNode instanceof EventGroup) ||
               (droppedNode instanceof SoundGroup && targetNode instanceof SoundGroup) ||
               (droppedNode instanceof Sound && targetNode instanceof SoundGroup) ||
               (droppedNode instanceof AudioData && targetNode instanceof WaveBank) ||
               (droppedNode instanceof WaveBank && targetNode instanceof WaveBankGroup) ||
               (droppedNode instanceof WaveBankGroup && targetNode instanceof WaveBankGroup) ||
               (droppedNode instanceof MixPreset && targetNode instanceof MixPresetGroup) ||
               (droppedNode instanceof MixBus && targetNode instanceof MixBus) ||
               (droppedNode instanceof MixPresetGroup && targetNode instanceof MixPresetGroup);
    }


    private List<Object> getSelectedNodes()
    {
        List<Object> list = new ArrayList<Object>();
        for (TreePath p : getSelectionPaths())
        {
            Object o = p.getLastPathComponent();

            if (o != null)
            {
                if (list.contains(o))
                {
                    throw new RuntimeException();
                }
                list.add(o);
            }
        }

        return list;
    }

    private boolean areAllNodesOfType(List<Object> nodes, Class cl)
    {
        for (int i = 0; i < nodes.size(); i++)
        {
            if (!nodes.get(i).getClass().equals(cl))
            {
                return false;
            }
        }

        return true;
    }

    private AbstractProjectDataAction createActionFromDrop(Object droppedNode, Object targetNode)
    {
        System.out.println("are all nodes of type: " + areAllNodesOfType(getSelectedNodes(), EventGroup.class));

        if (droppedNode instanceof EventGroup && targetNode instanceof EventGroup)
        {
            return new MoveGroupAction(KowalskiEditorFrame.getInstance().getCurrentProject(),
                        (EventGroup)droppedNode, (EventGroup)targetNode);
        }
        else if (droppedNode instanceof Event && targetNode instanceof EventGroup)
        {
            return new MoveEventAction(KowalskiEditorFrame.getInstance().getCurrentProject(),
                        (Event)droppedNode, (EventGroup)targetNode);
        }
        else if (droppedNode instanceof SoundGroup && targetNode instanceof SoundGroup)
        {
            return new MoveGroupAction(KowalskiEditorFrame.getInstance().getCurrentProject(),
                        (SoundGroup)droppedNode, (SoundGroup)targetNode);
        }
        else if (droppedNode instanceof WaveBankGroup && targetNode instanceof WaveBankGroup)
        {
            return new MoveGroupAction(KowalskiEditorFrame.getInstance().getCurrentProject(),
                        (WaveBankGroup)droppedNode, (WaveBankGroup)targetNode);
        }
        else if (droppedNode instanceof WaveBank && targetNode instanceof WaveBankGroup)
        {
            return new MoveWaveBankAction(KowalskiEditorFrame.getInstance().getCurrentProject(),
                        (WaveBank)droppedNode, (WaveBankGroup)targetNode);
        }
        else if (droppedNode instanceof Sound && targetNode instanceof SoundGroup)
        {
            return new MoveSoundAction(KowalskiEditorFrame.getInstance().getCurrentProject(),
                        (Sound)droppedNode, (SoundGroup)targetNode);
        }
        else if (droppedNode instanceof MixPresetGroup && targetNode instanceof MixPresetGroup)
        {
            return new MoveGroupAction(KowalskiEditorFrame.getInstance().getCurrentProject(),
                        (MixPresetGroup)droppedNode, (MixPresetGroup)targetNode);
        }
        else if (droppedNode instanceof MixPreset && targetNode instanceof MixPresetGroup)
        {
            return new MoveMixPresetAction(KowalskiEditorFrame.getInstance().getCurrentProject(),
                        (MixPreset)droppedNode, (MixPresetGroup)targetNode);
        }
        else if (droppedNode instanceof MixBus && targetNode instanceof MixBus)
        {
            return new MoveMixBusAction(KowalskiEditorFrame.getInstance().getCurrentProject(),
                        (MixBus)droppedNode, (MixBus)targetNode);
        }

        return null;
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        File projectDir = KowalskiEditorFrame.getInstance().getCurrentProjectDir();
        if (getSelectionPath() == null)
        {
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_SPACE)
        {
            Object selectedObject =
                    getSelectionPath().getLastPathComponent();
            KowalskiProject project = KowalskiEditorFrame.getInstance().getCurrentProject();
            if (selectedObject == null)
            {
                return;
            }
            else if (selectedObject instanceof AudioData)
            {
                try
                {
                    File f = ProjectDataUtils.getAudioDataFile(project, projectDir,
                            (AudioData)selectedObject);
                    CachedSoundPlayer.playSound(f);
                }
                catch (Exception ex)
                {
                    JOptionPane.showMessageDialog(KowalskiEditorFrame.getInstance(), ex.getMessage(),
                    "Playback error",
                    JOptionPane.ERROR_MESSAGE);
                }
            }
            else if (selectedObject instanceof AudioDataReference)
            {
                try
                {
                    AudioData wd =
                            ProjectDataUtils.resolveAudioDataReference(project,
                                (AudioDataReference)selectedObject);
                    File f = 
                            ProjectDataUtils.getAudioDataFile(project, projectDir,
                                wd);
                    CachedSoundPlayer.playSound(f);

                }
                catch (Exception ex)
                {
                    JOptionPane.showMessageDialog(KowalskiEditorFrame.getInstance(), ex.getMessage(),
                    "Playback error",
                    JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        KowalskiProject project = KowalskiEditorFrame.getInstance().getCurrentProject();

        AbstractProjectDataAction action = null;

        //
        // group operations
        //
        if (e.getSource() == newGroupItem)
        {
            action =
                    new CreateGroupAction(project, 
                            (NodeWithIDAndComments)popupMenuObject, popupMenuObject.getClass().getSimpleName());
        }
        else if (e.getSource() == removeGroupItem)
        {
            action =
                    new RemoveGroupAction(project,
                            (NodeWithIDAndComments)popupMenuObject);
        }

        //wave banks
        else if (e.getSource() == newWaveBankItem)
        {
            action =
                    new CreateWaveBankAction(project,
                            (WaveBankGroup)popupMenuObject);
        }
        else if (e.getSource() == removeWaveBankItem)
        {
            action =
                    new RemoveWaveBankAction(project,
                            (WaveBank)popupMenuObject);
        }
       

        // sounds
        else if (e.getSource() == newSoundItem)
        {
            action = new CreateSoundAction(project, (SoundGroup)popupMenuObject);
        }
        else if (e.getSource() == removeSoundItem)
        {
            action = new RemoveSoundAction(project, (Sound)popupMenuObject);
        }

        //events
        else if (e.getSource() == newEventItem)
        {
            action = new CreateEventAction(project, (EventGroup)popupMenuObject);
        }
        else if (e.getSource() == removeEventItem)
        {
            action = new RemoveEventAction(project, (Event)popupMenuObject);
        }

        //mixbus
        else if (e.getSource() == newMixBusItem)
        {
            action = new CreateMixBusAction(project, (MixBus)popupMenuObject);
        }
        else if (e.getSource() == removeMixBusItem)
        {
            action = new RemoveMixBusAction(project, (MixBus)popupMenuObject);
        }

        //mix presets
        else if (e.getSource() == newMixPresetItem)
        {
            action = new CreateMixPresetAction(project, (MixPresetGroup)popupMenuObject);
        }
        else if (e.getSource() == removeMixPresetItem)
        {
            action = new RemoveMixPresetAction(project, (MixPreset)popupMenuObject);
        }

        if (action != null)
        {
            KowalskiEditorFrame.getInstance().pushAndPerformAction(action);
        }
    }

    /**
     * Constructs a menu item hierarchy corresponding to the mix buses.
     
    private JMenu getMixBusHierarchyMenuItem()
    {
        KowalskiProject project = KowalskiEditorFrame.getInstance().getCurrentProject();
        MixBus masterBus = project.getMasterMixBus();

        JMenu root = new JMenu(masterBus.getID());

        populateMixBusHierarchyMenuItem(masterBus, root);

    }

    private void populateMixBusHierarchyMenuItem(MixBus root, JMenuItem item)
    {
        List<MixBus> subBuses = root.getSubBuses();
        for (int i = 0; i < subBuses.size(); i++)
        {
            //item.add
        }
    }*/

    private void initPopupMenu()
    {
        popupMenu = new JPopupMenu();
        //sound and soundgroup operations
        newGroupItem = new JMenuItem("New Group");
        newGroupItem.addActionListener(this);
        removeGroupItem = new JMenuItem("Remove Group");
        removeGroupItem.addActionListener(this);
        newSoundItem = new JMenuItem("New Sound");
        newSoundItem.addActionListener(this);
        removeSoundItem = new JMenuItem("Remove Sound");
        removeSoundItem.addActionListener(this);

        //event and eventgroup operations
        newEventItem = new JMenuItem("New Event");
        newEventItem.addActionListener(this);
        removeEventItem = new JMenuItem("Remove Event");
        removeEventItem.addActionListener(this);

        //mix bus operations
        newMixBusItem = new JMenuItem("New Mix Bus");
        newMixBusItem.addActionListener(this);
        removeMixBusItem = new JMenuItem("Remove Mix Bus");
        removeMixBusItem.addActionListener(this);

        //wave bank and wave bank groups operations
        newWaveBankItem = new JMenuItem("New Wave Bank");
        newWaveBankItem.addActionListener(this);
        removeWaveBankItem = new JMenuItem("Remove Wave Bank");
        removeWaveBankItem.addActionListener(this);
        removeAudioDataItem = new JMenuItem("Remove Audio Data Item");
        removeAudioDataItem.addActionListener(this);

        //mix preset and mix preset groups operations
        newMixPresetItem = new JMenuItem("New Mix Preset");
        newMixPresetItem.addActionListener(this);
        removeMixPresetItem = new JMenuItem("Remove Mix Preset");
        removeMixPresetItem.addActionListener(this);
    }

    void onPopupMenuTriggered(MouseEvent e)
    {    
        popupMenu.removeAll();
        TreePath clickedPath = getClosestPathForLocation(e.getX(), e.getY());
        popupMenuObject = clickedPath.getLastPathComponent();
        KowalskiProject project = KowalskiEditorFrame.getInstance().getCurrentProject();

        if (popupMenuObject instanceof SoundGroup)
        {
            popupMenu.add(newSoundItem);
            popupMenu.add(newGroupItem);
            //deteting the root group is not allowed
            if (popupMenuObject != project.getSoundRootGroup())
            {
                popupMenu.add(removeGroupItem);
            }
        }
        else if (popupMenuObject instanceof Sound)
        {
            popupMenu.add(removeSoundItem);
        }
        else if (popupMenuObject instanceof EventGroup)
        {
            popupMenu.add(newEventItem);
            popupMenu.add(newGroupItem);
            //deteting the root group is not allowed
            if (popupMenuObject != project.getEventRootGroup())
            {
                popupMenu.add(removeGroupItem);
            }
        }
        else if (popupMenuObject instanceof Event)
        {
            popupMenu.add(removeEventItem);
        }
        
        else if (popupMenuObject instanceof MixBus)
        {
            popupMenu.add(newMixBusItem);
            //can't delete the master bus
            if (popupMenuObject != project.getMasterMixBus())
            {
                popupMenu.add(removeMixBusItem);
            }
        }
        else if (popupMenuObject instanceof MixPresetGroup)
        {
            popupMenu.add(newMixPresetItem);
            popupMenu.add(newGroupItem);
            //can't delete the master bus
            if (popupMenuObject != project.getMixPresetRootGroup())
            {
                popupMenu.add(removeGroupItem);
            }
        }
        else if (popupMenuObject instanceof MixPreset)
        {
            popupMenu.add(removeMixPresetItem);
        }
        else if (popupMenuObject instanceof WaveBankGroup)
        {
            popupMenu.add(newWaveBankItem);
            popupMenu.add(newGroupItem);
            //can't delete the master bus
            if (popupMenuObject != project.getWaveBankRootGroup())
            {
                popupMenu.add(removeGroupItem);
            }
        }
        else if (popupMenuObject instanceof WaveBank)
        {
            popupMenu.add(removeWaveBankItem);
        }

        popupMenu.show(this, e.getX(), e.getY());
    }

    /*
    @Override
    public void dragGestureRecognized(DragGestureEvent e)
    {
        System.out.println("dragGestureRecognized " + e);
        //find the tree node under the cursor, if any.
        Point clickPoint = e.getDragOrigin();
        TreePath path = getPathForLocation (clickPoint.x, clickPoint.y);
        if (path == null)
        {
            System.out.println ("not on a node");
            return;
        }
        draggedNode = path.getLastPathComponent();
        dragSource.startDrag (e, Cursor.getDefaultCursor(),
                              new ProjectEntityTransferable(draggedNode), this);
    }
    */
    public void treeExpanded(TreeExpansionEvent e)
    {
        Object obj = e.getPath().getLastPathComponent();
        if (obj instanceof KowalskiProject)
        {
            return;
        }
        //System.out.println("Tree-expanded event detected" + e);
        ((NodeWithExpansionState)obj).setExpanded(true);
    }

    // Required by TreeExpansionListener interface.
    public void treeCollapsed(TreeExpansionEvent e)
    {
        Object obj = e.getPath().getLastPathComponent();
        if (obj instanceof KowalskiProject)
        {
            return;
        }
        //System.out.println("Tree-collapsed event detected" + e);
        ((NodeWithExpansionState)obj).setExpanded(false);
    }

    public void onProjectDataChanged(KowalskiProject project)
    {
        //TODO: do more fine grained reloading if this takes too long for big trees.
        ((ProjectTreeModel)getModel()).reload();
        reloadExpansionState();
        repaint();
    }
}
