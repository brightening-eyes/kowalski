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
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import kowalski.tools.data.xml.AudioData;
import kowalski.tools.data.xml.Event;
import kowalski.tools.data.xml.EventGroup;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.MixBus;
import kowalski.tools.data.xml.MixPreset;
import kowalski.tools.data.xml.MixPresetGroup;
import kowalski.tools.data.xml.Sound;
import kowalski.tools.data.xml.SoundGroup;
import kowalski.tools.data.xml.WaveBank;
import kowalski.tools.data.xml.WaveBankGroup;

/**
 * A tree model representing a Kowalski project tree.
 */
class ProjectTreeModel implements TreeModel
{
    private KowalskiProject project;
    private List<TreeModelListener> listeners =
            new ArrayList<TreeModelListener>();
    
    ProjectTreeModel(KowalskiProject proj)
    {
        project = proj;
    }

    public Object getRoot()
    {
        return project;
    }


    public boolean isLeaf(Object node)
    {
        //empty groups are leaves
        if (node instanceof SoundGroup)
        {
            return ((SoundGroup)node).getSubGroups().size() == 0 &&
                   ((SoundGroup)node).getSounds().size() == 0;
        }
        else if (node instanceof EventGroup)
        {
            return ((EventGroup)node).getSubGroups().size() == 0 &&
                   ((EventGroup)node).getEvents().size() == 0;
        }
        else if (node instanceof WaveBankGroup)
        {
            return ((WaveBankGroup)node).getSubGroups().size() == 0 &&
                   ((WaveBankGroup)node).getWaveBanks().size() == 0;
        }
        else if (node instanceof MixPresetGroup)
        {
            return ((MixPresetGroup)node).getSubGroups().size() == 0 &&
                   ((MixPresetGroup)node).getMixPresets().size() == 0;
        }
        else if (node instanceof MixBus)
        {
            return ((MixBus)node).getSubBuses().size() == 0;
        }

        //these entities are always leaves
        return node instanceof Event ||
               node instanceof Sound ||
               node instanceof AudioData ||
               node instanceof MixPreset;
    }
    
    public int getChildCount(Object parent)
    {
        if (parent instanceof EventGroup)
        {
            return ((EventGroup)parent).getSubGroups().size() +
                   ((EventGroup)parent).getEvents().size();
        }
        else if (parent instanceof SoundGroup)
        {
            return ((SoundGroup)parent).getSubGroups().size() +
                   ((SoundGroup)parent).getSounds().size();
        }
        else if (parent instanceof MixBus)
        {
            return ((MixBus)parent).getSubBuses().size();
        }
        else if (parent instanceof KowalskiProject)
        {
            return 5; //events, sounds, mixbuses, mixpresets, wavebanks
        }
        else if (parent instanceof WaveBankGroup)
        {
            return ((WaveBankGroup)parent).getSubGroups().size() +
                   ((WaveBankGroup)parent).getWaveBanks().size();
        }
        else if (parent instanceof MixPresetGroup)
        {
            return ((MixPresetGroup)parent).getSubGroups().size() +
                   ((MixPresetGroup)parent).getMixPresets().size();
        }
        else if (parent instanceof WaveBank)
        {
            return ((WaveBank)parent).getAudioDataList().size();
        }

        return 0;
        //throw new RuntimeException("ProjectTreeModel.getChildCount: unknown parent type " + parent);
    }
    
    public Object getChild(Object parent, int index)
    {
        if (parent == project)
        {
            if (index == 0)
            {
                return project.getEventRootGroup();
            }
            else if (index == 1)
            {
                return project.getSoundRootGroup();
            }
            else if (index == 2)
            {
                return project.getMasterMixBus();
            }
            else if (index == 3)
            {
                return project.getMixPresetRootGroup();
            }
            else if (index == 4)
            {
                return project.getWaveBankRootGroup();
            }
        }
        else if (parent instanceof MixBus)
        {
            return ((MixBus)parent).getSubBuses().get(index);
        }
        else if (parent instanceof EventGroup)
        {
            EventGroup eg = (EventGroup)parent;
            if (index >= eg.getSubGroups().size())
            {
                return eg.getEvents().get(index - eg.getSubGroups().size());
            }
            else
            {
                return eg.getSubGroups().get(index);
            }
        }
        else if (parent instanceof SoundGroup)
        {
            SoundGroup sg = (SoundGroup)parent;
            if (index >= sg.getSubGroups().size())
            {
                return sg.getSounds().get(index - sg.getSubGroups().size());
            }
            else
            {
                return sg.getSubGroups().get(index);
            }
        }
        else if (parent instanceof WaveBankGroup)
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
        else if (parent instanceof MixPresetGroup)
        {
            MixPresetGroup mpg = (MixPresetGroup)parent;
            if (index >= mpg.getSubGroups().size())
            {
                return mpg.getMixPresets().get(index - mpg.getSubGroups().size());
            }
            else
            {
                return mpg.getSubGroups().get(index);
            }
        }
        else if (parent instanceof WaveBank)
        {
            WaveBank wb = (WaveBank)parent;
            return wb.getAudioDataList().get(index);
        }

        return null;
    }

    public int getIndexOfChild(Object parent, Object child)
    {
        if (parent instanceof SoundGroup)
        {
            SoundGroup sg = (SoundGroup)parent;
            if (child instanceof SoundGroup)
            {
                return sg.getSounds().indexOf(child);
            }
            else if (child instanceof Sound)
            {
                return sg.getSubGroups().size() + sg.getSounds().indexOf(child);
            }
        }
        else if (parent instanceof EventGroup)
        {
            EventGroup eg = (EventGroup)parent;
            if (child instanceof EventGroup)
            {
                return eg.getEvents().indexOf(child);
            }
            else if (child instanceof Event)
            {
                return eg.getSubGroups().size() + eg.getEvents().indexOf(child);
            }
        }
        else if (parent instanceof MixPresetGroup)
        {
            MixPresetGroup mpg = (MixPresetGroup)parent;
            if (child instanceof MixPresetGroup)
            {
                return mpg.getSubGroups().indexOf(child);
            }
            else if (child instanceof MixPreset)
            {
                return mpg.getSubGroups().size() + mpg.getMixPresets().indexOf(child);
            }
        }
        else if (parent instanceof WaveBankGroup)
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
        }
        else if (parent instanceof WaveBank)
        {
            return ((WaveBank)parent).getAudioDataList().indexOf(child);
        }
        else if (parent instanceof MixBus)
        {
            return ((MixBus)parent).getSubBuses().indexOf(child);
        }

        return -1;
    }

    void reload()
    {
        for (int i = 0; i < listeners.size(); i++)
        {
            listeners.get(i).treeStructureChanged(new TreeModelEvent(this, new TreePath(project)));
        }   
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
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {
        //System.out.println("ProjectTreeMode.removeTreeModelListener: l " + l);
        listeners.remove(l);
    }
}
