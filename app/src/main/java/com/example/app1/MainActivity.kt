package com.example.app1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app1.data.Rate
import com.example.app1.ui.theme.App1Theme
import com.example.app1.viewmodel.CurrencyViewModel
import kotlin.math.sqrt
import com.example.app1.ui.ExchangeRateChart
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastUpdate: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private val shakeThreshold = 800f // Adjust this value to change sensitivity
    private var viewModel: CurrencyViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        enableEdgeToEdge()
        setContent {
            App1Theme {
                viewModel = viewModel()
                CurrencyConverterApp(viewModel!!)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val currentTime = System.currentTimeMillis()
                if ((currentTime - lastUpdate) > 100) { // Limit updates to every 100ms
                    val diffTime = currentTime - lastUpdate
                    lastUpdate = currentTime

                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    val speed = sqrt(
                        ((x - lastX) * (x - lastX) +
                                (y - lastY) * (y - lastY) +
                                (z - lastZ) * (z - lastZ)).toDouble()
                    ).toFloat() / diffTime * 10000

                    if (speed > shakeThreshold) {
                        // Shake detected, select random currencies
                        viewModel?.selectRandomCurrencies()
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterApp(
    viewModel: CurrencyViewModel
) {
    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Currency Converter", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            )
        },
        containerColor = Color(0xFF121212) // Dark background for the main container
    ) { padding ->
        if (viewModel.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${viewModel.error}", color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.amount,
                    onValueChange = { viewModel.updateAmount(it) },
                    label = { Text("Amount", color = Color.White) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        focusedBorderColor = Color.White
                    )
                )

                // Use a Column to stack the dropdowns vertically
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Space between dropdowns
                ) {
                    // From currency dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedFrom,
                        onExpandedChange = { expandedFrom = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = viewModel.fromCurrency?.currency ?: "Select currency",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                            modifier = Modifier.menuAnchor(),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF1E1E1E),
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedFrom,
                            onDismissRequest = { expandedFrom = false }
                        ) {
                            viewModel.currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text("${currency.code} - ${currency.currency}") },
                                    onClick = {
                                        viewModel.updateFromCurrency(currency)
                                        expandedFrom = false
                                    }
                                )
                            }
                        }
                    }

                    // To currency dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedTo,
                        onExpandedChange = { expandedTo = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = viewModel.toCurrency?.currency ?: "Select currency",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTo) },
                            modifier = Modifier.menuAnchor(),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF1E1E1E),
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTo,
                            onDismissRequest = { expandedTo = false }
                        ) {
                            viewModel.currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text("${currency.code} - ${currency.currency}") },
                                    onClick = {
                                        viewModel.updateToCurrency(currency)
                                        expandedTo = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Conversion result card
                if (viewModel.fromCurrency != null && viewModel.toCurrency != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format("%.2f %s", viewModel.convertedAmount, viewModel.toCurrency?.code),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Exchange Rate",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "1 ${viewModel.fromCurrency?.code} = ${String.format("%.4f", viewModel.fromCurrency?.mid?.div(viewModel.toCurrency?.mid ?: 1.0))} ${viewModel.toCurrency?.code}",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            
                            // Add selected date exchange rate if available
                            if (viewModel.selectedDate != "Select a point") {
                                Text(
                                    text = "Selected Date: ${viewModel.selectedDate}",
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Text(
                                    text = "Rate: 1 ${viewModel.fromCurrency?.code} = ${String.format("%.4f", viewModel.selectedValue)} ${viewModel.toCurrency?.code}",
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Historical rates card
                if (!viewModel.isLoadingHistory && viewModel.historicalRates.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Historical Exchange Rates",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp),
                                color = Color.White
                            )
                            ExchangeRateChart(
                                historicalRates = viewModel.historicalRates,
                                onPointSelected = { date, value ->
                                    viewModel.updateSelectedPoint(date, value)
                                }
                            )

                            // Display selected date and value below the chart
                            Text(
                                text = "Selected Date: ${viewModel.selectedDate}",
                                color = Color.White,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "Exchange Rate: 1 ${viewModel.fromCurrency?.code} = ${String.format("%.4f", viewModel.selectedValue)} ${viewModel.toCurrency?.code}",
                                color = Color.White,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else if (viewModel.isLoadingHistory) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Color.White
                    )
                }
            }
        }
    }
}