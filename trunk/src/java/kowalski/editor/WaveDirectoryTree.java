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

import kowalski.util.IconManager;
import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import kowalski.tools.data.AudioFileDescription;
import kowalski.tools.data.xml.KowalskiProject;

class WaveDirectoryTreeCellRenderer extends DefaultTreeCellRenderer
{
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
						  boolean sel,
						  boolean expanded,
						  boolean leaf, int row,
						  boolean hasFocus)
    {
        JLabel renderer = (JLabel)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        File f = (File)((DefaultMutableTreeNode)value).getUserObject();
        AudioFileDescription afd = AudioFileDataBase.getAudioFileDescription(f);
        if (f.isDirectory())
        {
            renderer.setIcon(IconManager.getImageIcon("groupicon.png"));
        }
        else if (afd != null)
        {
            renderer.setIcon(IconManager.getImageIcon("waveicon.png"));
        }
        else
        {
            renderer.setIcon(IconManager.getImageIcon("projecticon.png"));
        }

        if (value == tree.getModel().getRoot())
        {
            renderer.setText(f.toString());
        }
        else
        {
            renderer.setText(f.getName());
        }

        return renderer;
    }

}

class WaveDirectoryTreeNode extends DefaultMutableTreeNode
{
    
    private static final Icon waveIcon = IconManager.getImageIcon("waveicon.png");
    private static final Icon oggVorbisIcon = IconManager.getImageIcon("oggvorbisicon.png");
    private static final Icon adpcmIcon = IconManager.getImageIcon("waveiconcompressed.png");
    private static final Icon fileIcon = IconManager.getImageIcon("projecticon.png");
    private static final Icon folderIcon = IconManager.getImageIcon("groupicon.png");
    private Icon icon;
    private File file;

    WaveDirectoryTreeNode(File f)
    {
        super(f);
        file = f;
        if (file.isDirectory())
        {
            icon = folderIcon;
        }
        else
        {
            AudioFileDescription d = AudioFileDataBase.getAudioFileDescription(file);
            if (d != null)
            {
                if (d.getEncoding().equals(AudioFileDescription.Encoding.PCM))
                {
                    icon = waveIcon;
                }
                else if (d.getEncoding().equals(AudioFileDescription.Encoding.IMAADPCM))
                {
                    icon = adpcmIcon;
                }
                else if (d.getEncoding().equals(AudioFileDescription.Encoding.Vorbis))
                {
                    icon = oggVorbisIcon;
                }
            }
            else
            {
                icon = fileIcon;
            }
        }
    }

    File getFile()
    {
        return file;
    }

    Icon getIcon()
    {
        return icon;
    }

    @Override
    public String toString()
    {
        return file.getName();
    }
    
}

/**
 *
 */
class WaveDirectoryTree extends JTree
                        implements TreeSelectionListener, KeyListener, AudioFileDirectoryChangeListener


{
    private WaveDirectoryTreeNode rootNode;
    private boolean isTreeScanningInProgress = false;

    WaveDirectoryTree()
    {
        super();
        setRootVisible(true);
        addTreeSelectionListener(this);
        addKeyListener(this);
        setShowsRootHandles(true);
        setCellRenderer(new WaveDirectoryTreeCellRenderer());
        setDragEnabled(true);
        setTransferHandler(new MainTransferHandler());
        
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        //System.out.println("pressed: " + e);
        if (e.getKeyCode() == KeyEvent.VK_SPACE)
        {
            WaveDirectoryTreeNode selectedNode =
                    (WaveDirectoryTreeNode)getSelectionPath().getLastPathComponent();

            if (selectedNode == null)
            {
                return;
            }

            File selectedFile = (File)selectedNode.getFile();
            if (selectedFile == null || selectedFile.isDirectory())
            {
                return;
            }

            try
            {
                CachedSoundPlayer.playSound(selectedFile);
            }
            catch (Exception ex)
            {
                //ex.printStackTrace();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        //System.out.println("released: " + e);
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
        //System.out.println("typed: " + e);
    }

    /**
     * 
     * @param root
     */
    void setWaveDirectory(File root)
    {
        rootNode = null;
        //clear tree.
        ((DefaultTreeModel)getModel()).setRoot(rootNode);

        //nothing further if the new root directory is invalid
        if (root == null || !root.exists() || !root.isDirectory())
        {
            ((DefaultTreeModel)getModel()).reload();
            return;
        }

        startDirectoryScanningTask(root);
         
    }

    private void startDirectoryScanningTask(File root)
    {
        if (isTreeScanningInProgress)
        {
            return;
        }

        isTreeScanningInProgress = true;
        
        DirectoryScanTask t = new DirectoryScanTask(this, root);
        t.start();
    }

    synchronized void onDirectoryScanningTaskFinished(WaveDirectoryTreeNode root)
    {
        System.out.println("onDirectoryScanningTaskFinished " + root.getChildCount());
        rootNode = root;
        ((DefaultTreeModel)getModel()).setRoot(rootNode);
        ((DefaultTreeModel)getModel()).reload();
        //expandRow(0);
        isTreeScanningInProgress = false;
        repaint();
    }
/*
    @Override
    public void dragGestureRecognized(DragGestureEvent e)
    {
        System.out.println("dragGestureRecognized " + e);
        //find the tree node under the cursor, if any.
        Point clickPoint = e.getDragOrigin();
        TreePath path = getPathForLocation (clickPoint.x, clickPoint.y);
        if (path == null)
        {
            return;
        }
        draggedNode = path.getLastPathComponent();
        System.out.println ("wave tree. dragged node " + draggedNode);
        dragSource.startDrag (e, Cursor.getDefaultCursor(),
                              new ProjectEntityTransferable(draggedNode), this);
    }*/

    @Override
    public void valueChanged(TreeSelectionEvent e)
    {
        //
    }

    public void onAudioDataDirectoryChanged(File newDir)
    {
        setWaveDirectory(newDir);
    }

    Transferable createTransferable()
    {
        List<File> files = new ArrayList<File>();
        for (TreePath p : getSelectionPaths())
        {
            WaveDirectoryTreeNode n = 
                    (WaveDirectoryTreeNode)p.getLastPathComponent();
            if (n != null)
            {
                if (!n.getFile().isDirectory())
                {
                    files.add(n.getFile());
                }
            }
        }
                

        if (files.size() > 0)
        {
            return new ProjectObjectTransferable(files);
        }

        return null;
        
    }
}
