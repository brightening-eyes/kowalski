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

package kowalski.util;

import javax.swing.JFrame;


/**
 * A simple extension of JFrame decorated with a Kowalski icon instead
 * of the default icon (on OSs that support it, for example windows).
 *
 * Make sure JFrame.setDefaultLookAndFeelDecorated(true); is called
 * before instances of this class are created, or the custom icon will
 * not appear.
 */
public class JFrameWithKowalskiIcon extends JFrame
{
    /**
     * Constructor.
     */
    public JFrameWithKowalskiIcon()
    {
        setIconImage(IconManager.getImageIcon("logo24x24.png").getImage());
    }

}
