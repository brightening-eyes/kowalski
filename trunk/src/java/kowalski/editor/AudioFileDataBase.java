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

package kowalski.editor;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import kowalski.tools.data.AudioFileDescription;
import kowalski.tools.data.AudioFileParser;

/**
 *
 */
class AudioFileDataBase
{
    private static  Map<File, AudioFileDescription> descriptionsByFile =
            new Hashtable<File, AudioFileDescription>();

    private AudioFileDataBase()
    {

    }

    static AudioFileDescription getAudioFileDescription(File f)
    {
        return descriptionsByFile.get(f);
    }
   
    
    static void reload(File root)
    {
        if (!root.exists() || !root.isDirectory())
        {
            throw new RuntimeException();
        }

        descriptionsByFile.clear();
        reloadRecursively(root);
    }

    private static void reloadRecursively(File root)
    {

        for (File f : root.listFiles())
        {
            if (f.isDirectory())
            {
                reloadRecursively(f);
            }
            else
            {
                AudioFileDescription afd = AudioFileParser.getAudioFileFormat(f);
                if (afd != null)
                {
                    if (descriptionsByFile.containsKey(f))
                    {
                        throw new RuntimeException("descriptionsByFile already contains " + f);
                    }
                    descriptionsByFile.put(f, afd);
                }
            }
        }
    }
}
