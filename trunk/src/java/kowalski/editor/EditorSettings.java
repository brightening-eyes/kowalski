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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 */
public class EditorSettings
{
    private static final String PROPERTIES_FILE_NAME = "kowalskieditor.properties";

    EditorSettings()
    {   
        
    }
    
    String[] getRecentFileList()
    {
        return null;
    }

    void save()
    {
        
    }

    void addRecentFile(String file)
    {
        
    }

    private File getSettingsFile()
    {
        return new File(System.getProperty("user.home"), PROPERTIES_FILE_NAME);
    }

    private Properties loadSettings()
    {
        Properties p = new Properties();
        File f = getSettingsFile();

        if (!f.exists())
        {
            saveSettings(p);
        }

        //load settings
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(f);
            p.loadFromXML(fis);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return p;
    }

    private void saveSettings(Properties p)
    {
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(getSettingsFile());
            p.storeToXML(fos, "Kowalski Editor settings");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
    }

    private void setProperty(String key, String value)
    {   
        Properties p = loadSettings();
        p.setProperty(key, value);
        saveSettings(p);
        
    }
    
}
