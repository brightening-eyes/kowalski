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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import kowalski.Kowalski;
import kowalski.tools.data.EngineDataBuilder;
import kowalski.tools.data.ProjectDataException;
import kowalski.tools.data.ProjectDataUtils;
import kowalski.tools.data.WaveBankBuilder;
import kowalski.kwlError;
import kowalski.tools.data.xml.Event;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.WaveBank;

class KowalskiUpdateTask extends Thread
{

    private static final float UPDATE_INTERVAL_SEC = 0.03f;

    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                sleep((int)(UPDATE_INTERVAL_SEC * 1000));
            }
            catch (InterruptedException e)
            {

            }
            Kowalski.kwlUpdate(UPDATE_INTERVAL_SEC);
        }
    }
}

class EventPlaybackPanel extends AbstractPanel
{
    private int eventHandle = -1;
    private String eventPath;
    private JSlider volumeSlider;
    private JSlider pitchSlider;
    private LogPanel logPanel;

    EventPlaybackPanel(String eventHierarcyPath, LogPanel l)
    {

        logPanel = l;
        addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
                System.out.println("key typed: " + e);
            }

            public void keyPressed(KeyEvent e) {
                System.out.println("key pressed: " + e);
            }

            public void keyReleased(KeyEvent e) {
                System.out.println("key released: " + e);
            }
        });

        eventHandle = Kowalski.kwlEventGetHandle(eventHierarcyPath);
        eventPath = eventHierarcyPath;
        kwlError error = Kowalski.kwlGetError();
        if (!error.equals(kwlError.KWL_NO_ERROR))
        {
            logPanel.logError("Error getting event handle for " + eventHierarcyPath
                    + ", " + error);
        }

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(new JLabel("Event: "));
        add(new JLabel(eventHierarcyPath));

        JButton playButton = new JButton("Play");
        playButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Kowalski.kwlEventStart(eventHandle);
                kwlError error = Kowalski.kwlGetError();
                if (!error.equals(kwlError.KWL_NO_ERROR))
                {
                    logPanel.logError("Error starting event " + eventPath
                            + ", " + error);
                }
            }
        });
        add(playButton);

        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Kowalski.kwlEventStop(eventHandle);
            }
        });
        add(stopButton);

        add(new JLabel("Volume: "));
        volumeSlider = new JSlider(0, 200);
        volumeSlider.setValue(100);
        volumeSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                Kowalski.kwlEventSetGain(eventHandle, 0.01f * volumeSlider.getValue());
            }
        });
        add(volumeSlider);

        add(new JLabel("Pitch: "));
        pitchSlider = new JSlider(0, 200);
        pitchSlider.setValue(100);
        pitchSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                Kowalski.kwlEventSetPitch(eventHandle, 0.01f * pitchSlider.getValue());
            }
        });
        add(pitchSlider);
    }
}

/**
 * 
 */
class AuditionPanel extends AbstractPanel implements ProjectDataChangeListener
{
    private JPanel panel;
    private LogPanel logPanel;

    private void checkKowalskiError()
    {
        kwlError error = Kowalski.kwlGetError();
        //if (error != kwlError.KWL_NO_ERROR)
        {
            logPanel.logMessage("Kowalski error: " + error);
        }
    }

    AuditionPanel()
    {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
                System.out.println("key typed: " + e);
            }

            public void keyPressed(KeyEvent e) {
                System.out.println("key pressed: " + e);
            }

            public void keyReleased(KeyEvent e) {
                System.out.println("key released: " + e);
            }
        });

        JButton rebuildButton = new JButton("Rebuild binaries");
        rebuildButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRebuildBinaries();
            }
        });
        panel.add(rebuildButton);
        JScrollPane scrollPane = new JScrollPane(panel);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        scrollPane.setOpaque(false);
        add(scrollPane);
        logPanel = new LogPanel();
        add(logPanel);
        //add(new JScrollPane(new TreeTable()));
        
    }

    private void onRebuildBinaries()
    {
        KowalskiProject project = KowalskiEditorFrame.getInstance().getCurrentProject();
        List<WaveBank> waveBanks = project.getWaveBankRootGroup().getWaveBanks();

        //create temporary files for wave banks and engine data
        List<File> waveBankTempFiles = new ArrayList<File>();
        File engineDataTempFile = null;
        try
        {
             engineDataTempFile = File.createTempFile("tempenginedata",
                     EngineDataBuilder.ENGINE_DATA_FILE_SUFFIX);
             for (int i = 0; i < waveBanks.size(); i++)
             {
                waveBankTempFiles.add(File.createTempFile(waveBanks.get(i).getID(),
                        WaveBankBuilder.WAVE_BANK_FILE_SUFFIX));
             }
        }
        catch (IOException e)
        {
            cleanupTemporaryFiles(engineDataTempFile, waveBankTempFiles);
            KowalskiEditorFrame.getInstance().showErrorMessageDialog("Could not create "
                    + "temporary files necessary for event auditioning: " + e.getMessage());
            return;
        }


        //build wavebank and project binaries

        WaveBankBuilder wbs = new WaveBankBuilder();
        File projectDir = KowalskiEditorFrame.getInstance().getCurrentProjectDir();
        try
        {
            Map<String, WaveBank> waveBanksByPath = ProjectDataUtils.getWaveBanksByHierarchyPath(project);
            Iterator<String> it = waveBanksByPath.keySet().iterator();
            int i = 0;
            while (it.hasNext())
            {
                throw new UnsupportedOperationException();
                /*String path = it.next();
                WaveBank wb = waveBanksByPath.get(path);
                wbs.buildWaveBank(project, projectDir, wb, waveBankTempFiles.get(i), path);
                i++;*/
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            KowalskiEditorFrame.getInstance().showErrorMessageDialog(e);
            cleanupTemporaryFiles(engineDataTempFile, waveBankTempFiles);
            return;
        }

        EngineDataBuilder ebs = new EngineDataBuilder();
        try
        {
            ebs.buildEngineData(KowalskiEditorFrame.getInstance().getCurrentProjectPath(),
                                engineDataTempFile.getAbsolutePath());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            KowalskiEditorFrame.getInstance().showErrorMessageDialog(e);
            cleanupTemporaryFiles(engineDataTempFile, waveBankTempFiles);
            return;
        }

        //restart the engine (TODO: support that) and load engine data
        logPanel.logMessage("loading engine data " + engineDataTempFile.getAbsolutePath());
        //Kowalski.kwlInitialize(22050, 2);
        //Kowalski.kwlLoadEngineData(engineDataTempFile.getAbsolutePath());
        checkKowalskiError();

       //load wave banks
        for (int i = 0; i < waveBankTempFiles.size(); i++)
        {
            logPanel.logMessage("loading wave bank " + waveBankTempFiles.get(i).getAbsolutePath());
            Kowalski.kwlWaveBankLoad(waveBankTempFiles.get(i).getAbsolutePath());
            checkKowalskiError();
        }

        //create one playback panel per event
        try
        {
       
            Map<String, Event> eventsByPath = ProjectDataUtils.getEventsByEventHierarchyPath(project);
            Iterator<String> keyIterator = eventsByPath.keySet().iterator();
            while (keyIterator.hasNext())
            {
                String key = keyIterator.next();
                panel.add(new EventPlaybackPanel(key, logPanel));
            }
            validate();
            repaint();

            //remove any temporaty files created
            cleanupTemporaryFiles(engineDataTempFile, waveBankTempFiles);

            //start update thread
            KowalskiUpdateTask t = new KowalskiUpdateTask();
            t.start();
        }
        catch (ProjectDataException e)
        {
            e.printStackTrace();
        }

    }

    private void cleanupTemporaryFiles(File engineDataFile, List<File> waveBankFiles)
    {
        if (engineDataFile.exists())
        {
            logPanel.logMessage("deleting " + engineDataFile);
            engineDataFile.delete();
        }

        for (int i = 0; i < waveBankFiles.size(); i++)
        {
            if (waveBankFiles.get(i).exists())
            {
                logPanel.logMessage("deleting " + waveBankFiles.get(i));
                waveBankFiles.get(i).delete();
            }
        }
    }

    public void onProjectDataChanged(KowalskiProject project)
    {
        
    }
}
