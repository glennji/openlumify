package org.openlumify.core.model.user;

import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.util.*;

public class InMemoryGraphAuthorizationRepository implements GraphAuthorizationRepository {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(InMemoryGraphAuthorizationRepository.class);
    private List<String> authorizations = new ArrayList<>();

    @Override
    public void addAuthorizationToGraph(String... auths) {
        for (String auth : auths) {
            auth = auth.trim();
            LOGGER.info("Adding authorization to graph user %s", auth);
            authorizations.add(auth);
        }
    }

    @Override
    public void removeAuthorizationFromGraph(String auth) {
        LOGGER.info("Removing authorization to graph user %s", auth);
        authorizations.remove(auth);
    }

    @Override
    public List<String> getGraphAuthorizations() {
        LOGGER.info("getting authorizations");
        return new ArrayList<>(authorizations);
    }
}
