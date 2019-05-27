package org.openlumify.web.routes.search;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.search.SearchRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.ClientApiSearch;

@Singleton
public class SearchDelete implements ParameterizedHandler {
    private final SearchRepository searchRepository;

    @Inject
    public SearchDelete(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Handle
    public void handle(
            @Required(name = "id") String id,
            User user
    ) throws Exception {
        ClientApiSearch savedSearch = this.searchRepository.getSavedSearch(id, user);
        if (savedSearch == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find saved search with id " + id);
        }

        this.searchRepository.deleteSearch(id, user);
    }
}
