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
import kowalski.tools.data.xml.Event;
import kowalski.tools.data.xml.KowalskiProject;

/**
 *
 */
public class ChangeEventParameterAction extends AbstractProjectDataAction
{
    public final static String GAIN = "Gain";
    public final static String PITCH = "Pitch";
    public final static String IS_3D = "Positional Flag";

    private String parameterName;
    private Object newParameterValue;
    private Object oldParameterValue;
    private Event event;

    public ChangeEventParameterAction(KowalskiProject p, Event e, String param, Object value)
    {
        super(p, "Change Event " + param);
        event = e;
        parameterName = param;
        newParameterValue = value;
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        if (parameterName.equals(GAIN))
        {
            oldParameterValue = event.getGain();
            event.setGain(Float.parseFloat(newParameterValue.toString()));
        }
        else if (parameterName.equals(PITCH))
        {
            oldParameterValue = event.getPitch();
            event.setPitch(Float.parseFloat(newParameterValue.toString()));
        }
        else
        {
            throw new RuntimeException();
        }
    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
        if (parameterName.equals(GAIN))
        {
            event.setGain(Float.parseFloat(oldParameterValue.toString()));
        }
        else if (parameterName.equals(PITCH))
        {
            event.setPitch(Float.parseFloat(oldParameterValue.toString()));
        }
        else
        {
            throw new RuntimeException();
        }
    }
}
