package org.openlumify.core.model.properties.types;

import org.openlumify.core.util.JSONUtil;
import org.openlumify.web.clientapi.model.GraphPosition;

public class GraphPositionSingleValueOpenLumifyProperty extends SingleValueOpenLumifyProperty<GraphPosition, String> {
    public GraphPositionSingleValueOpenLumifyProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(GraphPosition value) {
        return value.toJSONObject().toString();
    }

    @Override
    public GraphPosition unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return GraphPosition.fromJSONObject(JSONUtil.parse(value.toString()));
    }
}
