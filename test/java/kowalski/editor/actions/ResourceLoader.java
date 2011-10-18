package kowalski.editor.actions;

import java.io.InputStream;

/**
 *
 */
class ResourceLoader
{
    InputStream load(String fileName)
    {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("kowalski/editor/actions/" + fileName);
        if (stream == null)
        {
            System.out.println("ResourceLoader.loadProjectDataXML: Stream is null!");
            throw new RuntimeException();
        }
        
        return stream;
    }
}
