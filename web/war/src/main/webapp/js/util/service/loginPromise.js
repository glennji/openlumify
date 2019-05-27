define(['util/promise', 'jquery'], function() {
    'use strict';

    return new Promise(function(f) {
        if ('openlumifyData' in window && openlumifyData.currentUser) {
            return f();
        }

        $(document)
            .on('currentUserOpenLumifyDataUpdated', function() {
                f();
            })
    })
});
