package org.openlumify.core.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.user.User;

@Singleton
public class WorkQueueElementAction extends Action {
    public static final String PROPERTY_PRIORITY = "priority";
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public WorkQueueElementAction(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void execute(ActionExecuteParameters parameters, User user, Authorizations authorizations) {
        Priority priority = getPriority(parameters);
        this.workQueueRepository.pushElement(parameters.getElement(), priority);
    }

    private Priority getPriority(ActionExecuteParameters parameters) {
        String priorityStr = parameters.getData().optString(PROPERTY_PRIORITY);
        return Priority.safeParse(priorityStr);
    }

    public static JSONObject createActionData(Priority priority) {
        JSONObject json = Action.createActionData(WorkQueueElementAction.class);
        if (priority != null) {
            json.put(PROPERTY_PRIORITY, priority.name());
        }
        return json;
    }
}
