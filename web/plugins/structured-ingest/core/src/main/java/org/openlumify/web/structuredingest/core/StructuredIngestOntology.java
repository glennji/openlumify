package org.openlumify.web.structuredingest.core;

import org.openlumify.core.model.properties.types.StringOpenLumifyProperty;

public class StructuredIngestOntology {
    public static final String IRI = "http://openlumify.org/structured-file";
    public static final String ELEMENT_HAS_SOURCE_IRI = IRI + "#elementHasSource";

    public static final StringOpenLumifyProperty ERROR_MESSAGE_PROPERTY = new StringOpenLumifyProperty(IRI + "#errorMessage");
    public static final StringOpenLumifyProperty TARGET_PROPERTY = new StringOpenLumifyProperty(IRI + "#targetPropertyName");
    public static final StringOpenLumifyProperty RAW_CELL_VALUE_PROPERTY = new StringOpenLumifyProperty(IRI + "#rawCellValue");
    public static final StringOpenLumifyProperty SHEET_PROPERTY = new StringOpenLumifyProperty(IRI + "#sheet");
    public static final StringOpenLumifyProperty ROW_PROPERTY = new StringOpenLumifyProperty(IRI + "#row");
}
