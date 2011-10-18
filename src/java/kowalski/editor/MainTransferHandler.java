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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;


class ProjectObjectTransferable implements Transferable
{

    private Object transferedObject;
    private static DataFlavor dataFlavour = null;

    ProjectObjectTransferable(Object obj)
    {
        transferedObject = obj;
    }

    static DataFlavor getFlavor()
    {
        initFlavor();
        return dataFlavour;
    }

    private static void initFlavor()
    {
        if (dataFlavour != null)
        {
            return;
        }
        String type = DataFlavor.javaJVMLocalObjectMimeType +
                   ";class=java.lang.Object";
        try
        {
            dataFlavour = new DataFlavor(type);
        }
        catch (ClassNotFoundException e)
        {
            //
        }
    }

    public DataFlavor[] getTransferDataFlavors()
    {
        initFlavor();
        return new DataFlavor[] {dataFlavour};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return dataFlavour.equals(flavor);
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
    {
        return transferedObject;
    }

}

/**
 * Takes care of drags and drops for all GUI components.
 */
class MainTransferHandler extends TransferHandler
{

    
    /**
     * Called when a drag starts on a given component. Packs whatever
     * data should be dragged into a transferable.
     */
    @Override
    protected Transferable createTransferable(JComponent c)
    {
        System.out.println("createTransferable called from " + c);
        if (c instanceof ProjectTree)
        {
            ProjectTree tree = (ProjectTree)c;
            return tree.createTransferable();
        }
        else if (c instanceof WaveDirectoryTree)
        {
            WaveDirectoryTree tree = (WaveDirectoryTree)c;
            return tree.createTransferable();
        }

        return null;
    }

    @Override
    public int getSourceActions(JComponent c)
    {
        //System.out.println("TreeTransferHandler.getSourceActions " + c);
        return COPY_OR_MOVE;
    }

    @Override
    protected void exportDone(JComponent source,
                              Transferable data,
                              int action)
    {
        //System.out.println("TreeTransferHandler.exportDone: from " + source);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support)
    {
        if (!support.isDrop())
        {
            return false;
        }

        boolean canImport = false;
        if (support.getComponent() instanceof ProjectTree)
        {
            ProjectTree tree = (ProjectTree)support.getComponent();
            JTree.DropLocation dropLocation =
                        (JTree.DropLocation)support.getDropLocation();
            //System.out.println("drop loc " + dropLocation);
            try
            {
                if (dropLocation.getPath() != null)
                {
                    //System.out.println("path " + dropLocation.getPath());
                    canImport |=
                        tree.canAcceptDrop(support.getTransferable().getTransferData(ProjectObjectTransferable.getFlavor()),
                                           dropLocation.getPath().getLastPathComponent());
                }
            }
            catch (IOException e)
            {
                //
            }
            catch (UnsupportedFlavorException e)
            {
                
            }
        }
        else if (support.getComponent() instanceof AudioDataTable)
        {
            canImport |= true;
        }

        boolean isFileDrop = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);

        canImport |= isFileDrop;
        
        return canImport;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support)
    {
        if (!canImport(support))
        {
            return false;
        }

        //try to open any dropped files regardless of the drop target.
        if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        {
            return openDroppedFiles(support);
        }
        else if (support.getComponent() instanceof ProjectTree)
        {
            ProjectTree tree = (ProjectTree)support.getComponent();
            JTree.DropLocation dropLocation =
                        (JTree.DropLocation)support.getDropLocation();
            try
            {
                tree.onNodeDropped(support.getTransferable().getTransferData(ProjectObjectTransferable.getFlavor()),
                                   dropLocation.getPath().getLastPathComponent());
            }
            /*
            catch (IOException e)
            {
                System.out.println("e " + e);
            }
            catch (UnsupportedFlavorException e)
            {
                System.out.println("e " + e);
            }*/
            catch (Exception e)
            {
                System.out.println("e " + e);
                e.printStackTrace();
            }
            return true;
        }
        else if (support.getComponent() instanceof AudioDataTable)
        {
            Object droppedObject = null;
            AudioDataTable table = (AudioDataTable)support.getComponent();
            try
            {
                droppedObject = support.getTransferable().getTransferData(ProjectObjectTransferable.getFlavor());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (UnsupportedFlavorException e)
            {
                e.printStackTrace();
            }
            System.out.println("droppedObject " + droppedObject.getClass());

            if (droppedObject instanceof List)
            {
                System.out.println("droppedObject " + droppedObject);
                table.onFilesDropped((List<File>)droppedObject);
                return true;
            }
        }


        return false;

    }

    private boolean openDroppedFiles(TransferHandler.TransferSupport support)
    {
        KowalskiEditorFrame frame = KowalskiEditorFrame.getInstance();
        Transferable t = support.getTransferable();
        try
        {
            /* fetch the data from the Transferable */
            Object data = t.getTransferData(DataFlavor.javaFileListFlavor);

            /* data of type javaFileListFlavor is a list of files */
            java.util.List fileList = (java.util.List)data;

            File[] files = new File[fileList.size()];
            /* loop through the files in the file list */
            int i = 0;
            for (Object fileObj : fileList)
            {
                File file = (File)fileObj;
                files[i] = file;
                i++;

                /* This is where you place your code for opening the
                 * document represented by the "file" variable.
                 * For example:
                 * - create a new internal frame with a text area to
                 *   represent the document
                 * - use a BufferedReader to read lines of the document
                 *   and append to the text area
                 * - add the internal frame to the desktop pane,
                 *   set its bounds and make it visible
                 */
            }
            
            return frame.onFilesDropped(files);
        }
        catch (UnsupportedFlavorException e)
        {
            e.printStackTrace();
            return false;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }


}
