package org.openlumify.core.action;

import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Visibility;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

public abstract class SetPropertyActionBase extends Action {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(SetPropertyActionBase.class);
    public static final String PROPERTY_PROPERTY_KEY = "propertyKey";
    public static final String PROPERTY_PROPERTY_NAME = "propertyName";
    public static final String PROPERTY_VISIBILITY = "visibility";
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    protected SetPropertyActionBase(
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void validateData(JSONObject actionData) {
        super.validateData(actionData);
        validateDataHas(actionData, PROPERTY_PROPERTY_KEY);
        validateDataHas(actionData, PROPERTY_PROPERTY_NAME);
        validateDataHas(actionData, PROPERTY_VISIBILITY);
    }

    @Override
    public void execute(ActionExecuteParameters parameters, User user, Authorizations authorizations) {
        String propertyKey = parameters.getData().getString(PROPERTY_PROPERTY_KEY);
        String propertyName = parameters.getData().getString(PROPERTY_PROPERTY_NAME);
        String visibility = parameters.getData().getString(PROPERTY_VISIBILITY);

        Object newValue = getNewValue(parameters);
        Visibility vis = new Visibility(visibility);
        LOGGER.debug("setting property %s:%s[%s] = %s", propertyName, propertyKey, vis, newValue);
        parameters.getElement().addPropertyValue(propertyKey, propertyName, newValue, vis, authorizations);
        graph.flush();
        Property property = parameters.getElement().getProperty(propertyKey, propertyName);
        workQueueRepository.pushGraphPropertyQueue(parameters.getElement(), property, Priority.NORMAL);

    }

    protected abstract Object getNewValue(ActionExecuteParameters parameters);

    protected static JSONObject createActionData(Class clazz, String propertyKey, String propertyName, String visibility) {
        JSONObject json = Action.createActionData(clazz);
        json.put(PROPERTY_PROPERTY_KEY, propertyKey);
        json.put(PROPERTY_PROPERTY_NAME, propertyName);
        json.put(PROPERTY_VISIBILITY, visibility);
        return json;
    }
}
