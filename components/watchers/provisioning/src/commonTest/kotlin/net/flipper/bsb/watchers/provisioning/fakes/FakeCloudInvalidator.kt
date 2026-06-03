package net.flipper.bsb.watchers.provisioning.fakes

import net.flipper.bsb.watchers.provisioning.CloudInvalidator

internal class FakeCloudInvalidator : CloudInvalidator {
    var invalidateCount: Int = 0
        private set

    override fun invalidate() {
        invalidateCount++
    }
}
