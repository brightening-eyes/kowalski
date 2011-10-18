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
 *
 */
public class RemoveEventAction extends AbstractProjectDataAction
{
    private Event eventToRemove;
    private EventGroup parentGroup;
    private int oldIndex;

    public RemoveEventAction(KowalskiProject p, Event event)
    {
        super(p, "Remove Event");
        eventToRemove = event;
        parentGroup = (EventGroup)ProjectDataUtils.getParentGroup(p, event);
        oldIndex = parentGroup.getEvents().indexOf(eventToRemove);
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        parentGroup.getEvents().remove(eventToRemove);
    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
        parentGroup.getEvents().add(oldIndex, eventToRemove);
    }

}
