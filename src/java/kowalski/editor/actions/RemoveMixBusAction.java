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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import kowalski.tools.data.ProjectDataException;
import kowalski.tools.data.ProjectDataUtils;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.MixBus;
import kowalski.tools.data.xml.MixBusParameters;
import kowalski.tools.data.xml.MixPreset;

/**
 *
 */
public class RemoveMixBusAction extends AbstractProjectDataAction
{
    private MixBus busToRemove;
    private MixBus parentBus;
    private Map<MixPreset, MixBusParameters> removedParamsByPreset =
            new Hashtable<MixPreset, MixBusParameters>();


    public RemoveMixBusAction(KowalskiProject p, MixBus b)
    {
        super(p, "Remove Mix Bus");
        busToRemove = b;
        parentBus = (MixBus)ProjectDataUtils.getParentGroup(project, busToRemove);
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        //keep track of which parameter sets were deleted
        List<MixPreset> mixPresets = ProjectDataUtils.getMixPresetList(project);
        for (int i = 0; i < mixPresets.size(); i++)
        {
            List<MixBusParameters> params = mixPresets.get(i).getMixBusParameterList();
            for (int j = 0; j < params.size(); j++)
            {
                MixBusParameters p = params.get(j);
                if (p.getMixBus().equals(busToRemove.getID()))
                {
                    removedParamsByPreset.put(mixPresets.get(i), p);
                    break;
                }
            }
        }

        ProjectDataUtils.removeMixBus(project, parentBus, busToRemove);

    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
        //this call adds new default mix preset parameter sets for the bus...
        ProjectDataUtils.addMixBus(project, parentBus, busToRemove);

        //...so restore the old parameter values
        List<MixPreset> presets = ProjectDataUtils.getMixPresetList(project);

        for (int i = 0; i < presets.size(); i++)
        {
            //find the newly added parameter set and set its values
            //to what they were before the bus was deleted
            List<MixBusParameters> paramSets = presets.get(i).getMixBusParameterList();
            MixBusParameters oldSet = removedParamsByPreset.get(presets.get(i));
            for (int j = 0; j < paramSets.size(); j++)
            {
                MixBusParameters paramSetj = paramSets.get(j);
                if (paramSetj.getMixBus().equals(busToRemove.getID()))
                {
                    paramSetj.setLeftGain(oldSet.getLeftGain());
                    paramSetj.setRightGain(oldSet.getRightGain());
                    paramSetj.setPitch(oldSet.getPitch());
                    break;
                }
            }
        }
    }
}
