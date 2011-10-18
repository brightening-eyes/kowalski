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

import java.io.IOException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.bind.JAXBException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * 
 */
public class KowalskiBinaryBuilderAntTask extends Task implements AbstractLogger
{
    private String waveBankDir;
    private String engineDataFile;
    private String projectFile;

    @Override
    public void execute()
            throws BuildException
    {
        if (projectFile == null)
        {
            throw new BuildException("The projectFile attribute must be set.");
        }
        else if (engineDataFile == null && waveBankDir == null)
        {
            throw new BuildException("At least one of the attributes waveBankdDir " +
                                     "engineDataFile must be set.");
        }

        if (engineDataFile != null)
        {
            EngineDataBuilder edb = new EngineDataBuilder();
            edb.setLogger(this);
            try
            {
                edb.buildEngineData(projectFile, engineDataFile);
            }
            catch (IOException e)
            {
                throw new BuildException("Error building engine data", e);
            }
            catch (JAXBException e)
            {
                throw new BuildException("Error building engine data", e);
            }
            catch (ProjectDataException e)
            {
                throw new BuildException("Error building engine data", e);
            }
        }
        if (waveBankDir != null)
        {
            WaveBankBuilder wbb = new WaveBankBuilder();
            wbb.setLogger(this);
            try
            {
                wbb.buildWavebanks(projectFile, waveBankDir);
            }
            catch (IOException e)
            {
                throw new BuildException("Error building wave bank data", e);
            }
            catch (JAXBException e)
            {
                throw new BuildException("Error building wave bank data", e);
            }
            catch (ProjectDataException e)
            {
                throw new BuildException("Error building wave bank data", e);
            }
            catch (UnsupportedAudioFileException e)
            {
                throw new BuildException("Error building wave bank data", e);
            }
        }
    }

    public void setProjectFile(String file)
    {
        projectFile = file;
    }

    public void setVerbose(String verbose)
    {

    }

    public void setEngineDataFile(String file)
    {
        engineDataFile = file;
    }

    public void setWavebankDir(String dir)
    {
        waveBankDir = dir;
    }

    public void logWarning(String message)
    {
        log("WARNING: " + message);
    }

    public void logError(String message)
    {
        log("ERROR: " + message);
    }

    public void logMessage(String message)
    {
        log(message);
    }
}
