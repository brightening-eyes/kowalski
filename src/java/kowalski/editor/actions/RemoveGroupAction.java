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

package kowalski.editor.actions;

import kowalski.tools.data.ProjectDataException;
import kowalski.tools.data.ProjectDataUtils;
import kowalski.tools.data.xml.EventGroup;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.MixPreset;
import kowalski.tools.data.xml.MixPresetGroup;
import kowalski.tools.data.xml.NodeWithIDAndComments;
import kowalski.tools.data.xml.SoundGroup;
import kowalski.tools.data.xml.WaveBankGroup;

/**
 *
 */
public class RemoveGroupAction extends AbstractProjectDataAction
{
    private NodeWithIDAndComments containingGroup;
    private NodeWithIDAndComments removedGroup;

    public RemoveGroupAction(KowalskiProject p,
                             NodeWithIDAndComments group)
    {
        super(p, "Remove " + group.getClass().getSimpleName());
        removedGroup = group;
        NodeWithIDAndComments topGroup = null;
        if (group instanceof EventGroup)
        {
            topGroup = p.getEventRootGroup();
        }
        else if (group instanceof MixPresetGroup)
        {
            topGroup = p.getMixPresetRootGroup();
        }
        else if (group instanceof SoundGroup)
        {
            topGroup = p.getSoundRootGroup();
        }
        else if (group instanceof WaveBankGroup)
        {
            topGroup = p.getWaveBankRootGroup();
        }
        containingGroup = ProjectDataUtils.getParentGroup(p, group);
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        //dont allow removal of mix preset groups containing the default mix preset
        //TODO: should this be handled elsewhere?
        MixPreset defaultPreset = ProjectDataUtils.getDefaultMixPreset(project);
        if (ProjectDataUtils.getParentGroup(project, defaultPreset) != null)
        {
            throw new ProjectDataException("Cannot remove mix preset group '" +
                                           removedGroup.getID() + "' because it contains the " +
                                           "default preset.");
        }

        ProjectDataUtils.removeGroup(project, containingGroup, removedGroup);
    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
        ProjectDataUtils.addGroup(project, containingGroup, removedGroup);
    }
}
