package org.openlumify.core.model.directory;

import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.DirectoryEntity;
import org.openlumify.web.clientapi.model.DirectoryGroup;
import org.openlumify.web.clientapi.model.DirectoryPerson;

import java.util.List;

public abstract class DirectoryRepository {
    public abstract List<DirectoryPerson> searchPeople(String search, User user);

    public abstract List<DirectoryGroup> searchGroups(String search, User user);

    public abstract DirectoryEntity findById(String id, User user);

    public abstract String getDirectoryEntityId(User user);

    public abstract List<DirectoryPerson> findAllPeopleInGroup(DirectoryGroup group);
}
