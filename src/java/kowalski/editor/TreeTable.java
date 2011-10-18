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

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.TableCellRenderer;

class TreeTableCellRenderer extends JTree
             implements TableCellRenderer
{
    protected int visibleRow;
    private JTable table;

    TreeTableCellRenderer(JTable t)
    {
        table = t;
    }

    public void setBounds(int x, int y, int w, int h)
    {
              super.setBounds(x, 0, w, table.getHeight());
    }

    public void paint(Graphics g)
    {
              g.translate(0, -visibleRow * getRowHeight());
              super.paint(g);
    }

    public Component getTableCellRendererComponent(JTable table,
              Object value,
              boolean isSelected,
              boolean hasFocus,
              int row, int column)
    {

        visibleRow = row;
        return this;
    }
 }
/**
 *
 */
class TreeTable extends JTable
{
    TreeTable()
    {
        super(new Object[][] {{"bla", "bla2"}, {"o", "p"}}, new Object[] {"col1", "col2"});
        getColumnModel().getColumn(0).setCellRenderer(new TreeTableCellRenderer(this));
    }
}
