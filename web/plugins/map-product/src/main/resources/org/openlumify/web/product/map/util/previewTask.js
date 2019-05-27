define([
    'openlayers',
    './layerHelpers'
], function(
    ol,
    { BaseLayerLoaded, listenForTileLoading, calculateExtent }) {

    const PREVIEW_WIDTH = 300;
    const PREVIEW_HEIGHT = 300;

    var canvas = null;
    var target = null;
    var previewMap = null;


    return previewTask;

    function previewTask({ map, layersWithSources, minResolution, maxResolution }) {
        const { base, cluster } = layersWithSources;

        var cancelled = false;

        cleanupMap();

        const promise = new Promise(function(resolve, reject, onCancel) {
            onCancel(function() {
                cleanupMap();
                cancelled = true;
            })
            execute(resolve, reject);
        })

        promise.teardown = function() {
            if (!cancelled) {
                promise.cancel();
            }
            canvas = null;
            cleanupMap();
        };

        return promise;

        function cleanupMap() {
            if (previewMap) {
                previewMap.setTarget(null);
                if (previewMap.dispose) {
                    previewMap.dispose()
                }
                previewMap = null;
            }
        }

        function execute(resolve, reject) {
            if (!canvas) {
                canvas = document.createElement('canvas');
                canvas.width = PREVIEW_WIDTH;
                canvas.height = PREVIEW_HEIGHT;
            }

            if (!target) {
                target = document.createElement('div');
                target.className = 'product-map-preview';
                target.style.visibility = 'hidden';
                target.style.position = 'absolute';
                target.style.zIndex = -1;
                target.style.pointerEvents = 'none';
                document.body.appendChild(target)
            }
            const size = map.getSize();
            target.style.width = size[0] + 'px'
            target.style.height = size[1] + 'px'

            const view = new ol.View({ zoom: 2, center: [0, 0] });
            previewMap = new ol.Map({ view, controls: [], target })
            const events = listenForTileLoading(previewMap, base.source)
            const layerGroup = map.getLayerGroup();
            previewMap.setLayerGroup(layerGroup);

            if (previewMap.get(BaseLayerLoaded)) {
                capture();
            } else {
                previewMap.once('change:' + BaseLayerLoaded, () => {
                    _.delay(capture, 250);
                });
            }

            function capture() {
                events.forEach(key => ol.Observable.unByKey(key));

                if (cancelled) return;

                const extent = calculateExtent(map, layersWithSources);

                if (!ol.extent.isEmpty(extent)) {
                    const view = previewMap.getView();
                    const size = previewMap.getSize();
                    view.fit(extent, {
                        size,
                        minResolution,
                        constrainResolution: false,
                        padding: [1, 1, 1, 1],
                        callback: draw
                    })
                } else {
                    draw();
                }

                function draw() {
                    previewMap.once('postcompose', event => {
                        if (cancelled) return;
                        var mapCanvas = event.context.canvas;
                        var context = canvas.getContext('2d');
                        var hRatio = PREVIEW_WIDTH / mapCanvas.width;
                        var vRatio = PREVIEW_HEIGHT / mapCanvas.height;
                        var ratio = Math.min(hRatio, vRatio);
                        canvas.width = Math.trunc(mapCanvas.width * ratio);
                        canvas.height = Math.trunc(mapCanvas.height * ratio);
                        context.drawImage(mapCanvas,
                            0, 0, mapCanvas.width, mapCanvas.height,
                            0, 0, canvas.width, canvas.height
                        );

                        _.defer(() => {
                            if (cancelled) return;
                            const dataUrl = canvas.toDataURL('image/png');
                            cleanupMap();
                            resolve(dataUrl);
                        })
                    })
                    previewMap.renderSync();
                }
            }
        }
    }
});

