define(['util/vertex/formatters'], function(F) {
    'use strict';

    return [
        {
            identifier: 'org.openlumify.layout.text',
            render: function(el, model, config) {
                if (config && config.style && _.isString(config.style)) {
                    var cls = config.style;
                    switch (config.style) {
                      case 'title':
                      case 'subtitle':
                      case 'heading1':
                      case 'heading2':
                      case 'heading3':
                      case 'body':
                      case 'footnote':
                        break;
                      default:
                        throw new Error('Unknown config style: ' + config.style)
                    }
                    el.classList.add(config.style)
                }

                if (config.truncate) {
                    const text = String(model);
                    el.textContent = F.string.truncate(text, config.truncate);
                    el.title = text;
                } else {
                    el.textContent = String(model);
                }
            }
        },
        {
            identifier: 'org.openlumify.layout.body',
            applyTo: function(model, options) {
                var enoughWidth = !_.contains(options.constraints, 'width');
                if (enoughWidth) {
                    var comment = _.findWhere(model.properties, { name: 'http://openlumify.org/comment#entry' }),
                        hasRelations = !_.isEmpty(model.edgeLabels);
                    return comment || hasRelations;
                }
                return false;
            },
            children: [
                { ref: 'org.openlumify.layout.body.split' },
                { componentPath: 'detail/text/text', className: 'org-openlumify-texts' }
            ]
        },
        {
            identifier: 'org.openlumify.layout.body.split',
            layout: { type: 'flex', options: { direction: 'row' }},
            children: [
                { ref: 'org.openlumify.layout.body.left', style: { flex: 1 }},
                { ref: 'org.openlumify.layout.body.right', style: { flex: 1 }}
            ]
        },
        {
            identifier: 'org.openlumify.layout.body.left',
            children: [
                { componentPath: 'detail/properties/properties', className: 'org-openlumify-properties', modelAttribute: 'data' }
            ]
        },
        {
            identifier: 'org.openlumify.layout.body.right',
            children: [
                { componentPath: 'comments/comments', className: 'org.openlumify-comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', className: 'org-openlumify-relationships', modelAttribute: 'data' }
            ]
        },
        {
            identifier: 'org.openlumify.layout.body',
            children: [
                { componentPath: 'detail/properties/properties', className: 'org-openlumify-properties', modelAttribute: 'data' },
                { componentPath: 'comments/comments', className: 'org.openlumify-comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', className: 'org-openlumify-relationships', modelAttribute: 'data' },
                { componentPath: 'detail/text/text', className: 'org-openlumify-texts' }
            ]
        },
        {
            identifier: 'org.openlumify.layout.header',
            children: [
                { ref: 'org.openlumify.layout.header.text' },
                { componentPath: 'detail/toolbar/toolbar', className: 'org-openlumify-toolbar' }
            ]
        }
    ];
});
