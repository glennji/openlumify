package org.openlumify.core.model.termMention;

import org.openlumify.core.model.properties.types.SingleValueOpenLumifyProperty;

public class TermMentionForProperty extends SingleValueOpenLumifyProperty<TermMentionFor, String> {
    public TermMentionForProperty(final String key) {
        super(key);
    }

    @Override
    public String wrap(TermMentionFor value) {
        return value.toString();
    }

    @Override
    public TermMentionFor unwrap(final Object value) {
        if (value == null) {
            return null;
        }
        return TermMentionFor.valueOf(value.toString());
    }
}

