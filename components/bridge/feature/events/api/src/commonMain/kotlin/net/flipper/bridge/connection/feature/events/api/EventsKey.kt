package net.flipper.bridge.connection.feature.events.api

data class EventsKey(val obj: Any)

fun List<UpdateEventData>.asEventsKey(): EventsKey {
    return EventsKey(this.map(UpdateEventData::sentAt).joinToString { it.toString() })
}
