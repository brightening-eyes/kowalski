package kowalski.tools.data;

import java.io.InputStream;

/**
 *
 */
class ResourceLoader
{
    InputStream load(String fileName)
    {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("kowalski/tools/data/" + fileName);
        if (stream == null)
        {
            System.out.println("ResourceLoader.loadProjectDataXML: Stream is null! filename=" + fileName);
            throw new RuntimeException();
        }
        
        return stream;
    }
}
