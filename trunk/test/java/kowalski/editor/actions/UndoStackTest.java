package kowalski.editor.actions;

import java.io.InputStream;
import junit.framework.TestCase;

/**
 * Tests the undo/redo actions by applying them to project data and
 * comparing the serialized xml result.
 */
public class UndoStackTest extends TestCase
{
    private ResourceLoader loader = new ResourceLoader();

    void testEventActions()
    {
        InputStream s = loader.load("minimal_valid_project.xml");
    }
}
