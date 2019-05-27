package org.openlumify.core.model.ontology;

import com.google.common.collect.ImmutableList;

public interface ExtendedDataTableProperty {
    String getIri();

    String getTitleFormula();

    String getSubtitleFormula();

    String getTimeFormula();

    ImmutableList<String> getTablePropertyIris();
}
