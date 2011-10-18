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

import javax.swing.JSlider;

/**
 *
 */
class PercentSlider extends JSlider
{
    PercentSlider()
    {
        init(100);
    }

    PercentSlider(int max)
    {
        init(max);
    }

    private void init(int max)
    {
        setPaintLabels(true);
        setPaintTrack(true);
        setPaintTicks(true);
        setMajorTickSpacing(50);
        setMinorTickSpacing(10);
        setMaximum(max);
        setMinimum(0);
    }
}
