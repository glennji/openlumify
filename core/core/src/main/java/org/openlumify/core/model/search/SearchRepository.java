package org.openlumify.core.model.search;

import org.json.JSONObject;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.ClientApiSearch;
import org.openlumify.web.clientapi.model.ClientApiSearchListResponse;

import java.util.Collection;

public abstract class SearchRepository {
    private final Collection<SearchRunner> searchRunners;

    protected SearchRepository(Configuration configuration) {
        searchRunners = InjectHelper.getInjectedServices(SearchRunner.class, configuration);
    }

    public abstract String saveSearch(
            String id,
            String name,
            String url,
            JSONObject searchParameters,
            User user
    );

    public abstract String saveGlobalSearch(
            String id,
            String name,
            String url,
            JSONObject searchParameters,
            User user
    );

    public abstract ClientApiSearchListResponse getSavedSearches(User user);

    public abstract ClientApiSearch getSavedSearch(String id, User user);

    public abstract ClientApiSearch getSavedSearchOnWorkspace(String id, User user, String workspaceId);

    public abstract void deleteSearch(String id, User user);

    public SearchRunner findSearchRunnerByUri(String searchUri) {
        for (SearchRunner searchRunner : searchRunners) {
            if (searchRunner.getUri().equals(searchUri)) {
                return searchRunner;
            }
        }
        return null;
    }
}
