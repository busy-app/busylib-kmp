package net.flipper.bsb.watchers.provisioning.fakes

import net.flipper.bsb.watchers.provisioning.CloudFetcherWatcher

internal data class CloudFetcherTestSetup(
    val watcher: CloudFetcherWatcher,
    val storage: FakePersistedStorage,
    val cloudBarsApi: FakeCloudBarsApi
)
