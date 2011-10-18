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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import kowalski.tools.data.ProjectDataException;
import kowalski.tools.data.ProjectDataUtils;
import kowalski.tools.data.xml.AudioData;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.WaveBank;

/**
 *
 */
public class AddAudioDataAction extends AbstractProjectDataAction
{
    private List<File> files;
    private WaveBank waveBank;
    private List<AudioData> audioDataItems;

    public AddAudioDataAction(KowalskiProject p, WaveBank b, List<File> f)
    {
        super(p, "Add Audio Data");
        files = f;
        waveBank = b;
        audioDataItems = new ArrayList<AudioData>();
    }
    @Override
    public void perform(KowalskiProject project) throws ProjectDataException
    {
        audioDataItems.clear();
        for (int i = 0; i < files.size(); i++)
        {
            audioDataItems.add(ProjectDataUtils.addAudioFileToWaveBank(project, waveBank, files.get(i)));
        }
    }

    @Override
    public void undo(KowalskiProject project) throws ProjectDataException
    {
        for (int i = 0; i < audioDataItems.size(); i++)
        {
            ProjectDataUtils.removeAudioDataFromWaveBank(project, waveBank, audioDataItems.get(i));
        }
    }

}
