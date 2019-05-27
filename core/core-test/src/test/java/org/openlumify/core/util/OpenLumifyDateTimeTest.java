package org.openlumify.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OpenLumifyDateTimeTest {
    @Test
    public void testGetHumanTimeAgo() {
        assertEquals("0 ms ago", OpenLumifyDateTime.getHumanTimeAgo(0));
        assertEquals("1 ms ago", OpenLumifyDateTime.getHumanTimeAgo(1));
        assertEquals("999 ms ago", OpenLumifyDateTime.getHumanTimeAgo(999));
        assertEquals("1 seconds ago", OpenLumifyDateTime.getHumanTimeAgo(1000));
        assertEquals("1 minutes ago", OpenLumifyDateTime.getHumanTimeAgo(60 * 1000));
        assertEquals("1 hours ago", OpenLumifyDateTime.getHumanTimeAgo(60 * 60 * 1000));
        assertEquals("1 days ago", OpenLumifyDateTime.getHumanTimeAgo(24 * 60 * 60 * 1000));
        assertEquals("5 days ago", OpenLumifyDateTime.getHumanTimeAgo(5 * 24 * 60 * 60 * 1000));
    }
}