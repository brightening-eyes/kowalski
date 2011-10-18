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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import kowalski.editor.AbstractPanel;

/**
 *
 */
class ConeEditorPanel extends JPanel
{
    private JSlider innerAngleSlider;
    private JSlider outerAngleSlider;
    private JSlider innerGainSlider;
    private JSlider outerGainSlider;
    private JPanel radialPlotPanel;
    List<JComponent> componentsToDisable;

    ConeEditorPanel()
    {
        setOpaque(false);
        componentsToDisable = new ArrayList<JComponent>();
        innerAngleSlider = createSlider(0, 360);
        componentsToDisable.add(innerAngleSlider);
        outerAngleSlider = createSlider(0, 360);
        componentsToDisable.add(outerAngleSlider);
        innerGainSlider = new GainSlider();//;
        componentsToDisable.add(innerGainSlider);
        outerGainSlider = new GainSlider();//createSlider(0, 360);
        componentsToDisable.add(outerGainSlider);
        radialPlotPanel = new JPanel();
        radialPlotPanel.setOpaque(false);
        componentsToDisable.add(radialPlotPanel);

        setLayout(new BorderLayout());
        JPanel sliderPanel = new AbstractPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        JPanel row1 = new AbstractPanel(new GridLayout(1, 1));

        JLabel innerConeAngleLabel = new JLabel("Inner cone angle", null, JLabel.CENTER);
        row1.add(innerConeAngleLabel);
        componentsToDisable.add(innerConeAngleLabel);

        JLabel outerConeAngleLabel = new JLabel("Outer cone angle", null, JLabel.CENTER);
        row1.add(outerConeAngleLabel);
        componentsToDisable.add(outerConeAngleLabel);
        
        JPanel row2 = new AbstractPanel(new GridLayout(1, 1));

        row2.add(innerAngleSlider);
        row2.add(outerAngleSlider);

        JPanel row3 = new AbstractPanel(new GridLayout(1, 1));

        JLabel innerConeGainLabel = new JLabel("Inner cone gain", null, JLabel.CENTER);
        componentsToDisable.add(innerConeGainLabel);
        row3.add(innerConeGainLabel);

        JLabel outerConeGainLabel = new JLabel("Outer cone gain", null, JLabel.CENTER);
        componentsToDisable.add(outerConeGainLabel);
        row3.add(outerConeGainLabel);

        JPanel row4 = new AbstractPanel(new GridLayout(1, 1));
        row4.add(innerGainSlider);
        row4.add(outerGainSlider);

        sliderPanel.add(row1);
        sliderPanel.add(row2);
        sliderPanel.add(row3);
        sliderPanel.add(row4);

        add(sliderPanel, BorderLayout.CENTER);
        radialPlotPanel.setLayout(new GridLayout(1, 1));

        radialPlotPanel.setSize(100, 100);
        radialPlotPanel.setPreferredSize(new Dimension(100, 100));
        radialPlotPanel.setMinimumSize(new Dimension(100, 100));
        add(radialPlotPanel, BorderLayout.EAST);
        //setComponentsEnabled(false);
    }

    void setComponentsEnabled(boolean enabled)
    {
        for (int i = 0; i < componentsToDisable.size(); i++)
        {
            componentsToDisable.get(i).setEnabled(enabled);
        }
    }

    private void onValueChanged()
    {
        if (innerAngleSlider.getValue() > outerAngleSlider.getValue())
        { 
            outerAngleSlider.setValue(innerAngleSlider.getValue());
        }
        repaint();
    }

    private float getInnerAngleRad()
    {
        return (float)Math.PI * innerAngleSlider.getValue() / 180.f;
    }

    private float getOuterAngleRad()
    {
        return (float)Math.PI * outerAngleSlider.getValue() / 180.f;
    }

    private JSlider createSlider(int min, int max)
    {
        JSlider slider = new JSlider(min, max);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setPaintTrack(true);
        slider.setMajorTickSpacing(90);
        slider.setMinorTickSpacing(10);
        slider.setName("owowowow");
        slider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                onValueChanged();
            }
        });
        return slider;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        float innerAngleRad = getInnerAngleRad();
        float innerGain = 0.6f;
        float outerAngleRad = getOuterAngleRad();
        float outerGain = 0.2f;

        float w = radialPlotPanel.getWidth();
        float h = radialPlotPanel.getWidth();
        Graphics2D graphics = (Graphics2D)radialPlotPanel.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        //coordinate axes
        graphics.setColor(new Color(0, 0, 0, 0.1f));
        graphics.drawLine(0, (int)(h / 2), (int)(w), (int)(h / 2));
        graphics.drawLine((int)(w / 2), 0, (int)(w / 2), (int)(h));

        //up
        graphics.setColor(new Color(0, 0, 0, 0.3f));
        graphics.drawLine((int)(w / 2), (int)(h / 2), (int)(w / 2), 0);

        //angle markers
        {
            graphics.setColor(new Color(0, 0, 0, 0.5f));
            float cosInner = (float)Math.cos(innerAngleRad / 2);
            float sinInner = (float)Math.sin(innerAngleRad / 2);
            float cosOuter = (float)Math.cos(outerAngleRad / 2);
            float sinOuter = (float)Math.sin(outerAngleRad / 2);
            graphics.drawLine((int)(w / 2), (int)(h / 2),
                              (int)(w / 2 + w * cosInner / 2), (int)(h / 2 - h * sinInner / 2));
            graphics.drawLine((int)(w / 2), (int)(h / 2),
                              (int)(w / 2 - w * cosInner / 2), (int)(h / 2 - h * sinInner / 2));
            graphics.drawLine((int)(w / 2), (int)(h / 2),
                              (int)(w / 2 + w * cosOuter / 2), (int)(h / 2 - h * sinOuter / 2));
            graphics.drawLine((int)(w / 2), (int)(h / 2),
                              (int)(w / 2 - w * cosOuter / 2), (int)(h / 2 - h * sinOuter / 2));
        }

        //plot
        int N = 50;
        int[] xPoints = new int[N];
        int[] yPoints = new int[N];

        graphics.setColor(new Color(1, 0, 0, 0.9f));
        for (int i = 0; i < N; i++)
        {
            double angle = i * 2.0 * Math.PI / N;
            float r = Math.abs(angle - Math.PI) < innerAngleRad / 2.0f ? innerGain : outerGain;
            xPoints[i] = (int)(0.5f * w + 0.5f * w * r * Math.cos(angle + 0.5 * Math.PI));
            yPoints[i] = (int)(0.5f * h + 0.5f * h * r * Math.sin(angle + 0.5 * Math.PI));
        }
        graphics.drawPolygon(xPoints, yPoints, N);
    }
}
