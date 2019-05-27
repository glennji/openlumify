package org.openlumify.tikaMimeType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.openlumify.core.ingest.graphProperty.MimeTypeGraphPropertyWorker;
import org.openlumify.core.ingest.graphProperty.MimeTypeGraphPropertyWorkerConfiguration;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;

import java.io.InputStream;

@Name("Tika MIME Type")
@Description("Uses Apache Tika to determine MIME type")
@Singleton
public class TikaMimeTypeGraphPropertyWorker extends MimeTypeGraphPropertyWorker {
    private TikaMimeTypeMapper mimeTypeMapper;

    @Inject
    public TikaMimeTypeGraphPropertyWorker(MimeTypeGraphPropertyWorkerConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        mimeTypeMapper = new TikaMimeTypeMapper();
    }

    public String getMimeType(InputStream in, String fileName) throws Exception {
        String mimeType = mimeTypeMapper.guessMimeType(in, fileName);
        if (mimeType == null) {
            return null;
        }
        return mimeType;
    }
}
