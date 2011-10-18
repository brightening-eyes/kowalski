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
import kowalski.tools.data.xml.Sound;

/**
 *
 */
public class ChangeSoundParameterAction extends AbstractProjectDataAction
{
    public final static String GAIN = "Gain";
    public final static String PITCH = "Pitch";
    public final static String GAIN_VARIATION = "Gain Variation";
    public final static String PITCH_VARIATION = "Pitch Variation";
    public final static String LOOP_COUNT = "Loop Count";

    private String parameterName;
    private Object newParameterValue;
    private Object oldParameterValue;
    private Sound sound;

    public ChangeSoundParameterAction(KowalskiProject p, Sound s, String param, Object value)
    {
        super(p, "Change Sound " + param);
        sound = s;
        parameterName = param;
        newParameterValue = value;
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        if (parameterName.equals(GAIN))
        {
            oldParameterValue = sound.getGain();
            sound.setGain(Float.parseFloat(newParameterValue.toString()));
        }
        else if (parameterName.equals(PITCH))
        {
            oldParameterValue = sound.getPitch();
            sound.setPitch(Float.parseFloat(newParameterValue.toString()));
        }
        else if (parameterName.equals(GAIN_VARIATION))
        {
            oldParameterValue = sound.getGainVariationPercent();
            sound.setGainVariationPercent(Float.parseFloat(newParameterValue.toString()));
        }
        else if (parameterName.equals(PITCH_VARIATION))
        {
            oldParameterValue = sound.getPitchVariationPercent();
            sound.setPitchVariationPercent(Float.parseFloat(newParameterValue.toString()));
        }
        else if (parameterName.equals(LOOP_COUNT))
        {
            oldParameterValue = sound.getPlaybackCount();
            sound.setPlaybackCount(Integer.parseInt(newParameterValue.toString()));
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
            sound.setGain(Float.parseFloat(oldParameterValue.toString()));
        }
        else if (parameterName.equals(PITCH))
        {
            sound.setPitch(Float.parseFloat(oldParameterValue.toString()));
        }
        else if (parameterName.equals(GAIN_VARIATION))
        {
            sound.setGainVariationPercent(Float.parseFloat(oldParameterValue.toString()));
        }
        else if (parameterName.equals(PITCH_VARIATION))
        {
            sound.setPitchVariationPercent(Float.parseFloat(oldParameterValue.toString()));
        }
        else if (parameterName.equals(LOOP_COUNT))
        {
            sound.setPlaybackCount(Integer.parseInt(oldParameterValue.toString()));
        }
        else
        {
            throw new RuntimeException();
        }
    }
}