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

/**
 * 
 */
class AudioPlaybackProgressTask extends Thread
{
    private StatusBar statusBar;
    private float durationMicroSec;
    private float progressPercent;
    private boolean interruptRequested = false;
    private int updateIntervalMs = 10;

    AudioPlaybackProgressTask(StatusBar bar, float durationMicros)
    {
        statusBar = bar;
        durationMicroSec = durationMicros;
    }

    void setDurationMicroSec(float d)
    {
        durationMicroSec = d;
    }

    synchronized void requestInterrupt()
    {
        interruptRequested = true;
    }

    @Override
    public void run()
    {
        interruptRequested = false;
        progressPercent = 0;
        statusBar.setProgressPercent(progressPercent);

        long dt = 0;
        long timeMillis;
        while (progressPercent <= 100)
        {
            if (interruptRequested)
            {
                statusBar.setProgressPercent(0.0f);
                return;
            }
            try
            {
                timeMillis = System.currentTimeMillis();
                sleep(updateIntervalMs);
                progressPercent += 100.f * 1000.0f * dt / (durationMicroSec);
                statusBar.setProgressPercent(progressPercent);
                dt = System.currentTimeMillis() - timeMillis;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        statusBar.setProgressPercent(0.0f);

    }
}
