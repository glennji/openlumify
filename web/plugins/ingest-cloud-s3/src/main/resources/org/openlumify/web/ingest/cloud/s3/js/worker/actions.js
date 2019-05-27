define(['data/web-worker/store/actions'], function(actions) {
    actions.protectFromWorker();

    return actions.createActions({
        workerImpl: 'org/openlumify/web/ingest/cloud/s3/dist/actions-impl',
        actions: {
            connect: (providerClass, credentials) => ({ providerClass, credentials }),
            openDirectory: (name) => (name),
            selectItem: (name) => (name),
            importSelected: () => {},
            refreshDirectories: () => {}
        }
    })
})
