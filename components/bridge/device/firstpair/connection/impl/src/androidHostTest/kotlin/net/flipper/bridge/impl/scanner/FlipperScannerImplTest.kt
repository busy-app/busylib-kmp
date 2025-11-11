package net.flipper.bridge.impl.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.api.scanner.FlipperScanner
import no.nordicsemi.kotlin.ble.android.mock.LatestApi
import no.nordicsemi.kotlin.ble.android.mock.MockEnvironment
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.mock.MockCentralManager
import no.nordicsemi.kotlin.ble.client.android.mock.mock
import kotlin.test.BeforeTest
import kotlin.test.Test

@Suppress("FunctionNaming")
class FlipperScannerImplTest {
    private lateinit var context: Context
    private lateinit var centralManager: MockCentralManager

    @BeforeTest
    fun setUp() {
        context = mockk()
        every {
            context.checkPermission(
                Manifest.permission.BLUETOOTH_CONNECT,
                any(),
                any()
            )
        } returns PackageManager.PERMISSION_GRANTED
    }

    @Test(expected = SecurityException::class)
    fun `request bluetooth permission`() = runTest {
        val flipperScanner = mockFlipperScanner(
            LatestApi(
                isBluetoothScanPermissionGranted = false
            )
        )

        flipperScanner.findFlipperDevices().first()
    }

    @Test
    fun `not request bluetooth permission on old device`() {
        val childScope = TestScope()
        centralManager = CentralManager.Factory.mock(
            scope = childScope,
            environment = MockEnvironment.Api26()
        )
        val flipperScanner = FlipperScannerImpl(
            centralManager = centralManager,
            context = context,
            bluetoothAdapter = mockk(),
        )

        flipperScanner.findFlipperDevices()
        childScope.cancel()
    }

    /*
    @Test
    fun `find correct single device`() = runTest {
        val flipperScanner = mockFlipperScanner(MockEnvironment.Api26())

        centralManager.simulatePeripherals(
            listOf(
                PeripheralSpec.simulatePeripheral(TEST_MAC_ADDRESS) {
                    bonded()
                }
            )
        )

        val flipperDevices = flipperScanner.findFlipperDevices().first()

        val foundDevice = flipperDevices.firstOrNull()
        assertNotNull(foundDevice)
        assertEquals(TEST_MAC_ADDRESS, foundDevice.device.address)
    }

    @Test
    fun `find correct two device`() = runTest {
        val bluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns "Flipper Test"
            every { address } returns ""
        }
        val secondBluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns null
            every { address } returns "80:E1:26:AF:AF:26"
        }

        val sr = ScanResult(bluetoothDevice, 0, 0, 0, 0, 0, 0, 0, null, 0L)
        val sr2 = ScanResult(secondBluetoothDevice, 0, 0, 0, 0, 0, 0, 0, null, 0L)

        every { scanner.scanFlow(any(), any()) } returns flowOf(sr, sr2)
        every { bluetoothAdapter.bondedDevices } returns emptySet()

        val flipperDevices = mutableListOf<Iterable<DiscoveredBluetoothDevice>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            flipperScanner.findFlipperDevices().toList(flipperDevices)
        }
        val resultList = flipperDevices.last().toList()
        val foundDevice = resultList.getOrNull(0)
        val foundDevice2 = resultList.getOrNull(1)
        Assert.assertNotNull(foundDevice)
        Assert.assertNotNull(foundDevice2)
        Assert.assertEquals(bluetoothDevice, foundDevice!!.device)
        Assert.assertEquals(secondBluetoothDevice, foundDevice2!!.device)
        collectJob.cancelAndJoin()
    }

    @Test
    fun `filter device by mac`() = runTest {
        val bluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns null
            every { address } returns "80:E1:26:A1:4C:2D"
        }

        val sr = ScanResult(bluetoothDevice, 0, 0, 0, 0, 0, 0, 0, null, 0L)

        every { scanner.scanFlow(any(), any()) } returns flowOf(sr)
        every { bluetoothAdapter.bondedDevices } returns emptySet()

        val flipperDevices = flipperScanner.findFlipperDevices().first()

        val foundDevice = flipperDevices.firstOrNull()
        Assert.assertNotNull(foundDevice)
        Assert.assertEquals(bluetoothDevice, foundDevice!!.device)
    }

    @Test
    fun `filter device by name`() = runTest {
        val bluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns "Flipper Dumper"
            every { address } returns ""
        }

        val sr = ScanResult(bluetoothDevice, 0, 0, 0, 0, 0, 0, 0, null, 0L)

        every { scanner.scanFlow(any(), any()) } returns flowOf(sr)
        every { bluetoothAdapter.bondedDevices } returns emptySet()

        val flipperDevices = flipperScanner.findFlipperDevices().first()

        val foundDevice = flipperDevices.firstOrNull()
        Assert.assertNotNull(foundDevice)
        Assert.assertEquals(bluetoothDevice, foundDevice!!.device)
    }

    @Test
    fun `block device with incorrect name`() = runTest {
        val bluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns "Dupper"
            every { address } returns ""
        }

        val sr = ScanResult(bluetoothDevice, 0, 0, 0, 0, 0, 0, 0, null, 0L)

        every { scanner.scanFlow(any(), any()) } returns flowOf(sr)
        every { bluetoothAdapter.bondedDevices } returns emptySet()

        val flipperDevices = mutableListOf<Iterable<DiscoveredBluetoothDevice>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            flipperScanner.findFlipperDevices().toList(flipperDevices)
        }

        Assert.assertTrue(flipperDevices.toList().isEmpty())

        collectJob.cancelAndJoin()
    }

    @Test
    fun `block device with empty name`() = runTest {
        val bluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns ""
            every { address } returns ""
        }

        val sr = ScanResult(bluetoothDevice, 0, 0, 0, 0, 0, 0, 0, null, 0L)

        every { scanner.scanFlow(any(), any()) } returns flowOf(sr)
        every { bluetoothAdapter.bondedDevices } returns emptySet()

        val flipperDevices = mutableListOf<Iterable<DiscoveredBluetoothDevice>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            flipperScanner.findFlipperDevices().toList(flipperDevices)
        }

        Assert.assertTrue(flipperDevices.toList().isEmpty())

        collectJob.cancelAndJoin()
    }

    @Test
    fun `block device with incorrect mac`() = runTest {
        val bluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns null
            every { address } returns "81:E1:26:A1:4C:2D"
        }

        val sr = ScanResult(bluetoothDevice, 0, 0, 0, 0, 0, 0, 0, null, 0L)

        every { scanner.scanFlow(any(), any()) } returns flowOf(sr)
        every { bluetoothAdapter.bondedDevices } returns emptySet()

        val flipperDevices = mutableListOf<Iterable<DiscoveredBluetoothDevice>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            flipperScanner.findFlipperDevices().toList(flipperDevices)
        }

        Assert.assertTrue(flipperDevices.toList().isEmpty())

        collectJob.cancelAndJoin()
    }

    @Test
    fun `block device with empty mac`() = runTest {
        val bluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns null
            every { address } returns ""
        }

        val sr = ScanResult(bluetoothDevice, 0, 0, 0, 0, 0, 0, 0, null, 0L)

        every { scanner.scanFlow(any(), any()) } returns flowOf(sr)
        every { bluetoothAdapter.bondedDevices } returns emptySet()

        val flipperDevices = mutableListOf<Iterable<DiscoveredBluetoothDevice>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            flipperScanner.findFlipperDevices().toList(flipperDevices)
        }

        Assert.assertTrue(flipperDevices.toList().isEmpty())

        collectJob.cancelAndJoin()
    }

    @Test
    fun `answer bounded device with correct mac`() = runTest {
        val alreadyConnectedBluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns null
            every { address } returns "80:E1:26:A1:4C:2D"
        }

        every { bluetoothAdapter.bondedDevices } returns setOf(
            alreadyConnectedBluetoothDevice
        )

        every { scanner.scanFlow(any(), any()) } returns emptyFlow()

        val flipperDevices = flipperScanner.findFlipperDevices().first()

        val foundDevice = flipperDevices.firstOrNull()
        Assert.assertNotNull(foundDevice)
        Assert.assertEquals(alreadyConnectedBluetoothDevice, foundDevice!!.device)
    }

    @Test
    fun `answer bounded device with correct name`() = runTest {
        val alreadyConnectedBluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns "Flipper Test"
            every { address } returns ""
        }

        every { bluetoothAdapter.bondedDevices } returns setOf(
            alreadyConnectedBluetoothDevice
        )

        every { scanner.scanFlow(any(), any()) } returns emptyFlow()

        val flipperDevices = flipperScanner.findFlipperDevices().first()

        val foundDevice = flipperDevices.firstOrNull()
        Assert.assertNotNull(foundDevice)
        Assert.assertEquals(alreadyConnectedBluetoothDevice, foundDevice!!.device)
    }

    @Test
    fun `not answer bounded device without correct name or mac`() = runTest {
        val alreadyConnectedBluetoothDevice = mockk<BluetoothDevice> {
            every { name } returns null
            every { address } returns ""
        }

        every { bluetoothAdapter.bondedDevices } returns setOf(
            alreadyConnectedBluetoothDevice
        )

        every { scanner.scanFlow(any(), any()) } returns emptyFlow()

        val flipperDevices = mutableListOf<Iterable<DiscoveredBluetoothDevice>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            flipperScanner.findFlipperDevices().toList(flipperDevices)
        }

        Assert.assertTrue(flipperDevices.toList().isEmpty())

        collectJob.cancelAndJoin()
    }*/

    private fun TestScope.mockFlipperScanner(
        environment: MockEnvironment = LatestApi()
    ): FlipperScanner {
        centralManager = CentralManager.Factory.mock(this, environment)
        advanceUntilIdle() // https://github.com/NordicSemiconductor/Kotlin-BLE-Library/issues/217
        return FlipperScannerImpl(
            centralManager = centralManager,
            context = context,
            bluetoothAdapter = mockk()
        )
    }
}
