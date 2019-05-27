package org.openlumify.core.model.notification;

import org.apache.commons.codec.binary.Hex;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.user.GraphAuthorizationRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.user.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class NotificationRepository {
    private final Graph graph;
    private final GraphRepository graphRepository;
    private AuthorizationRepository authorizationRepository;
    protected static final String VISIBILITY_STRING = "notifications";

    protected NotificationRepository(
            Graph graph,
            GraphRepository graphRepository,
            GraphAuthorizationRepository graphAuthorizationRepository
    ) {
        this.graph = graph;
        this.graphRepository = graphRepository;
        graphAuthorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
    }

    protected static String hash(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(s.getBytes());
            return Hex.encodeHexString(md5);
        } catch (NoSuchAlgorithmException e) {
            throw new OpenLumifyException("Could not find MD5", e);
        }
    }

    protected Graph getGraph() {
        return graph;
    }

    protected GraphRepository getGraphRepository() {
        return graphRepository;
    }

    protected Authorizations getAuthorizations(User user) {
        return getAuthorizationRepository().getGraphAuthorizations(user, VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
    }

    protected AuthorizationRepository getAuthorizationRepository() {
        if (authorizationRepository == null) {
            authorizationRepository = InjectHelper.getInstance(AuthorizationRepository.class);
        }
        return authorizationRepository;
    }
}
