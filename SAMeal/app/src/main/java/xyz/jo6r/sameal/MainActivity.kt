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
    val days = listOf("Čt (7.8)" to 1, "Pá (8.8)" to 2, "So (9.8)" to 3, "Ne (10.8)" to 4)
    val calendar = java.util.Calendar.getInstance()
    val dayValue = when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.THURSDAY -> 1
        java.util.Calendar.FRIDAY -> 2
        java.util.Calendar.SATURDAY -> 3
        java.util.Calendar.SUNDAY -> 4
        else -> 1
    }
    var selectedDay by remember { mutableStateOf(days.first { it.second == dayValue }) }

    val meals = listOf("Snídaně" to 1, "Oběd" to 2, "Večeře" to 3)
    val hour = LocalTime.now().hour
    val mealValue = when {
        hour in 7..10 -> 1
        hour in 11..15 -> 2
        hour in 17..21 -> 3
        else -> 1
    }
    var selectedMeal by remember { mutableStateOf(meals.first { it.second == mealValue }) }

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
                    ) { Text(day.first) }
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
                ) { Text(meal.first) }
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
        Text("Vybrané: den=${selectedDay.second}, jídlo=${selectedMeal.second}", modifier = Modifier.align(Alignment.CenterHorizontally))
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
