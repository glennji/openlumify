package org.openlumify.core.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.openlumify.core.model.workQueue.WorkQueueRepository;

@Singleton
public class SetPropertyAction extends SetPropertyActionBase {
    public static final String PROPERTY_VALUE = "value";

    @Inject
    public SetPropertyAction(
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        super(graph, workQueueRepository);
    }

    @Override
    public void validateData(JSONObject actionData) {
        super.validateData(actionData);
        validateDataHas(actionData, PROPERTY_VALUE);
    }

    @Override
    protected Object getNewValue(ActionExecuteParameters parameters) {
        return parameters.getData().get(PROPERTY_VALUE);
    }

    public static JSONObject createActionData(String propertyKey, String propertyName, Object value, String visibility) {
        JSONObject json = SetPropertyActionBase.createActionData(SetPropertyAction.class, propertyKey, propertyName, visibility);
        json.put(PROPERTY_VALUE, value);
        return json;
    }
}
