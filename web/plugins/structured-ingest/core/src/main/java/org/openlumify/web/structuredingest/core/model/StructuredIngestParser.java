package org.openlumify.web.structuredingest.core.model;

import org.openlumify.web.structuredingest.core.util.BaseStructuredFileParserHandler;

import java.io.InputStream;
import java.util.Set;

public interface StructuredIngestParser {

    Set<String> getSupportedMimeTypes();

    void ingest(InputStream inputStream, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception;

    ClientApiAnalysis analyze(InputStream inputStream) throws Exception;
}
