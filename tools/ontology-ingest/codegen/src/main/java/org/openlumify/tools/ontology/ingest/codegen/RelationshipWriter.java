package org.openlumify.tools.ontology.ingest.codegen;

import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.tools.ontology.ingest.common.RelationshipBuilder;
import org.openlumify.web.clientapi.model.ClientApiOntology;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class RelationshipWriter extends EntityWriter {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(RelationshipWriter.class);

    public RelationshipWriter(String outputDirectory, ClientApiOntology ontology, boolean writeCoreOpenLumifyClasses) {
        super(outputDirectory, ontology, writeCoreOpenLumifyClasses);
    }

    protected void writeClass(ClientApiOntology.Relationship relationship) {
        String relationshipPackage = packageNameFromIri(relationship.getTitle());
        if (relationshipPackage != null) {
            String relationshipClassName = classNameFromIri(relationship.getTitle());

            // Don't expose the openlumify internal relationships to the generated code
            if (!writeCoreOpenLumifyClasses && relationshipPackage.startsWith("org.openlumify")) {
                return;
            }

            LOGGER.debug("Create relationship %s.%s", relationshipPackage, relationshipClassName);

            try (PrintWriter writer = createWriter(relationshipPackage, relationshipClassName)) {
                Consumer<PrintWriter> constructorWriter = methodWriter -> {
                    relationship.getDomainConceptIris().stream().sorted().forEach(outConceptIri -> {
                        String outVertexClassName = packageNameFromIri(outConceptIri) + "." + classNameFromIri(outConceptIri);
                        relationship.getRangeConceptIris().stream().sorted().forEach(inConceptIri -> {
                            String inVertexClassName = packageNameFromIri(inConceptIri) + "." + classNameFromIri(inConceptIri);
                            methodWriter.println();
                            methodWriter.println("  public " + relationshipClassName + "(String id, " + outVertexClassName + " outVertex, " + inVertexClassName + " inVertex) {");
                            methodWriter.println("    super(id, inVertex.getId(), inVertex.getIri(), outVertex.getId(), outVertex.getIri());");
                            methodWriter.println("  } ");
                        });
                    });
                };

                writeClass(
                        writer,
                        relationshipPackage,
                        relationshipClassName,
                        RelationshipBuilder.class.getName(),
                        relationship.getTitle(),
                        findPropertiesByIri(relationship.getProperties()),
                        constructorWriter);
            } catch (IOException e) {
                throw new OpenLumifyException("Unable to create relationship class.", e);
            }
        }
    }
}
