package org.openlumify.core.cmdline;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLOntology;
import uk.ac.manchester.cs.owl.owlapi.OWLDatatypeImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OwlToJavaTest {
    @Mock
    private OWLOntology o;

    @Mock
    private OWLDataProperty dataProperty;

    private OwlToJava owlToJava;
    private OWLDatatype dataPropertyRange;
    private String dataPropertyDisplayType;
    private List<String> extendedDataTableNames;
    private List<String> dataPropertyDomains;

    @Before
    public void before() {
        owlToJava = new OwlToJava() {

            @Override
            protected OWLDatatype getDataPropertyRange(OWLOntology o, OWLDataProperty dataProperty) {
                return dataPropertyRange;
            }

            @Override
            protected List<String> getExtendedDataTableNames(OWLOntology o, OWLDataProperty dataProperty) {
                return extendedDataTableNames;
            }

            @Override
            protected String getDataPropertyDisplayType(OWLOntology o, OWLDataProperty dataProperty) {
                return dataPropertyDisplayType;
            }

            @Override
            protected List<String> getDataPropertyDomains(OWLOntology o, OWLDataProperty dataProperty) {
                return dataPropertyDomains;
            }
        };
        dataPropertyDisplayType = null;
        dataPropertyRange = null;
        extendedDataTableNames = new ArrayList<>();
        dataPropertyDomains = new ArrayList<>();
    }

    @Test
    public void exportDataProperty_string() throws Exception {
        String typeIri = "http://www.w3.org/2001/XMLSchema#string";
        String expectedType = "StringOpenLumifyProperty";
        dataPropertyDomains.add("http://openlumify.org#testClass");
        testExportDataProperty(typeIri, expectedType);
    }

    @Test
    public void exportDataProperty_string_longtext() throws Exception {
        String typeIri = "http://www.w3.org/2001/XMLSchema#string";
        String expectedType = "StreamingOpenLumifyProperty";
        dataPropertyDisplayType = "longtext";
        dataPropertyDomains.add("http://openlumify.org#testClass");
        testExportDataProperty(typeIri, expectedType);
    }

    @Test
    public void exportDataProperty_directoryEntry() throws Exception {
        String typeIri = "http://openlumify.org#directory/entity";
        String expectedType = "DirectoryEntityOpenLumifyProperty";
        dataPropertyDomains.add("http://openlumify.org#testClass");
        testExportDataProperty(typeIri, expectedType);
    }

    @Test
    public void exportDataProperty_hexBinary() throws Exception {
        String typeIri = "http://www.w3.org/2001/XMLSchema#hexBinary";
        String expectedType = "StreamingOpenLumifyProperty";
        dataPropertyDomains.add("http://openlumify.org#testClass");
        testExportDataProperty(typeIri, expectedType);
    }

    private void testExportDataProperty(String typeIri, String expectedType) {
        SortedMap<String, String> sortedValues = new TreeMap<>();
        SortedMap<String, String> sortedIntents = new TreeMap<>();
        IRI documentIri = IRI.create("http://openlumify.org/test");
        when(dataProperty.getIRI()).thenReturn(IRI.create("http://openlumify.org/test#prop"));

        dataPropertyRange = new OWLDatatypeImpl(IRI.create(typeIri));
        owlToJava.exportDataProperty(sortedValues, sortedIntents, documentIri, o, dataProperty);
        assertEquals(1, sortedValues.size());
        assertEquals("public static final " + expectedType + " PROP = new " + expectedType + "(\"http://openlumify.org/test#prop\");", sortedValues.get("PROP").trim());
    }

    @Test
    public void exportExtendedDataProperty() {
        SortedMap<String, String> sortedValues = new TreeMap<>();
        SortedMap<String, String> sortedIntents = new TreeMap<>();
        IRI documentIri = IRI.create("http://openlumify.org/test");
        when(dataProperty.getIRI()).thenReturn(IRI.create("http://openlumify.org/test#prop"));

        dataPropertyRange = new OWLDatatypeImpl(IRI.create("http://www.w3.org/2001/XMLSchema#string"));
        extendedDataTableNames.add("http://openlumify.org/test#table1");
        dataPropertyDomains.add("http://openlumify.org#testClass");
        owlToJava.exportDataProperty(sortedValues, sortedIntents, documentIri, o, dataProperty);
        assertEquals(2, sortedValues.size());
        assertEquals("public static final StringOpenLumifyProperty PROP = new StringOpenLumifyProperty(\"http://openlumify.org/test#prop\");", sortedValues.get("PROP").trim());
        assertEquals("public static final StringOpenLumifyExtendedData TABLE1_PROP = new StringOpenLumifyExtendedData(\"http://openlumify.org/test#table1\", \"http://openlumify.org/test#prop\");", sortedValues.get("TABLE1_PROP").trim());
    }
}
