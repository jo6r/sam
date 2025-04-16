package xyz.jo6r.qrmealapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import xyz.jo6r.qrmealapp.ui.theme.QrMealAppTheme
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
            QrMealAppTheme {
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

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(onClick = { /* TODO: akce */ }) {
                Text("den1")
            }
            Button(onClick = { /* TODO: akce */ }) {
                Text("den2")
            }
            Button(onClick = { /* TODO: akce */ }) {
                Text("den3")
            }
            Button(onClick = { /* TODO: akce */ }) {
                Text("den4")
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(onClick = { /* TODO: akce */ }) {
                Text("strava1")
            }
            Button(onClick = { /* TODO: akce */ }) {
                Text("strava2")
            }
            Button(onClick = { /* TODO: akce */ }) {
                Text("strava3")
            }
        }

        // Náhled kamery
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
                        .setTargetResolution(Size(1280, 720))
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
            text = "QR Kód: $qrCodeText",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = { /* TODO: akce pro ověření */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // výška tlačítka
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
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let { value ->
                    onResult(value)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
