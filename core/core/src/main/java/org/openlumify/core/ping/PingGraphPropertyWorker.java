package org.openlumify.core.ping;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.openlumify.core.ingest.graphProperty.GraphPropertyWorkData;
import org.openlumify.core.ingest.graphProperty.GraphPropertyWorker;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.core.model.properties.OpenLumifyProperties;

import java.io.InputStream;

@Name("Ping")
@Description("work on special Ping vertices to measure GPW wait time")
@Singleton
public class PingGraphPropertyWorker extends GraphPropertyWorker {
    private final PingUtil pingUtil;

    @Inject
    public PingGraphPropertyWorker(PingUtil pingUtil) {
        this.pingUtil = pingUtil;
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Vertex vertex = (Vertex) data.getElement();
        pingUtil.gpwUpdate(vertex, getGraph(), getAuthorizations());
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        return element instanceof Vertex
                && PingOntology.IRI_CONCEPT_PING.equals(OpenLumifyProperties.CONCEPT_TYPE.getPropertyValue(element))
                && property == null;
    }
}
