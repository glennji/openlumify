package org.openlumify.core.ingest.graphProperty;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.io.InputStream;
import java.util.Collection;

/**
 * By default raw properties will be assigned a mime type.
 *
 * Configuration:
 *
 * <pre><code>
 * org.openlumify.core.ingest.graphProperty.MimeTypeGraphPropertyWorker.handled.myTextProperty.propertyName=http://my.org#myTextProperty
 * org.openlumify.core.ingest.graphProperty.MimeTypeGraphPropertyWorker.handled.myOtherTextProperty.propertyName=http://my.org#myOtherTextProperty
 * </code></pre>
 */
@Singleton
public abstract class MimeTypeGraphPropertyWorker extends GraphPropertyWorker {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(MimeTypeGraphPropertyWorker.class);
    private static final String MULTI_VALUE_KEY = MimeTypeGraphPropertyWorker.class.getName();
    private final MimeTypeGraphPropertyWorkerConfiguration configuration;
    private Collection<PostMimeTypeWorker> postMimeTypeWorkers;

    @Inject
    protected MimeTypeGraphPropertyWorker(MimeTypeGraphPropertyWorkerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        postMimeTypeWorkers = InjectHelper.getInjectedServices(PostMimeTypeWorker.class, getConfiguration());
        for (PostMimeTypeWorker postMimeTypeWorker : postMimeTypeWorkers) {
            try {
                postMimeTypeWorker.prepare(workerPrepareData);
            } catch (Exception ex) {
                throw new OpenLumifyException("Could not prepare post mime type worker " + postMimeTypeWorker.getClass().getName(), ex);
            }
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String metadataValue = OpenLumifyProperties.MIME_TYPE_METADATA.getMetadataValue(property.getMetadata(), null);
        if (metadataValue != null) {
            return false;
        }

        return configuration.isHandled(element, property);
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String fileName = OpenLumifyProperties.FILE_NAME.getOnlyPropertyValue(data.getElement());
        String mimeType = getMimeType(in, fileName);
        if (mimeType == null) {
            return;
        }

        ExistingElementMutation<Vertex> m = ((Vertex) data.getElement()).prepareMutation();
        Metadata mimeTypeMetadata = data.createPropertyMetadata(getUser());
        OpenLumifyProperties.MIME_TYPE.addPropertyValue(m, getMultiKey(data.getProperty()), mimeType, mimeTypeMetadata, data.getProperty().getVisibility());
        m.setPropertyMetadata(data.getProperty(), OpenLumifyProperties.MIME_TYPE.getPropertyName(), mimeType, getVisibilityTranslator().getDefaultVisibility());
        m.save(getAuthorizations());
        getGraph().flush();

        runPostMimeTypeWorkers(mimeType, data);

        getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), data.getProperty(), data.getWorkspaceId(), data.getVisibilitySource(), data.getPriority());
    }

    private String getMultiKey(Property property) {
        return MULTI_VALUE_KEY + property.getKey();
    }

    private void runPostMimeTypeWorkers(String mimeType, GraphPropertyWorkData data) {
        for (PostMimeTypeWorker postMimeTypeWorker : postMimeTypeWorkers) {
            try {
                LOGGER.debug("running PostMimeTypeWorker: %s on element: %s, mimeType: %s", postMimeTypeWorker.getClass().getName(), data.getElement().getId(), mimeType);
                postMimeTypeWorker.executeAndCleanup(mimeType, data, getAuthorizations());
            } catch (Exception ex) {
                throw new OpenLumifyException("Failed running PostMimeTypeWorker " + postMimeTypeWorker.getClass().getName(), ex);
            }
        }
        if (postMimeTypeWorkers.size() > 0) {
            getGraph().flush();
        }
    }

    protected abstract String getMimeType(InputStream in, String fileName) throws Exception;
}
