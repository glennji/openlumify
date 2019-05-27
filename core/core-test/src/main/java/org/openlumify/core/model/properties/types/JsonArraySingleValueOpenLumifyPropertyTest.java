package org.openlumify.core.model.properties.types;

import org.json.JSONArray;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonArraySingleValueOpenLumifyPropertyTest {
    @Test
    public void testEquals() {
        JsonArraySingleValueOpenLumifyProperty prop = new JsonArraySingleValueOpenLumifyProperty("name");
        assertTrue(prop.isEquals(new JSONArray("[1,2]"), new JSONArray("[1,2]")));
        assertFalse(prop.isEquals(new JSONArray("[1,2]"), new JSONArray("[1,2,3]")));
    }
}