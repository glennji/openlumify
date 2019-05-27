define(['configuration/plugins/registry'], function(registry) {
    'use strict';

    registry.registerExtension('org.openlumify.user.account.page', {
        identifier: 'changeEmail',
        pageComponentPath: 'org.openlumify.useraccount.changeEmail'
    });

    define('org.openlumify.useraccount.changeEmail', [
        'flight/lib/component',
        'util/withFormFieldErrors',
        'tpl!util/alert'
    ], function(defineComponent, withFormFieldErrors, alertTemplate) {
        return defineComponent(ChangeEmail, withFormFieldErrors);

        function ChangeEmail() {
            this.defaultAttrs({
                buttonSelector: 'button.btn-primary',
                inputSelector: 'input'
            });

            this.after('initialize', function() {
                var self = this;

                require(['org/openlumify/web/changeEmail/template.hbs'], function(template) {
                    self.$node.html(template({
                        email: openlumifyData.currentUser.email
                    }));
                    self.validateEmail();
                });

                this.on('click', {
                    buttonSelector: this.onChange
                });
                this.on('change keyup', {
                    inputSelector: this.validateEmail
                });
            });

            this.validateEmail = function(event) {
                var inputs = this.select('inputSelector'),
                    anyInvalid = inputs.filter(function(i, input) {
                                    return input.validity && !input.validity.valid;
                                 }).length;

                this.select('buttonSelector').prop('disabled', Boolean(anyInvalid));
            };

            this.onChange = function(event) {
                var self = this,
                    btn = $(event.target).addClass('loading').prop('disabled', true),
                    newEmail = this.$node.find('.current').val();

                this.clearFieldErrors(this.$node);
                this.$node.find('.alert-info').remove();

                $.post('changeEmail', {
                    email: newEmail,
                    csrfToken: openlumifyData.currentUser.csrfToken
                })
                    .always(function() {
                        btn.removeClass('loading').prop('disabled', false);
                    })
                    .fail(function(e) {
                        self.markFieldErrors(e && e.responseText || e, self.$node);
                    })
                    .done(function() {
                        openlumifyData.currentUser.email = newEmail;
                        self.$node.prepend(alertTemplate({
                            message: i18n('useraccount.page.changeEmail.success')
                        }));
                    })
            };
        }
    })
})
