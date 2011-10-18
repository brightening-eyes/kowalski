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

import kowalski.util.JarResourceLoader;
import kowalski.util.JFrameWithKowalskiIcon;
import javax.swing.event.ChangeEvent;
import kowalski.tools.binaryfileviewer.BinaryFileViewerFrame;
import kowalski.editor.actions.AbstractProjectDataAction;
import java.awt.event.WindowEvent;
import kowalski.tools.data.ProjectDataXMLSerializer;
import kowalski.tools.data.ProjectDataValidator;
import kowalski.tools.data.ProjectDataException;
import kowalski.tools.data.ProjectDataUtils;
import java.awt.BorderLayout;
import java.awt.Component;

import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeListener;
import javax.xml.bind.JAXBException;
import kowalski.tools.data.WaveBankBuilder;
import kowalski.tools.data.EngineDataBuilder;
import kowalski.tools.data.xml.KowalskiProject;

/**
 *
 */
class KowalskiEditorFrame extends JFrameWithKowalskiIcon
{
    private static final String PROJECT_FILE_EXTENSION = ".xml";

    private EditorSettings editorSettings;
    /** */
    private KowalskiProject currentProject;
    /** */
    private ProjectTree projectTree;
    private File audioFileDirectory;
    private File currentProjectFile;
    private ProjectDataXMLSerializer xmlDeserializer;
    private BuildPanel buildPanel;
    private AuditionPanel auditionPanel;
    private EditPanel editPanel;
    private WaveDirectoryTree waveDirectoryTree;
    private StatusBar statusBar;
    private static KowalskiEditorFrame instance;
    private AudioPlaybackProgressTask audioPlaybackProgressTask;
    private JTabbedPane tabbedPane;
    
    private EngineDataBuilder engineDataBuilder;
    private WaveBankBuilder waveBankBinaryWriter;
    private boolean isEngineLibLoaded = false;
    JMenuItem redoMenuItem;
    JMenuItem undoMenuItem;

    private List<AbstractProjectDataAction> undoStack = new ArrayList<AbstractProjectDataAction>();
    private int undoOffset = 0;

    private List<ProjectDataChangeListener> projectDataChangeListeners =
            new ArrayList<ProjectDataChangeListener>();
    private List<AudioFileDirectoryChangeListener> audioDirectoryChangeListeners =
            new ArrayList<AudioFileDirectoryChangeListener>();

    /**
     * Constructor.
     */
    KowalskiEditorFrame()
    {
        super();

        addWindowListener( new WindowAdapter() {
            //TODO: does not get called on cmd-Q
            @Override
            public void windowClosing(WindowEvent e)
            {
                onExit();
            }
        });
        setTransferHandler(new MainTransferHandler());

        editorSettings = new EditorSettings();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        statusBar = new StatusBar();
        add(statusBar, BorderLayout.SOUTH);
        loadNativeLib();
        setTitle("Kowalski Editor");
        instance = this;
        initMenus();

        //
        engineDataBuilder = new EngineDataBuilder();
        waveBankBinaryWriter = new WaveBankBuilder();
        xmlDeserializer = ProjectDataXMLSerializer.getInstance();

        //
        projectTree = new ProjectTree(currentProject);
        addProjectDataChangeListener(projectTree);
        
        //openProjectXML(new File("../res/demodata/master/demoproject.xml"));
        onNewProject();

        waveDirectoryTree = new WaveDirectoryTree();
        addAudioDirectoryChangeListener(waveDirectoryTree);
        
        //create the three main panels and put them in a tabbed pane
        tabbedPane = new JTabbedPane();
        editPanel = new EditPanel(projectTree, waveDirectoryTree);
        tabbedPane.add("Edit", editPanel);
        auditionPanel = new AuditionPanel();
        tabbedPane.add("Audition", auditionPanel);
        buildPanel = new BuildPanel();
        tabbedPane.add("Build", buildPanel);

        addProjectDataChangeListener(editPanel);
        addProjectDataChangeListener(auditionPanel);
        addProjectDataChangeListener(buildPanel);

        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e)
            {
                Component selectedComponent = tabbedPane.getSelectedComponent();
                onTabbedPaneComponentChanged(selectedComponent);
            }
        });


        tabbedPane.setEnabledAt(1, isEngineLibLoaded);
        add(tabbedPane, BorderLayout.CENTER);

        fireAudioFileDirectoryChanged();
        fireProjectDataChanged();
    }

    private void onTabbedPaneComponentChanged(Component selectedComponent)
    {
        if (selectedComponent instanceof EditPanel)
        {
            //onEditPanelSelected();
        }
        else if (selectedComponent instanceof BuildPanel)
        {
            buildPanel.refresh();
        }
        else if (selectedComponent  instanceof AuditionPanel)
        {
            //onAuditionPanelSelected();
        }
    }

    private void loadNativeLib()
    {
        isEngineLibLoaded = JarResourceLoader.load();
    }

    boolean pushAndPerformAction(AbstractProjectDataAction action)
    {
        //first try to perform the action
        try
        {
            action.perform(currentProject);
        }
        catch (ProjectDataException e)
        {
            //failed to perform action. notify user and return false
            showErrorMessageDialog(e);
            return false;
        }

        //if successful, push it onto the undo stack
        undoStack.add(action);
        undoMenuItem.setEnabled(true);
        undoMenuItem.setText("Undo " + action.getName());
        fireProjectDataChanged();

        //TODO: remove this paranoid validation
        try
        {
            ProjectDataValidator.validateProjectData(currentProject);
        }
        catch (Exception e)
        {
            showErrorMessageDialog(e);
        }

        return true;
    }

    boolean isEngineLibLoaded()
    {
        return isEngineLibLoaded;
    }


    /**
     * Convenience method allowing quick access to the editor frame instance
     * from other classes.
     */
    static KowalskiEditorFrame getInstance()
    {
        return instance;
    }

    /**
     * Returns the project currently being edited.
     * @return
     */
    KowalskiProject getCurrentProject()
    {
        return currentProject;
    }

    String getCurrentProjectPath()
    {
        if (currentProjectFile == null)
        {
            return null;
        }

        return currentProjectFile.getAbsolutePath();
    }

    File getCurrentProjectDir()
    {
        if (currentProjectFile == null)
        {
            return null;
        }
        return currentProjectFile.getParentFile();
    }

    
    void showErrorMessageDialog(Exception e)
    {
        ErrorDialog ed = new ErrorDialog();
        ed.setException(e);
        JOptionPane.showMessageDialog(this, ed, "Error", JOptionPane.ERROR_MESSAGE);
    }

    void showErrorMessageDialog(String msg)
    {
        JOptionPane.showMessageDialog(this,
                    msg,
                    "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    void resetSoundPlaybackProgress()
    {
        //kill the old progress meter update thread
        if (audioPlaybackProgressTask != null)
        {
            audioPlaybackProgressTask.requestInterrupt();
            while(audioPlaybackProgressTask.isAlive())
            {
                //System.out.println("waiting for thread to die");
            }
        }
        statusBar.setProgressPercent(0);
    }

    void startSoundPlaybackProgress(float durationMicroSec)
    {
        //kill the old progress meter update thread
        if (audioPlaybackProgressTask != null)
        {
            audioPlaybackProgressTask.requestInterrupt();
            while(audioPlaybackProgressTask.isAlive())
            {
                //System.out.println("waiting for thread to die");
            }
        }
        //and start a new one
        audioPlaybackProgressTask = new AudioPlaybackProgressTask(statusBar, durationMicroSec);
        audioPlaybackProgressTask.start();

    }

    /*
    void playWave(File file)
    {
        String errorMsg = null;
        try
        {
            soundPlayer.playSound(file);
        }
        catch (IOException e)
        {
            errorMsg = e.getMessage();
        }
        catch (LineUnavailableException e)
        {
            errorMsg = e.getMessage();
        }
        catch (UnsupportedAudioFileException e)
        {
            errorMsg = e.getMessage();
        }

        if (errorMsg != null)
        {
            JOptionPane.showMessageDialog(this,
                    "Error playing " + file.getAbsolutePath() + "\n" + errorMsg,
                    "Error",
                JOptionPane.ERROR_MESSAGE);
        }

        
    }*/

    private void initMenus()
    {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);

        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);

        JMenu projectMenu = new JMenu("Project");
        menuBar.add(projectMenu);

        setJMenuBar(menuBar);

        //
        //edit menu
        //
        undoMenuItem = new JMenuItem("Undo");
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        undoMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onUndo();
            }
        });
        editMenu.add(undoMenuItem);

        redoMenuItem = new JMenuItem("Redo");
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        redoMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onRedo();
            }
        });
        editMenu.add(redoMenuItem);

        //
        //view menu
        //
        JMenuItem rescanWaveDirItem = new JMenuItem("Rescan wave directory");
        rescanWaveDirItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onRescanWaveDirectory();
            }
        });
        viewMenu.add(rescanWaveDirItem);

        //
        //project menu
        //
        JMenuItem setAudioDirItem = new JMenuItem("Set audio file directory");
        setAudioDirItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onSetAudioFileDir();
            }
        });
        projectMenu.add(setAudioDirItem);

        projectMenu.addSeparator();

        JMenuItem reportUnusedSoundsItem = new JMenuItem("Report unused sounds");
        reportUnusedSoundsItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException();
            }
        });
        projectMenu.add(reportUnusedSoundsItem);

        JMenuItem removeUnusedSoundsItem = new JMenuItem("Remove unused sounds");
        removeUnusedSoundsItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException();
            }
        });
        projectMenu.add(removeUnusedSoundsItem);

        JMenuItem reportUnusedAudioDataRefsItem = new JMenuItem("Report unused audio files");
        reportUnusedAudioDataRefsItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException();
            }
        });
        projectMenu.add(reportUnusedAudioDataRefsItem);

        JMenuItem removeUnusedAudioDataRefsItem = new JMenuItem("Remove unused audio file references");
        removeUnusedAudioDataRefsItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException();
            }
        });
        projectMenu.add(removeUnusedAudioDataRefsItem);

        JMenuItem reportNonExistingAudioFilesItem = new JMenuItem("Report non-existing audio files");
        reportNonExistingAudioFilesItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException();
            }
        });
        projectMenu.add(reportNonExistingAudioFilesItem);

        JMenuItem engineDataBinaryViewer = new JMenuItem("View engine data binary file");
        engineDataBinaryViewer.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onViewEngineDataBinary();
            }
        });


        //
        //file menu
        //
        JMenuItem newItem = new JMenuItem("New project");
        newItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onNewProject();
            }
        });

        /*
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

                toolsMenu.add(engineDataBinaryViewer);

        JMenuItem waveBankBinaryViewer = new JMenuItem("View wave bank binary file");
        waveBankBinaryViewer.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onViewWaveBankBinary();
            }
        });
        toolsMenu.add(waveBankBinaryViewer);
        */

        JMenu recentSubMenu = new JMenu("Recent");

        JMenu openSubMenu = new JMenu("Open");

        JMenuItem openItem = new JMenuItem("Open project");
        openItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onOpenProject();
            }
        });
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        JMenuItem saveItem = new JMenuItem("Save project");
        saveItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onSaveProject();
            }
        });
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));


        JMenuItem setWaveDirItem = new JMenuItem("Set wave directory");
        setWaveDirItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onSetAudioFileDir();
            }
        });

        JMenuItem saveAsItem = new JMenuItem("Save project as...");
        saveAsItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onSaveProjectAs();
            }
        });

        JMenu buildSubMenu = new JMenu("Build");

        JMenuItem buildEngineDataItem = new JMenuItem("Engine data");
        buildEngineDataItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onExportProjectBinary();
            }
        });

        JMenuItem buildWaveBankItem = new JMenuItem("Wave bank data");
        buildWaveBankItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onExportWaveBankBinaries();
            }
        });

        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onQuit();
            }
        });
        quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        fileMenu.add(newItem);
        openSubMenu.add(openItem);
        fileMenu.add(openSubMenu);
        fileMenu.add(recentSubMenu);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        buildSubMenu.add(buildEngineDataItem);
        buildSubMenu.add(buildWaveBankItem);
        fileMenu.add(buildSubMenu);

        //add quit and about items to the file menu if not on os x, where
        //it shows up in the application menu by default
        if (!SwingUtil.isOSX())
        {
            fileMenu.addSeparator();
            fileMenu.add(quitItem);

            JMenu helpMenu = new JMenu("Help");
            helpMenu.add(new JMenuItem("About"));
            menuBar.add(helpMenu);
        }
    }


    void onExportProjectBinary()
    {
        FileDialog fd = new FileDialog(this, "Export project binary data", FileDialog.SAVE);
        fd.setVisible(true);

        String selectedFile = fd.getFile();
        
        if (selectedFile == null)
        {
            return;
        }
        File file = new File(fd.getDirectory(), fd.getFile());

        try
        {
            engineDataBuilder.buildEngineData(getCurrentProjectPath(),
                                              file.getAbsolutePath());
        }
        catch (IOException e)
        {
            showErrorMessageDialog(e);
        }
        catch (JAXBException e)
        {
            showErrorMessageDialog(e);
        }
        catch (ProjectDataException e)
        {
            showErrorMessageDialog(e);
        }
        
    }

    void onExportWaveBankBinaries()
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.showDialog(this, null);
        if (fc.getSelectedFile() == null)
        {
            return;
        }
        File targetDir = fc.getSelectedFile();

        try
        {
            waveBankBinaryWriter.buildWavebanks(getCurrentProjectPath(), targetDir.getAbsolutePath());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            showErrorMessageDialog(e);
        }
    }

    void onNewProject()
    {
        currentProjectFile = null;
        //TODO: ask the user for a path
        String wavePath = "";
        KowalskiProject newProject = ProjectDataUtils.createBlankProject(wavePath);
        setProject(newProject);
        fireProjectDataChanged();
    }

    void onOpenProject()
    {
        FileDialog fd = new FileDialog(this);
        fd.setVisible(true);

        String f = fd.getFile();
        if (f == null)
        {
            return;
        }
        
        File selectedFile = new File(fd.getDirectory(), f);
        openProjectXML(selectedFile);
    
    }

    void onSaveProject()
    {
        if (currentProjectFile == null)
        {
            onSaveProjectAs();
            return;
        }

        try
        {
            xmlDeserializer.serializeKowalskiProject(currentProject, currentProjectFile);
        }
        catch (Exception e)
        {
            showErrorMessageDialog(e);
        }
    }

    void onSaveProjectAs()
    {
        FileDialog fd = new FileDialog(this, "Save project data", FileDialog.SAVE);
        fd.setVisible(true);

        String selectedFile = fd.getFile();

        if (selectedFile == null)
        {
            return;
        }
        File file = new File(fd.getDirectory(), fd.getFile() + PROJECT_FILE_EXTENSION);

        try
        {
            xmlDeserializer.serializeKowalskiProject(currentProject, file);
            currentProjectFile = file;
        }
        catch (Exception e)
        {
            showErrorMessageDialog(e);
        }
    }

    void onExit()
    {

    }

    boolean openProjectXML(File file)
    {
        KowalskiProject project = null;

        try
        {
            final FileInputStream fis = new FileInputStream(file);

            project = xmlDeserializer.deserializeKowalskiProject(fis);
            ProjectDataValidator.validateProjectData(project);
        }
        catch (FileNotFoundException e)
        {
            showErrorMessageDialog(e);
            return false;
        }
        catch (JAXBException e)
        {
            showErrorMessageDialog(e);
            return false;
        }
        catch (ProjectDataException e)
        {
            showErrorMessageDialog(e);
            return false;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        

        if (project.isAudioFileRootDirectoryRelative())
        {
            audioFileDirectory = new File(file.getParentFile(), project.getAudioFileRootDirectory());
            System.out.println(audioFileDirectory + ", " + audioFileDirectory.exists());
        }
        else
        {
            audioFileDirectory = new File(project.getAudioFileRootDirectory());
        }

        
        if (!audioFileDirectory.exists())
        {
            JOptionPane.showMessageDialog(this,
                                          "The specified audio file directory\n" +
                                          audioFileDirectory.getAbsolutePath() +
                                          "does not exist, please choose a valid " +
                                          "location.",
                                          "Error reading project data",
                                          JOptionPane.ERROR_MESSAGE);
            onSetAudioFileDir();
            
        }
        else if (!audioFileDirectory.isDirectory())
        {
            JOptionPane.showMessageDialog(this,
                                          "The specified audio file directory\n" +
                                          audioFileDirectory.getAbsolutePath() +
                                          " is not a directory, please choose a valid " +
                                          "location.",
                                          "Error reading project data",
                                          JOptionPane.ERROR_MESSAGE);
            onSetAudioFileDir();
        }

        List<String> nonExistingFileList =
                ProjectDataUtils.getRelativePathsOfNonExistingAudioFiles(project, getCurrentProjectDir());
        ProjectDataException e = new ProjectDataException("The following audio files do not exist:",
                nonExistingFileList);
        if (nonExistingFileList.size() > 0)
        {
            showErrorMessageDialog(e);
        }
        currentProjectFile = file;
        editorSettings.addRecentFile(file.toString());
        onRescanWaveDirectory();
        setProject(project);
        fireProjectDataChanged();
        return true;
    }

    void onQuit()
    {
        System.exit(0);
    }

    void onUndo()
    {
        if (undoStack.size() == 0)
        {
            return;
        }

        
        AbstractProjectDataAction action = undoStack.get(undoStack.size() - 1);
        undoStack.remove(undoStack.size() - 1);

        if (undoStack.size() > 0)
        {
            undoMenuItem.setText("Undo " + undoStack.get(undoStack.size() - 1).getName());
            undoMenuItem.setEnabled(true);
        }
        else
        {
            undoMenuItem.setText("Undo");
            undoMenuItem.setEnabled(false);
        }

        System.out.println("Undo: popped " + action + ", stack size after pop = " + undoStack.size());
        try
        {
            action.undo(currentProject);
            fireProjectDataChanged();
        }
        catch (ProjectDataException e)
        {
            //undo failed
            showErrorMessageDialog(e);
        }
    }

    void onRedo()
    {
        if (undoOffset <= 0)
        {
            return;
        }



        undoOffset--;

    }

    void onRescanWaveDirectory()
    {
        fireAudioFileDirectoryChanged();
    }

    private void setProject(KowalskiProject project)
    {
        currentProject = project;
        projectTree.setModel(new ProjectTreeModel(project));
        projectTree.reloadExpansionState();
    }

    void onSetAudioFileDir()
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.showDialog(this, null);
        if (fc.getSelectedFile() == null)
        {
            return;
        }
        audioFileDirectory = fc.getSelectedFile();
        currentProject.setAudioFileRootDirectory(audioFileDirectory.getAbsolutePath());
        fireAudioFileDirectoryChanged();
        repaint();
    }

    private void onViewWaveBankBinary()
    {
        FileDialog fd = new FileDialog(this, "Select wave bank file to view", FileDialog.LOAD);
        fd.setVisible(true);

        String selectedFile = fd.getFile();

        if (selectedFile == null)
        {
            return;
        }
        File file = new File(fd.getDirectory(), fd.getFile());

        try
        {
            viewBinary(file);
        }
        catch (Exception e)
        {
            showErrorMessageDialog(e);
        }

        
    }

    void onViewEngineDataBinary()
    {
        FileDialog fd = new FileDialog(this, "Select engine data file to view", FileDialog.LOAD);
        fd.setVisible(true);

        String selectedFile = fd.getFile();

        if (selectedFile == null)
        {
            return;
        }
        File file = new File(fd.getDirectory(), fd.getFile());

        try
        {
            viewBinary(file);
        }
        catch (Exception e)
        {
            showErrorMessageDialog(e);
        }
    }

    private void viewBinary(File file)
            throws Exception
    {
            BinaryFileViewerFrame frame =
                    new BinaryFileViewerFrame(file);
            frame.setVisible(true);

    }

    public void windowOpened(WindowEvent e)
    {

    }

    public void windowClosing(WindowEvent e)
    {

    }

    void addAudioDirectoryChangeListener(AudioFileDirectoryChangeListener l)
    {
        audioDirectoryChangeListeners.add(l);
    }

    void removeAudioDirectoryChangeListener(AudioFileDirectoryChangeListener l)
    {
        audioDirectoryChangeListeners.remove(l);
    }

    void addProjectDataChangeListener(ProjectDataChangeListener l)
    {
        projectDataChangeListeners.add(l);
    }

    void removeProjectDataChangeListener(ProjectDataChangeListener l)
    {
        projectDataChangeListeners.remove(l);
    }

    /**
     * Notifies all registered audio directory change listeners that
     * the audio directory or its contents has changed.
     */
    void fireAudioFileDirectoryChanged()
    {
        for (int i = 0; i < audioDirectoryChangeListeners.size(); i++)
        {
            audioDirectoryChangeListeners.get(i).onAudioDataDirectoryChanged(audioFileDirectory);
        }
    }

    /**
     * Notifies all registered project data change listeners that
     * the project data has changed.
     */
    void fireProjectDataChanged()
    {
        for (int i = 0; i < projectDataChangeListeners.size(); i++)
        {
            projectDataChangeListeners.get(i).onProjectDataChanged(currentProject);
        }
    }

    //TODO: unify the load code
    boolean onFilesDropped(File[] files)
    {
        if (files.length != 1)
        {
            return false;
        }

        String errorMessage = "";
        int numFailures = 0;
        
        //try to open the dropped file in the engine data viewer
        try
        {
            viewBinary(files[0]);
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            errorMessage += "Error reading binary file.\n";
            numFailures++;
        }

        //try to open the dropped file as a project
        if (!openProjectXML(files[0]))
        {
            errorMessage += "Error reading project data.";
            numFailures++;
        }

        if (numFailures == 2)
        {
            showErrorMessageDialog(errorMessage);
        }

        return numFailures < 2;
    }
}
