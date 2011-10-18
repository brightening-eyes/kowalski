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
import kowalski.tools.data.xml.NodeWithIDAndComments;

/**
 * 
 */
public class ChangeNodeWithIDAndCommentsParameterAction extends AbstractProjectDataAction
{
    public final static String ID = "ID";
    public final static String COMMENTS = "Comments";

    private String parameterName;
    private Object newParameterValue;
    private Object oldParameterValue;
    private NodeWithIDAndComments node;

    public ChangeNodeWithIDAndCommentsParameterAction(KowalskiProject p, 
                        NodeWithIDAndComments n, String param, Object value)
    {
        super(p, "Change " + n.getClass().getSimpleName() + " " + param);
        node = n;
        parameterName = param;
        newParameterValue = value;
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        if (parameterName.equals(ID))
        {
            oldParameterValue = node.getID();
            node.setID(newParameterValue.toString());
        }
        else if (parameterName.equals(COMMENTS))
        {
            oldParameterValue = node.getComments();
            node.setComments(newParameterValue.toString());
        }
        else
        {
            throw new RuntimeException();
        }
    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
        if (parameterName.equals(ID))
        {
            node.setID(oldParameterValue.toString());
        }
        else if (parameterName.equals(COMMENTS))
        {
            node.setComments(oldParameterValue.toString());
        }
        else
        {
            throw new RuntimeException();
        }
    }
}
