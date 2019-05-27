package org.openlumify.tools.ontology.ingest.codegen;

import com.google.common.base.Strings;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.tools.ontology.ingest.common.ConceptBuilder;
import org.openlumify.web.clientapi.model.ClientApiOntology;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class ConceptWriter extends EntityWriter {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(ConceptWriter.class);

    public ConceptWriter(String outputDirectory, ClientApiOntology ontology, boolean writeCoreOpenLumifyClasses) {
        super(outputDirectory, ontology, writeCoreOpenLumifyClasses);
    }

    protected void writeClass(ClientApiOntology.Concept concept) {
        String conceptPackage = packageNameFromIri(concept.getId());
        if (conceptPackage != null) {
            String conceptClassName = classNameFromIri(concept.getId());

            // Don't expose the openlumify internal concepts to the generated code
            if (!writeCoreOpenLumifyClasses && conceptPackage.startsWith("org.openlumify") && !conceptClassName.equals("Root")) {
                return;
            }

            LOGGER.debug("Create concept %s.%s", conceptPackage, conceptClassName);

            try (PrintWriter writer = createWriter(conceptPackage, conceptClassName)) {
                String parentClass = ConceptBuilder.class.getSimpleName();
                if (!Strings.isNullOrEmpty(concept.getParentConcept())) {
                    parentClass = packageNameFromIri(concept.getParentConcept()) + "." + classNameFromIri(concept.getParentConcept());
                }

                Consumer<PrintWriter> constructorWriter = methodWriter -> {
                    writer.println("  public " + conceptClassName + "(String id) { super(id); }");
                    writer.println();
                    writer.println("  public " + conceptClassName + "(String id, String visibility) { super(id, visibility); }");
                };

                writeClass(
                        writer,
                        conceptPackage,
                        conceptClassName,
                        parentClass,
                        concept.getId(),
                        findPropertiesByIri(concept.getProperties()),
                        constructorWriter);
            } catch (IOException e) {
                throw new OpenLumifyException("Unable to create concept class.", e);
            }
        }
    }
}