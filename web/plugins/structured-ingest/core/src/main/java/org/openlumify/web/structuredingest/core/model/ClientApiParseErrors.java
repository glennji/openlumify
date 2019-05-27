package org.openlumify.web.structuredingest.core.model;

import org.openlumify.web.clientapi.model.ClientApiObject;
import org.openlumify.web.structuredingest.core.util.mapping.PropertyMapping;

import java.util.ArrayList;
import java.util.List;

public class ClientApiParseErrors implements ClientApiObject {
    public List<ParseError> errors = new ArrayList<>();

    public static class ParseError {
        public Object rawPropertyValue;
        public PropertyMapping propertyMapping;
        public String message;
        public int sheetIndex;
        public long rowIndex;
    }
}
