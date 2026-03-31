package net.flipper.bridge.connection.feature.events.proto.protomapper.delegates

import BSB_Input.ButtonAction
import BSB_Input.InputEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent

object InputProtobufMapper {
    fun map(input: InputEvent): BusyLibUpdateEvent.Input? {
        val buttonEvent = input.button_event
        val switchEvent = input.switch_event
        val encoderEvent = input.encoder_event
        return when {
            buttonEvent != null -> {
                BusyLibUpdateEvent.Input.Button(
                    buttonName = buttonEvent.button.toString(),
                    isPressed = buttonEvent.action == ButtonAction.PRESS,
                )
            }
            switchEvent != null -> {
                BusyLibUpdateEvent.Input.Switch(
                    position = switchEvent.position.toString(),
                )
            }
            encoderEvent != null -> {
                BusyLibUpdateEvent.Input.Encoder(
                    delta = encoderEvent.delta,
                )
            }
            else -> null
        }
    }
}
