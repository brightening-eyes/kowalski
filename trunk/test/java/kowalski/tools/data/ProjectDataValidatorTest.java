package kowalski.tools.data;

import java.io.InputStream;
import javax.xml.bind.JAXBException;
import kowalski.tools.data.xml.KowalskiProject;

/**
 * Tests the ProjectDataVerifier class by throwing various malformed and well formed
 * data at it.
 */
public class ProjectDataValidatorTest extends TestCaseBase
{
    public void testDuplicateMixBusIDs()
    {
        String xmlFile = "duplicate_mix_bus_ids.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);
    }

    public void testDuplicateSoundGroupIDs()
    {
        String xmlFile = "duplicate_sound_group_ids_1.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);

        xmlFile = "duplicate_sound_group_ids_2.xml";
        project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);
    }

    public void testDuplicateSoundIDs()
    {
        String xmlFile = "duplicate_sound_ids_1.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);

        xmlFile = "duplicate_sound_ids_2.xml";
        project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);
    }

    public void testDuplicateEventGroupIDs()
    {
        String xmlFile = "duplicate_event_group_ids_1.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);

        xmlFile = "duplicate_event_group_ids_2.xml";
        project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);
    }

    public void testDuplicateEventIDs()
    {
        String xmlFile = "duplicate_event_ids_1.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);

        xmlFile = "duplicate_event_ids_2.xml";
        project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);
    }

    public void testInvalidAudioDataReference()
    {
        String xmlFile = "invalid_audio_data_reference.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);
    }

    public void testInvalidMixBusReference()
    {
        String xmlFile = "invalid_mix_bus_reference.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);
    }

    public void testInvalidSoundReference()
    {
        String xmlFile = "invalid_sound_reference.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);
    }

    public void testMinimalValidProject()
    {
        String xmlFile = "minimal_valid_project.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationIsSuccessful(xmlFile, project);
    }

    public void testMissingDefaultMixPreset()
    {
        String xmlFile = "missing_default_mix_preset.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);
    }

    public void testMultipleDefaultMixPresets()
    {
        String xmlFile = "multiple_default_mix_presets.xml";
        KowalskiProject project = loadProjectDataAndRequireNoJAXBException(xmlFile);
        verifyValidationThrowsProjectDataExpection(xmlFile, project);
    }
}
