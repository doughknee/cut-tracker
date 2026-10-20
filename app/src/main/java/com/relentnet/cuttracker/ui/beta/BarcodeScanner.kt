package com.relentnet.cuttracker.ui.beta

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.relentnet.cuttracker.data.LabelRead
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Hands back the first product barcode (EAN or UPC) the camera sees. ML Kit's bundled model reads it on the phone. */
@Composable
fun BarcodeScanner(onScan: (String) -> Unit, onDismiss: () -> Unit) {
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E).build())
    }
    DisposableEffect(Unit) { onDispose { scanner.close() } }
    CameraScanner("Point at the barcode on the pack.", DpSize(260.dp, 160.dp), onScan, onDismiss) { image, found ->
        scanner.process(image).addOnSuccessListener { codes -> codes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }?.let(found) }
    }
}

/**
 * Hands back the recognised text of the first frame that reads as a nutrition label (protein or calories found).
 * ML Kit's bundled Latin model runs on the phone; nothing leaves it, so this needs no online switch.
 */
@Composable
fun LabelScanner(onRead: (String) -> Unit, onDismiss: () -> Unit) {
    val reader = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(Unit) { onDispose { reader.close() } }
    val started = remember { System.currentTimeMillis() }
    var hint by remember { mutableStateOf("Fill the frame with the nutrition facts and hold still.") }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(8000); hint = "Still reading. Get closer so the table fills the frame, in good light. Labels in English read best." }
    CameraScanner(hint, DpSize(300.dp, 380.dp), onRead, onDismiss, sharp = true) { image, found ->
        reader.process(image).addOnSuccessListener { text ->
            // The first three seconds wait for a frame with both numbers; a blurry first frame usually has only one.
            val f = LabelRead.parse(text.text)
            val waited = System.currentTimeMillis() - started > 3000
            if ((f.protein != null && f.calories != null) || (waited && f.any)) found(text.text)
        }
    }
}

/**
 * A full-screen CameraX preview with a frame and a hint. [process] runs on every frame off the main thread and calls
 * its second argument with a result; the first result wins and closes the scanner. [sharp] asks for a larger analysis
 * image, which small print needs and a barcode does not.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraScanner(
    hint: String, frame: DpSize, onResult: (String) -> Unit, onDismiss: () -> Unit, sharp: Boolean = false,
    process: (InputImage, (String) -> Unit) -> Task<*>,
) {
    val c = CutTheme.colors
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; denied = !it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val done = remember { AtomicBoolean(false) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var failed by remember { mutableStateOf(false) }
    // The camera is bound to the page's lifecycle, so it has to be let go here or it keeps running after the scan.
    DisposableEffect(Unit) { onDispose { provider?.unbindAll(); executor.shutdown() } }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (!granted && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { granted = true; denied = false }
        onPauseOrDispose { }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(c.ground)) {
            if (granted) AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        val future = ProcessCameraProvider.getInstance(ctx)
                        future.addListener({
                            val p = future.get().also { provider = it }
                            val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
                            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).apply {
                                if (sharp) setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
                            }.build()
                            analysis.setAnalyzer(executor) { proxy ->
                                val image = proxy.image
                                if (image == null || done.get()) { proxy.close(); return@setAnalyzer }
                                process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees)) { r -> if (done.compareAndSet(false, true)) onResult(r) }
                                    .addOnCompleteListener { proxy.close() }
                            }
                            p.unbindAll()
                            runCatching { p.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis) }.onFailure { failed = true }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) else Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Camera access needed.", color = c.text, style = cutStyle(28, 800, 1f, -0.02f, display = true))
                Sub(if (denied) "Allow the camera in system settings, then come back." else "Asking for the camera.")
                if (denied) BigButton("Open settings", onClick = {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null)))
                }, modifier = Modifier.fillMaxWidth(), height = 48.dp, fontSize = 13)
            }
            if (failed) Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.systemBars).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("The camera did not start.", color = c.text, style = cutStyle(28, 800, 1f, -0.02f, display = true))
                Sub("Another app may be using it. Close this and try again.")
            }
            if (granted && !failed) Box(Modifier.align(Alignment.Center).size(frame).border(3.dp, c.accent, RoundedCornerShape(16.dp)))
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.systemBars).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (granted && !failed) Sub(hint, color = c.text)
                BigButton("Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth(), secondary = true, height = 48.dp, fontSize = 13)
            }
        }
    }
}
