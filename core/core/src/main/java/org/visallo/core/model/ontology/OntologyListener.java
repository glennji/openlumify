package org.visallo.core.model.ontology;

public interface OntologyListener {
    void clearCache();

    void clearCache(String workspaceId);
}
