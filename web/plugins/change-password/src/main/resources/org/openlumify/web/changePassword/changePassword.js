define(['configuration/plugins/registry'], function(registry) {
    'use strict';

    registry.registerExtension('org.openlumify.user.account.page', {
        identifier: 'changePassword',
        pageComponentPath: 'org.openlumify.useraccount.changePassword'
    });

    define('org.openlumify.useraccount.changePassword', [
        'flight/lib/component',
        'util/withFormFieldErrors',
        'tpl!util/alert'
    ], function(defineComponent, withFormFieldErrors, alertTemplate) {
        return defineComponent(ChangePassword, withFormFieldErrors);

        function ChangePassword() {
            this.defaultAttrs({
                buttonSelector: 'button'
            });

            this.after('initialize', function() {
                var self = this;

                require(['org/openlumify/web/changePassword/template.hbs'], function(template) {
                    self.$node.html(template({}));
                });

                this.on('click', {
                    buttonSelector: this.onChange
                })
            });

            this.onChange = function(event) {
                var self = this,
                    btn = $(event.target).addClass('loading').prop('disabled', true);

                this.clearFieldErrors(this.$node);
                this.$node.find('.alert-info').remove();

                $.post('changePassword', {
                    currentPassword: this.$node.find('.current').val(),
                    newPassword: this.$node.find('.new').val(),
                    newPasswordConfirmation: this.$node.find('.confirm').val(),
                    csrfToken: openlumifyData.currentUser.csrfToken
                })
                    .always(function() {
                        btn.removeClass('loading').prop('disabled', false);
                    })
                    .fail(function(e) {
                        self.markFieldErrors(e && e.responseText || e, self.$node);
                    })
                    .done(function() {
                        self.$node.prepend(alertTemplate({
                            message: i18n('useraccount.page.changePassword.success')
                        }));
                        self.$node.find('input').each(function() {
                            $(this).val('');
                        });
                    })
            };
        }
    })
})
