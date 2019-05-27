package org.openlumify.core.model.search;

import org.apache.commons.math3.util.Precision;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.query.*;
import org.vertexium.type.GeoShape;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.directory.DirectoryRepository;
import org.openlumify.core.model.ontology.OntologyProperty;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.trace.Trace;
import org.openlumify.core.trace.TraceSpan;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.core.util.JSONUtil;
import org.openlumify.web.clientapi.model.PropertyType;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

public abstract class VertexiumObjectSearchRunnerBase extends SearchRunner {
    private final Graph graph;
    private final DirectoryRepository directoryRepository;
    private final OntologyRepository ontologyRepository;
    private long defaultSearchResultCount;

    protected VertexiumObjectSearchRunnerBase(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration,
            DirectoryRepository directoryRepository
    ) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.directoryRepository = directoryRepository;
        defaultSearchResultCount = configuration.getInt(Configuration.DEFAULT_SEARCH_RESULT_COUNT, 100);
    }

    @Override
    public QueryResultsIterableSearchResults run(
            SearchOptions searchOptions,
            User user,
            Authorizations authorizations
    ) {
        JSONArray filterJson = getFilterJson(searchOptions, searchOptions.getWorkspaceId());

        QueryAndData queryAndData = getQuery(searchOptions, authorizations);
        applyFiltersToQuery(queryAndData, filterJson, user, searchOptions);
        applyConceptTypeFilterToQuery(queryAndData, searchOptions);
        applyEdgeLabelFilterToQuery(queryAndData, searchOptions);
        applySortToQuery(queryAndData, searchOptions);
        applyAggregationsToQuery(queryAndData, searchOptions);
        applyExtendedDataFilters(queryAndData, searchOptions);

        EnumSet<FetchHint> fetchHints = getFetchHints(searchOptions);

        Long size = searchOptions.getOptionalParameter("size", defaultSearchResultCount);
        if (size != null) {
            queryAndData.getQuery().limit(size);
        }

        Long offset = searchOptions.getOptionalParameter("offset", 0L);
        if (offset != null) {
            queryAndData.getQuery().skip(offset.intValue());
        }

        QueryResultsIterable<? extends VertexiumObject> searchResults = getSearchResults(queryAndData, fetchHints);

        return new QueryResultsIterableSearchResults(searchResults, queryAndData, offset, size);
    }

    private EnumSet<FetchHint> getFetchHints(SearchOptions searchOptions) {
        String fetchHintsString = searchOptions.getOptionalParameter("fetchHints", String.class);
        if (fetchHintsString == null) {
            return ClientApiConverter.SEARCH_FETCH_HINTS;
        }
        return FetchHint.parse(fetchHintsString);
    }

    private void applyExtendedDataFilters(QueryAndData queryAndData, SearchOptions searchOptions) {
        Query query = queryAndData.getQuery();
        String[] filterStrings = searchOptions.getOptionalParameter("extendedDataFilters[]", String[].class);
        if (filterStrings == null || filterStrings.length == 0) {
            return;
        }

        List<HasExtendedDataFilter> filters = new ArrayList<>();
        for (String filterString : filterStrings) {
            JSONObject filterJson = new JSONObject(filterString);
            String elementTypeString = filterJson.optString("elementType");
            ElementType elementType = elementTypeString == null ? null : ElementType.valueOf(elementTypeString);
            String elementId = filterJson.optString("elementId");
            String tableName = filterJson.optString("tableName");
            filters.add(new HasExtendedDataFilter(elementType, elementId, tableName));
        }
        query.hasExtendedData(filters);
    }

    private void applyAggregationsToQuery(QueryAndData queryAndData, SearchOptions searchOptions) {
        Query query = queryAndData.getQuery();
        String[] aggregates = searchOptions.getOptionalParameter("aggregations[]", String[].class);
        if (aggregates == null) {
            return;
        }
        for (String aggregate : aggregates) {
            JSONObject aggregateJson = new JSONObject(aggregate);
            Aggregation aggregation = getAggregation(aggregateJson);
            query.addAggregation(aggregation);
        }
    }

    private Aggregation getAggregation(JSONObject aggregateJson) {
        String aggregationName = aggregateJson.getString("name");
        String type = aggregateJson.getString("type");
        Aggregation aggregation;
        switch (type) {
            case "term":
                aggregation = getTermsAggregation(aggregationName, aggregateJson);
                break;
            case "geohash":
                aggregation = getGeohashAggregation(aggregationName, aggregateJson);
                break;
            case "histogram":
                aggregation = getHistogramAggregation(aggregationName, aggregateJson);
                break;
            case "statistics":
                aggregation = getStatisticsAggregation(aggregationName, aggregateJson);
                break;
            case "calendar":
                aggregation = getCalendarFieldAggregation(aggregationName, aggregateJson);
                break;
            default:
                throw new OpenLumifyException("Invalid aggregation type: " + type);
        }

        return addNestedAggregations(aggregation, aggregateJson);
    }

    private Aggregation addNestedAggregations(Aggregation aggregation, JSONObject aggregateJson) {
        JSONArray nestedAggregates = aggregateJson.optJSONArray("nested");
        if (nestedAggregates != null && nestedAggregates.length() > 0) {
            if (!(aggregation instanceof SupportsNestedAggregationsAggregation)) {
                throw new OpenLumifyException("Aggregation does not support nesting: " + aggregation.getClass().getName());
            }
            for (int i = 0; i < nestedAggregates.length(); i++) {
                JSONObject nestedAggregateJson = nestedAggregates.getJSONObject(i);
                Aggregation nestedAggregate = getAggregation(nestedAggregateJson);
                ((SupportsNestedAggregationsAggregation) aggregation).addNestedAggregation(nestedAggregate);
            }
        }

        return aggregation;
    }

    private Aggregation getTermsAggregation(String aggregationName, JSONObject aggregateJson) {
        String field = aggregateJson.getString("field");
        TermsAggregation terms = new TermsAggregation(aggregationName, field);
        int size = aggregateJson.optInt("size", 0);
        if (size > 0) {
            terms.setSize(size);
        }
        return terms;
    }

    private Aggregation getGeohashAggregation(String aggregationName, JSONObject aggregateJson) {
        String field = aggregateJson.getString("field");
        int precision = aggregateJson.getInt("precision");
        return new GeohashAggregation(aggregationName, field, precision);
    }

    private Aggregation getHistogramAggregation(String aggregationName, JSONObject aggregateJson) {
        String field = aggregateJson.getString("field");
        String interval = aggregateJson.getString("interval");
        Long minDocumentCount = JSONUtil.getOptionalLong(aggregateJson, "minDocumentCount");
        return new HistogramAggregation(aggregationName, field, interval, minDocumentCount);
    }

    private Aggregation getStatisticsAggregation(String aggregationName, JSONObject aggregateJson) {
        String field = aggregateJson.getString("field");
        return new StatisticsAggregation(aggregationName, field);
    }

    private Aggregation getCalendarFieldAggregation(String aggregationName, JSONObject aggregateJson) {
        String field = aggregateJson.getString("field");
        Long minDocumentCount = JSONUtil.getOptionalLong(aggregateJson, "minDocumentCount");
        String timeZoneString = aggregateJson.optString("timeZone");
        TimeZone timeZone = timeZoneString == null ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZoneString);
        int calendarField = getCalendarField(aggregateJson.getString("calendarField"));
        return new CalendarFieldAggregation(aggregationName, field, minDocumentCount, timeZone, calendarField);
    }

    private int getCalendarField(String calendarField) {
        try {
            Field field = Calendar.class.getDeclaredField(calendarField);
            return field.getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new OpenLumifyException("Invalid calendar field: " + calendarField, ex);
        }
    }

    protected void applySortToQuery(QueryAndData queryAndData, SearchOptions searchOptions) {
        String[] sorts = searchOptions.getOptionalParameter("sort[]", String[].class);
        if (sorts == null) {
            JSONArray sortsJson = searchOptions.getOptionalParameter("sort", JSONArray.class);
            if (sortsJson != null) {
                sorts = JSONUtil.toStringList(sortsJson).toArray(new String[sortsJson.length()]);
            }
        }
        if (sorts == null) {
            return;
        }
        for (String sort : sorts) {
            String propertyName = sort;
            SortDirection direction = SortDirection.ASCENDING;
            if (propertyName.toUpperCase().endsWith(":ASCENDING")) {
                direction = SortDirection.ASCENDING;
                propertyName = propertyName.substring(0, propertyName.length() - ":ASCENDING".length());
            } else if (propertyName.toUpperCase().endsWith(":DESCENDING")) {
                direction = SortDirection.DESCENDING;
                propertyName = propertyName.substring(0, propertyName.length() - ":DESCENDING".length());
            }
            queryAndData.getQuery().sort(propertyName, direction);
        }
    }

    protected QueryResultsIterable<? extends VertexiumObject> getSearchResults(QueryAndData queryAndData, EnumSet<FetchHint> fetchHints) {
        //noinspection unused
        try (TraceSpan trace = Trace.start("getSearchResults")) {
            return queryAndData.getQuery().search(getResultType(), fetchHints);
        }
    }

    protected abstract EnumSet<VertexiumObjectType> getResultType();

    protected abstract QueryAndData getQuery(SearchOptions searchOptions, Authorizations authorizations);

    protected void applyConceptTypeFilterToQuery(QueryAndData queryAndData, SearchOptions searchOptions) {
        Collection<OntologyRepository.ElementTypeFilter> conceptTypeFilters = getConceptTypeFilters(searchOptions);
        if (conceptTypeFilters != null) {
            ontologyRepository.addConceptTypeFilterToQuery(queryAndData.getQuery(), conceptTypeFilters, searchOptions.getWorkspaceId());
        }
    }

    protected void applyEdgeLabelFilterToQuery(QueryAndData queryAndData, SearchOptions searchOptions) {
        Collection<OntologyRepository.ElementTypeFilter> edgeFilters = getEdgeLabelFilters(searchOptions);
        if (edgeFilters != null) {
            ontologyRepository.addEdgeLabelFilterToQuery(queryAndData.getQuery(), edgeFilters, searchOptions.getWorkspaceId());
        }
    }

    protected Collection<OntologyRepository.ElementTypeFilter> getEdgeLabelFilters(SearchOptions searchOptions) {
        return getElementTypeFilters("edgeLabels", "edgeLabel", searchOptions);
    }

    protected Collection<OntologyRepository.ElementTypeFilter> getConceptTypeFilters(SearchOptions searchOptions) {
        return getElementTypeFilters("conceptTypes", "conceptType", searchOptions);
    }

    private Collection<OntologyRepository.ElementTypeFilter> getElementTypeFilters(String parameterName, String legacyParameterName, SearchOptions searchOptions) {
        String typesStr = searchOptions.getOptionalParameter(parameterName, String.class);
        if (typesStr != null) {
            JSONArray types = new JSONArray(typesStr);
            List<OntologyRepository.ElementTypeFilter> filters = new ArrayList<>(types.length());
            for (int i = 0; i < types.length(); i++) {
                JSONObject type = types.getJSONObject(i);
                OntologyRepository.ElementTypeFilter filter = ClientApiConverter.toClientApi(type, OntologyRepository.ElementTypeFilter.class);
                filters.add(filter);
            }
            return filters;
        }

        // Try the legacy parameter
        String elementType = searchOptions.getOptionalParameter(legacyParameterName, String.class);
        if (elementType != null) {
            Boolean includeChildNodes = searchOptions.getOptionalParameter("includeChildNodes", Boolean.class);
            if (includeChildNodes == null) {
                includeChildNodes = true;
            }
            return Collections.singleton(new OntologyRepository.ElementTypeFilter(elementType, includeChildNodes));
        }
        return null;
    }

    protected void applyFiltersToQuery(QueryAndData queryAndData, JSONArray filterJson, User user, SearchOptions searchOptions) {
        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject obj = filterJson.getJSONObject(i);
            if (obj.length() > 0) {
                if (obj.has("propertyName")) {
                    updateQueryWithPropertyNameFilter(queryAndData.getQuery(), obj, user, searchOptions);
                } else if (obj.has("dataType")) {
                    updateQueryWithDataTypeFilter(queryAndData.getQuery(), obj, user, searchOptions);
                } else {
                    throw new OpenLumifyException("Query filters must have either a propertyName or dataType field. Invalid filter: " + filterJson.toString());
                }
            }
        }
    }

    protected JSONArray getFilterJson(SearchOptions searchOptions, String workspaceId) {
        JSONArray filterJson = searchOptions.getRequiredParameter("filter", JSONArray.class);
        ontologyRepository.resolvePropertyIds(filterJson, workspaceId);
        return filterJson;
    }

    private void updateQueryWithDataTypeFilter(Query graphQuery, JSONObject obj, User user, SearchOptions searchOptions) {
        String dataType = obj.getString("dataType");
        String predicateString = obj.optString("predicate");
        PropertyType propertyType = PropertyType.valueOf(dataType);
        try {
            if ("has".equals(predicateString)) {
                graphQuery.has(PropertyType.getTypeClass(propertyType));
            } else if ("hasNot".equals(predicateString)) {
                graphQuery.hasNot(PropertyType.getTypeClass(propertyType));
            } else if ("in".equals(predicateString)) {
                JSONArray values = obj.getJSONArray("values");
                graphQuery.has(PropertyType.getTypeClass(propertyType), Contains.IN, JSONUtil.toList(values));
            } else {
                JSONArray values = obj.getJSONArray("values");
                Object value0 = jsonValueToObject(values, propertyType, 0);
                if (PropertyType.GEO_LOCATION.equals(propertyType)) {
                    GeoCompare geoComparePredicate = GeoCompare.valueOf(predicateString.toUpperCase());
                    graphQuery.has(GeoShape.class, geoComparePredicate, value0);
                } else {
                    throw new UnsupportedOperationException("Data type queries are not yet supported for type: " + dataType);
                }
            }
        } catch (ParseException ex) {
            throw new OpenLumifyException("Could not update query with filter:\n" + obj.toString(2), ex);
        }
    }

    private void updateQueryWithPropertyNameFilter(Query graphQuery, JSONObject obj, User user, SearchOptions searchOptions) {
        try {
            String predicateString = obj.optString("predicate");
            String propertyName = obj.getString("propertyName");
            if ("has".equals(predicateString)) {
                graphQuery.has(propertyName);
            } else if ("hasNot".equals(predicateString)) {
                graphQuery.hasNot(propertyName);
            } else if ("in".equals(predicateString)) {
                graphQuery.has(propertyName, Contains.IN, JSONUtil.toList(obj.getJSONArray("values")));
            } else {
                PropertyType propertyDataType = PropertyType.convert(obj.optString("propertyDataType"));
                JSONArray values = obj.getJSONArray("values");
                Object value0 = jsonValueToObject(values, propertyDataType, 0);

                if (PropertyType.STRING.equals(propertyDataType) && (predicateString == null || "~".equals(predicateString) || "".equals(predicateString))) {
                    graphQuery.has(propertyName, TextPredicate.CONTAINS, value0);
                } else if (PropertyType.DATE.equals(propertyDataType)) {
                    applyDateToQuery(graphQuery, obj, predicateString, values, searchOptions);
                } else if (PropertyType.BOOLEAN.equals(propertyDataType)) {
                    graphQuery.has(propertyName, Compare.EQUAL, value0);
                } else if (PropertyType.GEO_LOCATION.equals(propertyDataType)) {
                    GeoCompare geoComparePredicate = GeoCompare.valueOf(predicateString.toUpperCase());
                    graphQuery.has(propertyName, geoComparePredicate, value0);
                } else if ("<".equals(predicateString)) {
                    graphQuery.has(propertyName, Compare.LESS_THAN, value0);
                } else if (">".equals(predicateString)) {
                    graphQuery.has(propertyName, Compare.GREATER_THAN, value0);
                } else if ("range".equals(predicateString)) {
                    graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, value0);
                    graphQuery.has(propertyName, Compare.LESS_THAN_EQUAL, jsonValueToObject(values, propertyDataType, 1));
                } else if ("=".equals(predicateString) || "equal".equals(predicateString)) {
                    if (PropertyType.DIRECTORY_ENTITY.equals(propertyDataType) && value0 instanceof JSONObject) {
                        applyDirectoryEntityJsonObjectEqualityToQuery(graphQuery, propertyName, (JSONObject) value0, user);
                    } else if (PropertyType.DOUBLE.equals(propertyDataType)) {
                        applyDoubleEqualityToQuery(graphQuery, obj, value0);
                    } else {
                        graphQuery.has(propertyName, Compare.EQUAL, value0);
                    }
                } else {
                    throw new OpenLumifyException("unhandled query\n" + obj.toString(2));
                }
            }
        } catch (ParseException ex) {
            throw new OpenLumifyException("Could not update query with filter:\n" + obj.toString(2), ex);
        }
    }

    private void applyDirectoryEntityJsonObjectEqualityToQuery(Query graphQuery, String propertyName, JSONObject value0, User user) {
        String directoryEntityId = value0.optString("directoryEntityId", null);
        if (directoryEntityId != null) {
            graphQuery.has(propertyName, Compare.EQUAL, directoryEntityId);
        } else if (value0.optBoolean("currentUser", false)) {
            directoryEntityId = directoryRepository.getDirectoryEntityId(user);
            graphQuery.has(propertyName, Compare.EQUAL, directoryEntityId);
        } else {
            throw new OpenLumifyException("Invalid directory entity JSONObject filter:\n" + value0.toString(2));
        }
    }

    private void applyDoubleEqualityToQuery(Query graphQuery, JSONObject obj, Object value0) throws ParseException {
        String propertyName = obj.getString("propertyName");
        JSONObject metadata = obj.has("metadata") ? obj.getJSONObject("metadata") : null;

        if (metadata != null && metadata.has("http://openlumify.org#inputPrecision") && value0 instanceof Double) {
            double doubleParam = (double) value0;
            int inputPrecision = Math.max(metadata.getInt("http://openlumify.org#inputPrecision"), 0);
            double lowerBound = Precision.round(doubleParam, inputPrecision, BigDecimal.ROUND_DOWN);
            double upperBound = Precision.equals(doubleParam, lowerBound, Precision.EPSILON) ? lowerBound + Math.pow(10, -inputPrecision) :
                    Precision.round(doubleParam, inputPrecision, BigDecimal.ROUND_UP);

            graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, (lowerBound - Precision.EPSILON));
            graphQuery.has(propertyName, Compare.LESS_THAN, (upperBound + Precision.EPSILON));
        } else {
            graphQuery.has(propertyName, Compare.EQUAL, value0);
        }
    }

    private void applyDateToQuery(
            Query graphQuery,
            JSONObject obj,
            String predicate,
            JSONArray values,
            SearchOptions searchOptions
    ) throws ParseException {
        String propertyName = obj.getString("propertyName");
        PropertyType propertyDataType = PropertyType.DATE;
        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyName, searchOptions.getWorkspaceId());

        if (property != null && values.length() > 0) {
            String displayType = property.getDisplayType();
            boolean isDateOnly = displayType != null && displayType.equals("dateOnly");
            boolean isRelative = values.get(0) instanceof JSONObject;

            Calendar calendar = new GregorianCalendar();
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

            if (isRelative) {
                JSONObject fromNow = (JSONObject) values.get(0);
                calendar.setTime(new Date());
                moveDateToStart(calendar, isDateOnly);
                //noinspection MagicConstant
                calendar.add(fromNow.getInt("unit"), fromNow.getInt("amount"));
            } else {
                Date date0 = (Date) jsonValueToObject(values, propertyDataType, 0);
                calendar.setTime(date0);
            }

            if (predicate == null || predicate.equals("equal") || predicate.equals("=")) {
                moveDateToStart(calendar, isDateOnly);
                graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, calendar.getTime());

                moveDateToEnd(calendar, isDateOnly);
                graphQuery.has(propertyName, Compare.LESS_THAN, calendar.getTime());
            } else if (predicate.equals("range")) {
                if (!isRelative) {
                    moveDateToStart(calendar, isDateOnly);
                }
                graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, calendar.getTime());

                if (values.get(1) instanceof JSONObject) {
                    JSONObject fromNow = (JSONObject) values.get(1);
                    calendar.setTime(new Date());
                    moveDateToStart(calendar, isDateOnly);
                    //noinspection MagicConstant
                    calendar.add(fromNow.getInt("unit"), fromNow.getInt("amount"));
                    moveDateToEnd(calendar, isDateOnly);
                    graphQuery.has(propertyName, Compare.LESS_THAN, calendar.getTime());
                } else {
                    calendar.setTime((Date) jsonValueToObject(values, propertyDataType, 1));
                    moveDateToEnd(calendar, isDateOnly);
                    graphQuery.has(propertyName, Compare.LESS_THAN, calendar.getTime());
                }
            } else if (predicate.equals("<")) {
                moveDateToStart(calendar, isDateOnly);
                graphQuery.has(propertyName, Compare.LESS_THAN, calendar.getTime());
            } else if (predicate.equals(">")) {
                moveDateToEnd(calendar, isDateOnly);
                graphQuery.has(propertyName, Compare.GREATER_THAN_EQUAL, calendar.getTime());
            }
        }
    }

    private void moveDateToStart(Calendar calendar, boolean dateOnly) {
        if (dateOnly) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
        }
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void moveDateToEnd(Calendar calendar, boolean dateOnly) {
        if (dateOnly) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        } else {
            calendar.add(Calendar.MINUTE, 1);
        }
    }

    private Object jsonValueToObject(JSONArray values, PropertyType propertyDataType, int index) throws ParseException {
        // JSONObject can be sent to search in the case of relative date searching or advanced directory entry searching
        if (values.get(index) instanceof JSONObject) {
            return values.get(index);
        }
        return OntologyProperty.convert(values, propertyDataType, index);
    }

    protected OntologyRepository getOntologyRepository() {
        return ontologyRepository;
    }

    protected Graph getGraph() {
        return graph;
    }

    public static class QueryAndData {
        private final Query query;

        public QueryAndData(Query query) {
            this.query = query;
        }

        public Query getQuery() {
            return query;
        }
    }
}
