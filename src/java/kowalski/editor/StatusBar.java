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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JLabel;

/**
 *
 */
class StatusBar extends JLabel
{

    private String statusMessage;
    private float progressPercent;
    private static final Color progressBarColor = new Color(0, 0, 0, 0.2f);
    private static final Color progressBarDarkColor = new Color(0, 0, 0, 0.5f);
    private Color defaultForegroundColor;
    private Color errorColor = Color.RED;
    
    StatusBar()
    {
        final int minHeight = 7;
        setMinimumSize(new Dimension(100, minHeight));
        setPreferredSize(new Dimension(100, minHeight));
        setStatusMessage("");
        defaultForegroundColor = getForeground();
        progressPercent = 0.0f;
    }

    void setErrorMessage(String msg)
    {
        setForeground(errorColor);
    }

    void setStatusMessage(String msg)
    {
        setForeground(defaultForegroundColor);
        statusMessage = msg;
        setText(statusMessage);
    }

    void setProgressPercent(float percent)
    {
        progressPercent = Math.min(100.0f, Math.max(0.0f, percent));
        repaint();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g.setColor(progressBarColor);
        final int progressBarWidth = (int)(0.5f + getWidth() * 0.01f * progressPercent);
        g.fillRect(0, 0, progressBarWidth, getHeight());
        g.setColor(progressBarDarkColor);
        g.drawLine(progressBarWidth, 0, progressBarWidth, getHeight());
    }
}
