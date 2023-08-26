package code.z.pidcontroller

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import code.z.pidcontroller.ui.theme.PIDControllerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val CamingoCodeFont = FontFamily(
        Font(R.font.camingo_code_regular, FontWeight.Normal),
        Font(R.font.camingo_code_bold, FontWeight.Bold),
        Font(R.font.camingo_code_italic, FontWeight.Normal)
    )

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null

    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val kpIncrementCode = 'P'
    private val kpDecrementCode = 'Q'
    private val kdIncrementCode = 'D'
    private val kdDecrementCode = 'E'
    private val kiIncrementCode = 'I'
    private val kiDecrementCode = 'J'
    private val startCode = 'S'
    private val stopCode = 'T'

    private val baseTextColor = Color(0xFF36395A)
    private val baseBackgroundColor = Color(0xFFFCFCFD)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            showToast("Bluetooth is off!")
            finish()
        }

        setContent {
            PIDControllerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // PID controllers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    paddingValues = PaddingValues(
                                        5.dp, 0.dp
                                    )
                                ), horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val kp = remember { mutableStateOf(0.0F) }
                            val kd = remember { mutableStateOf(0.0F) }
                            val ki = remember { mutableStateOf(0.0F) }

                            ControllerColumn(
                                constant = "Kp",
                                state = kp,
                                incrementCode = kpIncrementCode,
                                decrementCode = kpDecrementCode
                            )
                            ControllerColumn(
                                constant = "Kd",
                                state = kd,
                                incrementCode = kdIncrementCode,
                                decrementCode = kdDecrementCode
                            )
                            ControllerColumn(
                                constant = "Ki",
                                state = ki,
                                incrementCode = kiIncrementCode,
                                decrementCode = kiDecrementCode
                            )
                        }

                        // Connection status
                        val connectionStatus = remember { mutableStateOf(false) }
                        val showDialog = remember { mutableStateOf(false) }

                        Text(
                            text = if (!connectionStatus.value) "Not Connected" else "Connected",
                            color = if (!connectionStatus.value) Color.Red else Color.Green,
                            modifier = Modifier.padding(
                                paddingValues = PaddingValues(
                                    0.dp, 50.dp
                                )
                            ),
                            fontFamily = CamingoCodeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )

                        // Connection controllers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    paddingValues = PaddingValues(
                                        5.dp, 10.dp, 5.dp, 0.dp
                                    )
                                ), horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CustomButton(buttonText = "Connect", width = 130.dp, onClick = {
                                showDialog.value = true
                            })

                            CustomButton(buttonText = "Disconnect", width = 130.dp, onClick = {
                                if (connectionStatus.value) {
                                    disconnectBluetooth()
                                    connectionStatus.value = false
                                    showToast("Disconnected")
                                }
                            })

                            if (showDialog.value) {
                                BluetoothDeviceListDialog(
                                    showDialog = showDialog,
                                    connectionStatus = connectionStatus,
                                    onDismissRequest = {
                                        showDialog.value = false
                                        if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                                            connectionStatus.value = true
                                        }
                                    })
                            }
                        }

                        // Bot on/off controller
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    paddingValues = PaddingValues(
                                        5.dp, 10.dp, 5.dp, 0.dp
                                    )
                                ), horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CustomButton(
                                buttonText = "Start",
                                width = 130.dp,
                                onClick = {
                                    if (connectionStatus.value) {
                                        sendDataOverBluetooth(startCode)
                                    } else {
                                        showToast("Bluetooth is not connected!")
                                    }
                                })
                            CustomButton(
                                buttonText = "Stop",
                                width = 130.dp,
                                onClick = {
                                    if (connectionStatus.value) {
                                        sendDataOverBluetooth(stopCode)
                                    } else {
                                        showToast("Bluetooth is not connected!")
                                    }
                                })
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BluetoothDeviceListDialog(
        showDialog: MutableState<Boolean>,
        connectionStatus: MutableState<Boolean>,
        onDismissRequest: () -> Unit
    ) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showToast("Bluetooth or Nearby Share permission is not set!")
            finish()
        }

        val pairedDevices = bluetoothAdapter.bondedDevices?.map { device -> device } ?: emptyList()

        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {},
            title = { Text(text = "Paired Devices") },
            text = {
                LazyColumn {
                    items(pairedDevices) { device ->
                        CustomButton(
                            buttonText = device.name,
                            subText = device.address,
                            height = 60.dp,
                            maxWidth = true,
                            onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(100)
                                    try {
                                        bluetoothSocket =
                                            device.createRfcommSocketToServiceRecord(uuid)
                                        bluetoothSocket?.connect()
                                        connectionStatus.value = true
                                        showToast("Device connected")
                                    } catch (e: IOException) {
                                        disconnectBluetooth()
                                        connectionStatus.value = false
                                        showToast("Failed to connect!")
                                    }
                                    showDialog.value = false
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            })
    }

    @Composable
    fun CustomButton(
        buttonText: String? = null,
        subText: String? = null,
        rId: Int? = null,
        height: Dp = 80.dp,
        width: Dp = 85.dp,
        maxWidth: Boolean = false,
        textColor: Color = baseTextColor,
        fontFamily: FontFamily = CamingoCodeFont,
        backgroundColor: Color = baseBackgroundColor,
        shape: Shape = RectangleShape,
        onClick: () -> Unit = {}
    ) {
        Button(
            onClick = onClick,
            shape = shape,
            modifier = if (!maxWidth) Modifier
                .size(width, height)
                .fillMaxSize() else Modifier
                .height(height)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor)
        ) {
            Column {
                if (buttonText != null) {
                    Text(
                        text = buttonText,
                        fontFamily = fontFamily,
                        color = textColor,
                    )
                } else {
                    Image(
                        painter = painterResource(id = rId!!),
                        contentDescription = null,
                    )
                }

                if (subText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subText, fontFamily = fontFamily, color = textColor
                    )
                }
            }
        }
    }


    @Composable
    fun ControllerColumn(
        constant: String,
        state: MutableState<Float>,
        incrementCode: Char,
        decrementCode: Char,
        height: Dp = 200.dp,
        width: Dp = 85.dp,
        textColor: Color = baseTextColor,
        backgroundColor: Color = baseBackgroundColor,
        fontFamily: FontFamily = CamingoCodeFont,
    ) {
        Column(
            modifier = Modifier
                .size(width, height)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomButton(
                rId = R.drawable.plus,
                height = 55.dp,
                maxWidth = true,
                onClick = {
                    if (bluetoothSocket == null) {
                        showToast("Bluetooth is not connected")
                        return@CustomButton
                    }
                    state.value += 0.1F
                    sendDataOverBluetooth(incrementCode)
                },
                backgroundColor = Color(android.R.color.black)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = constant,
                fontFamily = fontFamily,
                color = textColor,
                modifier = Modifier
                    .size(width, 35.dp)
                    .background(backgroundColor),
                textAlign = TextAlign.Center
            )
            Text(
                text = "%.1f".format(state.value),
                fontFamily = fontFamily,
                color = textColor,
                modifier = Modifier
                    .size(width, 35.dp)
                    .background(backgroundColor),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))
            CustomButton(
                rId = R.drawable.minus,
                height = 55.dp,
                maxWidth = true,
                onClick = {
                    if (bluetoothSocket == null) {
                        showToast("Bluetooth is not connected")
                        return@CustomButton
                    }
                    if (state.value >= 0.1F) {
                        state.value -= 0.1F
                        sendDataOverBluetooth(decrementCode)
                    }
                },
                backgroundColor = Color(android.R.color.transparent)
            )
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    private fun disconnectBluetooth() {
        bluetoothSocket?.close()
        bluetoothSocket = null
    }

    private fun sendDataOverBluetooth(data: Char) {
        bluetoothSocket?.outputStream?.write(data.code)
    }
}
