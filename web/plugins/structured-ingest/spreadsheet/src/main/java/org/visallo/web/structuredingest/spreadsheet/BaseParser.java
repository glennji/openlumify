package org.visallo.web.structuredingest.spreadsheet;

import org.apache.commons.lang.StringUtils;

public abstract class BaseParser {

    protected boolean rowIsBlank(String[] columnValues) {
        // skip over blank rows
        boolean allBlank = true;
        for (int i = 0; i < columnValues.length && allBlank; i++) {
            allBlank = allBlank && StringUtils.isBlank(columnValues[i]);
        }
        return allBlank;
    }

}
