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

package kowalski.demo;

import java.awt.GridLayout;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import kowalski.Kowalski;
import kowalski.util.JarResourceLoader;

/**
 *
 */
class DemoPanel extends JPanel
{
    private JSlider pitchSlider;
    private JSlider gainSlider;
    private JProgressBar leftBar;
    private JProgressBar rightBar;

    private int eventHandle;

    private class UpdateTask extends TimerTask
    {

        @Override
        public void run()
        {
            while (true)
            {
                update(0.02f);
                try
                {
                    Thread.sleep(20);
                }
                catch (InterruptedException e)
                {

                }
            }
        }
    }

    DemoPanel()
    {
        Kowalski.kwlInitialize(44100, 2, 0, 512);
        File tempEngineData = JarResourceLoader.writeResourceInJarToTempFile("demoproject.kwl");
        Kowalski.kwlEngineDataLoad(tempEngineData.getAbsolutePath());
        tempEngineData.delete();
        File tempWaveBank = JarResourceLoader.writeResourceInJarToTempFile("music.kwb");
        int waveBank = Kowalski.kwlWaveBankLoad(tempWaveBank.getAbsolutePath());
        tempWaveBank.delete();
        eventHandle = Kowalski.kwlEventGetHandle("music/music_stereo_ogg");
        Kowalski.kwlEventStartFade(eventHandle, 2.0f);
        Kowalski.kwlEnableLevelMetering();
        System.out.println(Kowalski.kwlGetError());

        pitchSlider = new JSlider(0, 1000);
        pitchSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                float val = 0.001f * pitchSlider.getValue();
                Kowalski.kwlEventSetPitch(eventHandle, 2 * val);
            }
        });
        
        gainSlider = new JSlider(0, 1000);
        gainSlider.setValue(1000);
        gainSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                float val = 0.001f * gainSlider.getValue();
                Kowalski.kwlEventSetGain(eventHandle, val);
            }
        });

        leftBar = new JProgressBar(0, 100);
        leftBar.setValue(24);

        rightBar = new JProgressBar(0, 100);
        rightBar.setValue(43);

        setLayout(new GridLayout(8, 1));

        add(new JLabel("Gain"));
        add(gainSlider);
        add(new JLabel("Pitch"));
        add(pitchSlider);
        add(new JLabel("Left output level"));
        add(leftBar);
        add(new JLabel("Right output level"));
        add(rightBar);

        Timer t = new Timer();
        UpdateTask ut = new UpdateTask();
        t.scheduleAtFixedRate(ut, 0, 20);
    }

    private void update(float timeStep)
    {
        Kowalski.kwlUpdate(timeStep);
        leftBar.setValue((int)(100 * Kowalski.kwlGetLevelLeft()));
        rightBar.setValue((int)(100 * Kowalski.kwlGetLevelRight()));

        if (Kowalski.kwlEventIsPlaying(eventHandle) == 0)
        {
            Kowalski.kwlEventStartFade(eventHandle, 2.0f);
        }
    }
}
