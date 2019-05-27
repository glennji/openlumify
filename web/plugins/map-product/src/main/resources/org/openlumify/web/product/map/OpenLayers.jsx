define([
    'create-react-class',
    'prop-types',
    'openlayers',
    './util/layerHelpers',
    './util/previewTask',
    'product/toolbar/ProductToolbar'
], function(
    createReactClass,
    PropTypes,
    ol,
    layerHelpers,
    previewTask,
    ProductToolbar) {

    const noop = function() {};

    const {
        BaseLayerLoaded,
        BaseLayerStarted,
        calculateExtent,
        listenForTileLoading
    } = layerHelpers;

    const ANIMATION_DURATION = 200,
        MIN_FIT_ZOOM_RESOLUTION = 30,
        MAX_FIT_ZOOM_RESOLUTION = 20000,
        PREVIEW_DEBOUNCE_SECONDS = 2,
        MAP_FIRST_RENDER = 'mapFirstRender',
        LAYERS_EXTENDED_DATA_KEY = 'org-openlumify-map-layers',
        BASE_LAYER_ID = 'base';

    const OpenLayers = createReactClass({
        propTypes: {
            product: PropTypes.object.isRequired,
            baseSource: PropTypes.string.isRequired,
            baseSourceOptions: PropTypes.object,
            sourcesByLayerId: PropTypes.object,
            generatePreview: PropTypes.bool,
            onSelectElements: PropTypes.func.isRequired,
            onUpdatePreview: PropTypes.func.isRequired,
            onTap: PropTypes.func,
            onContextTap: PropTypes.func,
            onZoom: PropTypes.func,
            onPan: PropTypes.func,
            onMouseOver: PropTypes.func,
            onMouseOut: PropTypes.func
        },

        getInitialState() {
            return { panning: false }
        },

        getDefaultProps() {
            return {
                generatePreview: false,
                onTap: noop,
                onContextTap: noop,
                onZoom: noop,
                onPan: noop
            }
        },

        componentWillReceiveProps(nextProps) {
            const { sourcesByLayerId: prevSourcesByLayerId, product: prevProduct } = this.props;
            const {
                sourcesByLayerId: nextSourcesByLayerId,
                product: nextProduct,
                registry,
                layerExtensions } = nextProps;
            const { map, layersWithSources } = this.state;
            const nextLayerIds = Object.keys(nextProps.sourcesByLayerId);

            if (layersWithSources && (
                    nextLayerIds.length !== Object.keys(layersWithSources).length
                    || nextLayerIds.some(layerId => !layersWithSources[layerId])
                )) {
                const previous = Object.keys(prevSourcesByLayerId);
                const newLayers = [];
                const nextLayers = map.getLayerGroup().getLayers().getArray().slice(0);
                const existingLayersById = _.indexBy(nextLayers, layer => layer.get('id'));
                const newLayersWithSources = {};
                const addLayer = (initializer, layerId, options, map) => {
                    const layerWithSource = this.initializeLayer(initializer, layerId, options, map);
                    const layers = layerWithSource.layers || [layerWithSource.layer];

                    layers.forEach(layer => {
                        const config = nextProps.layerConfig && nextProps.layerConfig[layer.get('id')];
                        if (config) {
                            layerHelpers.setLayerConfig(config, layer);
                        }

                        newLayersWithSources[layerId] = layerWithSource;
                        nextLayers.push(layer);
                    })

                };

                Object.keys(nextSourcesByLayerId).forEach((layerId) => {
                    if (!prevSourcesByLayerId[layerId]) {
                        newLayers.push(layerId);
                    } else {
                        const layerIndex = previous.indexOf(layerId);
                        previous.splice(layerIndex, 1);
                    }
                });

                previous.forEach(layerId => {
                    const layerIndex = nextLayers.findIndex(layer => layer.get('id') === layerId);
                    nextLayers.splice(layerIndex, 1);
                });

                newLayers.forEach(layerId => {
                    if (!existingLayersById[layerId]) {
                        const { type, features, ...options } = nextSourcesByLayerId[layerId];
                        const initializer = layerHelpers.byType[type] || registry['org.openlumify.map.layer'].find(e => e.type === type);

                        if (initializer) {
                            addLayer(initializer, layerId, options, map)
                        } else {
                            console.warn('Sources present for layer: ' + layerId + ', but no layer type defined for: ' + type);
                        }
                    }
                });

                if (nextProduct.id !== prevProduct.id) {
                    _.mapObject(layerExtensions, (e, layerId) => {
                        if (!(layerId in newLayersWithSources) && !(layerId in nextSourcesByLayerId)) {
                            addLayer(e, layerId, e.options, map);
                        }
                    })
                }

                map.getLayerGroup().setLayers(new ol.Collection(nextLayers));

                if (previous.length || Object.keys(newLayersWithSources).length) {
                    this.setState({ layersWithSources: {
                        ..._.omit(layersWithSources, previous),
                        ...newLayersWithSources
                    }});
                }
            }
        },

        componentDidUpdate(prevProps, prevState) {
            const { map, layersWithSources } = this.state;
            const { product, sourcesByLayerId, layerExtensions, layerConfig, focused, generatePreview } = this.props;

            let changed = false;
            let fit = [];

            const layers = map.getLayers();

            layers.forEach(layer => {
                const layerId = layer.get('id');

                const layerType = layer.get('type');
                const layerHelper = layerHelpers.byType[layerType] || layerExtensions[layerId];
                const layerWithSources = layersWithSources[layerId];
                const nextSource = sourcesByLayerId[layerId];
                const prevSource = prevProps.sourcesByLayerId[layerId];

                if (layerHelper && layerWithSources) {
                    const shouldUpdate = _.isFunction(layerHelper.shouldUpdate)
                        ? layerHelper.shouldUpdate(nextSource, prevSource, layerWithSources, focused)
                        : true;

                    if (shouldUpdate && _.isFunction(layerHelper.update) && nextSource) {
                        const { changed: c = true, fitFeatures = [] } = layerHelper.update(nextSource, layerWithSources, focused) || {};
                        changed = changed || c;
                        if (fitFeatures) fit.push(...fitFeatures)
                    }
                }
            });

            const newLayerOrder = product.extendedData
                && product.extendedData[LAYERS_EXTENDED_DATA_KEY]
                && product.extendedData[LAYERS_EXTENDED_DATA_KEY].layerOrder;
            const prevLayerOrder = prevProps.product.extendedData
                && prevProps.product.extendedData[LAYERS_EXTENDED_DATA_KEY]
                && prevProps.product.extendedData[LAYERS_EXTENDED_DATA_KEY].layerOrder;
            if (map && (map !== prevState.map || newLayerOrder !== prevLayerOrder)
                || prevState.layersWithSources.length !== Object.keys(layersWithSources).length
                || prevState.layersWithSource.some(layerId => !layersWithSources[layerId])) {
                this.applyLayerOrder();
            }


            if (map && (!prevState.map || prevProps.layerConfig !== layerConfig)) {
                this.applyLayerConfig();
            }

            if (generatePreview) {
                this.setLoadingNewProduct();
                this.fit({ animate: false });
            } else if (fit.length) {
                this.fit({
                    limitToFeatures: fit,
                    animate: !this.isLoadingNewProduct
                });
            }

            if (generatePreview || changed) {
                this.updatePreview();
            }
        },

        setLoadingNewProduct() {
            // Assume we are still loading the product for a while
            this.isLoadingNewProduct = true;
            _.delay(() => {
                this.isLoadingNewProduct = false;
            }, 1000);
        },

        _updatePreview() {
            var task = this.updatingPreviewTask;

            if (task) {
                task.cancel();
            }

            const { onUpdatePreview } = this.props;
            const { map, layersWithSources } = this.state;

            task = previewTask({
                map,
                layersWithSources,
                minResolution: MIN_FIT_ZOOM_RESOLUTION,
                maxResolution: MAX_FIT_ZOOM_RESOLUTION
            });
            task.then(img => {
                onUpdatePreview(img)
            })
            this.updatingPreviewTask = task;
        },

        componentDidMount() {
            this.olEvents = [];
            this.domEvents = [];
            this.updatePreview = _.debounce(this._updatePreview, PREVIEW_DEBOUNCE_SECONDS * 1000);
            const { map, layersWithSources } = this.configureMap();

            this.setState({ map, layersWithSources })
        },

        componentWillUnmount() {
            this._canvasPreviewBuffer = null;
            if (this.updatingPreviewTask) {
                this.updatingPreviewTask.teardown();
            }
            clearTimeout(this._handleMouseMoveTimeout);
            if (this.domEvents) {
                this.domEvents.forEach(fn => fn());
                this.domEvents = null;
            }
            if (this.olEvents) {
                this.olEvents.forEach(key => ol.Observable.unByKey(key));
                this.olEvents = null;
            }
        },

        render() {
            // Cover the map when panning/dragging to avoid sending events there
            const moveWrapper = this.state.panning ? (<div className="draggable-wrapper"/>) : '';
            return (
                <div style={{height: '100%'}}>
                    <div style={{height: '100%'}} ref="map"></div>
                    <ProductToolbar
                        product={this.props.product}
                        injectedProductProps={this.getInjectedToolProps()}
                        rightOffset={this.props.panelPadding.right}
                        showNavigationControls={true}
                        onFit={this.onControlsFit}
                        onZoom={this.onControlsZoom} />
                    {moveWrapper}
                </div>
            )
        },

        onControlsFit() {
            this.fit();
        },

        onControlsZoom(type) {
            const { map } = this.state;
            const view = map.getView();

            if (!this._slowZoomIn) {
                this._slowZoomIn = _.throttle(zoomByDelta(1), ANIMATION_DURATION, {trailing: false});
                this._slowZoomOut = _.throttle(zoomByDelta(-1), ANIMATION_DURATION, {trailing: false});
            }

            if (type === 'in') {
                this._slowZoomIn();
            } else {
                this._slowZoomOut();
            }

            function zoomByDelta(delta) {
                return () => {
                    var currentResolution = view.getResolution();
                    if (currentResolution) {
                        view.animate({
                            resolution: view.constrainResolution(currentResolution, delta),
                            duration: ANIMATION_DURATION
                        });
                    }
                }
            }
        },

        fit(options) {
            const { map } = this.state;

            if (this._fitDelayed) {
                clearTimeout(this._fitDelayed);
            }

            // Wait for map to load completely
            // (after first render)
            if (map.get(MAP_FIRST_RENDER)) {
                this.fitInternal(options)
            } else {
                this._fitDelayed = _.delay(() => {
                    this.fitInternal(options)
                }, 100)
            }
        },

        fitInternal(options = {}) {
            const { animate = true, limitToFeatures = [] } = options;
            const { map, layersWithSources } = this.state;
            const view = map.getView();
            const initialLoading = this.isLoadingNewProduct;
            const changeZoom = initialLoading || limitToFeatures.length !== 1;
            let extent;

            if (limitToFeatures.length) {
                extent = calculateExtent(map, layersWithSources, limitToFeatures)
            } else {
                extent = calculateExtent(map, layersWithSources);
            }

            if (!ol.extent.isEmpty(extent)) {
                var resolution = view.getResolution(),
                    extentWithPadding = extent,
                    { left, right, top, bottom } = this.props.panelPadding,
                    clientBox = this.refs.map.getBoundingClientRect(),
                    padding = 20,
                    viewportWidth = clientBox.width - left - right - padding * 2,
                    viewportHeight = clientBox.height - top - bottom - padding * 2,
                    extentWithPaddingSize = ol.extent.getSize(extentWithPadding),
                    currentExtent = view.calculateExtent([viewportWidth, viewportHeight]),

                    // Figure out ideal resolution based on available realestate
                    idealResolution = Math.max(
                        extentWithPaddingSize[0] / viewportWidth,
                        extentWithPaddingSize[1] / viewportHeight
                    );


                if (!initialLoading && limitToFeatures.length) {
                    const horizontalSync = ((left + padding) / 2 - (right + padding) / 2) * resolution;
                    const verticalSync = ((top + padding) / 2 - (bottom + padding) / 2) * resolution;
                    currentExtent[0] += horizontalSync;
                    currentExtent[1] += verticalSync;
                    currentExtent[2] += horizontalSync;
                    currentExtent[3] += verticalSync;

                    var insideCurrentView = ol.extent.containsExtent(currentExtent, extent);
                    if (insideCurrentView) {
                        return;
                    }
                }

                const newResolution = changeZoom ? view.constrainResolution(
                    Math.min(MAX_FIT_ZOOM_RESOLUTION, Math.max(idealResolution, MIN_FIT_ZOOM_RESOLUTION)), -1
                ) : view.getResolution();
                const center = ol.extent.getCenter(extentWithPadding);
                const offsetX = left - right;
                const offsetY = top - bottom;
                const lon = offsetX * newResolution / 2;
                const lat = offsetY * newResolution / 2;
                center[0] = center[0] - lon;
                center[1] = center[1] - lat;

                const options = { center };
                if (changeZoom) {
                    options.resolution = newResolution;
                }

                view.cancelAnimations();
                view.animate({
                    ...options,
                    duration: animate ? ANIMATION_DURATION : 0
                })
            } else {
                view.cancelAnimations();
                view.animate({
                    ...this.getDefaultViewParameters(),
                    duration: animate ? ANIMATION_DURATION : 0
                });
            }
        },

        getDefaultViewParameters() {
            return {
                zoom: 2,
                minZoom: 1,
                center: [0, 0]
            };
        },

        configureMap() {
            const { baseSource, baseSourceOptions = {}, sourcesByLayerId, layerExtensions } = this.props;
            const layersWithSources = {};

            const addLayer = (layerExtension, id, options, map) => {
                if (layersWithSources[id]) return;

                const layerWithSource = this.initializeLayer(layerExtension, id, options, map);
                const layers = layerWithSource.layers || [layerWithSource.layer];

                layers.forEach(l => { map.addLayer(l); });
                layersWithSources[id] = layerWithSource;
            };

            const map = new ol.Map({
                loadTilesWhileInteracting: true,
                keyboardEventTarget: document,
                controls: [],
                layers: [],
                target: this.refs.map
            });

            map.once('postrender', () => {
                map.set(MAP_FIRST_RENDER, true);
            })

            // add the base(tile) layer
            addLayer(layerHelpers.byType.tile, BASE_LAYER_ID, { source: baseSource, sourceOptions: baseSourceOptions }, map);
            // add layers from org.openlumify.map.layer registered extensions
            _.mapObject(layerExtensions, (extension, layerId) => { addLayer(extension, layerId, extension.options, map) });
            // add layers from sources passed in props
            _.mapObject(sourcesByLayerId, ({ type, features, ...options }, layerId) => {
                const initializer = layerHelpers.byType[type];
                if (initializer) {
                    addLayer(initializer, layerId, options, map);
                } else {
                    console.warn('Sources present for layer: ' + layerId + ', but no layer type defined for: ' + type);
                }
            });
            this.configureEvents(map);

            const view = new ol.View(this.getDefaultViewParameters());
            this.olEvents.push(view.on('change:center', (event) => this.props.onPan(event)));
            this.olEvents.push(view.on('change:resolution', (event) => this.props.onZoom(event)));

            map.setView(view);
            return { map, layersWithSources}
        },

        configureEvents(map) {
            var self = this;

            this.olEvents.push(map.on('click', function(event) {
                self.props.onTap(event);
            }));
            this.olEvents.push(map.on('pointerup', function(event) {
                const { pointerEvent } = event;
                if (pointerEvent && pointerEvent.button === 2) {
                    self.props.onContextTap(ol, event);
                }
            }));

            const viewport = map.getViewport();
            this.domEvent(viewport, 'contextmenu', function(event) {
                event.preventDefault();
            })
            this.domEvent(viewport, 'mouseup', function(event) {
                event.preventDefault();
                if (event.button === 2 || event.ctrlKey) {
                    // TODO
                    //self.handleContextMenu(event);
                }
            });
            this.domEvent(viewport, 'mousemove', event => {
                const pixel = map.getEventPixel(event);
                const hit = map.getFeaturesAtPixel(pixel);
                if (hit) {
                    this.handleMouseMove(hit);
                    map.getTarget().style.cursor = 'pointer';
                } else {
                    this.handleMouseMove();
                    map.getTarget().style.cursor = '';
                }
            });
        },

        initializeLayer(layerExtension, layerId, options, map) {
            const { baseSource, baseSourceOptions, sourcesByLayerId, generatePreview, layerExtensions, layerConfig, ...handlers } = this.props;
            const layerHelper = layerExtension.type && layerHelpers.byType[layerExtension.type] || layerExtension;
            const layerWithSource = layerHelper.configure(layerId, options, map);

            if (_.isFunction(layerHelper.addEvents)) {
                this.olEvents.concat(layerHelper.addEvents(map, layerWithSource, handlers));
            }

            const layers = layerWithSource.layers || [layerWithSource.layer];
            layers.forEach(layer => {
                const config = layerConfig && layerConfig[layer.get('id')];
                if (config) {
                    layerHelpers.setLayerConfig(config, layer);
                }
            });

            return layerWithSource;
        },

        applyLayerOrder() {
            const { map } = this.state;
            const { product, setLayerOrder } = this.props;
            const layersById = _.indexBy(map.getLayers().getArray(), layer => layer.get('id'));
            const nextLayerGroup = map.getLayerGroup();
            let layerOrder = product.extendedData
                && product.extendedData[LAYERS_EXTENDED_DATA_KEY]
                && product.extendedData[LAYERS_EXTENDED_DATA_KEY].layerOrder.slice(0) || [];

            let orderedLayers = new ol.Collection();
            let newLayers = [];

            orderedLayers.push(layersById[BASE_LAYER_ID]);
            delete layersById[BASE_LAYER_ID];

            if (layerOrder.length) {
                layerOrder = layerOrder.reverse();

                layerOrder.forEach((layerId, i) => {
                    const layer = layersById[layerId];
                    if (layer) {
                        orderedLayers.push(layer);

                        delete layersById[layerId];
                    }
                });

                _.mapObject(layersById, (layer, layerId) => {
                    orderedLayers.push(layer);
                    newLayers.push(layerId);
                });

                nextLayerGroup.setLayers(orderedLayers);
            } else {
                newLayers = map.getLayers().getArray().slice(1).reduce((ids, layer) => {
                    ids.push(layer.get('id'));
                    return ids;
                }, []);
            }

            if (newLayers.length) {
                setLayerOrder(layerOrder.concat(newLayers.reverse()))
            }
        },

        applyLayerConfig() {
            const map = this.state.map;
            const layerConfig = this.props.layerConfig;

            if (layerConfig) {
                const layersById = _.indexBy(map.getLayers().getArray(), layer => layer.get('id'));

                _.mapObject(layersById, (layer, layerId) => {
                    const config = layerConfig[layerId];
                    layerHelpers.setLayerConfig(config, layersById[layerId]);
                });
            }
        },

        domEvent(el, type, handler) {
            this.domEvents.push(() => el.removeEventListener(type, handler));
            el.addEventListener(type, handler, false);
        },

        handleMouseMove(features) {
            const { onMouseOver, onMouseOut } = this.props;
            const { map } = this.state;

            if (!onMouseOver && !onMouseOut ) {
                return;
            }

            const stillHoveringSameFeature = features &&
                this._handleMouseMoveFeatures &&
                this._handleMouseMoveFeatures.length === features.length &&
                this._handleMouseMoveFeatures[0] === features[0];

            if (!stillHoveringSameFeature) {
                clearTimeout(this._handleMouseMoveTimeout);

                if (features && features.length) {
                    this._handleMouseMoveTimeout = setTimeout(() => {
                        this._handleMouseMoveFeatures = features;
                        if (onMouseOver) {
                            onMouseOver(ol, map, this._handleMouseMoveFeatures)
                        }
                    }, 250);
                } else if (this._handleMouseMoveFeatures) {
                    if (onMouseOut) {
                        onMouseOut(ol, map, this._handleMouseMoveFeatures)
                    }
                    this._handleMouseMoveFeatures = null;
                }
            }
        },

        /**
         * Map work product toolbar item component
         *
         * @typedef org.openlumify.product.toolbar.item~MapComponent
         * @property {function} requestUpdate Reload the maps extensions and styles.
         * Call when the result of extensions will change from variables
         * outside of inputs (preferences, etc).
         * @property {object} product The map product
         * @property {object} ol The [Openlayers Api](http://openlayers.org/en/latest/apidoc/)
         * @property {object} map [map](http://openlayers.org/en/latest/apidoc/ol.Map.html) instance
         * @property {Object.<string, layerWithSource>} layersWithSources Keyed by the id of the layer, the map's rendered layers with their sources
         * @property {object} cluster deprecated, access this from inside {@link org.openlumify.product.toolbar.item~layersWithSources} instead
         * @property {object} cluster.clusterSource [multiPointCluster](https://github.com/openlumify/openlumify/blob/master/web/plugins/map-product/src/main/resources/org/openlumify/web/product/map/multiPointCluster.js) that implements the [`ol.source.Cluster`](http://openlayers.org/en/latest/apidoc/ol.source.Cluster.html) interface to cluster the `source` features.
         * @property {object} cluster.source The [`ol.source.Vector`](http://openlayers.org/en/latest/apidoc/ol.source.Vector.html) source of all map pins before clustering.
         * @property {object} cluster.layer The [`ol.layer.Vector`](http://openlayers.org/en/latest/apidoc/ol.layer.Vector.html) pin layer
         */
        getInjectedToolProps() {
            const { clearCaches: requestUpdate, product } = this.props;
            const { map, layersWithSources } = this.state;
            let props = {};

            if (map && layersWithSources) {
                /**
                 * @typedef {object} org.openlumify.product.toolbar.item~layerWithSource
                 * @property {object} source The [`ol.source`](http://openlayers.org/en/latest/apidoc/ol.source.html) of the layer
                 * @property {object} layer The [`ol.layer`](http://openlayers.org/en/latest/apidoc/ol.layer.html) rendered in the map
                 */
                /**
                 * @typedef {object.<string,layerWithSource>} org.openlumify.product.toolbar.item~layersWithSources
                 *
                 * Keyed by layerId, the map's rendered sources with the layers they are backing
                 */
                props = { product, ol, map, cluster: layersWithSources.cluster, layersWithSources, requestUpdate }
            }

            return props;
        }
    })

    return OpenLayers;
})

