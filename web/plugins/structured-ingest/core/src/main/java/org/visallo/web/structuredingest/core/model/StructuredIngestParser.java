package org.visallo.web.structuredingest.core.model;

import org.visallo.web.structuredingest.core.util.BaseStructuredFileParserHandler;

import java.io.InputStream;
import java.util.Set;

public interface StructuredIngestParser {

    Set<String> getSupportedMimeTypes();

    void ingest(StructuredIngestInputStreamSource inputStreamSource, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception;

    ClientApiAnalysis analyze(StructuredIngestInputStreamSource inputStreamSource) throws Exception;
}
