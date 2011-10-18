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

import java.util.AbstractList;
import java.util.ArrayList;
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
public class CreateMixBusAction extends AbstractProjectDataAction
{
    private MixBus createdMixBus;
    private MixBus parentBus;
    private String defaultID = "new_bus";

    public CreateMixBusAction(KowalskiProject p, MixBus b)
    {
        super(p, "Create Mix Bus");
        parentBus = b;
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        createdMixBus = ProjectDataUtils.createAndAddMixBus(project, parentBus, defaultID);
    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
        ProjectDataUtils.removeMixBus(project, parentBus, createdMixBus);
    }
}
