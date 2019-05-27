require(['configuration/plugins/registry'], function(registry) {
    registry.registerExtension('org.openlumify.ingest.cloud', {
        identifier: 'org.openlumify.web.ingest.cloud.s3.AmazonS3CloudResourceSource',
        componentPath: 'org/openlumify/web/ingest/cloud/s3/dist/Config'
    })
});
