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

/**
 * A command line interface to create engine data and/or wave bank data from a project xml file.
 */
public class KowalskiBinaryBuilderCLI implements AbstractLogger
{
    /**
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        KowalskiBinaryBuilderCLI b = new KowalskiBinaryBuilderCLI();

        String projectPath = null;
        String outputEngineData = null;
        String waveBankDirectory = null;

        for (int i = 0; i < args.length - 1; i++)
        {
            String argi = args[i];

            //input project xml. required.
            if (argi.equals("-p"))
            {
                projectPath = args[i + 1];
            }
            //output engine binary file. this or -w required
            else if (argi.trim().equals("-o"))
            {
                outputEngineData = args[i + 1];
            }
            //output wave bank dir
            else if (argi.trim().equals("-w"))
            {
                waveBankDirectory = args[i + 1];
            }
        }

        if (projectPath == null)
        {
            b.logError("No Kowalski project data file was provided.");
            printUsage();
            return;
        }

        if (outputEngineData == null && waveBankDirectory == null)
        {
            b.logError("No wave bank directory or engine data binary location was provided.");
            printUsage();
            return;
        }

        if (projectPath != null)
        {
            EngineDataBuilder engineDataBuilder = new EngineDataBuilder();
            engineDataBuilder.setLogger(b);
            try
            {
                engineDataBuilder.buildEngineData(projectPath, outputEngineData);
            }
            catch (Exception e)
            {
                b.logError("Error writing engine data binary:");
                b.logError(e.toString());

                return;
            }
        }

        if (waveBankDirectory != null)
        {
            WaveBankBuilder wavebankBuilder = new WaveBankBuilder();
            wavebankBuilder.setLogger(b);
            try
            {
                wavebankBuilder.buildWavebanks(projectPath, waveBankDirectory);
            }
            catch (Exception e)
            {
                b.logError("Error writing wave bank binaries");
                b.logError(e.toString());
                return;
            }
            System.out.println("Wave bank binaries written to " + waveBankDirectory);
        }
    }

    private static void printUsage()
    {
        System.out.println("Usage:");
        System.out.println("-p Path to project XML data. Required.");
        System.out.println("-o Engine data output file. Required if -w is missing.");
        System.out.println("-w Output wave bank directory. Required if -o is missing.");
    }

    public void logWarning(String message)
    {
        System.out.println(message);
    }

    public void logError(String message) {
        System.out.println(message);
    }

    public void logMessage(String message)
    {
        System.out.println(message);
    }
}
