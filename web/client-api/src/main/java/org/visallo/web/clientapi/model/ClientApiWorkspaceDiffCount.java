package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspaceDiffCount implements ClientApiObject {
    private long total;
    private List<String> ids;

    public ClientApiWorkspaceDiffCount(long total, List<String> ids) {
        this.total = total;
        this.ids = ids;
    }

    public long getTotal() {
        return total;
    }

    public List<String> getIds() {
        return ids;
    }
}
