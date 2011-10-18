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

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.xml.bind.JAXBException;
import kowalski.tools.data.ProjectDataException;

/**
 *
 */
class ErrorDialog extends JPanel
{
    private JTextPane detailedTextArea = new JTextPane();
    private JTextPane errorMessage = new JTextPane();
    private Dimension tabbedPaneSize = new Dimension(450, 230);

    ErrorDialog()
    {
        setLayout(new BorderLayout());

        detailedTextArea.setBackground(null);
        detailedTextArea.setEditable(false);
        detailedTextArea.setBorder(null);
        detailedTextArea.setOpaque(false);

        errorMessage.setBackground(null);
        errorMessage.setEditable(false);
        errorMessage.setBorder(null);
        errorMessage.setOpaque(false);

        //TODO: bg???
        JTabbedPane tp = new JTabbedPane();

        JScrollPane errorSp = new JScrollPane(errorMessage);
        errorSp.setOpaque(false);
        errorSp.setBackground(null);
        errorSp.setBorder(null);
        errorSp.getViewport().setOpaque(false);
        errorSp.getViewport().setBackground(null);

        JScrollPane detailsSp = new JScrollPane(detailedTextArea);
        detailsSp.setOpaque(false);
        detailsSp.setBackground(null);
        detailsSp.setBorder(null);
        detailsSp.getViewport().setOpaque(false);
        detailsSp.getViewport().setBackground(null);

        tp.add("Message", errorSp);
        tp.add("Details", detailsSp);
        add(tp, BorderLayout.CENTER);
        tp.setPreferredSize(tabbedPaneSize);
        errorMessage.setEditable(false);
        errorMessage.setOpaque(false);
    }

    void setException(Exception e)
    {
        String message = e.getMessage();

        if (message == null &&
            e instanceof JAXBException)
        {
            JAXBException je = (JAXBException)e;
            message = je.toString() + "\n" + je.getCause().toString();
            /*
            Throwable le = je.getLinkedException();
            while (le != null)
            {
                message = le.getMessage() + "\n";
                je.
                if (le instanceof JAXBException)
                {
                    le = ((JAXBException)le).getLinkedException();
                }
                else
                {
                    le = null;
                }
            }*/
        }
        else if (e instanceof ProjectDataException)
        {
            ProjectDataException pde = (ProjectDataException)e;

            if (message != null && message.length() > 0)
            {
                message += "\n\n";
            }
            else
            {
                message = "";
            }
            for (int i = 0; i < pde.getMessages().size(); i++)
            {
                message += pde.getMessages().get(i) + "\n\n";
            }
        }
        errorMessage.setText(message);
        errorMessage.moveCaretPosition(0);

        String stackTrace = e.getClass().getName() + "\n\n";

        stackTrace += message + "\n\n";

        stackTrace += "Stacktrace:\n---------------------\n";
        for (StackTraceElement element : e.getStackTrace())
        {
            stackTrace += element + "\n";
        }
        detailedTextArea.setText(stackTrace);
        errorMessage.moveCaretPosition(0);



        repaint();

    }
}
