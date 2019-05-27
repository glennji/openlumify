package org.openlumify.web.structuredingest.core.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.structuredingest.core.model.StructuredIngestParser;

import java.util.Collection;
import java.util.Set;

@Singleton
public class StructuredIngestParserFactory {

    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(StructuredIngestParserFactory.class);

    private final Configuration configuration;

    @Inject
    public StructuredIngestParserFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    public Collection<StructuredIngestParser> getParsers() {
        return InjectHelper.getInjectedServices(StructuredIngestParser.class, configuration);
    }

    public StructuredIngestParser getParser(String mimeType) {
        Collection<StructuredIngestParser> parsers = getParsers();
        for (StructuredIngestParser parser : parsers) {
            Set<String> supported = parser.getSupportedMimeTypes();
            if (supported.size() == 0) {
                LOGGER.warn("Parsers should support at least one mimeType: %s", parser.getClass().getName());
            } else if (supported.stream().anyMatch(s -> s.toLowerCase().equals(mimeType.toLowerCase()))) {
                return parser;
            }
        }
        return null;
    }
}
