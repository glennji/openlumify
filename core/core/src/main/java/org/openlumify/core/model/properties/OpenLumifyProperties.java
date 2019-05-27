package org.openlumify.core.model.properties;

import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.properties.types.*;
import org.openlumify.core.model.termMention.TermMentionForProperty;

import java.lang.reflect.Field;

public class OpenLumifyProperties {
    public static final String CONCEPT_TYPE_THING = "http://www.w3.org/2002/07/owl#Thing";
    public static final String EDGE_LABEL_HAS_SOURCE = "http://openlumify.org#hasSource";
    public static final String GEO_LOCATION_RANGE = "http://openlumify.org#geolocation";

    public static final StringMetadataOpenLumifyProperty LANGUAGE_METADATA = new StringMetadataOpenLumifyProperty("http://openlumify.org#language");
    public static final StringMetadataOpenLumifyProperty TEXT_DESCRIPTION_METADATA = new StringMetadataOpenLumifyProperty("http://openlumify.org#textDescription");
    public static final StringMetadataOpenLumifyProperty MIME_TYPE_METADATA = new StringMetadataOpenLumifyProperty("http://openlumify.org#mimeType");
    public static final StringMetadataOpenLumifyProperty SOURCE_FILE_NAME_METADATA = new StringMetadataOpenLumifyProperty("http://openlumify.org#sourceFileName");
    public static final StringMetadataOpenLumifyProperty LINK_TITLE_METADATA = new StringMetadataOpenLumifyProperty("http://openlumify.org#linkTitle");
    public static final LongMetadataOpenLumifyProperty SOURCE_FILE_OFFSET_METADATA = new LongMetadataOpenLumifyProperty("http://openlumify.org#sourceFileOffset");
    public static final DerivedFromMetadataOpenLumifyProperty DERIVED_FROM_METADATA = new DerivedFromMetadataOpenLumifyProperty("http://openlumify.org#derivedFrom");

    public static final DateSingleValueOpenLumifyProperty MODIFIED_DATE = new DateSingleValueOpenLumifyProperty("http://openlumify.org#modifiedDate");
    public static final DateMetadataOpenLumifyProperty MODIFIED_DATE_METADATA = new DateMetadataOpenLumifyProperty("http://openlumify.org#modifiedDate");

    public static final DoubleMetadataOpenLumifyProperty CONFIDENCE_METADATA = new DoubleMetadataOpenLumifyProperty("http://openlumify.org#confidence");

    public static final VisibilityJsonOpenLumifyProperty VISIBILITY_JSON = new VisibilityJsonOpenLumifyProperty("http://openlumify.org#visibilityJson");
    public static final VisibilityJsonMetadataOpenLumifyProperty VISIBILITY_JSON_METADATA = new VisibilityJsonMetadataOpenLumifyProperty("http://openlumify.org#visibilityJson");

    public static final StreamingSingleValueOpenLumifyProperty METADATA_JSON = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#metadataJson");
    public static final StreamingOpenLumifyProperty TEXT = new StreamingOpenLumifyProperty("http://openlumify.org#text");
    public static final StreamingSingleValueOpenLumifyProperty RAW = new StreamingSingleValueOpenLumifyProperty("http://openlumify.org#raw");
    public static final StreamingOpenLumifyProperty CACHED_IMAGE = new StreamingOpenLumifyProperty("http://openlumify.org#cached-image");

    public static final StringOpenLumifyProperty GRAPH_PROPERTY_WORKER_WHITE_LIST = new StringOpenLumifyProperty("http://openlumify.org#graphPropertyWorkerWhiteList");
    public static final StringOpenLumifyProperty GRAPH_PROPERTY_WORKER_BLACK_LIST = new StringOpenLumifyProperty("http://openlumify.org#graphPropertyWorkerBlackList");
    public static final ConceptTypeSingleValueOpenLumifyProperty CONCEPT_TYPE = new ConceptTypeSingleValueOpenLumifyProperty("http://openlumify.org#conceptType");
    public static final StringOpenLumifyProperty CONTENT_HASH = new StringOpenLumifyProperty("http://openlumify.org#contentHash");
    public static final StringOpenLumifyProperty FILE_NAME = new StringOpenLumifyProperty("http://openlumify.org#fileName");
    public static final StringOpenLumifyProperty ENTITY_IMAGE_URL = new StringOpenLumifyProperty("http://openlumify.org#entityImageUrl");
    public static final StringSingleValueOpenLumifyProperty ENTITY_IMAGE_VERTEX_ID = new StringSingleValueOpenLumifyProperty("http://openlumify.org#entityImageVertexId");
    public static final StringOpenLumifyProperty MIME_TYPE = new StringOpenLumifyProperty("http://openlumify.org#mimeType");
    public static final StringSingleValueOpenLumifyProperty MODIFIED_BY = new StringSingleValueOpenLumifyProperty("http://openlumify.org#modifiedBy");
    public static final StringMetadataOpenLumifyProperty MODIFIED_BY_METADATA = new StringMetadataOpenLumifyProperty("http://openlumify.org#modifiedBy");
    public static final PropertyJustificationMetadataSingleValueOpenLumifyProperty JUSTIFICATION = new PropertyJustificationMetadataSingleValueOpenLumifyProperty("http://openlumify.org#justification");
    public static final PropertyJustificationMetadataMetadataOpenLumifyProperty JUSTIFICATION_METADATA = new PropertyJustificationMetadataMetadataOpenLumifyProperty("http://openlumify.org#justification");
    public static final StringOpenLumifyProperty PROCESS = new StringOpenLumifyProperty("http://openlumify.org#process");
    public static final StringOpenLumifyProperty ROW_KEY = new StringOpenLumifyProperty("http://openlumify.org#rowKey");
    public static final StringOpenLumifyProperty SOURCE = new StringOpenLumifyProperty("http://openlumify.org#source");
    public static final StringOpenLumifyProperty SOURCE_URL = new StringOpenLumifyProperty("http://openlumify.org#sourceUrl");
    public static final StringOpenLumifyProperty TITLE = new StringOpenLumifyProperty("http://openlumify.org#title");
    public static final StringOpenLumifyProperty COMMENT = new StringOpenLumifyProperty("http://openlumify.org/comment#entry");
    public static final StringMetadataOpenLumifyProperty COMMENT_PATH_METADATA = new StringMetadataOpenLumifyProperty("http://openlumify.org/comment#path");

    public static final DetectedObjectProperty DETECTED_OBJECT = new DetectedObjectProperty("http://openlumify.org#detectedObject");

    public static final LongSingleValueOpenLumifyProperty TERM_MENTION_START_OFFSET = new LongSingleValueOpenLumifyProperty("http://openlumify.org/termMention#startOffset");
    public static final LongSingleValueOpenLumifyProperty TERM_MENTION_END_OFFSET = new LongSingleValueOpenLumifyProperty("http://openlumify.org/termMention#endOffset");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_PROCESS = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#process");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_PROPERTY_KEY = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#propertyKey");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_PROPERTY_NAME = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#propertyName");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_RESOLVED_EDGE_ID = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#resolvedEdgeId");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_TITLE = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#title");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_CONCEPT_TYPE = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#conceptType");
    public static final VisibilityJsonOpenLumifyProperty TERM_MENTION_VISIBILITY_JSON = new VisibilityJsonOpenLumifyProperty("http://openlumify.org/termMention#visibilityJson");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_REF_PROPERTY_KEY = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#ref/propertyKey");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_REF_PROPERTY_NAME = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#ref/propertyName");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_REF_PROPERTY_VISIBILITY = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#ref/propertyVisibility");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_FOR_ELEMENT_ID = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#forElementId");
    public static final TermMentionForProperty TERM_MENTION_FOR_TYPE = new TermMentionForProperty("http://openlumify.org/termMention#forType");
    public static final StringSingleValueOpenLumifyProperty TERM_MENTION_SNIPPET = new StringSingleValueOpenLumifyProperty("http://openlumify.org/termMention#snippet");
    public static final String TERM_MENTION_LABEL_HAS_TERM_MENTION = "http://openlumify.org/termMention#hasTermMention";
    public static final String TERM_MENTION_LABEL_RESOLVED_TO = "http://openlumify.org/termMention#resolvedTo";
    public static final String TERM_MENTION_RESOLVED_FROM = "http://openlumify.org/termMention#resolvedFrom";

    private OpenLumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }

    public static boolean isBuiltInProperty(String propertyName) {
        return isBuiltInProperty(OpenLumifyProperties.class, propertyName);
    }

    public static boolean isBuiltInProperty(Class propertiesClass, String propertyName) {
        for (Field field : propertiesClass.getFields()) {
            try {
                Object fieldValue = field.get(null);
                if (fieldValue instanceof OpenLumifyPropertyBase) {
                    if (((OpenLumifyPropertyBase) fieldValue).getPropertyName().equals(propertyName)) {
                        return true;
                    }
                }
            } catch (IllegalAccessException e) {
                throw new OpenLumifyException("Could not get field: " + field, e);
            }
        }
        return false;
    }
}
