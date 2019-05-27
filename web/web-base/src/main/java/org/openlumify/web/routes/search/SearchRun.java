package org.openlumify.web.routes.search;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.openlumify.core.model.search.SearchRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiSearch;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.parameterProviders.OpenLumifyBaseParameterProvider;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Singleton
public class SearchRun implements ParameterizedHandler {
    private final SearchRepository searchRepository;

    @Inject
    public SearchRun(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Handle
    public void handle(
            @ActiveWorkspaceId String workspaceId,
            @Required(name = "id") String id,
            User user,
            HttpServletRequest request,
            OpenLumifyResponse response
    ) throws Exception {
        ClientApiSearch savedSearch = this.searchRepository.getSavedSearch(id, user);
        if (savedSearch == null) {
            response.respondWithNotFound("Could not find search with id " + id);
            return;
        }

        request.setAttribute(OpenLumifyBaseParameterProvider.VISALLO_WORKSPACE_ID_HEADER_NAME, workspaceId);
        if (savedSearch.parameters != null) {
            for (Object k : savedSearch.parameters.keySet()) {
                String key = (String) k;
                Object value = savedSearch.parameters.get(key);

                if (value instanceof List) {
                    List list = (List) value;
                    String[] valueArray = new String[list.size()];
                    value = list.toArray(valueArray);
                } else {
                    value = value.toString();
                }

                request.setAttribute(key, value);
            }
        }

        RequestDispatcher dispatcher = request.getServletContext().getRequestDispatcher(savedSearch.url);
        dispatcher.forward(request, response.getHttpServletResponse());
    }
}
