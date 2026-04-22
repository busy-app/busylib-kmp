package net.flipper.bsb.watchers.provisioning.fakes

import net.flipper.bsb.watchers.provisioning.HardwareIdProvisioningWatcher

internal data class HardwareIdProvisioningTestSetup(
    val watcher: HardwareIdProvisioningWatcher,
    val storage: FakePersistedStorage
)
