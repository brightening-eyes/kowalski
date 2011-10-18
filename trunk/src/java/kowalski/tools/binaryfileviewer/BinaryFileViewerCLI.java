package kowalski.tools.binaryfileviewer;

import java.io.File;
import java.io.IOException;
import javax.swing.JFrame;

/**
 * A command line tool for viewing wave bank and engine data binaries.
 */
public class BinaryFileViewerCLI
{
    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("Excpected 1 argument (the file to view), got " + args.length);
            return;
        }

        File fileToViev = new File(args[0]);
        if (!fileToViev.exists())
        {
            System.out.println("The file " + args[0] + " does not exist.");
            return;
        }
        else if (!fileToViev.exists())
        {
            System.out.println(args[0] + " is a directory and not a file as expected.");
            return;
        }

        try
        {
            JFrame.setDefaultLookAndFeelDecorated(true);
            BinaryFileViewerFrame frame = new BinaryFileViewerFrame(fileToViev);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }
        catch (IOException e)
        {
            System.out.println("Error viewing " + fileToViev);
            e.printStackTrace();
        }
    }
}
