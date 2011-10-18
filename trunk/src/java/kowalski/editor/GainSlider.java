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

import java.awt.Font;
import java.util.Hashtable;
import java.util.Iterator;
import javax.swing.JLabel;
import javax.swing.JSlider;

/**
 *
 */
class GainSlider extends JSlider
{
    GainSlider()
    {
        setPaintLabels(true);
        setPaintTrack(true);
        //setPaintLabels(true);
        setMaximum(1000);
        setMinimum(0);
        setLabelTable(createTicks());
    }

    void setGain(float gain)
    {
        if (gain < 0 || gain > 2)
        {
            throw new IllegalArgumentException();
        }
    }

    float getGain()
    {
        return 0.0f;
    }

    private Hashtable<Integer, JLabel> createTicks()
    {
        System.out.println(getFont());
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
        Hashtable<Integer, JLabel> map = new Hashtable<Integer, JLabel>();

        float[] dbTicks = {Float.NEGATIVE_INFINITY, -60, -40, -20, -10, 0, 10, 20};

        for (int i = 0; i < dbTicks.length; i++)
        {
            JLabel label = new JLabel(i == 0 ? "-inf dB" : (int)dbTicks[i] + "dB");
            label.setFont(font);
            map.put(i == 0 ? 0 : (int)(1000.0f * Math.pow(10, 0.05 / 5 * dbTicks[i]) / 1.5), label);
        }

        
        Iterator<Integer> it = map.keySet().iterator();
        while (it.hasNext())
        {
            int tick = it.next();
            System.out.println(tick + ": " + map.get(tick));
        }


        return map;
    }

}
