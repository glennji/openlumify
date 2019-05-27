package org.openlumify.vertexium.model.ontology;

import org.vertexium.Authorizations;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.ontology.OntologyProperties;
import org.openlumify.core.user.User;
import org.openlumify.core.util.JSONUtil;
import org.openlumify.web.clientapi.model.SandboxStatus;

import java.util.*;

public class InMemoryConcept extends Concept {
    private String title;
    private String color;
    private String displayName;
    private String displayType;
    private String titleFormula;
    private String subtitleFormula;
    private String timeFormula;
    private String conceptIRI;
    private List<String> addRelatedConceptWhiteList;
    private byte[] glyphIcon;
    private String glyphIconFilePath;
    private byte[] glyphIconSelected;
    private String glyphIconSelectedFilePath;
    private byte[] mapGlyphIcon;
    private boolean userVisible = true;
    private boolean updateable = true;
    private boolean deleteable = true;
    private Boolean searchable;
    private Boolean addable;
    private Map<String, String> metadata = new HashMap<>();
    private Set<String> intents = new HashSet<>();
    private String workspaceId;

    public InMemoryConcept(String conceptIRI, String parentIRI, String workspaceId) {
        super(parentIRI, new ArrayList<>());
        this.conceptIRI = conceptIRI;
        this.workspaceId = workspaceId;
    }

    InMemoryConcept shallowCopy() {
        InMemoryConcept other = new InMemoryConcept(conceptIRI, getParentConceptIRI(), workspaceId);
        other.getProperties().addAll(getProperties());
        other.title = title;
        other.color = color;
        other.displayName = displayName;
        other.displayType = displayType;
        other.titleFormula = titleFormula;
        other.subtitleFormula = subtitleFormula;
        other.timeFormula = timeFormula;
        other.addRelatedConceptWhiteList = addRelatedConceptWhiteList;
        other.glyphIcon = glyphIcon;
        other.glyphIconFilePath = glyphIconFilePath;
        other.glyphIconSelected = glyphIconSelected;
        other.glyphIconSelectedFilePath = glyphIconSelectedFilePath;
        other.mapGlyphIcon = mapGlyphIcon;
        other.userVisible = userVisible;
        other.updateable = updateable;
        other.deleteable = deleteable;
        other.searchable = searchable;
        other.addable = addable;
        other.metadata.putAll(metadata);
        other.intents.addAll(intents);
        return other;
    }

    @Override
    public String getIRI() {
        return this.conceptIRI;
    }

    @Override
    public String[] getIntents() {
        return this.intents.toArray(new String[this.intents.size()]);
    }

    @Override
    public void addIntent(String intent, User user, Authorizations authorizations) {
        this.intents.add(intent);
    }

    @Override
    public void removeIntent(String intent, Authorizations authorizations) {
        this.intents.remove(intent);
    }

    @Override
    public String getId() {
        return this.conceptIRI;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean hasGlyphIconResource() {
        return glyphIcon != null || glyphIconFilePath != null;
    }

    @Override
    public boolean hasGlyphIconSelectedResource() {
        return glyphIconSelected != null || glyphIconSelectedFilePath != null;
    }

    @Override
    public String getColor() {
        return color;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDisplayType() {
        return displayType;
    }

    @Override
    public String getTitleFormula() {
        return titleFormula;
    }

    @Override
    public Boolean getSearchable() {
        return searchable;
    }

    @Override
    public String getSubtitleFormula() {
        return this.subtitleFormula;
    }

    @Override
    public String getTimeFormula() {
        return this.timeFormula;
    }

    @Override
    public boolean getUserVisible() {
        return userVisible;
    }

    @Override
    public boolean getDeleteable() {
        return deleteable;
    }

    @Override
    public boolean getUpdateable() {
        return updateable;
    }

    @Override
    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    @Override
    public List<String> getAddRelatedConceptWhiteList() {
        return addRelatedConceptWhiteList;
    }

    @Override
    public void setProperty(String name, Object value, User user, Authorizations authorizations) {
        if (OntologyProperties.COLOR.getPropertyName().equals(name)) {
            this.color = (String) value;
        } else if (OntologyProperties.DISPLAY_TYPE.getPropertyName().equals(name)) {
            this.displayType = (String) value;
        } else if (OntologyProperties.TITLE_FORMULA.getPropertyName().equals(name)) {
            this.titleFormula = (String) value;
        } else if (OntologyProperties.SUBTITLE_FORMULA.getPropertyName().equals(name)) {
            this.subtitleFormula = (String) value;
        } else if (OntologyProperties.TIME_FORMULA.getPropertyName().equals(name)) {
            this.timeFormula = (String) value;
        } else if (OntologyProperties.USER_VISIBLE.getPropertyName().equals(name)) {
            this.userVisible = (Boolean) value;
        } else if (OntologyProperties.GLYPH_ICON.getPropertyName().equals(name)) {
            this.glyphIcon = (byte[]) value;
        } else if (OntologyProperties.GLYPH_ICON_FILE_NAME.getPropertyName().equals(name)) {
            this.glyphIconFilePath = (String) value;
        } else if (OntologyProperties.GLYPH_ICON_SELECTED.getPropertyName().equals(name)) {
            this.glyphIconSelected = (byte[]) value;
        } else if (OntologyProperties.GLYPH_ICON_SELECTED_FILE_NAME.getPropertyName().equals(name)) {
            this.glyphIconSelectedFilePath = (String) value;
        } else if (OntologyProperties.MAP_GLYPH_ICON.getPropertyName().equals(name)) {
            this.mapGlyphIcon = (byte[]) value;
        } else if (OntologyProperties.TITLE.getPropertyName().equals(name)) {
            this.title = (String) value;
        } else if (OntologyProperties.DISPLAY_NAME.getPropertyName().equals(name)) {
            this.displayName = (String) value;
        } else if (OntologyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName().equals(name)) {
            this.addRelatedConceptWhiteList = JSONUtil.toStringList(JSONUtil.parseArray((String) value));
        } else if (OntologyProperties.SEARCHABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.searchable = (Boolean) value;
            } else {
                this.searchable = Boolean.parseBoolean((String) value);
            }
        } else if (OntologyProperties.ADDABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.addable = (Boolean) value;
            } else {
                this.addable = Boolean.parseBoolean((String) value);
            }
        } else if (OntologyProperties.UPDATEABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.updateable = (Boolean) value;
            } else {
                this.updateable = Boolean.parseBoolean((String) value);
            }
        } else if (OntologyProperties.DELETEABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.deleteable = (Boolean) value;
            } else {
                this.deleteable = Boolean.parseBoolean((String) value);
            }
        } else if (value != null) {
            metadata.put(name, value.toString());
        }
    }

    @Override
    public void removeProperty(String name, Authorizations authorizations) {
        if (OntologyProperties.COLOR.getPropertyName().equals(name)) {
            this.color = null;
        } else if (OntologyProperties.DISPLAY_TYPE.getPropertyName().equals(name)) {
            this.displayType = null;
        } else if (OntologyProperties.TITLE_FORMULA.getPropertyName().equals(name)) {
            this.titleFormula = null;
        } else if (OntologyProperties.SUBTITLE_FORMULA.getPropertyName().equals(name)) {
            this.subtitleFormula = null;
        } else if (OntologyProperties.TIME_FORMULA.getPropertyName().equals(name)) {
            this.timeFormula = null;
        } else if (OntologyProperties.USER_VISIBLE.getPropertyName().equals(name)) {
            this.userVisible = true;
        } else if (OntologyProperties.GLYPH_ICON.getPropertyName().equals(name)) {
            this.glyphIcon = null;
        } else if (OntologyProperties.GLYPH_ICON_FILE_NAME.getPropertyName().equals(name)) {
            this.glyphIconFilePath = null;
        } else if (OntologyProperties.GLYPH_ICON_SELECTED.getPropertyName().equals(name)) {
            this.glyphIconSelected = null;
        } else if (OntologyProperties.GLYPH_ICON_SELECTED_FILE_NAME.getPropertyName().equals(name)) {
            this.glyphIconSelectedFilePath = null;
        } else if (OntologyProperties.MAP_GLYPH_ICON.getPropertyName().equals(name)) {
            this.mapGlyphIcon = null;
        } else if (OntologyProperties.TITLE.getPropertyName().equals(name)) {
            this.title = null;
        } else if (OntologyProperties.DISPLAY_NAME.getPropertyName().equals(name)) {
            this.displayName = null;
        } else if (OntologyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName().equals(name)) {
            this.addRelatedConceptWhiteList = Collections.emptyList();
        } else if (OntologyProperties.SEARCHABLE.getPropertyName().equals(name)) {
            this.searchable = null;
        } else if (OntologyProperties.ADDABLE.getPropertyName().equals(name)) {
            this.addable = null;
        } else if (OntologyProperties.UPDATEABLE.getPropertyName().equals(name)) {
            this.updateable = true;
        } else if (OntologyProperties.DELETEABLE.getPropertyName().equals(name)) {
            this.deleteable = true;
        } else if (OntologyProperties.INTENT.getPropertyName().equals(name)) {
            intents.clear();
        } else if (metadata.containsKey(name)) {
            metadata.remove(name);
        }
    }

    @Override
    public byte[] getGlyphIcon() {
        return glyphIcon;
    }

    @Override
    public byte[] getGlyphIconSelected() {
        return glyphIconSelected;
    }

    @Override
    public String getGlyphIconFilePath() {
        return glyphIconFilePath;
    }

    @Override
    public String getGlyphIconSelectedFilePath() {
        return glyphIconSelectedFilePath;
    }

    @Override
    public byte[] getMapGlyphIcon() {
        return mapGlyphIcon;
    }

    public String getConceptIRI() {
        return conceptIRI;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void removeWorkspaceId() {
        workspaceId = null;
    }

    void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public SandboxStatus getSandboxStatus() {
        return workspaceId == null ? SandboxStatus.PUBLIC : SandboxStatus.PRIVATE;
    }
}
