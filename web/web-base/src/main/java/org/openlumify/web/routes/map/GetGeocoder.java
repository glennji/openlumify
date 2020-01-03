package org.openlumify.web.routes.map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.ParameterizedHandler;
import org.visallo.webster.annotations.Handle;
import org.visallo.webster.annotations.Required;
import org.openlumify.core.geocoding.GeocodeResult;
import org.openlumify.core.geocoding.GeocoderRepository;
import org.openlumify.web.OpenLumifyResponse;
import org.openlumify.web.clientapi.model.ClientApiMapGeocodeResponse;

import java.util.List;

@Singleton
public class GetGeocoder implements ParameterizedHandler {
    private final GeocoderRepository geocoderRepository;

    @Inject
    public GetGeocoder(GeocoderRepository geocoderRepository) {
        this.geocoderRepository = geocoderRepository;
    }

    @Handle
    public void handle(
            @Required(name = "q") String query,
            OpenLumifyResponse response
    ) throws Exception {
        List<GeocodeResult> geocoderResults = this.geocoderRepository.find(query);
        ClientApiMapGeocodeResponse result = toClientApi(geocoderResults);
        response.respondWithClientApiObject(result);
    }

    private ClientApiMapGeocodeResponse toClientApi(List<GeocodeResult> geocoderResults) {
        ClientApiMapGeocodeResponse result = new ClientApiMapGeocodeResponse();

        for (GeocodeResult geocodeResult : geocoderResults) {
            result.results.add(toClientApi(geocodeResult));
        }

        return result;
    }

    private ClientApiMapGeocodeResponse.Result toClientApi(GeocodeResult geocodeResult) {
        String name = geocodeResult.getName();
        double latitude = geocodeResult.getLatitude();
        double longitude = geocodeResult.getLongitude();
        return new ClientApiMapGeocodeResponse.Result(name, latitude, longitude);
    }
}