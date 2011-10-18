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
import kowalski.tools.data.xml.KowalskiProject;

/**
 * Abstract base class for a reversible operation on project data.
 */
public abstract class AbstractProjectDataAction
{
    private String name;
    protected KowalskiProject project;

    public AbstractProjectDataAction(KowalskiProject p, String actionName)
    {
        if (p == null)
        {
            throw new IllegalArgumentException("Project is null.");
        }
        if (actionName == null)
        {
            throw new IllegalArgumentException("Action name is null.");
        }
        if (actionName.length() == 0)
        {
            throw new IllegalArgumentException("Action name is empty.");
        }

        name = actionName;
        project = p;
    }

    /**
     * Performs the action on a given KowalskiProject instance.
     * @param project The project data to perform the action upon.
     * @throws ProjectDataException If the data resulting from the action is not valid.
     */
    public abstract void perform(KowalskiProject project)
            throws ProjectDataException;


    /**
     * Performs the inverse of the action on a given KowalskiProject instance.
     * @param project The project data to perform the inverse action upon.
     * @throws ProjectDataException If the data resulting from the inverse action is not valid.
     */
    public abstract void undo(KowalskiProject project)
            throws ProjectDataException;
    
    /**
     * Returns the name of the action.
     * @return The action name.
     */
    public final String getName()
    {
        return name;
    }
}

