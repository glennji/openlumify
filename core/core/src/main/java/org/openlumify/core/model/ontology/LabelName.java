package org.openlumify.core.model.ontology;

public enum LabelName {
    HAS_PROPERTY("http://openlumify.org/ontology#hasProperty"),
    HAS_EDGE("http://openlumify.org/ontology#hasEdge"),
    IS_A("http://openlumify.org/ontology#isA"),
    INVERSE_OF("http://openlumify.org/ontology#inverseOf");

    private final String text;

    LabelName(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
