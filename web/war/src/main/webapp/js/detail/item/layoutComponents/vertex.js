define([
    'detail/toolbar/toolbar',
    'util/vertex/formatters',
    'util/requirejs/promise!util/service/ontologyPromise',
], function(Toolbar, F, ontology) {
    'use strict';

    const MaxTitleWords = 12;

    var conceptDisplay = _.compose(_.property('displayName'), F.vertex.concept),
        vertexDisplay = F.vertex.title;

    return [
        {
            applyTo: { type: 'vertex' },
            identifier: 'org.openlumify.layout.root',
            layout: { type: 'flex', options: { direction: 'column' }},
            componentPath: 'detail/item/vertex',
            children: [
                { ref: 'org.openlumify.layout.header', style: { flex: '0 0 auto' } },
                { ref: 'org.openlumify.layout.body', style: { flex: '1 1 auto', overflow: 'auto' } }
            ]
        },
        {
            applyTo: {
                constraints: ['width', 'height'],
                contexts: ['popup'],
                type: 'vertex'
            },
            identifier: 'org.openlumify.layout.root',
            componentPath: 'detail/item/vertex',
            className: 'popupDetailPane',
            children: [
                { ref: 'org.openlumify.layout.header.text' },
                { ref: 'org.openlumify.layout.body' }
            ]
        },
        {
            applyTo: {
                constraints: ['width', 'height'],
                contexts: ['popup']
            },
            identifier: 'org.openlumify.layout.body',
            children: [
                { ref: 'org.openlumify.layout.formulas' }
            ]
        },
        {
            identifier: 'org.openlumify.layout.formulas',
            children: [
                { ref: 'org.openlumify.layout.formula.item', model: _.partial(modelTransformForFormula, 'subtitle') },
                { ref: 'org.openlumify.layout.formula.item', model: _.partial(modelTransformForFormula, 'time') }
            ]
        },
        {
            identifier: 'org.openlumify.layout.formula.item',
            collectionItem: {
                render: function(el, model) {
                    el.textContent = model;
                }
            }
        },
        {
            applyTo: { displayType: 'video' },
            identifier: 'org.openlumify.layout.body',
            children: [
                { componentPath: 'detail/video/video', className: 'org-openlumify-video' },
                { componentPath: 'detail/properties/properties', className: 'org-openlumify-properties', modelAttribute: 'data' },
                { componentPath: 'comments/comments', className: 'org.openlumify-comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', className: 'org-openlumify-relationships', modelAttribute: 'data' },
                { componentPath: 'detail/text/text', className: 'org-openlumify-texts' }
            ]
        },
        {
            applyTo: { displayType: 'video' },
            identifier: 'org.openlumify.layout.body.split',
            children: [
                { componentPath: 'detail/video/video', className: 'org-openlumify-video' },
                { ref: 'org.openlumify.layout.body.split.artifact' }
            ]
        },
        {
            applyTo: { displayType: 'image' },
            identifier: 'org.openlumify.layout.body',
            children: [
                { componentPath: 'detail/image/image', className: 'org-openlumify-image' },
                { componentPath: 'detail/detectedObjects/detectedObjects' },
                { componentPath: 'detail/properties/properties', className: 'org-openlumify-properties', modelAttribute: 'data' },
                { componentPath: 'comments/comments', className: 'org.openlumify-comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', className: 'org-openlumify-relationships', modelAttribute: 'data' },
                { componentPath: 'detail/text/text', className: 'org-openlumify-texts' }
            ]
        },
        {
            applyTo: { displayType: 'image' },
            identifier: 'org.openlumify.layout.body.split',
            children: [
                { componentPath: 'detail/image/image', className: 'org-openlumify-image' },
                { componentPath: 'detail/detectedObjects/detectedObjects' },
                { ref: 'org.openlumify.layout.body.split.artifact' }
            ]
        },
        {
            applyTo: { displayType: 'audio' },
            identifier: 'org.openlumify.layout.body',
            children: [
                { componentPath: 'detail/audio/audio', className: 'org-openlumify-audio' },
                { componentPath: 'detail/properties/properties', className: 'org-openlumify-properties', modelAttribute: 'data' },
                { componentPath: 'comments/comments', className: 'org.openlumify-comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', className: 'org-openlumify-relationships', modelAttribute: 'data' },
                { componentPath: 'detail/text/text', className: 'org-openlumify-texts' }
            ]
        },
        {
            applyTo: { displayType: 'audio' },
            identifier: 'org.openlumify.layout.body.split',
            children: [
                { componentPath: 'detail/audio/audio', className: 'org-openlumify-audio' },
                { ref: 'org.openlumify.layout.body.split.artifact' }
            ]
        },
        {
            applyTo: { displayType: 'video', constraints: ['width'] },
            identifier: 'org.openlumify.layout.body',
            children: [
                { componentPath: 'detail/video/video', className: 'org-openlumify-video' },
                { componentPath: 'detail/properties/properties', className: 'org-openlumify-properties', modelAttribute: 'data' },
                { componentPath: 'comments/comments', className: 'org.openlumify-comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', className: 'org-openlumify-relationships', modelAttribute: 'data' },
                { componentPath: 'detail/text/text', className: 'org-openlumify-texts' }
            ]
        },
        {
            applyTo: { displayType: 'image', constraints: ['width'] },
            identifier: 'org.openlumify.layout.body',
            children: [
                { componentPath: 'detail/image/image', className: 'org-openlumify-image'},
                { componentPath: 'detail/detectedObjects/detectedObjects' },
                { componentPath: 'detail/properties/properties', className: 'org-openlumify-properties', modelAttribute: 'data' },
                { componentPath: 'comments/comments', className: 'org.openlumify-comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', className: 'org-openlumify-relationships', modelAttribute: 'data' },
                { componentPath: 'detail/text/text', className: 'org-openlumify-texts' }
            ]
        },
        {
            applyTo: { displayType: 'audio', constraints: ['width'] },
            identifier: 'org.openlumify.layout.body',
            children: [
                { componentPath: 'detail/audio/audio' },
                { componentPath: 'detail/properties/properties', className: 'org-openlumify-properties', modelAttribute: 'data' },
                { componentPath: 'comments/comments', className: 'org.openlumify-comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', className: 'org-openlumify-relationships', modelAttribute: 'data' },
                { componentPath: 'detail/text/text', className: 'org-openlumify-texts' }
            ]
        },
        {
            identifier: 'org.openlumify.layout.body.split.artifact',
            layout: { type: 'flex', options: { direction: 'row' }},
            children: [
                { ref: 'org.openlumify.layout.body.left', style: { flex: 1 }},
                { ref: 'org.openlumify.layout.body.right', style: { flex: 1 }}
            ]
        },
        {
            applyTo: { type: 'vertex' },
            identifier: 'org.openlumify.layout.header.text',
            layout: { type: 'flex', options: { direction: 'column' }},
            className: 'vertex-header',
            children: [
                { componentPath: 'detail/headerImage/image', className: 'entity-glyphicon', modelAttribute: 'data' },
                { ref: 'org.openlumify.layout.text', style: 'title', model: vertexDisplay, truncate: MaxTitleWords },
                { ref: 'org.openlumify.layout.text', style: 'subtitle', model: conceptDisplay }
            ]
        }
    ]

    function modelTransformForFormula(formula, model) {
        if (!_.contains(['subtitle', 'time', 'title'], formula)) {
            throw new Error('Not a valid formula', formula);
        }
        return Promise.require('util/vertex/formatters').then(function(F) {
            var names = [],
                subtitle = F.vertex[formula](model, names),
                propertyName = _.last(names)
            if (propertyName) {
                var ontologyProperty = ontology.properties.byTitle[propertyName];
                if (ontologyProperty) {
                    propertyName = ontologyProperty.displayName;
                }
                if (!subtitle) return [];
            }
            return _.compact([propertyName, subtitle]);
        });
    }
});
