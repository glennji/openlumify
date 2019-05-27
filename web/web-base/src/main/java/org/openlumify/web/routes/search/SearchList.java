package org.openlumify.web.routes.search;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.core.model.search.SearchRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.ClientApiSearchListResponse;

@Singleton
public class SearchList implements ParameterizedHandler {
    private final SearchRepository searchRepository;

    @Inject
    public SearchList(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Handle
    public ClientApiSearchListResponse handle(User user) throws Exception {
        return this.searchRepository.getSavedSearches(user);
    }
}
