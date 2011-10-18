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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import kowalski.tools.data.xml.KowalskiProject;
import kowalski.tools.data.xml.NodeWithIDAndComments;
import kowalski.tools.data.xml.ObjectFactory;
import org.xml.sax.SAXException;

/**
 * This class provides a convenient wrapper around the JAXB XML processing
 * functionality needed for Kowalski project data validation, serialization and deserialization.
 * Due to the overhead associated with creating the underlying JAXB objects,
 * there is only one instance of this class which is accessed through the
 * static getInstance() method.
 */
public class ProjectDataXMLSerializer
{
    private Unmarshaller projectXMLUnmarshaller;
    private Marshaller projectXMLMarshaller;

    private static final String SCHEMA_PACKAGE =
            "kowalski.tools.data.xml";
    private static final String KOWALSKI_PROJECT_SCHEMA =
            "kowalski1.0.xsd";
    private OutputStream validationOutputStream = new ByteArrayOutputStream();//System.out;

    private static ProjectDataXMLSerializer instance;

    public static ProjectDataXMLSerializer getInstance()
    {
        if (instance == null)
        {
            instance = new ProjectDataXMLSerializer();
        }

        return instance;
    }

    private ProjectDataXMLSerializer()
    {
        //create a jaxb context
        JAXBContext jaxbContext = null;
        try
        {
            ClassLoader cl = ObjectFactory.class.getClassLoader();
            jaxbContext = JAXBContext.newInstance(SCHEMA_PACKAGE, cl);
        }
        catch (JAXBException e)
        {
            e.printStackTrace();
            throw new RuntimeException("unable to create jaxb context for " +
                    SCHEMA_PACKAGE + ":\n" +
                    e.getMessage());
        }

        //create schemas
        SchemaFactory schemaFactory = SchemaFactory.newInstance(
            XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema kowalskiProjectSchema = null;
        {
            Source kowalskiSchemaSource = new StreamSource(
                ObjectFactory.class.getResourceAsStream(KOWALSKI_PROJECT_SCHEMA));
            try
            {
                kowalskiProjectSchema = schemaFactory.newSchema(kowalskiSchemaSource);
            }
            catch (SAXException e)
            {
                e.printStackTrace();
                throw new RuntimeException("unable to load schema " +
                                KOWALSKI_PROJECT_SCHEMA);
            }
        }

        //finally create unmarshallers and marshallers
        try
        {
            projectXMLUnmarshaller =
                jaxbContext.createUnmarshaller();
        }
        catch (Exception e)
        {
            throw new RuntimeException();
        }
        projectXMLUnmarshaller.setSchema(kowalskiProjectSchema);

        try
        {
            projectXMLMarshaller =
                jaxbContext.createMarshaller();
            projectXMLMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        }
        catch (Exception e)
        {
            throw new RuntimeException();
        }
        projectXMLMarshaller.setSchema(kowalskiProjectSchema);
    }

    public KowalskiProject deserializeKowalskiProject(String projectPath)
        throws JAXBException, IOException
    {
        FileInputStream fis = new FileInputStream(projectPath);
        return deserializeKowalskiProject(fis);
    }

    /**
     *
     * @param stream
     * @return
     */
    public KowalskiProject deserializeKowalskiProject(InputStream stream)
        throws JAXBException
    {
        //unmarshal the project
        KowalskiProject project =
                (KowalskiProject) projectXMLUnmarshaller.unmarshal(stream);

        //sort nodes with IDs
        ProjectDataUtils.sortNodesWithID(project);
        ProjectDataUtils.sortAudioDataNodes(project);


        return project;
    }

    /**
     * 
     * @param g
     * @param f
     */
    public void serializeKowalskiProject(KowalskiProject p, File f)
            throws JAXBException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("File is null");
        }
        if (p == null)
        {
            throw new IllegalArgumentException("Project is null");
        }
        projectXMLMarshaller.marshal(p, f);
    }

    /**
     *
     * @param g
     * @param f
     */
    public void serializeKowalskiProject(KowalskiProject p, OutputStream stream)
            throws JAXBException
    {
        projectXMLMarshaller.marshal(p, stream);
    }

    /**
     *
     * @param g
     * @param f
     */
    public void validateKowalskiProject(KowalskiProject p)
            throws JAXBException
    {
        projectXMLMarshaller.marshal(p, validationOutputStream);
    }
    
    /**
     * Validate event
     * @param p
     * @throws JAXBException
     */
    public void validateNodeWithIDAndComments(NodeWithIDAndComments e)
            throws JAXBException
    {
        projectXMLMarshaller.marshal(new JAXBElement<NodeWithIDAndComments>(new QName("", "rootTag"),
                NodeWithIDAndComments.class, e), validationOutputStream);
    }
}
