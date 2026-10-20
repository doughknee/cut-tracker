package com.relentnet.cuttracker.ui.route

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.App
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.io.File

/** CameraX capture. Saves to filesDir/photos/YYYY-MM-DD.jpg and records the ProgressPhoto. */
@Composable
fun CameraScreen(vm: AppViewModel, onDone: () -> Unit) {
    val c = CutTheme.colors
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val haptics = LocalHaptics.current
    val st by vm.state.collectAsStateWithLifecycle()
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; denied = !it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    // Coming back from system settings with the camera allowed opens the viewfinder without another tap.
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (!granted && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { granted = true; denied = false }
        onPauseOrDispose { }
    }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var busy by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(c.ground)) {
        if (granted) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        val future = ProcessCameraProvider.getInstance(ctx)
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
                            provider.unbindAll()
                            runCatching { provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture) }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Eyebrow("Progress photo")
                Text("Camera access needed.", color = c.text, style = cutStyle(28, 800, 1f, -0.02f, display = true))
                Sub(if (denied) "Allow the camera in system settings, then come back. Photos never leave the phone." else "Asking for the camera.")
                if (denied) BigButton("Open settings", onClick = {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null)))
                }, modifier = Modifier.fillMaxWidth())
                BigButton("Back", onClick = onDone, modifier = Modifier.fillMaxWidth(), secondary = true)
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.systemBars).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Sub("Same mirror, same light.", color = c.text)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BigButton("Cancel", onClick = onDone, modifier = Modifier.size(100.dp, 48.dp), secondary = true, height = 48.dp, fontSize = 12)
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(c.accent).border(4.dp, c.text, CircleShape)
                        .clickable(enabled = granted && !busy, role = Role.Button, onClickLabel = "Take the photo") {
                            busy = true
                            haptics.tap()
                            val file = File(App.of(context).repo.photosDir, "${st.today}.jpg")
                            capture.takePicture(
                                ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(r: ImageCapture.OutputFileResults) { vm.savePhoto(file.absolutePath); onDone() }
                                    override fun onError(e: ImageCaptureException) { busy = false; vm.showToast("Could not save the photo") }
                                },
                            )
                        },
                )
                Box(Modifier.size(100.dp, 48.dp))
            }
        }
    }
}
