package org.openlumify.web.routes;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.graph.GraphRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.BadRequestException;
import org.openlumify.web.clientapi.model.VisibilityJson;

import java.io.IOException;
import java.util.ResourceBundle;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public abstract class SetPropertyVisibilityTestBase extends RouteTestBase {
    protected Authorizations authorizations;

    @Before
    public void before() throws IOException {
        super.before();
        graphRepository = Mockito.mock(GraphRepository.class);
        authorizations = graph.createAuthorizations("A");
    }

    public abstract void testValid() throws Exception;

    protected void testValid(Element element) throws Exception {
        String newVisibilitySource = new VisibilityJson("A").getSource();
        String oldVisibilitySource = new VisibilityJson("").getSource();
        handle(
                element.getId(),
                newVisibilitySource,
                oldVisibilitySource,
                "k1",
                "p1",
                WORKSPACE_ID,
                resourceBundle,
                user,
                authorizations
        );

        verify(graphRepository).updatePropertyVisibilitySource(
                eq(element),
                eq("k1"),
                eq("p1"),
                eq(""),
                eq("A"),
                eq(WORKSPACE_ID),
                eq(user),
                eq(authorizations)
        );
    }

    @Test
    public void testNotFound() throws Exception {
        String newVisibilitySource = new VisibilityJson("A").getSource();
        String oldVisibilitySource = new VisibilityJson("").getSource();
        Authorizations authorizations = graph.createAuthorizations();

        try {
            handle(
                    "badElementId",
                    newVisibilitySource,
                    oldVisibilitySource,
                    "k1",
                    "p1",
                    WORKSPACE_ID,
                    resourceBundle,
                    user,
                    authorizations
            );
            fail("expected exception");
        } catch (OpenLumifyResourceNotFoundException ex) {
            assertEquals("badElementId", ex.getResourceId());
        }
    }

    public abstract void testBadVisibility() throws Exception;

    protected void testBadVisibility(Element element) throws Exception {
        String newVisibilitySource = new VisibilityJson("bad").getSource();
        String oldVisibilitySource = new VisibilityJson("").getSource();

        try {
            handle(
                    element.getId(),
                    newVisibilitySource,
                    oldVisibilitySource,
                    "k1",
                    "p1",
                    WORKSPACE_ID,
                    resourceBundle,
                    user,
                    authorizations
            );
            fail("expected exception");
        } catch (BadRequestException ex) {
            assertEquals("newVisibilitySource", ex.getParameterName());
        }
    }

    protected abstract void handle(
            String elementId,
            String newVisibilitySource,
            String oldVisibilitySource,
            String propertyKey,
            String propertyName,
            String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception;
}
