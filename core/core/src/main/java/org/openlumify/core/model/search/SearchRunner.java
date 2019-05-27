package org.openlumify.core.model.search;

import org.vertexium.Authorizations;
import org.openlumify.core.user.User;

public abstract class SearchRunner {
    public abstract String getUri();

    public abstract SearchResults run(
            SearchOptions searchOptions,
            User user,
            Authorizations authorizations
    );
}
