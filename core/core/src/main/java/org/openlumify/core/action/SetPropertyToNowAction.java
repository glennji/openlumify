package org.openlumify.core.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.openlumify.core.model.workQueue.WorkQueueRepository;

import java.util.Date;

@Singleton
public class SetPropertyToNowAction extends SetPropertyActionBase {
    @Inject
    public SetPropertyToNowAction(
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        super(graph, workQueueRepository);
    }

    @Override
    protected Object getNewValue(ActionExecuteParameters parameters) {
        return new Date();
    }

    public static JSONObject createActionData(String propertyKey, String propertyName, String visibility) {
        return SetPropertyActionBase.createActionData(SetPropertyToNowAction.class, propertyKey, propertyName, visibility);
    }
}
