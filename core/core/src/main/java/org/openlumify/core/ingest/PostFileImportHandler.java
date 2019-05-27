package org.openlumify.core.ingest;

import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.openlumify.core.model.properties.types.PropertyMetadata;
import org.openlumify.core.model.properties.types.OpenLumifyPropertyUpdate;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.user.User;

import java.util.List;

/**
 * Provides a hook to modify the vertex before it goes into the Graph Property Worker pipeline.
 */
public abstract class PostFileImportHandler {
    public abstract void handle(
            Graph graph,
            Vertex vertex,
            List<OpenLumifyPropertyUpdate> changedProperties,
            Workspace workspace,
            PropertyMetadata propertyMetadata,
            Visibility visibility,
            User user,
            Authorizations authorizations
    );
}
