package net.flipper.bridge.connection.feature.rpc.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("MagicNumber")
object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AstraKDuration", PrimitiveKind.STRING)

    private enum class Delimiter(val value: String) {
        W("w"),
        D("d"),
        H("h"),
        M("m"),
        S("s")
    }

    private inline fun <T> Iterable<T>.sumOf(selector: (T) -> Duration): Duration {
        var sum: Duration = 0.seconds
        for (element in this) {
            sum += selector(element)
        }
        return sum
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        val string = fromDuration(value)
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Duration {
        val string = decoder.decodeString()
        return toDuration(string)
    }

    fun fromDuration(duration: Duration): String {
        return duration.toComponents { days, hours, minutes, seconds, _ ->
            buildString {
                if (days >= 7) {
                    append("${days / 7}${Delimiter.W.value}")
                    append(" ")
                }
                if (days % 7 != 0L) {
                    append("${days % 7}${Delimiter.D.value}")
                    append(" ")
                }
                if (hours > 0) {
                    append("${hours}${Delimiter.H.value}")
                    append(" ")
                }
                if (minutes > 0) {
                    append("${minutes}${Delimiter.M.value}")
                    append(" ")
                }
                if (seconds > 0) {
                    append("${seconds}${Delimiter.S.value}")
                    append(" ")
                }
                if (days.plus(hours).plus(minutes).plus(seconds) == 0L) {
                    append("0${Delimiter.S.value}")
                    append(" ")
                }
            }.trimEnd()
        }
    }

    // 1 year 2 month 3 weeks 4 days 5 hours 10 minutes 30 seconds
    // 3w4d6h10m30s
    @Suppress("MaxLineLength")
    fun toDuration(value: String): Duration {
        val split = value
            .replace(Delimiter.W.value, Delimiter.W.value.plus(" "))
            .replace(Delimiter.D.value, Delimiter.D.value.plus(" "))
            .replace(Delimiter.H.value, Delimiter.H.value.plus(" "))
            .replace(Delimiter.M.value, Delimiter.M.value.plus(" "))
            .replace(Delimiter.S.value, Delimiter.S.value.plus(" "))
            .split(" ")
            .filter { string -> string.isNotBlank() }
        val durationList = split.map { part ->
            val delimiter = Delimiter.entries
                .firstOrNull { delimiter -> part.contains(delimiter.value) }
                ?: error("Wrong usage on argument. Could not determine delimiter $value. Should be as 3w4d6h10m30s")

            val intAmount = part
                .replace(delimiter.value, "")
                .toIntOrNull()
                ?: error("Wrong usage on argument. Could not convert to int $value. Should be as 3w4d6h10m30s")

            when (delimiter) {
                Delimiter.W -> (intAmount * 7).days
                Delimiter.D -> intAmount.days
                Delimiter.H -> intAmount.hours
                Delimiter.M -> intAmount.minutes
                Delimiter.S -> intAmount.seconds
            }
        }
        return durationList.sumOf { duration -> duration }
    }
}
