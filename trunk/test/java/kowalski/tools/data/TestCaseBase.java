package kowalski.tools.data;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.InputStream;
import java.util.List;
import javax.xml.bind.JAXBException;
import junit.framework.TestCase;
import kowalski.tools.data.xml.KowalskiProject;

/**
 *
 */
class TestCaseBase extends TestCase
{
    protected ResourceLoader loader = new ResourceLoader();

    protected void verifyValidationThrowsProjectDataExpection(String xmlFile, KowalskiProject project)
    {
        try
        {
            ProjectDataValidator.validateProjectData(project);
            fail(xmlFile + " should not pass validation.");
        }
        catch (JAXBException e)
        {
            fail(xmlFile + " should pass xsd validation.\n" + e.getLinkedException().getMessage());
        }
        catch (ProjectDataException e)
        {
            //the expected outcome.
            System.out.println("ProjectDataException thrown on validation of " + xmlFile + " as expected:");
            List<String> messages = e.getMessages();

            for (int i = 0; i < messages.size(); i++)
            {
                System.out.println(messages.get(i));
            }
            System.out.println("");
        }
    }

    protected void verifyValidationIsSuccessful(String xmlFile, KowalskiProject project)
    {
        try
        {
            ProjectDataValidator.validateProjectData(project);
        }
        catch (JAXBException e)
        {
            fail(xmlFile + " should pass xsd validation.\n" + e.getLinkedException().getMessage());
        }
        catch (ProjectDataException e)
        {
            fail(xmlFile + " should not cause a ProjectDataException.");
        }
    }

    protected KowalskiProject loadProjectDataAndRequireNoJAXBException(String xmlFile)
    {
        InputStream stream = loader.load(xmlFile);

        if (stream == null)
        {
            fail(xmlFile + " not found");
        }

        KowalskiProject project = null;
        try
        {
            project = ProjectDataXMLSerializer.getInstance().deserializeKowalskiProject(stream);
        }
        catch (JAXBException e)
        {
            fail(xmlFile + " should pass xsd validation.\n" + e.getLinkedException().getMessage());
        }

        return project;
    }
}
