package org.openlumify.web.routes.directory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.webster.ParameterizedHandler;
import org.openlumify.webster.annotations.Handle;
import org.openlumify.webster.annotations.Required;
import org.openlumify.core.exception.OpenLumifyResourceNotFoundException;
import org.openlumify.core.model.directory.DirectoryRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.clientapi.model.DirectoryEntity;

@Singleton
public class DirectoryGet implements ParameterizedHandler {
    private final DirectoryRepository directoryRepository;

    @Inject
    public DirectoryGet(DirectoryRepository directoryRepository) {
        this.directoryRepository = directoryRepository;
    }

    @Handle
    public DirectoryEntity handle(
            @Required(name = "id", allowEmpty = false) String id,
            User user
    ) {
        DirectoryEntity directoryEntity = this.directoryRepository.findById(id, user);
        if (directoryEntity == null) {
            throw new OpenLumifyResourceNotFoundException("Could not find directory entry with id: " + id);
        }

        return directoryEntity;
    }
}
