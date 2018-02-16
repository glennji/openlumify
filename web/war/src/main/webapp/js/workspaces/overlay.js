
define([
    'flight/lib/component',
    './overlay.hbs',
    'util/formatters',
    'util/privileges',
    'util/withDataRequest',
    'configuration/plugins/registry'
], function(
    defineComponent,
    template,
    F,
    Privilege,
    withDataRequest,
    registry) {
    'use strict';

    /**
     * Create new clickable sections in the user account dialog accessed when
     * clicking the username in the case overlay.
     *
     * Create a key in a message bundle for the name of the section:
     *
     *      useraccount.page.[Identifier].displayName=[String to display]
     *
     * @param {string} identifier The unique identifier for this page
     * @param {string} pageComponentPath The path to component to render in the
     * content area of the modal.
     */
    registry.documentExtensionPoint('org.visallo.user.account.page',
        'Add new tabs to user account modal dialog',
        function(e) {
            return ('identifier' in e) && ('pageComponentPath' in e);
        },
        'http://docs.visallo.org/extension-points/front-end/userAccount'
    );


    const LAST_SAVED_UPDATE_FREQUENCY_SECONDS = 30;
    const UPDATE_WORKSPACE_DIFF_SECONDS = 3;
    const SHOW_UNPUBLUSHED_CHANGES_SECONDS = 3;
    const COMMENT_ENTRY_IRI = 'http://visallo.org/comment#entry';
    const MAX_HOVER_IDS = 1000;

    return defineComponent(WorkspaceOverlay, withDataRequest);

    function isWorkspaceDiffPost(settings) {
        var route = ~['workspace/undo', 'workspace/publish'].indexOf(settings.url),
            isPost = (/post/i).test(settings.type);

        return !!(route && isPost);
    }

    function WorkspaceOverlay() {

        this.defaultAttrs({
            userSelector: '.user',
            nameSelector: '.name',
            toggleTimelineSelector: '.toggle-timeline'
        });

        this.after('initialize', function() {
            var self = this;

            this.menuBarWidth = 30;
            requestAnimationFrame(function() {
                self.menuBarWidth = $('.menubar-pane').width();
            })

            this.updateDiffBadge = _.throttle(this.updateDiffBadge.bind(this), UPDATE_WORKSPACE_DIFF_SECONDS * 1000, { leading: false })

            this.$node.hide().html(template({}));

            this.updateUserTooltip({user: visalloData.currentUser});

            requestAnimationFrame(function() {
                self.$node.addClass('visible');
            });

            this.on(document, 'workspaceSaving', this.onWorkspaceSaving);
            this.on(document, 'didToggleDisplay', this.onDidToggleDisplay);
            this.on(document, 'workspaceSaved', this.onWorkspaceSaved);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'workspaceUpdated', this.onWorkspaceUpdated);
            this.on(document, 'switchWorkspace', this.onSwitchWorkspace);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);

            this.on(document, 'verticesUpdated', this.updateDiffBadge);
            this.on(document, 'verticesDeleted', this.updateDiffBadge);
            this.on(document, 'edgesUpdated', this.updateDiffBadge);
            this.on(document, 'edgesDeleted', this.updateDiffBadge);
            this.on(document, 'ontologyUpdated', this.updateDiffBadge);
            this.on(document, 'textUpdated', this.updateDiffBadge);
            this.on(document, 'updateDiff', this.updateDiffBadge);

            this.on(document, 'toggleDiffPanel', this.toggleDiffPanel);
            this.on(document, 'escape', this.closeDiffPanel);

            this.on('click', {
                toggleTimelineSelector: this.onToggleTimeline
            });

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: ['graph.help.scope', 'map.help.scope'].map(i18n),
                shortcuts: {
                    'alt-d': { fire: 'toggleDiffPanel', desc: i18n('workspaces.help.show_diff') }
                }
            });

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: ['graph.help.scope', 'map.help.scope'].map(i18n),
                shortcuts: {
                    'alt-i': { fire: 'toggleTimeline', desc: i18n('workspaces.help.toggle_timeline') }
                }
            });
        });

        this.onDidToggleDisplay = function(event, data) {
            if (data.name === 'products-full' && data.visible) {
                this.select('toggleTimelineSelector').show();
            } else if (data.visible && data.type === 'full') {
                this.select('toggleTimelineSelector').hide()
                this.trigger('toggleTimeline');
            }
        };

        this.onToggleTimeline = function() {
            this.trigger('toggleTimeline');
        }

        this.toggleDiffPanel = function() {
            var badge = this.$node.find('.badge');
            if (badge.is(':visible')) {
                badge.popover('toggle');
            }
        };

        this.closeDiffPanel = function() {
            var badge = this.$node.find('.badge');
            if (badge.is(':visible')) {
                badge.popover('hide');
            }
        };

        this.onGraphPaddingUpdated = function(event, data) {
            this.$node.css('left', data.padding.l + this.menuBarWidth);

            var width = $(window).width(),
                height = $(window).height(),
                popover = this.$node.find('.popover'),
                paddingH = 100,
                paddingV = 75,
                popoverCss = {
                    maxWidth: (width - this.menuBarWidth - (data.padding.l + data.padding.r) - paddingH),
                    maxHeight: (height - (data.padding.t + data.padding.b) - paddingV)
                };

            this.popoverCss = popoverCss;
            if (popover.length) {
                this.updatePopoverSize(popover);
            }
        };

        this.updatePopoverSize = function(tip) {
            var css = {};
            if (tip.width() > this.popoverCss.maxWidth) {
                css.width = this.popoverCss.maxWidth + 'px';
            }
            if (tip.height() > this.popoverCss.maxHeight) {
                css.height = this.popoverCss.maxHeight + 'px';
            }

            tip.resizable('option', 'maxWidth', this.popoverCss.maxWidth);
            tip.resizable('option', 'maxHeight', this.popoverCss.maxHeight);
            if (_.keys(css).length) {
                tip.css(css);
            }
        }

        this.setContent = function(title, editable, commentable) {
            this.select('nameSelector').text(title);
        };

        this.updateWithNewWorkspaceData = function(workspace) {
            this.setContent(
                workspace.title,
                workspace.editable,
                workspace.commentable
            );
            clearTimeout(this.updateTimer);
        };

        this.onSwitchWorkspace = function(event, data) {
            if (this.previousWorkspace !== data.workspaceId) {
                this.previousDiff = null;
                this.$node.find('.badge').popover('destroy').remove();
            }
        };

        this.onWorkspaceLoaded = function(event, data) {
            this.$node.show();
            this.updateWithNewWorkspaceData(data);
            this.previousWorkspace = data.workspaceId;
            this.updateDiffBadge();
        };

        this.onWorkspaceUpdated = function(event, data) {
            if (visalloData.currentWorkspaceId === data.workspace.workspaceId) {
                this.updateWithNewWorkspaceData(data.workspace);
            }
        };

        this.onWorkspaceSaving = function(event, data) {
            clearTimeout(this.updateTimer);
        };

        this.onWorkspaceSaved = function(event, data) {
            clearTimeout(this.updateTimer);
            this.lastSaved = F.date.utc(Date.now());

            if (data.title) {
                this.select('nameSelector').text(data.title);
            }
        };

        this.onDiffBadgeMouse = function(event) {
            this.trigger(
                event.type === 'mouseenter' ? 'focusElements' : 'defocusElements',
                { elementIds: this.currentDiffIds || [] }
            );
        };

        this.updateDiffBadge = function(event, data) {
            var self = this,
                node = this.select('nameSelector'),
                badge = this.$node.find('.badge');

            if (!badge.length) {
                badge = $('<span class="badge"></span>')
                    .insertAfter(node)
                    .on('mouseenter mouseleave', this.onDiffBadgeMouse.bind(this))
            }

            this.dataRequest('workspace', 'diffCount').then(diffCount => {
                const { total, ids } = diffCount;
                const same = this.previousDiff && _.isEqual(diffCount, this.previousCounts);
                if (same) {
                    return;
                }
                this.previousCounts = diffCount;
                this.formattedCount = F.number.pretty(total);

                this.currentDiffIds = ids;
                if (this.currentDiffIds.length > MAX_HOVER_IDS) {
                    this.currentDiffIds = null;
                }

                var popover = badge.data('popover'),
                    tip = popover && popover.tip();

                if (tip && tip.is(':visible')) {
                    if (this.attacher) {
                        this.attacher.params({ total }).attach()
                    }
                    popover.show();
                } else {

                    //require([
                        //'util/component/attacher',
                        //'workspaces/diff/DiffContainer'
                    //], (Attacher, DiffContainer) => {
                        badge
                            .popover('destroy')
                            .popover({
                                placement: 'top',
                                content: i18n('workspaces.diff.loading'),
                                title: i18n('workspaces.diff.header.unpublished_changes')
                            });

                        popover = badge.data('popover');
                        tip = popover.tip();

                        tip.css({ width: '400px', height: '250px' }).data('sizePreference', 'diff')

                        // We fill in our own content
                        popover.setContent = function() {}
                        const teardown = () => {
                            if (this.attacher) {
                                this.attacher.teardown();
                                this.attacher = null;
                            }
                        };
                        badge.on('shown', () => {
                            var left = 10;
                            tip.find('.arrow').css({
                                left: parseInt(badge.position().left - (left / 2) + badge.width() / 2, 10) + 'px',
                                marginLeft: 0
                            })

                            var css = {
                                top: (parseInt(tip.css('top')) - 10) + 'px'
                            };
                            tip.resizable({
                                handles: 'n, e, ne',
                                maxWidth: this.popoverCss.maxWidth,
                                maxHeight: this.popoverCss.maxHeight
                            }).css({top: top});

                            this.updatePopoverSize(tip);

                            const $popoverContent = tip.find('.popover-content');

                            $popoverContent
                                .toggleClass(
                                    'loading-small-animate',
                                    !this.attacher
                                );

                            require([
                                'util/component/attacher',
                                'workspaces/diff/DiffContainer'
                            ], (Attacher, DiffContainer) => {
                                if (this.attacher) {
                                    this.attacher.params({ total }).attach();
                                } else {
                                    this.attacher = Attacher()
                                        .node($popoverContent)
                                        .params({ total })
                                        .component(DiffContainer)

                                    this.attacher.attach().then(() => {
                                        $popoverContent.removeClass('loading-small-animate');
                                    })
                                }
                            });
                        }).on('hide', teardown)

                        teardown();
                    //});
                }

                badge.removePrefixedClasses('badge-').addClass('badge-info')
                    .attr('title', i18n('workspaces.diff.unpublished_change.' + (
                        total === 1 ?
                        'one' : 'some'), this.formattedCount))
                    .text(total > 0 ? this.formattedCount : '');

                if (total > 0) {
                    this.animateBadge(badge);
                } else if (total === 0) {
                    badge.popover('destroy');
                }
            }).catch(error => {
                console.error(error)
                badge.removePrefixedClasses('badge-').addClass('badge-error').text('Error')
                this.animateBadge(badge);
            })
        };

        var badgeReset, animateTimer;
        this.animateBadge = function(badge) {
            badge.text(this.formattedCount).css('width', 'auto');

            var self = this,
                html = '<span class="number">' + this.formattedCount + '</span>' +
                    '<span class="suffix"> ' + i18n('workspaces.diff.unpublished') + '</span>',
                previousWidth = badge.outerWidth(),
                findWidth = function() {
                    return (
                        badge.find('.number').outerWidth(true) +
                        badge.find('.suffix').outerWidth(true) +
                        parseInt(badge.css('paddingRight'), 10) * 2
                    ) + 'px';
                };

            if (animateTimer) {
                clearTimeout(animateTimer);
                animateTimer = _.delay(
                    badgeReset.bind(null, previousWidth),
                    SHOW_UNPUBLUSHED_CHANGES_SECONDS * 1000
                );
                return badge.html(html).css({ width: findWidth() })
            }

            var duration = '0.5s';
            badge.css({
                width: previousWidth + 'px',
                backgroundColor: '#0088cc',
                transition: 'all cubic-bezier(.29,.79,0,1.48) ' + duration,
                position: 'relative'
            }).html(html);

            requestAnimationFrame(function() {
                badge.css({
                    backgroundColor: '#1ab2ff',
                    width: findWidth()
                }).find('.suffix').css({
                    transition: 'opacity ease-out ' + duration
                })

                animateTimer = _.delay((badgeReset = function(previousWidth) {
                    animateTimer = null;
                    badge.on(TRANSITION_END, function(e) {
                        if (e.originalEvent.propertyName === 'width') {
                            badge.off(TRANSITION_END);
                            badge.text(self.formattedCount).css('width', 'auto');
                        }
                    }).css({
                        transition: 'all cubic-bezier(.92,-0.42,.37,1.31) ' + duration,
                        backgroundColor: '#0088cc',
                        width: previousWidth + 'px'
                    }).find('.suffix').css('opacity', 0);
                }).bind(null, previousWidth), SHOW_UNPUBLUSHED_CHANGES_SECONDS * 1000);
            })
        };

        this.updateUserTooltip = function(data) {
            if (data && data.user) {
                this.select('userSelector')
                    .text(data.user.displayName)
                    .css({ cursor: 'pointer' })
                    .on('click', function() {
                        require([
                            'workspaces/userAccount/modal.hbs',
                            'workspaces/userAccount/userAccount'
                        ], function(modalTemplate, UserAccount) {
                            var modal = $(modalTemplate({
                                displayName: visalloData.currentUser.displayName
                            })).appendTo(document.body);
                            UserAccount.attachTo(modal);
                            modal.modal('show');
                        });
                    })
                    .tooltip({
                        placement: 'right',
                        title: i18n('workspaces.overlay.open.useraccount'),
                        trigger: 'hover',
                        delay: { show: 250, hide: 0 }
                    })
            }
        }
    }
});
