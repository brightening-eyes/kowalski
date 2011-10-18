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

package kowalski.tools.data;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import kowalski.tools.data.xml.KowalskiProject;

/**
 *
 */
public class ProjectDataValidatorCLI
{
    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("Expected 1 argument (the file to validate), got " + args.length);
            return;
        }

        File file = new File(args[0]);
        if (!file.exists())
        {
            System.out.println(file + " does not exist.");
            return;
        }
        if (file.isDirectory())
        {
            System.out.println(file + " is a directory and not a file as required.");
        }
        
        ProjectDataXMLSerializer s = ProjectDataXMLSerializer.getInstance();
        KowalskiProject project = null;
        
        try
        {
            project = s.deserializeKowalskiProject(file.getPath());
        }
        catch (IOException e)
        {
            printErrorMessage(e);
            return;
        }
        catch (JAXBException e)    
        {
            printErrorMessage(e);
            return;
        }

        try
        {
            ProjectDataValidator.validateProjectData(project);
        }
        catch (JAXBException e)
        {
            printErrorMessage(e);
            return;
        }
        catch (ProjectDataException e)
        {
            printErrorMessage(e);
            return;
        }

        System.out.println("No errors found.");

    }

    private static void printErrorMessage(Exception e)
    {
        System.out.println("Validation failed:");
        if (e instanceof JAXBException)
        {
            JAXBException je = (JAXBException)e;
            System.out.println(je.toString() + "\n" + je.getCause().toString());
        }
        else
        {
            System.out.println(e.toString());
        }
        System.out.println("");
        System.out.println("Stack trace:");
        e.printStackTrace();
    }
}
