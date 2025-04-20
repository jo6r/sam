package xyz.jo6r.sameal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import xyz.jo6r.sameal.ui.theme.SAMealTheme
import java.time.LocalTime
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        }

        setContent {
            SAMealTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QRScannerView()
                }
            }
        }
    }
}

@Composable
fun QRScannerView() {
    val context = LocalContext.current
    var qrCodeText by remember { mutableStateOf("Žádný QR kód") }

    // vyber dne
    val calendar = java.util.Calendar.getInstance()
    val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)

    val selectedDayValue = when (dayOfWeek) {
        java.util.Calendar.THURSDAY -> 1
        java.util.Calendar.FRIDAY -> 2
        java.util.Calendar.SATURDAY -> 3
        java.util.Calendar.SUNDAY -> 4
        else -> 1
    }

    val days = listOf("Čt (7.8)" to 1, "Pá (8.8)" to 2, "So (9.8)" to 3, "Ne (10.8)" to 4)
    var selectedDay by remember {
        mutableStateOf(days.firstOrNull { it.second == selectedDayValue } ?: days[0])
    }

    // vyber sravy
    val meals = listOf("Snídaně" to 1, "Oběd" to 2, "Večeře" to 3)
    val currentHour = LocalTime.now().hour
    var selectedMeal by remember {
        mutableStateOf(
            when (currentHour) {
                in 7..10 -> meals[0]
                in 11..15 -> meals[1]
                in 17..21 -> meals[2]
                else -> meals[0]
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            days.chunked(2).forEach { rowDays ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowDays.forEach { day ->
                        Button(
                            onClick = { selectedDay = day },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedDay == day) MaterialTheme.colorScheme.primary else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(day.first)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            meals.forEach { meal ->
                Button(
                    onClick = { selectedMeal = meal },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMeal == meal) MaterialTheme.colorScheme.primary else Color.LightGray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(meal.first)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val barcodeScanner = BarcodeScanning.getClient()
                    val analysis = ImageAnalysis.Builder()

                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy) { result ->
                            qrCodeText = result
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            context as ComponentActivity,
                            cameraSelector,
                            preview,
                            analysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }, modifier = Modifier.fillMaxSize())
        }

        Text(
            text = "Vybrane hodnoty ${selectedDay.second} , ${selectedMeal.second}",
        )

        Text(
            text = "QR kód: $qrCodeText",
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Ověř QR kód")
            }
        }
    }
}

private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onResult: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    mediaImage?.let {
        val image = InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let(onResult)
            }
            .addOnCompleteListener { imageProxy.close() }
    } ?: imageProxy.close()
}
