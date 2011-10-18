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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 */
class DirectoryScanTask extends Thread
{

    private boolean interruptRequested = false;
    private int numFiles = 0;
    private WaveDirectoryTree tree;
    private File root;

    DirectoryScanTask(WaveDirectoryTree t, File r)
    {
        numFiles = 30;
        tree = t;
        root = r;
    }

    synchronized void requestInterrupt()
    {
        interruptRequested = true;
    }

    private void populateDirectoryTree(File file,
                                       WaveDirectoryTreeNode treeNode,
                                       boolean separateFilesAndFolders,
                                       boolean showHiddenFiles)
    {
        List<File> folders = new ArrayList<File>();
        List<File> files = new ArrayList<File>();
        List<File> allFiles = new ArrayList<File>();

        for (File f : file.listFiles())
        {
            if (f.isHidden() && !showHiddenFiles)
            {
                continue;
            }

            if (separateFilesAndFolders)
            {
                if (f.isDirectory())
                {
                    folders.add(f);
                }
                else
                {
                    files.add(f);
                }
            }
            else
            {
                allFiles.add(f);
            }
        }

        //sort the endries (some of the lists will be empty)
        Collections.sort(files);
        Collections.sort(folders);
        Collections.sort(allFiles);

        if (separateFilesAndFolders)
        {
            for (int i = 0; i < folders.size(); i++)
            {
               File filei = folders.get(i);
               WaveDirectoryTreeNode dirTreeNode =
                    new WaveDirectoryTreeNode(filei);
               treeNode.add(dirTreeNode);
               populateDirectoryTree(filei, dirTreeNode, separateFilesAndFolders, showHiddenFiles);
            }
            for (int i = 0; i < files.size(); i++)
            {
                File filei = files.get(i);
                WaveDirectoryTreeNode fileTreeNode =
                    new WaveDirectoryTreeNode(filei);
                treeNode.add(fileTreeNode);
            }
        }
        else
        {
            for (int i = 0; i < allFiles.size(); i++)
            {
                File filei = allFiles.get(i);
                if (filei.isDirectory())
                {
                    WaveDirectoryTreeNode dirTreeNode =
                        new WaveDirectoryTreeNode(filei);
                    treeNode.add(dirTreeNode);
                    populateDirectoryTree(filei, dirTreeNode, separateFilesAndFolders, showHiddenFiles);
                }
                else
                {
                    WaveDirectoryTreeNode fileTreeNode =
                        new WaveDirectoryTreeNode(filei);
                    treeNode.add(fileTreeNode);
                }
            }
        }
    }

    private int countFiles(File root, boolean showHiddenFiles, int currentCount)
    {
        int nFiles = 0;
        for (File f : root.listFiles())
        {
            if (f.isHidden() && !showHiddenFiles)
            {
                continue;
            }

            if (!f.isDirectory())
            {
                nFiles++;
            }
        }

        for (File f : root.listFiles())
        {
            if (f.isHidden() && !showHiddenFiles)
            {
                continue;
            }

            if (f.isDirectory())
            {
                return currentCount + countFiles(f, showHiddenFiles, currentCount);
            }
            else
            {
                return nFiles;
            }
        }

        return 0;
    }

    @Override
    public void run()
    {
        System.out.println("rebuilding database");
        AudioFileDataBase.reload(root);
        System.out.println("done");

        System.out.println("scanning dir...");
        //create tree node structure
        boolean showHiddenFiles = false;
        boolean separateFilesAndFolders = !SwingUtil.isOSX();
        WaveDirectoryTreeNode rootNode = new WaveDirectoryTreeNode(root);
        populateDirectoryTree(root, rootNode, separateFilesAndFolders, showHiddenFiles);
        System.out.println("done ...");
        tree.onDirectoryScanningTaskFinished(rootNode);
    }
}
