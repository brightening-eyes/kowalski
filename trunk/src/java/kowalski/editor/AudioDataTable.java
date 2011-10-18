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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import kowalski.tools.data.AudioFileDescription;
import kowalski.tools.data.ProjectDataException;
import kowalski.tools.data.ProjectDataUtils;
import kowalski.editor.AudioFileDataBase;
import kowalski.util.IconManager;
import kowalski.editor.KowalskiEditorFrame;
import kowalski.editor.MainTransferHandler;
import kowalski.editor.actions.AbstractProjectDataAction;
import kowalski.editor.actions.AbstractProjectDataAction;
import kowalski.editor.actions.AddAudioDataAction;
import kowalski.editor.actions.AddAudioDataAction;
import kowalski.editor.actions.ChangeAudioDataParameterAction;
import kowalski.editor.actions.ChangeAudioDataParameterAction;
import kowalski.editor.actions.RemoveAudioDataAction;
import kowalski.editor.actions.RemoveAudioDataAction;
import kowalski.tools.data.xml.AudioData;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.WaveBank;

/*
 // Install the custom editor on the first column
int vColIndex = 0;
TableColumn col = table.getColumnModel().getColumn(vColIndex);
col.setCellEditor(new MyTableCellEditor());

public class MyTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    // This is the component that will handle the editing of the cell value
    JComponent component = new JTextField();

    // This method is called when a cell value is edited by the user.
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int rowIndex, int vColIndex) {
        // 'value' is value contained in the cell located at (rowIndex, vColIndex)

        if (isSelected) {
            // cell (and perhaps other cells) are selected
        }

        // Configure the component with the specified value
        ((JTextField)component).setText((String)value);

        // Return the configured component
        return component;
    }

    // This method is called when editing is completed.
    // It must return the new value to be stored in the cell.
    public Object getCellEditorValue() {
        return ((JTextField)component).getText();
    }
}
 */

class AudioDataTableCellEditor extends AbstractCellEditor implements TableCellEditor
{
    private JCheckBox checkBox = new JCheckBox();

    AudioDataTableCellEditor()
    {
        checkBox.setBackground(null);
        checkBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                stopCellEditing();

            }
        });
    }


    public Object getCellEditorValue()
    {
        return checkBox.isSelected();
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        checkBox.setSelected((Boolean)value);
        return checkBox;
    }
}

class AudioDataTableCellRenderer extends DefaultTableCellRenderer
{
    private JLabel redBgLabel = new JLabel();
    private JCheckBox checkBox = new JCheckBox();
    
    AudioDataTableCellRenderer()
    {
        checkBox.setOpaque(false);
        checkBox.setBackground(null);
        
        redBgLabel.setOpaque(true);
        redBgLabel.setBackground(Color.RED);
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column)
    {
        JLabel l = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        AudioData ad = (AudioData)table.getValueAt(row, 0);
        l.setBorder(null);
        setIcon(null);
        if (column == 0)
        {
            File f = null;
            try
            {
                f = ProjectDataUtils.getAudioDataFile(KowalskiEditorFrame.getInstance().getCurrentProject(),
                                                      KowalskiEditorFrame.getInstance().getCurrentProjectDir(),
                                                      ad);
            }
            catch (ProjectDataException e)
            {

            }
            
            AudioFileDescription d = f == null ? null : AudioFileDataBase.getAudioFileDescription(f);
            if (f == null)
            {
                l = redBgLabel;
                l.setIcon(IconManager.getImageIcon("invalidwaveicon.png"));
            }
            else if (d == null)
            {
                l.setIcon(IconManager.getImageIcon("projecticon.png"));
            }
            else if (d.getEncoding().equals(AudioFileDescription.Encoding.PCM))
            {
                l.setIcon(IconManager.getImageIcon("waveicon.png"));
            }
            else if (d.getEncoding().equals(AudioFileDescription.Encoding.Vorbis))
            {
                l.setIcon(IconManager.getImageIcon("oggvorbisicon.png"));
            }
            else if (d.getEncoding().equals(AudioFileDescription.Encoding.IMAADPCM))
            {
                l.setIcon(IconManager.getImageIcon("waveiconcompressed.png"));
            }
            else if (d.getEncoding().equals(AudioFileDescription.Encoding.AAC))
            {
                l.setIcon(IconManager.getImageIcon("apple.png"));
            }
            else
            {
                l.setIcon(IconManager.getImageIcon("projecticon.png"));
            }

            l.setText(ad.getRelativePath());
        }
        else if (column == 6)
        {

            if (((AudioDataTableModel)(table.getModel())).isStreamable(ad))
            {
                checkBox.setEnabled(true);
                checkBox.setSelected(ad.isStreamFromDisk());
                return checkBox;
            }
            else
            {
                checkBox.setEnabled(false);
                checkBox.setSelected(false);
                l.setText("");
                return l;
            }
        }
        else
        {
            l.setText(value.toString());
        }

        return l;
    }
}

class AudioDataTableModel extends AbstractTableModel
{
    private List<AudioData> items;
    static final String[] COLUMN_NAMES = {"File name", "Encoding", "Channels", "Rate",
                        "Bits/sample", "File size", "Stream"};
    AudioDataTableModel(List<AudioData> i)
    {
        items = new ArrayList<AudioData>(i);
    }

    public int getRowCount()
    {
        return items.size();
    }

    public int getColumnCount()
    {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column)
    {
        return COLUMN_NAMES[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return columnIndex == 6 && isStreamable((AudioData)getValueAt(rowIndex, 0));
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex)
    {
        if (columnIndex == 6)
        {
            AudioData ad = (AudioData)getValueAt(rowIndex, 0);
            KowalskiEditorFrame.getInstance().pushAndPerformAction(
                    new ChangeAudioDataParameterAction(KowalskiEditorFrame.getInstance().getCurrentProject(), ad, ChangeAudioDataParameterAction.STREAM_FROM_DISK, value));
            //ad.setStreamFromDisk((Boolean)value);
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) 
    {
        AudioData d = (AudioData)items.get(rowIndex);
        File f = null;
        try
        {
            f = ProjectDataUtils.getAudioDataFile(KowalskiEditorFrame.getInstance().getCurrentProject(),
                                                  KowalskiEditorFrame.getInstance().getCurrentProjectDir(),
                                                  d);
        }
        catch (ProjectDataException e)
        {
            
        }

        AudioFileDescription afd = f == null ? null : AudioFileDataBase.getAudioFileDescription(f);


        if (columnIndex == 0)
        {
            return d;
        }
        else if (columnIndex == 1)
        {
            return afd == null ? "-" : afd.getEncoding();
        }
        else if (columnIndex == 2)
        {
            return afd == null ? "-" : afd.getNumChannels();
        }
        else if (columnIndex == 3)
        {
            return afd == null ? "-" : afd.getSampleRate();
        }
        else if (columnIndex == 4)
        {
            if (afd == null)
            {
                return "-";
            }
            if (afd.getEncoding().equals(AudioFileDescription.Encoding.PCM))
            {
                return afd.getBitsPerSample();
            }
            else
            {
                return "-";
            }
        }
        else if (columnIndex == 5)
        {
            return f == null ? "-" : f.length();
        }
        else if (columnIndex == 6)
        {
            return d.isStreamFromDisk();
        }
        else
        {
            throw new RuntimeException();
        }
    }

    boolean isStreamable(AudioData ad)
    {
        try
        {
            File f = ProjectDataUtils.getAudioDataFile(KowalskiEditorFrame.getInstance().getCurrentProject(),
                                                       KowalskiEditorFrame.getInstance().getCurrentProjectDir(),
                                                       ad);
            AudioFileDescription afd = AudioFileDataBase.getAudioFileDescription(f);
            if (afd == null)
            {
                return true;
            }
            return !afd.getEncoding().equals(AudioFileDescription.Encoding.PCM);
        }
        catch (ProjectDataException e)
        {
            return false;
        }
    }

}


/**
 *
 */
class AudioDataTable extends JScrollPane implements KeyListener
{
    private WaveBank waveBank;
    private JTable table;

    AudioDataTable(WaveBank w)
    {
        if (w == null)
        {
            throw new RuntimeException();
        }
        waveBank = w;
        table = new JTable();
        init(w.getAudioDataList());
        table.setDragEnabled(true);
        setTransferHandler(new MainTransferHandler());
        setViewportView(table);
        table.addKeyListener(this);
        //setFocusable(true);
    }

    void onFilesDropped(List<File> files)
    {
        KowalskiProject project = KowalskiEditorFrame.getInstance().getCurrentProject();
        AbstractProjectDataAction action = new AddAudioDataAction(project, waveBank, files);
        KowalskiEditorFrame.getInstance().pushAndPerformAction(action);
    }
    

    private void init(List<AudioData> items)
    {
        table.setModel(new AudioDataTableModel(items));
        table.setRowHeight(IconManager.getImageIcon("waveicon.png").getIconHeight() + 2);
        
        table.getColumnModel().getColumn(0).setCellRenderer(new AudioDataTableCellRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new AudioDataTableCellRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new AudioDataTableCellEditor());
        table.getColumnModel().getColumn(0).setPreferredWidth(250);
    }

    public void keyTyped(KeyEvent e)
    {
        //
    }

    public void keyPressed(KeyEvent e)
    {
        System.out.println(e);
        //TODO: detect command-backspace on macs
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE ||
            e.getKeyCode() == KeyEvent.VK_DELETE)
        {
            int[] selectedRows = table.getSelectedRows();
            List<AudioData> selectedItems = new ArrayList<AudioData>();
            for (int idx : selectedRows)
            {
                selectedItems.add((AudioData)table.getModel().getValueAt(idx, 0));
            }
            AbstractProjectDataAction action =
                    new RemoveAudioDataAction(KowalskiEditorFrame.getInstance().getCurrentProject(),
                            waveBank, selectedItems);
            KowalskiEditorFrame.getInstance().pushAndPerformAction(action);
        }
    }

    public void keyReleased(KeyEvent e)
    {
        //
    }
}

