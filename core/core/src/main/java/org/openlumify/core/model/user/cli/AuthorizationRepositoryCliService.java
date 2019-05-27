package org.openlumify.core.model.user.cli;

import org.openlumify.core.model.user.cli.args.Args;
import org.openlumify.core.model.user.cli.args.CreateUserArgs;
import org.openlumify.core.user.User;

import java.util.Collection;

public interface AuthorizationRepositoryCliService {
    void onCreateUser(UserAdmin userAdmin, CreateUserArgs createUserArgs, User user, User authUser);

    void onPrintUser(UserAdmin userAdmin, Args args, String formatString, User user);

    Collection<String> getActions(UserAdmin userAdmin);

    Args createArguments(UserAdmin userAdmin, String action);

    int run(UserAdmin userAdmin, String action, Args args, User authUser);

    void validateArguments(UserAdmin userAdmin, String action, Args args);

    void printHelp(UserAdmin userAdmin, String action);
}
