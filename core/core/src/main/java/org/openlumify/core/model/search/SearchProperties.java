package org.openlumify.core.model.search;

import org.openlumify.core.model.properties.types.JsonSingleValueOpenLumifyProperty;
import org.openlumify.core.model.properties.types.StringSingleValueOpenLumifyProperty;

public class SearchProperties {
    public static final String IRI = "http://openlumify.org/search";

    public static final String HAS_SAVED_SEARCH = "http://openlumify.org/search#hasSavedSearch";

    public static final String CONCEPT_TYPE_SAVED_SEARCH = "http://openlumify.org/search#savedSearch";
    public static final String CONCEPT_TYPE_GLOBAL_SAVED_SEARCH_ROOT = "http://openlumify.org/search#globalSavedSearchRoot";

    public static final StringSingleValueOpenLumifyProperty NAME = new StringSingleValueOpenLumifyProperty("http://openlumify.org/search#name");
    public static final StringSingleValueOpenLumifyProperty URL = new StringSingleValueOpenLumifyProperty("http://openlumify.org/search#url");
    public static final JsonSingleValueOpenLumifyProperty PARAMETERS = new JsonSingleValueOpenLumifyProperty("http://openlumify.org/search#parameters");
}
