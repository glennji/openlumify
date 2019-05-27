package org.openlumify.web.parameterProviders;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.vertexium.model.user.InMemoryUser;
import org.openlumify.web.CurrentUser;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.openlumify.web.parameterProviders.OpenLumifyBaseParameterProvider.*;

@RunWith(MockitoJUnitRunner.class)
public class OpenLumifyBaseParameterProviderTest {
    private static final String USER_ID = "user123";
    private static final String WORKSPACE_ID = "workspace123";

    @Mock
    private HttpServletRequest request;

    @Mock
    private WorkspaceRepository workspaceRepository;

    private User user = new InMemoryUser(USER_ID);

    @Before
    public void beforeTest() {
        when(request.getAttribute(CurrentUser.CURRENT_USER_REQ_ATTR_NAME)).thenReturn(user);
    }

    @Test
    public void testGetActiveWorkspaceIdOrDefaultFromAttribute() {
        when(request.getAttribute(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        String workspaceId = getActiveWorkspaceIdOrDefault(request, workspaceRepository);
        assertEquals(WORKSPACE_ID, workspaceId);
    }

    @Test
    public void testGetActiveWorkspaceIdOrDefaultFromHeader() {
        when(request.getHeader(VISALLO_WORKSPACE_ID_HEADER_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        String workspaceId = getActiveWorkspaceIdOrDefault(request, workspaceRepository);
        assertEquals(WORKSPACE_ID, workspaceId);
    }

    @Test
    public void testGetActiveWorkspaceIdOrDefaultFromParameter() {
        when(request.getParameter(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        String workspaceId = getActiveWorkspaceIdOrDefault(request, workspaceRepository);
        assertEquals(WORKSPACE_ID, workspaceId);
    }

    @Test
    public void testGetActiveWorkspaceIdOrDefaultWithMissingWorkspaceId() {
        String workspaceId = getActiveWorkspaceIdOrDefault(request, workspaceRepository);
        assertNull(workspaceId);
    }

    @Test(expected = OpenLumifyAccessDeniedException.class)
    public void testGetActiveWorkspaceIdOrDefaultWithNoUser() {
        when(request.getParameter(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        getActiveWorkspaceIdOrDefault(request, workspaceRepository);
    }

    @Test(expected = OpenLumifyAccessDeniedException.class)
    public void testGetActiveWorkspaceIdOrDefaultWithNoAccess() {
        when(request.getParameter(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(false);

        getActiveWorkspaceIdOrDefault(request, workspaceRepository);
    }

    @Test
    public void testGetActiveWorkspaceId() {
        when(request.getParameter(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(true);

        String workspaceId = getActiveWorkspaceId(request, workspaceRepository);
        assertEquals(WORKSPACE_ID, workspaceId);
    }

    @Test(expected = OpenLumifyException.class)
    public void testGetActiveWorkspaceIdWithMissingWorkspaceId() {
        getActiveWorkspaceId(request, workspaceRepository);
    }

    @Test(expected = OpenLumifyAccessDeniedException.class)
    public void testGetActiveWorkspaceIdNoAccess() {
        when(request.getParameter(WORKSPACE_ID_ATTRIBUTE_NAME)).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user)).thenReturn(false);

        getActiveWorkspaceId(request, workspaceRepository);
    }
}