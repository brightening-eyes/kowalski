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
import kowalski.tools.data.xml.MixPresetGroup;
import kowalski.tools.data.xml.NodeWithIDAndComments;
import kowalski.tools.data.xml.SoundGroup;
import kowalski.tools.data.xml.WaveBankGroup;

/**
 *
 */
public class MoveGroupAction extends AbstractProjectDataAction
{
    private NodeWithIDAndComments oldParentGroup;
    private NodeWithIDAndComments newParentGroup;
    private NodeWithIDAndComments groupToMove;

    public MoveGroupAction(KowalskiProject p,
                           NodeWithIDAndComments group,
                           NodeWithIDAndComments target)
    {
        super(p, "Move " + group.getClass().getSimpleName());
        groupToMove = group;
        newParentGroup = target;
        oldParentGroup = ProjectDataUtils.getParentGroup(p, groupToMove);
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
       ProjectDataUtils.moveGroup(project, groupToMove, newParentGroup);
    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
       ProjectDataUtils.moveGroup(project, groupToMove, oldParentGroup);
    }

}
