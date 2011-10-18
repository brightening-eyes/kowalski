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

import java.util.ArrayList;
import java.util.List;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.WaveBank;
import kowalski.tools.data.xml.WaveBankGroup;

/**
 * A tree model used to display the wave bank hierarchy of a Kowalski project.
 */
class WaveBankTreeModel implements TreeModel
{
    private WaveBankGroup waveBankRoot;

    WaveBankTreeModel(WaveBankGroup root)
    {
        waveBankRoot = root;
    }

    public Object getRoot()
    {
        return waveBankRoot;
    }


    public boolean isLeaf(Object node)
    {
        //empty groups are leaves
        return node instanceof WaveBank;
    }

    public int getChildCount(Object parent)
    {
        if (parent instanceof WaveBankGroup)
        {
            return ((WaveBankGroup)parent).getSubGroups().size() +
                   ((WaveBankGroup)parent).getWaveBanks().size();
        }

        return 0;
    }

    public Object getChild(Object parent, int index)
    {
        WaveBankGroup wbg = (WaveBankGroup)parent;
        if (index >= wbg.getSubGroups().size())
        {
            return wbg.getWaveBanks().get(index - wbg.getSubGroups().size());
        }
        else
        {
            return wbg.getSubGroups().get(index);
        }
    }

    public int getIndexOfChild(Object parent, Object child)
    {
        WaveBankGroup wbg = (WaveBankGroup)parent;
        if (child instanceof WaveBankGroup)
        {
            return wbg.getSubGroups().indexOf(child);
        }
        else if (child instanceof WaveBank)
        {
            return wbg.getSubGroups().size() + wbg.getWaveBanks().indexOf(child);
        }

        return -1;
    }

    
    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        //System.out.println("ProjectTreeMode.valueForPathChanged: path " + path +
        //        ", newValue " + newValue);

    }

    @Override
    public void addTreeModelListener(TreeModelListener l)
    {
        //System.out.println("ProjectTreeMode.addTreeModelListener: l " + l);
        
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {
        //System.out.println("ProjectTreeMode.removeTreeModelListener: l " + l);
    }
}

/**
 * A tree used to display the wave bank hierarchy of a Kowalski project.
 */
class WaveBankTree extends JTree
{
    
    WaveBankTree()
    {
        super(new WaveBankTreeModel(null));
        setCellRenderer(new ProjectTreeCellRenderer());
        setRootVisible(true);
        setShowsRootHandles(true);
        
    }

    void refresh(KowalskiProject p)
    {
        setModel(new WaveBankTreeModel(p.getWaveBankRootGroup()));
    }

    List<WaveBank> getSelectedWaveBanks()
    {
        List<WaveBank> selectedBanks = new ArrayList<WaveBank>();
        TreePath[] selectedPaths = getSelectionPaths();
        if (selectedPaths == null)
        {
            return selectedBanks;
        }

        for (TreePath path : selectedPaths)
        {
            try
            {
                selectedBanks.add((WaveBank)path.getLastPathComponent());
            }
            catch (ClassCastException e)
            {

            }
        }

        return selectedBanks;

    }

    List<WaveBank> getAllWaveBanks()
    {
        List<WaveBank> selectedBanks = new ArrayList<WaveBank>();
        gatherWaveBanks(getModel().getRoot(), selectedBanks);
        return selectedBanks;
    }

    private void gatherWaveBanks(Object root, List<WaveBank> waveBanks)
    {
        int numChildren = getModel().getChildCount(root);

        for (int i = 0; i < numChildren; i++)
        {
            Object childi = getModel().getChild(root, i);
            if (getModel().isLeaf(childi))
            {
                waveBanks.add((WaveBank)childi);
            }
            else
            {
                gatherWaveBanks(childi, waveBanks);
            }
        }
    }

}
