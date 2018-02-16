package org.visallo.web.structuredingest.spreadsheet;

import com.google.common.collect.Sets;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.structuredingest.core.model.ClientApiAnalysis;
import org.visallo.web.structuredingest.core.model.StructuredIngestInputStreamSource;
import org.visallo.web.structuredingest.core.util.StructuredFileAnalyzerHandler;
import org.visallo.web.structuredingest.core.model.StructuredIngestParser;
import org.visallo.web.structuredingest.core.util.BaseStructuredFileParserHandler;
import org.visallo.web.structuredingest.core.model.ParseOptions;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class CsvParser extends BaseParser implements StructuredIngestParser {

    private final static String CSV_MIME_TYPE = "text/csv";

    @Override
    public Set<String> getSupportedMimeTypes() {
        return Sets.newHashSet(CSV_MIME_TYPE);
    }

    @Override
    public void ingest(StructuredIngestInputStreamSource source, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception {
        parseCsvSheet(source, parseOptions, parserHandler);
    }

    @Override
    public ClientApiAnalysis analyze(StructuredIngestInputStreamSource source) {
        StructuredFileAnalyzerHandler handler = new StructuredFileAnalyzerHandler();
        handler.getHints().sendColumnIndices = true;
        handler.getHints().allowHeaderSelection = true;

        ParseOptions options = new ParseOptions();
        options.hasHeaderRow = false;
        parseCsvSheet(source, options, handler);
        return handler.getResult();
    }

    private int getTotalRows(StructuredIngestInputStreamSource source, ParseOptions options) {
        try (
                InputStream in = source.getInputStream();
                Reader reader = new InputStreamReader(in)
        ) {
            int row = 0;
            try (CSVReader csvReader = getReader(reader, options)) {
                Iterator<String[]> rowIterator = csvReader.iterator();
                while (rowIterator.hasNext()) {
                    if (!rowIsBlank(rowIterator.next())) {
                        row++;
                    }
                }
            }
            return row;
        } catch (IOException e) {
            throw new VisalloException("Could not read csv", e);
        }
    }

    private void parseCsvSheet(StructuredIngestInputStreamSource source, ParseOptions options, BaseStructuredFileParserHandler handler) {
        handler.newSheet("");
        handler.setTotalRows(getTotalRows(source, options));

        read(source, options, handler, true);
        handler.prepareFinished();
        read(source, options, handler, false);
        handler.cleanup();
    }

    private CSVReader getReader(Reader reader, ParseOptions options) {
        CSVParser parser = new CSVParserBuilder()
                .withQuoteChar(options.quoteChar)
                .withSeparator(options.separator)
                .build();
        CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(reader).withCSVParser(parser);
        return csvReaderBuilder.build();
    }

    private void read(StructuredIngestInputStreamSource source, ParseOptions options, BaseStructuredFileParserHandler handler, boolean prepare) {
        try (
                InputStream in = source.getInputStream();
                Reader reader = new InputStreamReader(in)
        ) {
            int row = 0;

            try (CSVReader csvReader = getReader(reader, options)) {
                String[] columnValues;

                while ((columnValues = csvReader.readNext()) != null) {
                    if (row < options.startRowIndex) {
                        row++;
                        continue;
                    }
                    if (rowIsBlank(columnValues)) {
                        continue;
                    }

                    if (row == options.startRowIndex && options.hasHeaderRow) {
                        if (prepare) {
                            for (String headerColumn : columnValues) {
                                handler.addColumn(headerColumn);
                            }
                        }
                    } else {
                        if (prepare) {
                            if (!handler.prepareRow(Arrays.asList(columnValues), row)) {
                                break;
                            }
                        } else if (!handler.addRow(Arrays.asList(columnValues), row)) {
                            break;
                        }
                    }
                    row++;
                }
            }
        } catch (IOException ex) {
            throw new VisalloException("Could not read csv", ex);
        }
    }
}

