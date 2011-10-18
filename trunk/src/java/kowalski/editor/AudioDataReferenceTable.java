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

import java.util.ArrayList;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import kowalski.editor.MainTransferHandler;
import kowalski.tools.data.xml.AudioDataReference;
import kowalski.tools.data.xml.Sound;


class AudioDataReferenceTableModel extends AbstractTableModel implements TableModelListener
{
    private Sound sound;
    static final String[] COLUMN_NAMES = {"Audio file", "Wave bank", "Probability weight"};
    AudioDataReferenceTableModel(Sound s)
    {
        sound = s;
        addTableModelListener(this);
    }

    @Override
    public int getRowCount()
    {
        if (sound == null)
        {
            return 0;
        }
        return sound.getAudioDataReferences().size();
    }
    
    @Override
    public int getColumnCount()
    {
        if (sound == null)
        {
            return 0;
        }
        return COLUMN_NAMES.length;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return columnIndex == COLUMN_NAMES.length - 1;
    }

    @Override
    public String getColumnName(int column)
    {
        return COLUMN_NAMES[column];
    }

    @Override
    public void setValueAt(Object value, int row, int col) {

        if (col == COLUMN_NAMES.length - 1)
        {
            float newWeight = 0;
            try
            {
                newWeight = Float.parseFloat(value.toString());
            }
            catch (NumberFormatException e)
            {
                return;
            }

            if (newWeight < 0)
            {
                return;
            }
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        AudioDataReference afd = sound.getAudioDataReferences().get(rowIndex);
        if (columnIndex == 0)
        {
            return afd.getRelativePath();
        }
        else if (columnIndex == 1)
        {
            return afd.getWaveBank();
        }
        else if (columnIndex == 2)
        {
            return afd.getProbabilityWeight();
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
class AudioDataReferenceTable extends JScrollPane
{
    private JTable table;

    AudioDataReferenceTable(Sound s)
    {
        table = new JTable(new AudioDataReferenceTableModel(s));
        table.setDragEnabled(true);
        table.setTransferHandler(new MainTransferHandler());
        setViewportView(table);
    }
}
