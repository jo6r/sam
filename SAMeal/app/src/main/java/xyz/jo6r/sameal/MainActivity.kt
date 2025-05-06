package xyz.jo6r.sameal

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import xyz.jo6r.sameal.ui.theme.SAMealTheme
import java.net.HttpURLConnection
import java.net.URL
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

@OptIn(ExperimentalGetImage::class)
@SuppressLint("ContextCastToActivity")
@Composable
fun QRScannerView() {
    val activity = LocalContext.current as ComponentActivity
    var qrCodeText by remember { mutableStateOf("") }

    // States for API result
    val coroutineScope = rememberCoroutineScope()
    var paymentStatus by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val versionName = context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName


    // Dny a jídla
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

    Row(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "build: $versionName",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 8.sp,
            modifier = Modifier.padding(8.dp).align(Alignment.Bottom)
        )
    }

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

        Spacer(modifier = Modifier.height(6.dp))

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
                it.surfaceProvider = previewView.surfaceProvider
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
                .size(250.dp)
//                .border(2.dp, Color.Red, RoundedCornerShape(8.dp))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(64.dp))
        Text("${selectedDay.first}, ${selectedMeal.first}, $qrCodeText", modifier = Modifier.align(Alignment.CenterHorizontally))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    if (qrCodeText.isBlank()) {
                        errorMessage = "Není načten QR kód"
                    } else {
                        coroutineScope.launch(Dispatchers.IO) {
                            paymentStatus = null
                            errorMessage = null
                            try {
                                val url =
                                    URL("https://api.samorlova.cz?id=${qrCodeText}&den=${selectedDay.first}&strava_program=${selectedMeal.first}")
                                (url.openConnection() as HttpURLConnection).run {
                                    requestMethod = "GET"
                                    setRequestProperty("Authorization", "123")
                                    setRequestProperty("SAMeal", "$versionName")
                                    connectTimeout = 5000
                                    readTimeout = 5000
                                    val code = responseCode
                                    if (code == HttpURLConnection.HTTP_OK) {
                                        val response =
                                            inputStream.bufferedReader().use { it.readText() }
                                        val json = JSONObject(response)
                                        if (json.getString("result") == "success") {
                                            paymentStatus = json.getJSONObject("data").getString("zaplaceno")
                                        } else {
                                            errorMessage = json.getString("error")
                                        }
                                    } else {
                                        errorMessage = "Server HTTP error $code"
                                    }
                                }
                            } catch (e: JSONException) {
                                errorMessage = "Error parse response. " + e.localizedMessage
                            } catch (e: Exception) {
                                errorMessage = "Fatal error. " + e.localizedMessage
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Ověř QR kód")
            }
        }


        // Show result
        paymentStatus?.let {
            Spacer(modifier = Modifier.height(8.dp))
            if (it == "ANO") {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Úspěch",
                    tint = Color.Green,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )

            } else {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Neúspěch",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )

            }
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color.Red, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
