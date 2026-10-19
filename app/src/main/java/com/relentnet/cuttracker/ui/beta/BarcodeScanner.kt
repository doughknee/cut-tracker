package com.relentnet.cuttracker.ui.beta

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A full-screen CameraX preview that hands back the first product barcode (EAN or UPC) it sees. ML Kit's bundled
 * model does the reading on the phone; nothing leaves it from here.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScanner(onScan: (String) -> Unit, onDismiss: () -> Unit) {
    val c = CutTheme.colors
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; denied = !it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E).build())
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val done = remember { AtomicBoolean(false) }
    DisposableEffect(Unit) { onDispose { scanner.close(); executor.shutdown() } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(c.ground)) {
            if (granted) AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        val future = ProcessCameraProvider.getInstance(ctx)
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
                            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                            analysis.setAnalyzer(executor) { proxy ->
                                val image = proxy.image
                                if (image == null || done.get()) { proxy.close(); return@setAnalyzer }
                                scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                                    .addOnSuccessListener { codes ->
                                        val code = codes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                                        if (code != null && done.compareAndSet(false, true)) onScan(code)
                                    }
                                    .addOnCompleteListener { proxy.close() }
                            }
                            provider.unbindAll()
                            runCatching { provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis) }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) else Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Camera access needed.", color = c.text, style = cutStyle(28, 800, 1f, -0.02f, display = true))
                Sub(if (denied) "Allow the camera in system settings, then come back." else "Asking for the camera.")
            }
            Box(Modifier.align(Alignment.Center).size(260.dp, 160.dp).border(3.dp, c.accent, RoundedCornerShape(16.dp)))
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.systemBars).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Sub("Point at the barcode on the pack.", color = c.text)
                BigButton("Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth(), secondary = true, height = 48.dp, fontSize = 13)
            }
        }
    }
}
