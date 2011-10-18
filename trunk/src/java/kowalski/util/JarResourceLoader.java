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

package kowalski.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class that loads the native Kowalski Engine library, if available,
 * from the classpath.
 */
public class JarResourceLoader
{
    /** An array of library file names to try to load.*/
    private static String[] libFileNames =
    {
        "kowalski_java.jnilib", //OSX
        //TODO: add library names for more platforms here
    };

    /**
     * 
     * @param fallbackDir
     * @return
     */
    public static boolean load(String fallbackDir)
    {
        if (!load())
        {

            for (int i = 0; i < libFileNames.length; i++)
            {
                String libFileName = libFileNames[i];
                File file = new File(fallbackDir, libFileName);
                try
                {
                    System.load(file.getAbsolutePath());
                    return true;
                }
                catch (Exception e)
                {

                }
            }

            return false;
        }

        return true;
    }

    /**
     * Tries to load the Kowalski Engine native library for the current platform.
     * @return True if the library was successfully loaded, false otherwise.
     */
    public static boolean load()
    {
        //loop over the candidate lib file names and try to load each.
        //stop if successful of if all loads failed.
        for (int i = 0; i < libFileNames.length; i++)
        {
            String libFileName = libFileNames[i];

            File tempFile = null;
            try
            {
                //try to read the current lib file into an output stream.
                tempFile = writeResourceInJarToTempFile(libFileName);

                //try to load the lib from the temp file...
                System.load(tempFile.getAbsolutePath());
                //...and delete the file
                tempFile.delete();
                return true;
            }
            catch (Exception e)
            {
                //clean up the temp file if it was created
                if (tempFile != null && tempFile.exists())
                {
                    tempFile.delete();
                }
            }
        }

        return false;
    }

    public static File writeResourceInJarToTempFile(String pathInJar)
    {
        InputStream loadedLibStream = ClassLoader.getSystemResourceAsStream(pathInJar);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] tempBytes = new byte[100000];
        try
        {
            int bytesRead = loadedLibStream.read(tempBytes);
            while (bytesRead >= 0)
            {
                baos.write(tempBytes, 0, bytesRead);
                bytesRead = loadedLibStream.read(tempBytes);
            }
            loadedLibStream.close();
        
            //create a temporary file to load.
            File tempFile = File.createTempFile("kowalski_temp_file", ".tmp");
            FileOutputStream tempFileStream = new FileOutputStream(tempFile);
            tempFileStream.write(baos.toByteArray());
            tempFileStream.close();

            return tempFile;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
