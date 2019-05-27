#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
define(['public/v1/api'], function(openlumify) {
    'use strict';

    openlumify.registry.registerExtension('org.openlumify.authentication', {
        componentPath: '${packageInPathFormat}/auth/authentication'
    })
});
