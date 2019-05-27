package org.openlumify.web.routes.vertex;

import org.openlumify.webster.annotations.Handle;
import org.vertexium.*;
import org.vertexium.query.*;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.search.QueryResultsIterableSearchResults;
import org.openlumify.core.model.search.SearchOptions;
import org.openlumify.core.model.search.VertexiumObjectSearchRunnerBase;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.*;
import org.openlumify.web.parameterProviders.ActiveWorkspaceId;
import org.openlumify.web.routes.search.WebSearchOptionsFactory;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class VertexiumObjectSearchBase {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(VertexiumObjectSearchBase.class);
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T.*");
    private final VertexiumObjectSearchRunnerBase searchRunner;
    private final Graph graph;

    public VertexiumObjectSearchBase(
            Graph graph,
            VertexiumObjectSearchRunnerBase searchRunner
    ) {
        checkNotNull(searchRunner, "searchRunner is required");
        this.searchRunner = searchRunner;
        this.graph = graph;
    }

    @Handle
    public ClientApiElementSearchResponse handle(
            HttpServletRequest request,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        SearchOptions searchOptions = WebSearchOptionsFactory.create(request, workspaceId);
        try (QueryResultsIterableSearchResults searchResults = this.searchRunner.run(searchOptions, user, authorizations)) {
            Map<Object, Double> scores = null;
            if (searchResults.getQueryResultsIterable() instanceof IterableWithScores) {
                scores = ((IterableWithScores<?>) searchResults.getQueryResultsIterable()).getScores();
            }

            List<ClientApiVertexiumObject> vertexiumObjects = convertElementsToClientApi(
                    searchResults.getQueryAndData(),
                    searchResults.getQueryResultsIterable(),
                    scores,
                    searchOptions.getWorkspaceId(),
                    authorizations
            );

            ClientApiElementSearchResponse results = new ClientApiElementSearchResponse();
            results.getElements().addAll(vertexiumObjects);
            results.setNextOffset((int) (searchResults.getOffset() + searchResults.getSize()));

            Boolean fetchReferencedElements = searchOptions.getOptionalParameter("fetchReferencedElements", Boolean.class);
            if (fetchReferencedElements != null && fetchReferencedElements) {
                results.setReferencedElements(findReferencedElements(vertexiumObjects, workspaceId, authorizations));
            }

            addSearchResultsDataToResults(results, searchResults.getQueryAndData(), searchResults.getQueryResultsIterable());

            return results;
        }
    }

    protected List<ClientApiVertexiumObject> findReferencedElements(
            List<ClientApiVertexiumObject> searchResults,
            String workspaceId,
            Authorizations authorizations
    ) {
        Set<String> edgeIds = new HashSet<>();
        Set<String> vertexIds = new HashSet<>();
        for (ClientApiVertexiumObject searchResult : searchResults) {
            if (searchResult instanceof ClientApiEdge) {
                vertexIds.add(((ClientApiEdge) searchResult).getInVertexId());
                vertexIds.add(((ClientApiEdge) searchResult).getOutVertexId());
            } else if (searchResult instanceof ClientApiExtendedDataRow) {
                ClientApiExtendedDataRow row = (ClientApiExtendedDataRow) searchResult;
                switch (row.getId().getElementType()) {
                    case "EDGE":
                        edgeIds.add(row.getId().getElementId());
                        break;
                    case "VERTEX":
                        vertexIds.add(row.getId().getElementId());
                        break;
                    default:
                        throw new OpenLumifyException("Unhandled " + ElementType.class.getName() + ": " + row.getId().getElementType());
                }
            }
        }

        List<ClientApiVertexiumObject> results = new ArrayList<>();
        if (vertexIds.size() > 0) {
            Iterable<Vertex> vertices = graph.getVertices(vertexIds, ClientApiConverter.SEARCH_FETCH_HINTS, authorizations);
            for (Vertex vertex : vertices) {
                results.add(ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations));
            }
        }
        if (edgeIds.size() > 0) {
            Iterable<Edge> edges = graph.getEdges(edgeIds, ClientApiConverter.SEARCH_FETCH_HINTS, authorizations);
            for (Edge edge : edges) {
                results.add(ClientApiConverter.toClientApiEdge(edge, workspaceId));
            }
        }
        return results;
    }

    private void addSearchResultsDataToResults(
            ClientApiElementSearchResponse results,
            VertexiumObjectSearchRunnerBase.QueryAndData queryAndData,
            QueryResultsIterable<? extends VertexiumObject> searchResults
    ) {
        results.setTotalHits(searchResults.getTotalHits());
        if (searchResults instanceof IterableWithSearchTime) {
            results.setSearchTime(((IterableWithSearchTime) searchResults).getSearchTimeNanoSeconds());
        }
        for (Aggregation aggregation : queryAndData.getQuery().getAggregations()) {
            results.getAggregates().put(aggregation.getAggregationName(), toClientApiAggregateResult(searchResults, aggregation));
        }
    }

    private ClientApiSearchResponse.AggregateResult toClientApiAggregateResult(
            QueryResultsIterable<? extends VertexiumObject> searchResults,
            Aggregation aggregation
    ) {
        AggregationResult aggResult;
        if (aggregation instanceof TermsAggregation) {
            aggResult = searchResults.getAggregationResult(aggregation.getAggregationName(), TermsResult.class);
        } else if (aggregation instanceof GeohashAggregation) {
            aggResult = searchResults.getAggregationResult(aggregation.getAggregationName(), GeohashResult.class);
        } else if (aggregation instanceof HistogramAggregation || aggregation instanceof CalendarFieldAggregation) {
            aggResult = searchResults.getAggregationResult(aggregation.getAggregationName(), HistogramResult.class);
        } else if (aggregation instanceof StatisticsAggregation) {
            aggResult = searchResults.getAggregationResult(aggregation.getAggregationName(), StatisticsResult.class);
        } else {
            throw new OpenLumifyException("Unhandled aggregation type: " + aggregation.getClass().getName());
        }
        return toClientApiAggregateResult(aggResult);
    }

    private Map<String, ClientApiSearchResponse.AggregateResult> toClientApiNestedResults(Map<String, AggregationResult> nestedResults) {
        Map<String, ClientApiSearchResponse.AggregateResult> results = new HashMap<>();
        for (Map.Entry<String, AggregationResult> entry : nestedResults.entrySet()) {
            ClientApiSearchResponse.AggregateResult aggResult = toClientApiAggregateResult(entry.getValue());
            results.put(entry.getKey(), aggResult);
        }
        if (results.size() == 0) {
            return null;
        }
        return results;
    }

    private ClientApiSearchResponse.AggregateResult toClientApiAggregateResult(AggregationResult aggResult) {
        if (aggResult instanceof TermsResult) {
            return toClientApiTermsAggregateResult((TermsResult) aggResult);
        }
        if (aggResult instanceof GeohashResult) {
            return toClientApiGeohashResult((GeohashResult) aggResult);
        }
        if (aggResult instanceof HistogramResult) {
            return toClientApiHistogramResult((HistogramResult) aggResult);
        }
        if (aggResult instanceof StatisticsResult) {
            return toClientApiStatisticsResult((StatisticsResult) aggResult);
        }
        throw new OpenLumifyException("Unhandled aggregation result type: " + aggResult.getClass().getName());
    }

    private ClientApiSearchResponse.AggregateResult toClientApiStatisticsResult(StatisticsResult agg) {
        ClientApiSearchResponse.StatisticsAggregateResult result = new ClientApiSearchResponse.StatisticsAggregateResult();
        result.setCount(agg.getCount());
        result.setAverage(agg.getAverage());
        result.setMin(agg.getMin());
        result.setMax(agg.getMax());
        result.setStandardDeviation(agg.getStandardDeviation());
        result.setSum(agg.getSum());
        return result;
    }

    private ClientApiSearchResponse.AggregateResult toClientApiHistogramResult(HistogramResult agg) {
        DateFormat bucketDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ClientApiSearchResponse.HistogramAggregateResult result = new ClientApiSearchResponse.HistogramAggregateResult();
        for (HistogramBucket histogramBucket : agg.getBuckets()) {
            ClientApiSearchResponse.HistogramAggregateResult.Bucket b = new ClientApiSearchResponse.HistogramAggregateResult.Bucket(
                    histogramBucket.getCount(),
                    toClientApiNestedResults(histogramBucket.getNestedResults())
            );
            String key = histogramBucket.getKey().toString();
            if (DATE_TIME_PATTERN.matcher(key).matches()) {
                try {
                    Date date = bucketDateFormat.parse(key);
                    if (date != null) {
                        key = String.valueOf(date.getTime());
                    }
                } catch (ParseException pe) {
                    LOGGER.warn("Unable to parse histogram date", pe);
                }
            }
            result.getBuckets().put(key, b);
        }
        return result;
    }

    private ClientApiSearchResponse.AggregateResult toClientApiGeohashResult(GeohashResult agg) {
        ClientApiSearchResponse.GeohashAggregateResult result = new ClientApiSearchResponse.GeohashAggregateResult();
        result.setMaxCount(agg.getMaxCount());
        for (GeohashBucket geohashBucket : agg.getBuckets()) {
            ClientApiSearchResponse.GeohashAggregateResult.Bucket b = new ClientApiSearchResponse.GeohashAggregateResult.Bucket(
                    ClientApiConverter.toClientApiGeoRect(geohashBucket.getGeoCell()),
                    ClientApiConverter.toClientApiGeoPoint(geohashBucket.getGeoPoint()),
                    geohashBucket.getCount(),
                    toClientApiNestedResults(geohashBucket.getNestedResults())
            );
            result.getBuckets().put(geohashBucket.getKey(), b);
        }
        return result;
    }

    private ClientApiSearchResponse.TermsAggregateResult toClientApiTermsAggregateResult(TermsResult agg) {
        ClientApiSearchResponse.TermsAggregateResult result = new ClientApiSearchResponse.TermsAggregateResult();
        for (TermsBucket termsBucket : agg.getBuckets()) {
            ClientApiSearchResponse.TermsAggregateResult.Bucket b = new ClientApiSearchResponse.TermsAggregateResult.Bucket(
                    termsBucket.getCount(),
                    toClientApiNestedResults(termsBucket.getNestedResults())
            );
            result.getBuckets().put(termsBucket.getKey().toString(), b);
        }
        return result;
    }

    protected List<ClientApiVertexiumObject> convertElementsToClientApi(
            VertexiumObjectSearchRunnerBase.QueryAndData queryAndData,
            Iterable<? extends VertexiumObject> searchResults,
            Map<Object, Double> scores,
            String workspaceId,
            Authorizations authorizations
    ) {
        List<ClientApiVertexiumObject> results = new ArrayList<>();
        for (VertexiumObject vertexiumObject : searchResults) {
            Integer commonCount = getCommonCount(queryAndData, vertexiumObject);
            ClientApiVertexiumObject vo;
            if (vertexiumObject instanceof Vertex) {
                vo = ClientApiConverter.toClientApiVertex((Vertex) vertexiumObject, workspaceId, commonCount, authorizations);
            } else if (vertexiumObject instanceof Edge) {
                vo = ClientApiConverter.toClientApiEdge((Edge) vertexiumObject, workspaceId);
            } else if (vertexiumObject instanceof ExtendedDataRow) {
                vo = ClientApiConverter.toClientApiExtendedDataRow((ExtendedDataRow) vertexiumObject, workspaceId);
            } else {
                throw new OpenLumifyException("Unhandled " + VertexiumObject.class.getName() + ": " + vertexiumObject.getClass().getName());
            }
            if (scores != null) {
                vo.setScore(scores.get(vertexiumObject.getId()));
            }
            results.add(vo);
        }
        return results;
    }

    protected Integer getCommonCount(VertexiumObjectSearchRunnerBase.QueryAndData queryAndData, VertexiumObject vertexiumObject) {
        return null;
    }
}
