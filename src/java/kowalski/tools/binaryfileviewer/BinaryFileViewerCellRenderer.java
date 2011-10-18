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

package kowalski.tools.binaryfileviewer;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import kowalski.util.IconManager;

/**
 * A cell renderer for a JTree displaying binary files.
 */
class BinaryFileViewerCellRenderer extends DefaultTreeCellRenderer
{
    BinaryFileViewerCellRenderer()
    {
        setOpenIcon(null);
        setClosedIcon(null);
        setLeafIcon(IconManager.getImageIcon("projecticon.png"));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus)
    {
        JLabel ret = (JLabel)super.getTreeCellRendererComponent(tree,
                                                                value,
                                                                sel,
                                                                expanded,
                                                                leaf,
                                                                row,
                                                                hasFocus);

        if (value == tree.getModel().getRoot())
        {
            ret.setIcon(IconManager.getImageIcon("logo.png"));
        }

        return ret;
    }
}