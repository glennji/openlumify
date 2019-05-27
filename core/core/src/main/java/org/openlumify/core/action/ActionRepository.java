package org.openlumify.core.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.config.Configuration;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class ActionRepository {
    private Collection<Action> actions;
    private final Configuration configuration;

    @Inject
    public ActionRepository(Configuration configuration) {
        this.configuration = configuration;
    }

    public Action getActionFromActionData(JSONObject json) {
        String type = json.optString(Action.PROPERTY_TYPE);
        checkNotNull(type, "Action json missing \"" + Action.PROPERTY_TYPE + "\" property");
        for (Action action : getActions()) {
            if (action.getClass().getName().equals(type)) {
                return action;
            }
        }
        return null;
    }

    protected Collection<Action> getActions() {
        // late bind the actions to avoid circular references
        if (actions == null) {
            actions = InjectHelper.getInjectedServices(Action.class, configuration);
        }
        return actions;
    }

    public void checkActionData(JSONObject actionData) {
        checkNotNull(actionData, "actionData cannot by null");
        Action action = getActionFromActionData(actionData);
        checkNotNull(action, "Could not find action for data: " + actionData.toString());
        action.validateData(actionData);
    }
}
