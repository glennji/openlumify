package org.openlumify.vertexium.model.workspace;

import org.junit.Test;
import org.vertexium.*;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyPropertyDefinition;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.workspace.*;
import org.openlumify.core.model.workspace.product.WorkProductService;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.vertexium.util.IterableUtils.count;
import static org.vertexium.util.IterableUtils.toList;
import static org.openlumify.core.model.ontology.OntologyRepository.PUBLIC;

public class VertexiumWorkspaceRepositoryTest extends WorkspaceRepositoryTestBase {
    private User user;
    private User otherUser;

    private VertexiumWorkspaceRepository workspaceRepository;

    protected WorkspaceRepository getWorkspaceRepository() {
        if (workspaceRepository != null) {
            return workspaceRepository;
        }
        workspaceRepository = new VertexiumWorkspaceRepository(
                getGraph(),
                getConfiguration(),
                getGraphRepository(),
                getUserRepository(),
                getGraphAuthorizationRepository(),
                getWorkspaceDiffHelper(),
                getLockRepository(),
                getVisibilityTranslator(),
                getTermMentionRepository(),
                getOntologyRepository(),
                getWorkQueueRepository(),
                getAuthorizationRepository()
        ) {
            @Override
            protected Collection<WorkspaceListener> getWorkspaceListeners() {
                return VertexiumWorkspaceRepositoryTest.this.getWorkspaceListeners();
            }
        };

        List<WorkProductService> workProductServices = new ArrayList<>();
        workProductServices.add(new MockWorkProductService());
        workspaceRepository.setWorkProductServices(workProductServices);

        return workspaceRepository;
    }

    @Override
    public void before() throws Exception {
        super.before();
        user = getUserRepository().findOrAddUser(
                "vertexium-junit",
                "Vertexium Junit",
                "vertexium-junit@openlumify.com",
                "password"
        );
        otherUser = getUserRepository().findOrAddUser(
                "other-junit",
                "Other Junit",
                "other.junit@openlumify.com",
                "password"
        );

        User systemUser = getUserRepository().getSystemUser();
        Concept thing = getOntologyRepository().getEntityConcept(PUBLIC);
        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(Collections.singletonList(thing), "prop1", "Prop 1", PropertyType.STRING);
        propertyDefinition.setTextIndexHints(Collections.singleton(TextIndexHint.EXACT_MATCH));
        propertyDefinition.setUserVisible(true);
        getOntologyRepository().getOrCreateProperty(propertyDefinition, systemUser, PUBLIC);
    }

    @Test
    public void testAddWorkspace() {
        Authorizations allAuths = getGraph().createAuthorizations(WorkspaceRepository.VISIBILITY_STRING);
        int startingVertexCount = count(getGraph().getVertices(allAuths));
        int startingEdgeCount = count(getGraph().getEdges(allAuths));

        String workspaceId = "testWorkspaceId";
        Workspace workspace = getWorkspaceRepository().add(workspaceId, "workspace1", user);
        assertTrue(getGraphAuthorizationRepository().getGraphAuthorizations().contains(workspaceId));

        assertEquals(startingVertexCount + 1, count(getGraph().getVertices(allAuths))); // +1 = the workspace vertex
        assertEquals(
                startingEdgeCount + 1,
                count(getGraph().getEdges(allAuths))
        ); // +1 = the edge between workspace and user1

        Authorizations noAuthorizations = getAuthorizationRepository().getGraphAuthorizations(user);
        assertNull("Should not have access", getGraph().getVertex(workspaceId, noAuthorizations));

        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(user,
                WorkspaceRepository.VISIBILITY_STRING,
                workspace.getWorkspaceId());
        assertNotNull("Should have access", getGraph().getVertex(workspaceId, authorizations));

        Workspace foundWorkspace = getWorkspaceRepository().findById(workspaceId, user);
        assertEquals(workspaceId, foundWorkspace.getWorkspaceId());
    }

    @Test
    public void testAccessControl() {
        Authorizations allAuths = getGraph().createAuthorizations(
                WorkspaceRepository.VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        int startingVertexCount = count(getGraph().getVertices(allAuths));
        int startingEdgeCount = count(getGraph().getEdges(allAuths));

        String workspace1Id = "testWorkspace1Id";
        String workspace1Title = "workspace1";
        getWorkspaceRepository().add(workspace1Id, workspace1Title, user);

        String workspace2Id = "testWorkspace2Id";
        String workspace2Title = "workspace2";
        getWorkspaceRepository().add(workspace2Id, workspace2Title, user);

        String workspace3Id = "testWorkspace3Id";
        String workspace3Title = "workspace3";
        getWorkspaceRepository().add(workspace3Id, workspace3Title, otherUser);

        assertEquals(startingVertexCount + 3, count(getGraph().getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(
                startingEdgeCount + 3,
                count(getGraph().getEdges(allAuths))
        ); // +3 = the edges between workspaces and users

        List<Workspace> user1Workspaces = toList(getWorkspaceRepository().findAllForUser(user));
        assertEquals(2, user1Workspaces.size());
        boolean foundWorkspace1 = false;
        boolean foundWorkspace2 = false;
        for (Workspace workspace : user1Workspaces) {
            if (workspace.getDisplayTitle().equals(workspace1Title)) {
                foundWorkspace1 = true;
            } else if (workspace.getDisplayTitle().equals(workspace2Title)) {
                foundWorkspace2 = true;
            }
        }
        assertTrue("foundWorkspace1", foundWorkspace1);
        assertTrue("foundWorkspace2", foundWorkspace2);

        List<Workspace> user2Workspaces = toList(getWorkspaceRepository().findAllForUser(otherUser));
        assertEquals(1, user2Workspaces.size());
        assertEquals(workspace3Title, user2Workspaces.get(0).getDisplayTitle());

        try {
            getWorkspaceRepository().updateUserOnWorkspace(
                    user2Workspaces.get(0),
                    user.getUserId(),
                    WorkspaceAccess.READ,
                    user
            );
            fail("user1 should not have access to user2's workspace");
        } catch (OpenLumifyAccessDeniedException ex) {
            assertEquals(user, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        WorkspaceRepository.UpdateUserOnWorkspaceResult updateUserOnWorkspaceResult = getWorkspaceRepository().updateUserOnWorkspace(
                user2Workspaces.get(0),
                user.getUserId(),
                WorkspaceAccess.READ,
                otherUser
        );
        assertEquals(WorkspaceRepository.UpdateUserOnWorkspaceResult.ADD, updateUserOnWorkspaceResult);
        assertEquals(startingVertexCount + 3, count(getGraph().getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(
                startingEdgeCount + 4,
                count(getGraph().getEdges(allAuths))
        ); // +4 = the edges between workspaces and users
        List<WorkspaceUser> usersWithAccess = getWorkspaceRepository().findUsersWithAccess(
                user2Workspaces.get(0).getWorkspaceId(),
                otherUser
        );
        boolean foundUser1 = false;
        boolean foundUser2 = false;
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId())) {
                assertEquals(WorkspaceAccess.READ, userWithAccess.getWorkspaceAccess());
                foundUser1 = true;
            } else if (userWithAccess.getUserId().equals(otherUser.getUserId())) {
                assertEquals(WorkspaceAccess.WRITE, userWithAccess.getWorkspaceAccess());
                foundUser2 = true;
            } else {
                fail("Unexpected user " + userWithAccess.getUserId());
            }
        }
        assertTrue("could not find user1", foundUser1);
        assertTrue("could not find user2", foundUser2);

        try {
            getWorkspaceRepository().deleteUserFromWorkspace(user2Workspaces.get(0), user.getUserId(), user);
            fail("user1 should not have write access to user2's workspace");
        } catch (OpenLumifyAccessDeniedException ex) {
            assertEquals(user, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        try {
            getWorkspaceRepository().delete(user2Workspaces.get(0), user);
            fail("user1 should not have write access to user2's workspace");
        } catch (OpenLumifyAccessDeniedException ex) {
            assertEquals(user, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        updateUserOnWorkspaceResult = getWorkspaceRepository().updateUserOnWorkspace(
                user2Workspaces.get(0),
                user.getUserId(),
                WorkspaceAccess.WRITE,
                otherUser
        );
        assertEquals(WorkspaceRepository.UpdateUserOnWorkspaceResult.UPDATE, updateUserOnWorkspaceResult);
        assertEquals(startingVertexCount + 3, count(getGraph().getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(
                startingEdgeCount + 4,
                count(getGraph().getEdges(allAuths))
        ); // +4 = the edges between workspaces and users

        getWorkspaceRepository().deleteUserFromWorkspace(user2Workspaces.get(0), user.getUserId(), otherUser);
        assertEquals(startingVertexCount + 3, count(getGraph().getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(
                startingEdgeCount + 3,
                count(getGraph().getEdges(allAuths))
        ); // +3 = the edges between workspaces and users

        getWorkspaceRepository().delete(user2Workspaces.get(0), otherUser);
        assertEquals(startingVertexCount + 2, count(getGraph().getVertices(allAuths))); // +2 = the workspace vertices
        assertEquals(
                startingEdgeCount + 2,
                count(getGraph().getEdges(allAuths))
        ); // +2 = the edges between workspaces and users
    }
}
