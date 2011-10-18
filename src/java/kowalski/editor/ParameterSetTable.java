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
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import kowalski.tools.data.xml.MixBusParameters;
import kowalski.tools.data.xml.MixPreset;

class ParameterSetTableCellRenderer extends DefaultTableCellRenderer
{

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column)
    {
        JLabel l = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        l.setIcon(null);
        if (column == 0)
        {
            l.setIcon(IconManager.getImageIcon("mixbusicon.png"));
        }
        return l;
    }
}

class ParameterSetTableModel extends AbstractTableModel implements TableModelListener
{
    private MixPreset preset;

    static final String[] COLUMN_NAMES = {"Mix bus", "Gain left", "Gain right", "Pitch"};
    ParameterSetTableModel(MixPreset p)
    {
        preset = p;

        addTableModelListener(this);
    }

    @Override
    public int getRowCount()
    {
        return preset.getMixBusParameterList().size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMN_NAMES.length;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return columnIndex > 0;
    }

    @Override
    public String getColumnName(int column)
    {
        return COLUMN_NAMES[column];
    }

    @Override
    public void setValueAt(Object value, int row, int col) 
    {
        if (col == 0)
        {
            return;
        }

        float val = 0;
        try
        {
            val = Float.parseFloat(value.toString());
        }
        catch (NumberFormatException e)
        {
            return;
        }

        if (val < 0)
        {
            return;
        }

        MixBusParameters p = preset.getMixBusParameterList().get(row);
        if (col == 1)
        {
            p.setLeftGain(val);
        }
        else if (col == 2)
        {
            p.setRightGain(val);
        }
        else if (col == 3)
        {
            p.setPitch(val);
        }
        
        fireTableCellUpdated(row, col);

    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        MixBusParameters params = preset.getMixBusParameterList().get(rowIndex);
        if (columnIndex == 0)
        {
            return params.getMixBus();
        }
        else if (columnIndex == 1)
        {
            return params.getLeftGain();
        }
        else if (columnIndex == 2)
        {
            return params.getRightGain();
        }
        else if (columnIndex == 3)
        {
            return params.getPitch();
        }

        return null;
    }

    public void tableChanged(TableModelEvent e)
    {
        System.out.println(e);
    }

}

/**
 * 
 */
class ParameterSetTable extends JScrollPane
{
    private JTable table;

    ParameterSetTable(MixPreset p)
    {
        table = new JTable(new ParameterSetTableModel(p));
        table.getColumnModel().getColumn(0).setCellRenderer(new ParameterSetTableCellRenderer());
        setViewportView(table);
    }

}
