package net.flipper.bridge.connection.transport.ble.api

// Max chunk size, limited by firmware:
// https://github.com/flipperdevices/bsb-firmware/blob/d429cf84d9ec7a0160b22726491aca7aef259c8d/applications/system/ble_usart_echo/ble_usart_echo.c#L20
const val MAX_ATTRIBUTE_SIZE = 237
