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

import java.util.List;
import kowalski.tools.data.ProjectDataException;
import kowalski.tools.data.ProjectDataUtils;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.MixBus;
import kowalski.tools.data.xml.MixBusParameters;
import kowalski.tools.data.xml.MixPreset;

/**
 *
 */
public class ChangeMixBusID extends AbstractProjectDataAction
{
    private MixBus busToChange;
    private String newID;
    private String oldID;
    
    public ChangeMixBusID(KowalskiProject p, MixBus bus, String newId)
    {
        super(p, "Change Mix Bus ID");
        newID = newId;
        oldID = bus.getID();
        busToChange = bus;
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        //rename the bus
        updateMixPresets(newID, oldID);
    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
        //rename the bus

        //update events and mix presets referencing the bus
        updateMixPresets(oldID, newID);
    }

    private void updateMixPresets(String nextID, String currentID)
    {
        //update events and mix presets referencing the bus
        List<MixPreset> mixPresets = ProjectDataUtils.getMixPresetList(project);
        for (int i = 0; i < mixPresets.size(); i++)
        {
            List<MixBusParameters> params = mixPresets.get(i).getMixBusParameterList();
            for (int j = 0; j < params.size(); j++)
            {
                MixBusParameters p = params.get(j);
                if (p.getMixBus().equals(currentID))
                {
                    p.setMixBus(nextID);
                }
            }
        }
    }

}
