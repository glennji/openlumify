package org.openlumify.web.clientapi;

import org.openlumify.web.clientapi.codegen.ApiException;
import org.openlumify.web.clientapi.codegen.LongrunningprocessApi;
import org.openlumify.web.clientapi.model.ClientApiLongRunningProcess;
import org.openlumify.web.clientapi.util.ClientApiConverter;
import org.json.JSONObject;

public class LongRunningProcessApiExt extends LongrunningprocessApi {
    public ClientApiLongRunningProcess waitById(String longRunningProcessId) throws ApiException {
        while (true) {
            ClientApiLongRunningProcess longRunningProcess = findById(longRunningProcessId);
            if (longRunningProcess.getEndTime() != null || longRunningProcess.getError() != null || longRunningProcess.isCanceled()) {
                return longRunningProcess;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException("Could not sleep", e);
            }
        }
    }

    public ClientApiLongRunningProcess findById(String longRunningProcessId) throws ApiException {
        String s = findByIdRaw(longRunningProcessId);
        ClientApiLongRunningProcess proc = ClientApiConverter.toClientApi(s, ClientApiLongRunningProcess.class);
        if (proc != null) {
            JSONObject json = new JSONObject(s);
            String resultString = json.optString("results");
            proc.setResultsString(resultString);
        }
        return proc;
    }
}
