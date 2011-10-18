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
import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * 
 */
abstract class CachedSoundPlayer
{
    private static Map<File, Clip> clipsByFile =
            new Hashtable<File, Clip>();
    /** TODO: keep track of the total number of cached frames. */
    private static final long MAX_CACHED_FRAMES = 1000;

    static void addSoundToCache(File file)
            throws IOException, 
                   LineUnavailableException,
                   UnsupportedAudioFileException
    {
        Clip clip = clipsByFile.get(file);

        if (clip == null)
        {
            clip = AudioSystem.getClip();
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            clip.open(ais);
            clipsByFile.put(file, clip);
        }
    }

    static void playSound(File file)
            throws IOException, LineUnavailableException,
            UnsupportedAudioFileException
    {
        if (clipsByFile.containsKey(file))
        {
            Clip clip = clipsByFile.get(file);
            if (clip.isActive())
            {
                clip.stop();
                clip.setFramePosition(0);
                KowalskiEditorFrame.getInstance().resetSoundPlaybackProgress();
                return;
            }
        }

        stopAllSounds();
        addSoundToCache(file);
        Clip clip = clipsByFile.get(file);

        clip.setFramePosition(0);
        clip.start();
        KowalskiEditorFrame.getInstance().startSoundPlaybackProgress(clip.getMicrosecondLength());
    }

    static void stopAllSounds()
    {
        Collection<Clip> clips = clipsByFile.values();
        Iterator<Clip> iterator = clips.iterator();

        while (iterator.hasNext())
        {
            Clip c = iterator.next();
            if (c.isActive())
            {
                c.stop();
                c.setFramePosition(0);
            }
        }
    }

    static void stopSound(File file)
    {
        Clip clip = clipsByFile.get(file);
        if (clip != null && clip.isActive())
        {
            clip.stop();
        }
    }

    static void addSoundsToChache(File[] files)
            throws IOException, LineUnavailableException,
            UnsupportedAudioFileException
    {
        for (File f : files)
        {
            addSoundToCache(f);
        }
    }

    static void removeSoundFromCache(File file)
    {
        Clip clip = clipsByFile.get(file);
        if (clip != null)
        {
            clip.stop();
            clip.close();
            clipsByFile.remove(file);
        }
    }
    
    static boolean canPlayFile(File file)
    {
        if (file == null)
        {
            return false;
        }
        try
        {
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            ais.close();
        }
        catch (IOException e)
        {
            //e.printStackTrace();
            return false;
        }
        catch (UnsupportedAudioFileException e)
        {
            //e.printStackTrace();
            return false;
        }

        return true;
    }
}
