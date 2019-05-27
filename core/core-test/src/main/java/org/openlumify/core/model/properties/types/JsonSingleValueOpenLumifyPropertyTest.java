package org.openlumify.core.model.properties.types;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonSingleValueOpenLumifyPropertyTest {
    @Test
    public void testEquals() {
        JsonSingleValueOpenLumifyProperty prop = new JsonSingleValueOpenLumifyProperty("name");
        assertTrue(prop.isEquals(new JSONObject("{a:1}"), new JSONObject("{a:1}")));
        assertFalse(prop.isEquals(new JSONObject("{a:1}"), new JSONObject("{a:1,b:2}")));
    }
}