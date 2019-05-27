package org.openlumify.core.model.workspace;

import org.openlumify.core.model.properties.types.BooleanSingleValueOpenLumifyProperty;
import org.openlumify.core.model.properties.types.StreamingOpenLumifyProperty;
import org.openlumify.core.model.properties.types.StringSingleValueOpenLumifyProperty;
import org.openlumify.core.model.properties.types.StringOpenLumifyProperty;

public class WorkspaceProperties {
    public static final String WORKSPACE_CONCEPT_IRI = "http://openlumify.org/workspace#workspace";
    public static final String DASHBOARD_CONCEPT_IRI = "http://openlumify.org/workspace#dashboard";
    public static final String PRODUCT_CONCEPT_IRI = "http://openlumify.org/workspace#product";
    public static final String DASHBOARD_ITEM_CONCEPT_IRI = "http://openlumify.org/workspace#dashboardItem";

    public static final String WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI = "http://openlumify.org/workspace#toEntity";
    public static final String WORKSPACE_TO_ONTOLOGY_RELATIONSHIP_IRI = "http://openlumify.org/workspace#toOntology";
    public static final String WORKSPACE_TO_USER_RELATIONSHIP_IRI = "http://openlumify.org/workspace#toUser";
    public static final String WORKSPACE_TO_DASHBOARD_RELATIONSHIP_IRI = "http://openlumify.org/workspace#toDashboard";
    public static final String WORKSPACE_TO_PRODUCT_RELATIONSHIP_IRI = "http://openlumify.org/workspace#toProduct";

    public static final String PRODUCT_TO_ENTITY_RELATIONSHIP_IRI = "http://openlumify.org/workspace/product#toEntity";
    public static final BooleanSingleValueOpenLumifyProperty PRODUCT_TO_ENTITY_IS_ANCILLARY = new BooleanSingleValueOpenLumifyProperty("http://openlumify.org/workspace/product#toEntity/ancillary");

    public static final String DASHBOARD_TO_DASHBOARD_ITEM_RELATIONSHIP_IRI = "http://openlumify.org/workspace#toDashboardItem";

    public static final StringSingleValueOpenLumifyProperty TITLE = new StringSingleValueOpenLumifyProperty("http://openlumify.org/workspace#workspace/title");
    public static final BooleanSingleValueOpenLumifyProperty WORKSPACE_TO_USER_IS_CREATOR = new BooleanSingleValueOpenLumifyProperty("http://openlumify.org/workspace#toUser/creator");
    public static final StringSingleValueOpenLumifyProperty WORKSPACE_TO_USER_ACCESS = new StringSingleValueOpenLumifyProperty("http://openlumify.org/workspace#toUser/access");
    public static final StringSingleValueOpenLumifyProperty LAST_ACTIVE_PRODUCT_ID = new StringSingleValueOpenLumifyProperty("http://openlumify.org/workspace#lastActiveProductId");

    public static final StringSingleValueOpenLumifyProperty DASHBOARD_ITEM_EXTENSION_ID = new StringSingleValueOpenLumifyProperty("http://openlumify.org/workspace#extensionId");
    public static final StringSingleValueOpenLumifyProperty DASHBOARD_ITEM_CONFIGURATION = new StringSingleValueOpenLumifyProperty("http://openlumify.org/workspace#configuration");

    public static final StringSingleValueOpenLumifyProperty PRODUCT_KIND = new StringSingleValueOpenLumifyProperty("http://openlumify.org/product#kind");
    public static final StringOpenLumifyProperty PRODUCT_DATA = new StringOpenLumifyProperty("http://openlumify.org/product#data");
    public static final StringOpenLumifyProperty PRODUCT_EXTENDED_DATA = new StringOpenLumifyProperty("http://openlumify.org/product#extendedData");
    public static final StreamingOpenLumifyProperty PRODUCT_PREVIEW_DATA_URL = new StreamingOpenLumifyProperty("http://openlumify.org/product#previewDataUrl");

}
