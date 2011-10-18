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

package kowalski.tools.data;

import java.util.ArrayList;
import java.util.List;

/**
 * An exception class used to communicate violations of the prescribed
 * structure of Kowalski project data. Supports multiple separate messages in
 * a single instance.
 */
public class ProjectDataException extends Exception
{
    private List<String> messages = new ArrayList<String>();

    public ProjectDataException(String str, List<String> msgs)
    {
        super(str);
        messages = msgs;
    }

    public ProjectDataException(List<String> msgs)
    {
        messages = msgs;
    }

    public List<String> getMessages()
    {
        return messages;
    }
    
    public ProjectDataException(String msg)
    {
        messages.add(msg);
    }

    @Override
    public String toString()
    {
        String str = getClass().getSimpleName() + ": ";
        if (getMessage() != null)
        {
            if (getMessage().length() > 0)
            {
                str += getMessage();
            }
        }

        str += "\n";
        for (int i = 0; i < messages.size(); i++)
        {
            str += messages.get(i);
            if (i < messages.size() - 1)
            {
                str += "\n";
            }
        }

        return str;
    }
}
