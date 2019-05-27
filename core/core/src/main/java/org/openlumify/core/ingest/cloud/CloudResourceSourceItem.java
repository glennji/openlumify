package org.openlumify.core.ingest.cloud;

import java.io.InputStream;

public interface CloudResourceSourceItem {

    InputStream getInputStream();
    String getName();
    Long getSize();

}
