package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_Timer.Profiles
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object ProfilesProtobufMapper {
    fun map(profiles: Profiles): BusyLibUpdateEvent.Profiles {
        val byName = profiles.profiles.mapNotNull { profile ->
            val json = profile.json?.data_?.toByteArray()?.decodeToString() ?: return@mapNotNull null
            if (profile.name.isEmpty()) return@mapNotNull null
            profile.name to json
        }.toMap()
        return BusyLibUpdateEvent.Profiles(byName = byName)
    }
}
