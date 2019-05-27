package org.openlumify.vertexium.model.ontology;

import com.google.common.collect.ImmutableList;
import org.vertexium.Vertex;
import org.openlumify.core.model.ontology.ExtendedDataTableProperty;
import org.openlumify.core.model.ontology.OntologyProperties;

import java.util.LinkedHashSet;

public class VertexiumExtendedDataTableOntologyProperty extends VertexiumOntologyProperty implements ExtendedDataTableProperty {
    private LinkedHashSet<String> tablePropertyIris = new LinkedHashSet<>();

    public VertexiumExtendedDataTableOntologyProperty(Vertex vertex, ImmutableList<String> dependentPropertyIris, String workspaceId) {
        super(vertex, dependentPropertyIris, workspaceId);
    }

    @Override
    public String getTitleFormula() {
        return OntologyProperties.TITLE_FORMULA.getPropertyValue(getVertex());
    }

    @Override
    public String getSubtitleFormula() {
        return OntologyProperties.SUBTITLE_FORMULA.getPropertyValue(getVertex());
    }

    @Override
    public String getTimeFormula() {
        return OntologyProperties.TIME_FORMULA.getPropertyValue(getVertex());
    }

    @Override
    public ImmutableList<String> getTablePropertyIris() {
        return ImmutableList.copyOf(tablePropertyIris);
    }

    void addProperty(String tablePropertyIri) {
        this.tablePropertyIris.add(tablePropertyIri);
    }
}
