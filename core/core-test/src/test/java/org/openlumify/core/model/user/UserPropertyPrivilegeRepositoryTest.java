package org.openlumify.core.model.user;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Graph;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.config.HashMapConfigurationLoader;
import org.openlumify.core.model.notification.UserNotificationRepository;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.user.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserPropertyPrivilegeRepositoryTest {
    private UserPropertyPrivilegeRepository userPropertyPrivilegeRepository;

    @Mock
    private User user;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Graph graph;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Before
    public void before() {
        Map config = new HashMap();
        config.put(
                UserPropertyPrivilegeRepository.CONFIGURATION_PREFIX + ".defaultPrivileges",
                "defaultPrivilege1,defaultPrivilege2"
        );
        Configuration configuration = new HashMapConfigurationLoader(config).createConfiguration();
        userPropertyPrivilegeRepository = new UserPropertyPrivilegeRepository(
                ontologyRepository,
                configuration,
                userNotificationRepository,
                workQueueRepository
        ) {
            @Override
            protected Iterable<PrivilegesProvider> getPrivilegesProviders(Configuration configuration) {
                return Lists.newArrayList();
            }
        };
    }

    @Test
    public void testGetPrivilegesForNewUser() {
        HashSet<String> expected = Sets.newHashSet("defaultPrivilege1", "defaultPrivilege2");

        Set<String> privileges = userPropertyPrivilegeRepository.getPrivileges(user);
        assertEquals(expected, privileges);
    }

    @Test
    public void testGetPrivilegesForExisting() {
        HashSet<String> expected = Sets.newHashSet("userPrivilege1", "userPrivilege2");
        when(user.getProperty(eq(UserPropertyPrivilegeRepository.PRIVILEGES_PROPERTY_IRI)))
                .thenReturn("userPrivilege1,userPrivilege2");

        Set<String> privileges = userPropertyPrivilegeRepository.getPrivileges(user);
        assertEquals(expected, privileges);
    }
}