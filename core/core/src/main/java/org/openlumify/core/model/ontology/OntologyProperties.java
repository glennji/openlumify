package org.openlumify.core.model.ontology;

import com.google.common.collect.ImmutableSet;
import org.openlumify.core.model.properties.types.*;

import java.util.Set;

public class OntologyProperties {
    public static final String EDGE_LABEL_DEPENDENT_PROPERTY = "http://openlumify.org#dependentPropertyIri";

    public static final StringSingleValueOpenLumifyProperty TITLE = new StringSingleValueOpenLumifyProperty("http://openlumify.org#title");
    public static final StreamingOpenLumifyProperty ONTOLOGY_FILE = new StreamingOpenLumifyProperty("http://openlumify.org#ontologyFile");
    public static final StringOpenLumifyProperty ONTOLOGY_FILE_MD5 = new StringOpenLumifyProperty("http://openlumify.org#ontologyFileMd5");
    public static final IntegerSingleValueOpenLumifyProperty DEPENDENT_PROPERTY_ORDER_PROPERTY_NAME = new IntegerSingleValueOpenLumifyProperty("order");
    public static final StringOpenLumifyProperty TEXT_INDEX_HINTS = new StringOpenLumifyProperty("http://openlumify.org#textIndexHints");
    public static final StringSingleValueOpenLumifyProperty ONTOLOGY_TITLE = new StringSingleValueOpenLumifyProperty("http://openlumify.org#ontologyTitle");
    public static final StringSingleValueOpenLumifyProperty DISPLAY_NAME = new StringSingleValueOpenLumifyProperty("http://openlumify.org#displayName");
    public static final StringSingleValueOpenLumifyProperty DISPLAY_TYPE = new StringSingleValueOpenLumifyProperty("http://openlumify.org#displayType");
    public static final BooleanSingleValueOpenLumifyProperty USER_VISIBLE = new BooleanSingleValueOpenLumifyProperty("http://openlumify.org#userVisible");
    public static final StringSingleValueOpenLumifyProperty GLYPH_ICON_FILE_NAME = new StringSingleValueOpenLumifyProperty("http://openlumify.org#glyphIconFileName");
    public static final StreamingSingleValueOpenLumifyProperty GLYPH_ICON = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#glyphIcon");
    public static final StringSingleValueOpenLumifyProperty GLYPH_ICON_SELECTED_FILE_NAME = new StringSingleValueOpenLumifyProperty("http://openlumify.org#glyphIconSelectedFileName");
    public static final StreamingSingleValueOpenLumifyProperty GLYPH_ICON_SELECTED = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#glyphIconSelected");
    public static final StreamingSingleValueOpenLumifyProperty MAP_GLYPH_ICON = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#mapGlyphIcon");
    public static final StringSingleValueOpenLumifyProperty MAP_GLYPH_ICON_FILE_NAME = new StringSingleValueOpenLumifyProperty("http://openlumify.org#mapGlyphIconFileName");
    public static final JsonArraySingleValueOpenLumifyProperty ADD_RELATED_CONCEPT_WHITE_LIST = new JsonArraySingleValueOpenLumifyProperty("http://openlumify.org#addRelatedConceptWhiteList");
    public static final StringOpenLumifyProperty INTENT = new StringOpenLumifyProperty("http://openlumify.org#intent");
    public static final BooleanSingleValueOpenLumifyProperty SEARCHABLE = new BooleanSingleValueOpenLumifyProperty("http://openlumify.org#searchable");
    public static final BooleanSingleValueOpenLumifyProperty SORTABLE = new BooleanSingleValueOpenLumifyProperty("http://openlumify.org#sortable");
    public static final BooleanSingleValueOpenLumifyProperty ADDABLE = new BooleanSingleValueOpenLumifyProperty("http://openlumify.org#addable");
    public static final StringSingleValueOpenLumifyProperty DISPLAY_FORMULA = new StringSingleValueOpenLumifyProperty("http://openlumify.org#displayFormula");
    public static final StringSingleValueOpenLumifyProperty PROPERTY_GROUP = new StringSingleValueOpenLumifyProperty("http://openlumify.org#propertyGroup");
    public static final StringSingleValueOpenLumifyProperty VALIDATION_FORMULA = new StringSingleValueOpenLumifyProperty("http://openlumify.org#validationFormula");
    public static final StringSingleValueOpenLumifyProperty TIME_FORMULA = new StringSingleValueOpenLumifyProperty("http://openlumify.org#timeFormula");
    public static final StringSingleValueOpenLumifyProperty TITLE_FORMULA = new StringSingleValueOpenLumifyProperty("http://openlumify.org#titleFormula");
    public static final StringSingleValueOpenLumifyProperty SUBTITLE_FORMULA = new StringSingleValueOpenLumifyProperty("http://openlumify.org#subtitleFormula");
    public static final StringSingleValueOpenLumifyProperty COLOR = new StringSingleValueOpenLumifyProperty("http://openlumify.org#color");
    public static final StringSingleValueOpenLumifyProperty DATA_TYPE = new StringSingleValueOpenLumifyProperty("http://openlumify.org#dataType");
    public static final DoubleSingleValueOpenLumifyProperty BOOST = new DoubleSingleValueOpenLumifyProperty("http://openlumify.org#boost");
    public static final IntegerSingleValueOpenLumifyProperty SORT_PRIORITY = new IntegerSingleValueOpenLumifyProperty("http://openlumify.org#sortPriority");
    public static final JsonSingleValueOpenLumifyProperty POSSIBLE_VALUES = new JsonSingleValueOpenLumifyProperty("http://openlumify.org#possibleValues");
    public static final BooleanSingleValueOpenLumifyProperty DELETEABLE = new BooleanSingleValueOpenLumifyProperty("http://openlumify.org#deleteable");
    public static final BooleanSingleValueOpenLumifyProperty UPDATEABLE = new BooleanSingleValueOpenLumifyProperty("http://openlumify.org#updateable");

    public static final Set<String> CHANGEABLE_PROPERTY_IRI = ImmutableSet.of(
            DISPLAY_TYPE.getPropertyName(),
            USER_VISIBLE.getPropertyName(),
            DELETEABLE.getPropertyName(),
            UPDATEABLE.getPropertyName(),
            ADDABLE.getPropertyName(),
            SORTABLE.getPropertyName(),
            SORT_PRIORITY.getPropertyName(),
            SEARCHABLE.getPropertyName(),
            INTENT.getPropertyName(),
            POSSIBLE_VALUES.getPropertyName(),
            COLOR.getPropertyName(),
            SUBTITLE_FORMULA.getPropertyName(),
            TIME_FORMULA.getPropertyName(),
            TITLE_FORMULA.getPropertyName(),
            VALIDATION_FORMULA.getPropertyName(),
            PROPERTY_GROUP.getPropertyName(),
            DISPLAY_FORMULA.getPropertyName(),
            GLYPH_ICON_FILE_NAME.getPropertyName(),
            GLYPH_ICON.getPropertyName(),
            GLYPH_ICON_SELECTED_FILE_NAME.getPropertyName(),
            GLYPH_ICON_SELECTED.getPropertyName(),
            MAP_GLYPH_ICON.getPropertyName(),
            MAP_GLYPH_ICON_FILE_NAME.getPropertyName(),
            ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName()
    );
}