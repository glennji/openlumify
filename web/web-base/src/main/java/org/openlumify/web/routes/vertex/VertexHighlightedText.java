package org.openlumify.web.routes.vertex;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.vertexium.*;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Optional;
import org.visallo.webster.annotations.Required;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.EntityHighlighter;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.ingest.video.VideoTranscript;
import org.openlumify.core.model.properties.MediaOpenLumifyProperties;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.termMention.TermMentionRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.JsonSerializer;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.WebConfiguration;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.openlumify.core.util.StreamUtil.stream;

@Singleton
public class VertexHighlightedText implements ParameterizedHandler {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexHighlightedText.class);
    private final Graph graph;
    private final EntityHighlighter entityHighlighter;
    private final TermMentionRepository termMentionRepository;
    private final Configuration configuration;

    @Inject
    public VertexHighlightedText(
            final Graph graph,
            final EntityHighlighter entityHighlighter,
            final TermMentionRepository termMentionRepository,
            final Configuration configuration
            ) {
        this.graph = graph;
        this.entityHighlighter = entityHighlighter;
        this.termMentionRepository = termMentionRepository;
        this.configuration = configuration;
    }

    @Handle
    public void handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "propertyKey") String propertyKey,
            @Optional(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations,
            OpenLumifyResponse response
    ) throws Exception {
        Authorizations authorizationsWithTermMention = termMentionRepository.getAuthorizations(authorizations);

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find vertex with id: " + graphVertexId);
        }

        if (Strings.isNullOrEmpty(propertyName)) {
            propertyName = OpenLumifyProperties.TEXT.getPropertyName();
        }

        Long maxTextLength = configuration.getLong(WebConfiguration.MAX_TEXT_LENGTH, -1L);

        StreamingPropertyValue textPropertyValue = (StreamingPropertyValue) artifactVertex.getPropertyValue(propertyKey, propertyName);
        if (textPropertyValue != null) {
            response.setContentType("text/html");

            LOGGER.debug("returning text for vertexId:%s property:%s", artifactVertex.getId(), propertyKey);
            InputStream inputStream = textPropertyValue.getInputStream();

            if (inputStream == null) {
                response.respondWithHtml("");
            } else {
                Iterable<Vertex> termMentions = termMentionRepository.findByOutVertexAndProperty(artifactVertex.getId(), propertyKey, propertyName, authorizationsWithTermMention);
                List<String> resolvedToVertexIds = stream(termMentions)
                        .map(OpenLumifyProperties.TERM_MENTION_FOR_ELEMENT_ID::getPropertyValue)
                        .filter(id -> id != null)
                        .collect(Collectors.toList());
                Map<String, Boolean> resolvedVerticesExist = graph.doVerticesExist(resolvedToVertexIds, authorizations);

                termMentions = stream(termMentions)
                        .filter(termMention -> {
                            String resolvedToVertexId = OpenLumifyProperties.TERM_MENTION_FOR_ELEMENT_ID.getPropertyValue(termMention);
                            return resolvedToVertexId == null || resolvedVerticesExist.getOrDefault(resolvedToVertexId, false);
                        }).collect(Collectors.toList());

                entityHighlighter.transformHighlightedText(inputStream, response.getOutputStream(), termMentions, maxTextLength, workspaceId, authorizationsWithTermMention);
            }
        }

        VideoTranscript videoTranscript = MediaOpenLumifyProperties.VIDEO_TRANSCRIPT.getPropertyValue(artifactVertex, propertyKey);
        if (videoTranscript != null) {
            LOGGER.debug("returning video transcript for vertexId:%s property:%s", artifactVertex.getId(), propertyKey);
            Iterable<Vertex> termMentions = termMentionRepository.findByOutVertexAndProperty(artifactVertex.getId(), propertyKey, propertyName, authorizations);
            VideoTranscript highlightedVideoTranscript = entityHighlighter.getHighlightedVideoTranscript(videoTranscript, termMentions, workspaceId, authorizations);
            response.setContentType("application/json");
            response.respondWithJson(highlightedVideoTranscript.toJson());
        }

        videoTranscript = JsonSerializer.getSynthesisedVideoTranscription(artifactVertex, propertyKey);
        if (videoTranscript != null) {
            LOGGER.debug("returning synthesised video transcript for vertexId:%s property:%s", artifactVertex.getId(), propertyKey);
            Iterable<Vertex> termMentions = termMentionRepository.findByOutVertexAndProperty(artifactVertex.getId(), propertyKey, propertyName, authorizations);
            VideoTranscript highlightedVideoTranscript = entityHighlighter.getHighlightedVideoTranscript(videoTranscript, termMentions, workspaceId, authorizationsWithTermMention);
            response.setContentType("application/json");
            response.respondWithJson(highlightedVideoTranscript.toJson());
        }
    }
}
