package net.flipper.bsb.watchers.provisioning

/**
 * Triggers a re-fetch and reconciliation of cloud devices against local storage.
 *
 * Extracted from [CloudFetcherWatcher] so collaborators (e.g.
 * [HardwareIdProvisioningWatcher]) depend on the invalidation capability rather
 * than the concrete watcher.
 */
interface CloudInvalidator {
    fun invalidate()
}
