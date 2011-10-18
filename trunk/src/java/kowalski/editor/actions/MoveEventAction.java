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
import kowalski.tools.data.xml.Event;
import kowalski.tools.data.xml.EventGroup;
import kowalski.tools.data.xml.KowalskiProject;

/**
 * Move an EventBase to an EventGroup
 */

public class MoveEventAction extends AbstractProjectDataAction
{
    private Event eventToMove;
    private EventGroup targetGroup;
    private EventGroup oldParent;

    public MoveEventAction(KowalskiProject p,
                    Event eToMove,
                    EventGroup targetG)
    {
        super(p, "Move Event '" + eToMove.getID() + "'");
        eventToMove = eToMove;
        targetGroup = targetG;
        oldParent = (EventGroup)ProjectDataUtils.getParentGroup(project, eToMove);
    }

    @Override
    public void perform(KowalskiProject project)
            throws ProjectDataException
    {
        //move the event
        ProjectDataUtils.moveEvent(project,
                                eventToMove, targetGroup);
    }

    @Override
    public void undo(KowalskiProject project)
            throws ProjectDataException
    {
        ProjectDataUtils.moveEvent(project,
                                   eventToMove, oldParent);
    }
}
