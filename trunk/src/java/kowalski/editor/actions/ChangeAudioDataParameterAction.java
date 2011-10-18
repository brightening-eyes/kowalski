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
import kowalski.tools.data.xml.AudioData;
import kowalski.tools.data.xml.KowalskiProject;

/**
 *
 */
public class ChangeAudioDataParameterAction extends AbstractProjectDataAction
{
    public final static String STREAM_FROM_DISK = "Stream Flag";

    private String parameterName;
    private Object newParameterValue;
    private Object oldParameterValue;
    private AudioData audioData;

    public ChangeAudioDataParameterAction(KowalskiProject p, AudioData ad, String param, Object value)
    {
        super(p, "Change Audio Data " + param);
        audioData = ad;
        parameterName = param;
        newParameterValue = value;
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        if (parameterName.equals(STREAM_FROM_DISK))
        {
            oldParameterValue = audioData.isStreamFromDisk();
            audioData.setStreamFromDisk(Boolean.parseBoolean(newParameterValue.toString()));
        }
        else
        {
            throw new RuntimeException();
        }
    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
        if (parameterName.equals(STREAM_FROM_DISK))
        {
            audioData.setStreamFromDisk(Boolean.parseBoolean(oldParameterValue.toString()));
        }
        else
        {
            throw new RuntimeException();
        }
    }
}

