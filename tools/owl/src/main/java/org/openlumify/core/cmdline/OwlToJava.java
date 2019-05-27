package org.openlumify.core.cmdline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.openlumify.core.cmdline.converters.IRIConverter;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.properties.types.*;
import org.openlumify.core.util.OWLOntologyUtil;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.vertexium.util.IterableUtils.toList;
import static org.openlumify.core.util.StreamUtil.stream;

@Parameters(commandDescription = "Create a Java class similar to OpenLumifyProperties for a specific IRI")
public class OwlToJava extends CommandLineTool {
    @Parameter(names = {"--iri", "-i"}, required = true, arity = 1, converter = IRIConverter.class, description = "The IRI of the ontology you would like exported")
    private IRI iri;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new OwlToJava(), args);
    }

    @Override
    protected int run() throws Exception {
        SortedMap<String, String> sortedIntents = new TreeMap<>();
        SortedMap<String, String> sortedValues = new TreeMap<>();

        OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
        OWLOntologyManager m = getOntologyRepository().createOwlOntologyManager(config, null);

        OWLOntology o = m.getOntology(iri);
        if (o == null) {
            System.err.println("Could not find ontology " + iri);
            return 1;
        }

        System.out.println("public class Ontology {");
        System.out.println("    public static final String IRI = \"" + iri + "\";");
        System.out.println("");

        sortedValues.clear();
        for (OWLObjectProperty objectProperty : o.getObjectPropertiesInSignature()) {
            if (!o.isDeclared(objectProperty, Imports.EXCLUDED)) {
                continue;
            }
            exportObjectProperty(sortedValues, sortedIntents, o, objectProperty);
        }
        writeValues(sortedValues);
        System.out.println();

        sortedValues.clear();
        for (OWLClass owlClass : o.getClassesInSignature()) {
            if (!o.isDeclared(owlClass, Imports.EXCLUDED)) {
                continue;
            }
            exportClass(sortedValues, sortedIntents, o, owlClass);
        }
        writeValues(sortedValues);
        System.out.println();

        sortedValues.clear();
        for (OWLDataProperty dataProperty : o.getDataPropertiesInSignature()) {
            if (!o.isDeclared(dataProperty, Imports.EXCLUDED)) {
                continue;
            }
            exportDataProperty(sortedValues, sortedIntents, iri, o, dataProperty);
        }
        writeValues(sortedValues);
        System.out.println();

        writeValues(sortedIntents);
        System.out.println();

        System.out.println("}");

        return 0;
    }

    private void writeValues(SortedMap<String, String> sortedValues) {
        for (Map.Entry<String, String> sortedValue : sortedValues.entrySet()) {
            System.out.println(sortedValue.getValue());
        }
    }

    private void exportObjectProperty(SortedMap<String, String> sortedValues, SortedMap<String, String> sortedIntents, OWLOntology o, OWLObjectProperty objectProperty) {
        String iri = objectProperty.getIRI().toString();
        String label = OWLOntologyUtil.getLabel(o, objectProperty);
        String javaConst = toJavaConst(label);

        addIntents(sortedIntents, OWLOntologyUtil.getIntents(o, objectProperty));

        sortedValues.put(javaConst, String.format("    public static final String EDGE_LABEL_%s = \"%s\";", javaConst, iri));
    }

    private void exportClass(SortedMap<String, String> sortedValues, SortedMap<String, String> sortedIntents, OWLOntology o, OWLClass owlClass) {
        String iri = owlClass.getIRI().toString();
        String label = OWLOntologyUtil.getLabel(o, owlClass);
        String javaConst = toJavaConst(label);

        addIntents(sortedIntents, OWLOntologyUtil.getIntents(o, owlClass));

        sortedValues.put(javaConst, String.format("    public static final String CONCEPT_TYPE_%s = \"%s\";", javaConst, iri));
    }

    void exportDataProperty(SortedMap<String, String> sortedValues, SortedMap<String, String> sortedIntents, IRI documentIri, OWLOntology o, OWLDataProperty dataProperty) {
        String iri = dataProperty.getIRI().toString();
        String javaConstName = iriToJavaConstName(documentIri, iri);
        OWLDatatype range = getDataPropertyRange(o, dataProperty);
        String rangeIri = range.getIRI().toString();

        addIntents(sortedIntents, OWLOntologyUtil.getIntents(o, dataProperty));

        List<String> extendedDataTableNames = getExtendedDataTableNames(o, dataProperty);
        if (extendedDataTableNames.size() > 0) {
            String type;

            if ("http://www.w3.org/2001/XMLSchema#double".equals(rangeIri)
                    || "http://www.w3.org/2001/XMLSchema#float".equals(rangeIri)) {
                type = DoubleOpenLumifyExtendedData.class.getSimpleName();
            } else if ("http://www.w3.org/2001/XMLSchema#int".equals(rangeIri)
                    || "http://www.w3.org/2001/XMLSchema#integer".equals(rangeIri)
                    || "http://www.w3.org/2001/XMLSchema#unsignedByte".equals(rangeIri)) {
                type = IntegerOpenLumifyExtendedData.class.getSimpleName();
            } else if ("http://openlumify.org#geolocation".equals(rangeIri)) {
                type = GeoPointOpenLumifyExtendedData.class.getSimpleName();
            } else if ("http://www.w3.org/2001/XMLSchema#dateTime".equals(rangeIri)) {
                type = DateOpenLumifyExtendedData.class.getSimpleName();
            } else if ("http://www.w3.org/2001/XMLSchema#date".equals(rangeIri)) {
                type = DateOpenLumifyExtendedData.class.getSimpleName();
            } else if ("http://www.w3.org/2001/XMLSchema#string".equals(rangeIri)) {
                type = StringOpenLumifyExtendedData.class.getSimpleName();
            } else if ("http://www.w3.org/2001/XMLSchema#boolean".equals(rangeIri)) {
                type = BooleanOpenLumifyExtendedData.class.getSimpleName();
            } else {
                throw new OpenLumifyException("Could not map range type " + rangeIri);
            }

            for (String extendedDataTableName : extendedDataTableNames) {
                String name = iriToJavaConstName(documentIri, extendedDataTableName) + "_" + javaConstName;
                sortedValues.put(name, String.format("    public static final %s %s = new %s(\"%s\", \"%s\");", type, name, type, extendedDataTableName, iri));
            }
        }

        if (getDataPropertyDomains(o, dataProperty).size() > 0 || getDataPropertyObjectDomains(o, dataProperty).size() > 0) {
            if ("http://openlumify.org#extendedDataTable".equals(rangeIri)) {
                sortedValues.put(javaConstName, String.format("    public static final String %s = \"%s\";", javaConstName, iri));
                return;
            }

            String type;
            if ("http://www.w3.org/2001/XMLSchema#double".equals(rangeIri)
                    || "http://www.w3.org/2001/XMLSchema#float".equals(rangeIri)) {
                type = DoubleOpenLumifyProperty.class.getSimpleName();
            } else if ("http://www.w3.org/2001/XMLSchema#int".equals(rangeIri)
                    || "http://www.w3.org/2001/XMLSchema#integer".equals(rangeIri)
                    || "http://www.w3.org/2001/XMLSchema#unsignedByte".equals(rangeIri)) {
                type = IntegerOpenLumifyProperty.class.getSimpleName();
            } else if ("http://www.w3.org/2001/XMLSchema#unsignedLong".equals(rangeIri)) {
                type = LongOpenLumifyProperty.class.getSimpleName();
            } else if ("http://openlumify.org#geolocation".equals(rangeIri)) {
                type = GeoPointOpenLumifyProperty.class.getSimpleName();
            } else if ("http://www.w3.org/2001/XMLSchema#string".equals(rangeIri)) {
                String displayType = getDataPropertyDisplayType(o, dataProperty);
                if ("longText".equalsIgnoreCase(displayType)) {
                    type = StreamingOpenLumifyProperty.class.getSimpleName();
                } else {
                    type = StringOpenLumifyProperty.class.getSimpleName();
                }
            } else if ("http://www.w3.org/2001/XMLSchema#boolean".equals(rangeIri)) {
                type = BooleanOpenLumifyProperty.class.getSimpleName();
            } else if ("http://openlumify.org#currency".equals(rangeIri)) {
                type = DoubleOpenLumifyProperty.class.getSimpleName(); // TODO should this be a CurrenyOpenLumifyProperties
            } else if ("http://www.w3.org/2001/XMLSchema#dateTime".equals(rangeIri)) {
                type = DateOpenLumifyProperty.class.getSimpleName();
            } else if ("http://www.w3.org/2001/XMLSchema#date".equals(rangeIri)) {
                type = DateOpenLumifyProperty.class.getSimpleName();
            } else if ("http://openlumify.org#directory/entity".equals(rangeIri)) {
                type = DirectoryEntityOpenLumifyProperty.class.getSimpleName();
            } else if ("http://www.w3.org/2001/XMLSchema#hexBinary".equals(rangeIri)) {
                type = StreamingOpenLumifyProperty.class.getSimpleName();
            } else {
                throw new OpenLumifyException("Could not map range type " + rangeIri);
            }

            sortedValues.put(javaConstName, String.format("    public static final %s %s = new %s(\"%s\");", type, javaConstName, type, iri));
        }
    }

    protected List<String> getExtendedDataTableNames(OWLOntology o, OWLDataProperty dataProperty) {
        return OWLOntologyUtil.getExtendedDataTableNames(o, dataProperty);
    }

    private String iriToJavaConstName(IRI documentIri, String iri) {
        String iriPartAfterHash = getIriPartAfterHash(iri);
        String javaConstName = toJavaConst(iriPartAfterHash);
        if (!iri.startsWith(documentIri.toString())) {
            String lastIriPart = getLastIriPart(iri);
            javaConstName = toJavaConst(lastIriPart) + "_" + javaConstName;
        }
        return javaConstName;
    }

    protected List<String> getDataPropertyDomains(OWLOntology o, OWLDataProperty dataProperty) {
        return EntitySearcher.getDomains(dataProperty, o).stream()
                .map(d -> ((HasIRI) d).getIRI().toString())
                .collect(Collectors.toList());
    }

    protected List<String> getDataPropertyObjectDomains(OWLOntology o, OWLDataProperty dataProperty) {
        return stream(OWLOntologyUtil.getObjectPropertyDomains(o, dataProperty))
                .map(OWLOntologyUtil::getOWLAnnotationValueAsString)
                .collect(Collectors.toList());
    }

    protected String getDataPropertyDisplayType(OWLOntology o, OWLDataProperty dataProperty) {
        return OWLOntologyUtil.getDisplayType(o, dataProperty);
    }

    protected OWLDatatype getDataPropertyRange(OWLOntology o, OWLDataProperty dataProperty) {
        return (OWLDatatype) toList(EntitySearcher.getRanges(dataProperty, o)).get(0);
    }

    private String getIriPartAfterHash(String iri) {
        int lastHash = iri.lastIndexOf('#');
        if (lastHash > 0) {
            return iri.substring(lastHash + 1);
        }
        return iri;
    }

    private void addIntents(SortedMap<String, String> sortedIntents, String[] intents) {
        for (String intent : intents) {
            String javaConstName = toJavaConst(intent);
            sortedIntents.put(javaConstName, String.format("    public static final String INTENT_%s = \"%s\";", javaConstName, intent));
        }
    }

    private String getLastIriPart(String iri) {
        int lastSlash = iri.lastIndexOf('/');
        if (lastSlash < 0) {
            return iri;
        }
        String lastPart = iri.substring(lastSlash + 1);
        int hash = lastPart.indexOf('#');
        if (hash > 0) {
            lastPart = lastPart.substring(0, hash);
        }
        return lastPart;
    }

    private String toJavaConst(String label) {
        boolean lastCharLower = false;
        StringBuilder result = new StringBuilder();
        for (char c : label.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (lastCharLower) {
                    result.append('_');
                }
                lastCharLower = false;
            } else if (Character.isLowerCase(c)) {
                lastCharLower = true;
            }
            if (Character.isLetterOrDigit(c)) {
                result.append(Character.toUpperCase(c));
            } else {
                result.append('_');
            }
        }
        String stringResult = result.toString();
        stringResult = stringResult.replaceAll("_+", "_");
        return stringResult;
    }
}
