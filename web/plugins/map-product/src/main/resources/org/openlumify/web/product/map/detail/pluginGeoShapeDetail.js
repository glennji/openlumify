require(['configuration/plugins/registry', 'util/vertex/formatters'], function(registry, F) {

    const GEOSHAPE_MIMETYPES = [
        'application/vnd.geo+json',
        'application/vnd.google-earth.kml+xml'
    ];

    // element inspector
    registry.registerExtension('org.openlumify.layout.component', {
        identifier: 'org.openlumify.layout.body',
        applyTo: (model, { constraints }) => constraints.includes('width') && !constraints.includes('height') && isGeoShapeFile(model),
        children: [
            {
                componentPath: 'org/openlumify/web/product/map/dist/geoShapePreview',
                className: 'org-openlumify-map-geoshape-preview'
            },
            { componentPath: 'detail/properties/properties', className: 'org-openlumify-properties', modelAttribute: 'data' },
            { componentPath: 'comments/comments', className: 'org.openlumify-comments', modelAttribute: 'data' },
            { componentPath: 'detail/relationships/relationships', className: 'org-openlumify-relationships', modelAttribute: 'data' }
        ]
    });

    // fullscreen
    registry.registerExtension('org.openlumify.layout.component', {
        identifier: 'org.openlumify.layout.body',
        applyTo: (model, { constraints }) => !constraints.length && isGeoShapeFile(model),
        children: [
            {
                componentPath: 'org/openlumify/web/product/map/dist/geoShapePreview',
                className: 'org-openlumify-map-geoshape-preview'
            },
            { ref: 'org.openlumify.layout.body.split' }
        ]
    });

    function isGeoShapeFile(model) {
        if (F.vertex.displayType(model) === 'document') {
            const mimeType = F.vertex.prop(model, 'http://openlumify.org#mimeType');
            if (GEOSHAPE_MIMETYPES.includes(mimeType)) {
                return true;
            }
        }

        return false
    }
});
