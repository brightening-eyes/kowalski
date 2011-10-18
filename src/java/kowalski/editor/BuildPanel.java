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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.xml.bind.JAXBException;
import kowalski.tools.data.AbstractLogger;
import kowalski.tools.data.EngineDataBuilder;
import kowalski.tools.data.ProjectDataException;
import kowalski.tools.data.WaveBankBuilder;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.WaveBank;

class LogPanel extends JScrollPane implements AbstractLogger
{

    private JTextPane textPane;
    private PrintStream printStream;

    LogPanel()
    {


        textPane = new JTextPane();
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textPane.setEditable(false);
        setViewportView(textPane);
    }


    void clear()
    {
        textPane.setText("");
        repaint();
    }

    synchronized public void logError(String str)
    {
        log(str, Color.RED);
    }

    synchronized public void logWarning(String str)
    {
        log(str, Color.ORANGE);
    }

    synchronized public void logMessage(String str)
    {
        log(str, Color.BLACK);
    }

    private void log(String str, Color col)
    {
        try
        {
            StyleContext sc = StyleContext.getDefaultStyleContext();
            AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY,
                                        StyleConstants.Foreground, col);

            textPane.getDocument().insertString(textPane.getDocument().getLength(), str + "\n", aset);
        }
        catch (BadLocationException e)
        {

        }
        textPane.repaint();
    }
}


class BuildTask extends Thread
{
    private LogPanel logPanel;
    private BuildPanel buildPanel;
    private KowalskiProject project;
    private boolean buildEngineData = false;
    private boolean buildWaveBanks = false;
    private List<WaveBank> waveBanksToBuild;
    
    BuildTask(LogPanel lp, BuildPanel bp, KowalskiProject p)
    {
        logPanel = lp;
        buildPanel = bp;
        project = p;
    }

    @Override
    public void run()
    {
        logPanel.clear();
        buildPanel.setGUIEnabled(false);
        Exception e = null;
        String projectPath = KowalskiEditorFrame.getInstance().getCurrentProjectPath();
        if (buildEngineData)
        {
            EngineDataBuilder edb = new EngineDataBuilder();
            edb.setLogger(logPanel);
            
            try
            {
                edb.buildEngineData(projectPath,
                                    buildPanel.getEngineDataPath().getAbsolutePath());
            }
            catch (IOException ex)
            {
                logPanel.logError(ex.toString());
                ex.printStackTrace();
            }
            catch (JAXBException ex)
            {
                logPanel.logError(ex.toString());
                ex.printStackTrace();
            }
            catch (ProjectDataException ex)
            {
                logPanel.logError(ex.toString());
                ex.printStackTrace();
            }

        }

        if (buildWaveBanks)
        {
            WaveBankBuilder wbb = new WaveBankBuilder();
            wbb.setLogger(logPanel);

            try
            {
                wbb.buildWavebanks(project,
                                   new File(KowalskiEditorFrame.getInstance().getCurrentProjectPath()),
                                   buildPanel.getWaveBankPath().getAbsolutePath(),
                                   waveBanksToBuild);
            }
            catch (IOException ex)
            {
                logPanel.logError(ex.toString());
                ex.printStackTrace();
            }
            catch (UnsupportedAudioFileException ex)
            {
                logPanel.logError(ex.toString());
                ex.printStackTrace();
            }
            catch (ProjectDataException ex)
            {
                logPanel.logError(ex.getMessages().get(0));
                ex.printStackTrace();
            }
            
            /*
            Iterator<WaveBank> it = waveBanksToBuild.keySet().iterator();
            while (it.hasNext())
            {
                WaveBank wb = it.next();
                String path = waveBanksToBuild.get(wb);
                File outFile = buildPanel.getWaveBankPath();
                try
                {
                    wbb.buildWaveBank(project, projectDir, wb, outFile, path);
                }
                catch (IOException ex)
                {
                    logPanel.logError(ex.toString());
                    
                }
                catch (ProjectDataException ex)
                {
                    logPanel.logError(ex.getMessages().get(0));
                }
            }
            
             */
            
        }
        
        buildPanel.setGUIEnabled(true);
    }

    void startWaveBankBuild(List<WaveBank> waveBanks)
    {
        buildWaveBanks = true;
        buildEngineData = false;
        waveBanksToBuild = waveBanks;
        start();
    }

    void startEngineDataBuild()
    {
        buildWaveBanks = false;
        buildEngineData = true;
        start();
    }
}

/**
 *
 */
class BuildPanel extends AbstractPanel implements ProjectDataChangeListener
{
    private LogPanel logPanel;
    /** Used to enable/disable the GUI./*/
    private List<JComponent> components = new ArrayList<JComponent>();
    private WaveBankTree waveBankTree;

    BuildPanel()
    {
        AbstractPanel controlsPanel = new AbstractPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));

        JButton buildEngineDataButton = new JButton("Build engine data");
        buildEngineDataButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onBuildEngineData();
            }
        });
        controlsPanel.add(buildEngineDataButton);
        components.add(buildEngineDataButton);

        waveBankTree = new WaveBankTree();
        controlsPanel.add(new JScrollPane(waveBankTree));
        components.add(waveBankTree);

        
        JButton buildAllWaveBanksButton = new JButton("Build all wave banks");
        buildAllWaveBanksButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onBuildAllWaveBanks();
            }
        });
        controlsPanel.add(buildAllWaveBanksButton);
        components.add(buildAllWaveBanksButton);

        JButton buildSelectedWaveBanksButton = new JButton("Build selected wave banks");
        buildSelectedWaveBanksButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onBuildSelectedWaveBanks();
            }
        });

        controlsPanel.add(buildSelectedWaveBanksButton);
        components.add(buildSelectedWaveBanksButton);

        JSplitPane s = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        s.setOpaque(false);
        s.setBorder(null);
        logPanel = new LogPanel();
        s.setRightComponent(logPanel);
        s.setLeftComponent(controlsPanel);

        //s.setResizeWeight(0.5);
        setLayout(new BorderLayout());
        add(s, BorderLayout.CENTER);
    }

    File getWaveBankPath()
    {
        File currentProjDir = KowalskiEditorFrame.getInstance().getCurrentProjectDir();
        if (currentProjDir == null)
        {
            return new File(System.getProperty("user.home"));
        }

        return currentProjDir;
    }

    File getEngineDataPath()
    {
        File currentProjDir = KowalskiEditorFrame.getInstance().getCurrentProjectDir();
        if (currentProjDir == null)
        {
            return new File(System.getProperty("user.home"));
        }

        return currentProjDir;
    }

    void setGUIEnabled(boolean enabled)
    {
        for (int i = 0; i < components.size(); i++)
        {
            components.get(i).setEnabled(enabled);
        }
        repaint();
        
    }

    private void onBuildEngineData()
    {
        BuildTask bt =
                new BuildTask(logPanel, this,
                              KowalskiEditorFrame.getInstance().getCurrentProject());
        bt.startEngineDataBuild();
    
    }

    private void onBuildAllWaveBanks()
    {
        BuildTask bt =
                new BuildTask(logPanel, this,
                              KowalskiEditorFrame.getInstance().getCurrentProject());
        bt.startWaveBankBuild(waveBankTree.getAllWaveBanks());

    }

    private void onBuildSelectedWaveBanks()
    {
        BuildTask bt =
                new BuildTask(logPanel, this,
                              KowalskiEditorFrame.getInstance().getCurrentProject());
        bt.startWaveBankBuild(waveBankTree.getSelectedWaveBanks());

    }

    void refresh()
    {
        waveBankTree.refresh(KowalskiEditorFrame.getInstance().getCurrentProject());
        repaint();
    }

    public void onProjectDataChanged(KowalskiProject project)
    {
        refresh();
    }
}
