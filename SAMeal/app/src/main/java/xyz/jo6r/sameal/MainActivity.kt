package xyz.jo6r.sameal

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
    val activity = LocalContext.current as ComponentActivity
    var qrCodeText by remember { mutableStateOf("Žádný QR kód") }

    // Dny a jídla
    // Map code to label
    val days = listOf(
        "ct" to "Čt (7.8)",
        "pa" to "Pá (8.8)",
        "so" to "So (9.8)",
        "ne" to "Ne (10.8)"
    )
    val calendar = java.util.Calendar.getInstance()
    val currentCode = when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.THURSDAY -> "ct"
        java.util.Calendar.FRIDAY -> "pa"
        java.util.Calendar.SATURDAY -> "so"
        java.util.Calendar.SUNDAY -> "ne"
        else -> "ct"
    }
    var selectedDay by remember { mutableStateOf(days.first { it.first == currentCode }) }

    val hour = LocalTime.now().hour
    val meals = listOf(
        "snidane" to "Snídaně",
        "obed" to "Oběd",
        "vecere" to "Večeře"
    )

    val currentMealCode = when {
        hour in 7..10 -> "snidane"
        hour in 11..15 -> "obed"
        hour in 17..21 -> "vecere"
        else -> "snidane"
    }
    var selectedMeal by remember { mutableStateOf(meals.first { it.first == currentMealCode }) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Výběr dní po dvou tlačítkách
        days.chunked(2).forEach { rowDays ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowDays.forEach { day ->
                    Button(
                        onClick = { selectedDay = day },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDay == day) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text(day.second) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Výběr jídel
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            meals.forEach { meal ->
                Button(
                    onClick = { selectedMeal = meal },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMeal == meal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) { Text(meal.second) }
            }
        }

        // PreviewView setup
        val previewView = remember { PreviewView(activity) }
        LaunchedEffect(previewView) {
            val cameraProvider = ProcessCameraProvider.getInstance(activity).get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
                        proxy.image?.let { mediaImage ->
                            val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                            BarcodeScanning.getClient().process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let { qrCodeText = it }
                                }
                                .addOnCompleteListener { proxy.close() }
                        } ?: proxy.close()
                    }
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    activity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) { e.printStackTrace() }
        }

        Spacer(modifier = Modifier.height(64.dp))
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .size(300.dp)
                .border(2.dp, Color.Red, RoundedCornerShape(8.dp))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(64.dp))
        Text("den=${selectedDay.first}, jídlo=${selectedMeal.first}", modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("QR kód: $qrCodeText", modifier = Modifier.align(Alignment.CenterHorizontally))
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
