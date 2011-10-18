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
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.Sound;
import kowalski.tools.data.xml.SoundGroup;

/**
 *
 */
public class RemoveSoundAction extends AbstractProjectDataAction
{
    
    private Sound soundToRemove;
    private SoundGroup parentGroup;
    private int oldIndex;

    public RemoveSoundAction(KowalskiProject p, Sound sound)
    {
        super(p, "Remove Sound");
        soundToRemove = sound;
        parentGroup = (SoundGroup)ProjectDataUtils.getParentGroup(p, sound);
        oldIndex = parentGroup.getSounds().indexOf(soundToRemove);
    }

    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        ProjectDataUtils.removeSound(project, parentGroup, soundToRemove);
    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
        parentGroup.getSounds().add(oldIndex, soundToRemove);
    }

}
