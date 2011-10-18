package kowalski.tools.data;

import java.io.IOException;
import javax.xml.bind.JAXBException;
import junit.framework.TestCase;

/**
 *
 */
public class EngineDataBinarySerializerTest extends TestCase
{
    public void testSerializeProjectWithNoEvents()
    {
        EngineDataBuilder builder = new EngineDataBuilder();
        try
        {
            builder.buildEngineData("kowalski/tools/data/minimal_valid_project.xml", "");
        }
        catch (ProjectDataException e)
        {
            //the expected outcome
        }
        catch (JAXBException e)
        {
            fail();
        }
        catch (IOException e)
        {
            fail();
        }
        fail();
    }

    public void testSerializeProjectWithNoWaveBanks()
    {
        //TODO
        fail();
    }

    public void testSerializeProjectWithNoAudioData()
    {
        //TODO
        fail();
    }
}
