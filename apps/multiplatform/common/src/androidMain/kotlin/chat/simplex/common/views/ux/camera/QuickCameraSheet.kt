package chat.simplex.common.views.ux.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import boofcv.abst.fiducial.QrCodeDetector
import boofcv.alg.color.ColorFormat
import boofcv.android.ConvertCameraImage
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.struct.image.GrayU8
import chat.simplex.common.helpers.APPLICATION_ID
import chat.simplex.common.platform.TAG
import chat.simplex.common.platform.androidAppContext
import chat.simplex.common.platform.tmpDir
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import com.google.common.util.concurrent.ListenableFuture
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Single always-on camera screen for the SimpleUX central quick-access button:
// the shutter is always available, and a SimpleX link entering the frame
// surfaces a confirm card instead of requiring a separate "scan mode" swipe
// or toggle (see the design discussion in plans/ — auto-detect, tap to
// confirm, never auto-navigate on a bare scan).
//
// The chrome is SimpleUX's Luxury Mineral layer (CameraChrome.kt /
// CameraReticule.kt): dark mineral surfaces regardless of the app theme, an
// island-style control bar (gallery, shutter, torch) and a gold reticule.
//
// The BoofCV QR detection logic here mirrors newchat/QRCodeScanner.android.kt
// (throttled instead of per-frame) rather than sharing code with it, to avoid
// editing that upstream file for this fork-only entry point.
@Composable
fun QuickCameraSheet(
  onClose: () -> Unit,
  onPhotoCaptured: (Uri) -> Unit,
  onQrCode: suspend (String) -> Boolean
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  val permissionLauncher = rememberPermissionLauncher { granted -> hasCameraPermission = granted }
  LaunchedEffect(Unit) {
    if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
  }

  val galleryLauncher = rememberGetContentLauncher { uri ->
    if (uri != null) onPhotoCaptured(uri)
  }

  val cameraProviderFuture by produceState<ListenableFuture<ProcessCameraProvider>?>(initialValue = null) {
    value = ProcessCameraProvider.getInstance(context)
  }
  val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
  val camera = remember { mutableStateOf<Camera?>(null) }
  val torchOn = remember { mutableStateOf(false) }
  val detectedLink = remember { mutableStateOf<String?>(null) }
  val connecting = remember { mutableStateOf(false) }
  val lastAnalysisAt = remember { longArrayOf(0L) }
  val reticulePulse = remember { Animatable(1f) }

  val hasFlashUnit = camera.value?.cameraInfo?.hasFlashUnit() == true

  DisposableEffect(lifecycleOwner) {
    onDispose { cameraProviderFuture?.get()?.unbindAll() }
  }

  LaunchedEffect(detectedLink.value) {
    if (detectedLink.value == null) return@LaunchedEffect
    // One-shot arrival feedback per NEW code: a light haptic and a single gold
    // pulse of the reticule. Never re-triggered while the same code stays in
    // frame (the analyzer only reports changed payloads).
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    reticulePulse.snapTo(0f)
    reticulePulse.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
  }

  fun takePhoto() {
    val capture = imageCapture.value ?: return
    val file = File.createTempFile("quick-photo", ".jpg", tmpDir)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    capture.takePicture(
      outputOptions,
      ContextCompat.getMainExecutor(context),
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
          val uri = FileProvider.getUriForFile(androidAppContext, "$APPLICATION_ID.provider", file)
          onPhotoCaptured(uri)
        }
        override fun onError(exc: ImageCaptureException) {
          Log.e(TAG, "QuickCameraSheet: photo capture failed: ${exc.localizedMessage}")
        }
      }
    )
  }

  Box(Modifier.fillMaxSize().background(CameraChromeCanvas)) {
    if (!hasCameraPermission) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CameraPermissionCard {
          permissionLauncher.launch(Manifest.permission.CAMERA)
        }
      }
    } else {
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
          PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
          }
        }
      ) { previewView ->
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        cameraProviderFuture?.addListener({
          val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
          val detector: QrCodeDetector<GrayU8> = FactoryFiducial.qrcode(null, GrayU8::class.java)
          val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
          val capture = ImageCapture.Builder().build()
          imageCapture.value = capture

          // Mirrors QRCodeScanner.android.kt's approach of doing the actual detection work on
          // Main via withApi (not the analyzer's own background executor thread), so Compose
          // state reads/writes here are safe. The throttle below keeps Main-thread load well
          // under what the always-on QR-only scanner already does per frame.
          val analyzer = ImageAnalysis.Analyzer { proxy ->
            withApi {
              val now = System.currentTimeMillis()
              if (now - lastAnalysisAt[0] < 350L || connecting.value) {
                proxy.close()
                return@withApi
              }
              lastAnalysisAt[0] = now
              val gray = imageProxyToGrayU8(proxy)
              if (gray != null) {
                detector.process(gray)
                val qr = detector.detections.firstOrNull()
                if (qr != null && qr.message != null && qr.message != detectedLink.value) {
                  detectedLink.value = qr.message
                }
              }
              proxy.close()
            }
          }
          val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setImageQueueDepth(1)
            .build()
            .also { it.setAnalyzer(cameraExecutor, analyzer) }

          try {
            cameraProviderFuture?.get()?.unbindAll()
            val bound = cameraProviderFuture?.get()
              ?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture, imageAnalysis)
            camera.value = bound
          } catch (e: Exception) {
            Log.e(TAG, "QuickCameraSheet: ${e.localizedMessage}")
          }
        }, ContextCompat.getMainExecutor(context))
      }

      CameraTopBar(onClose)

      CameraReticule(
        pulse = reticulePulse.value,
        modifier = Modifier.align(Alignment.Center).padding(bottom = 88.dp)
      )

      // Bottom control island, same family as the chat-list island bar
      // (dark glass capsule, specular hairline rim, icon + label items).
      Box(
        Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 16.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(32.dp),
          color = CameraChromeIsland,
          elevation = 12.dp,
          modifier = Modifier
            .border(
              width = 1.dp,
              brush = Brush.verticalGradient(listOf(CameraChromeRimHighlight, CameraChromeRimLowlight)),
              shape = RoundedCornerShape(32.dp)
            )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            CameraIslandItem(
              label = stringResource(MR.strings.quick_camera_gallery),
              icon = MR.images.ic_photo_library,
              active = false,
              onClick = { galleryLauncher.launch("image/*") }
            )
            Spacer(Modifier.width(14.dp))
            CameraShutterButton(enabled = imageCapture.value != null) { takePhoto() }
            Spacer(Modifier.width(14.dp))
            // Cameras without a flash unit simply don't offer the toggle.
            if (hasFlashUnit) {
              CameraIslandItem(
                label = stringResource(MR.strings.quick_camera_torch),
                icon = if (torchOn.value) MR.images.ic_bolt else MR.images.ic_bolt_off,
                active = torchOn.value,
                onClick = {
                  val cam = camera.value ?: return@CameraIslandItem
                  val newState = !torchOn.value
                  cam.cameraControl.enableTorch(newState)
                  torchOn.value = newState
                }
              )
            }
          }
        }
      }

      AnimatedVisibility(
        visible = detectedLink.value != null,
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 132.dp, start = 16.dp, end = 16.dp)
      ) {
        val link = detectedLink.value
        Row(
          Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(CameraChromeCardTop, CameraChromeCardBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(CameraChromeRimHighlight, CameraChromeRimLowlight)), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            stringResource(MR.strings.quick_camera_link_detected),
            color = CameraChromeTextPrimary,
            modifier = Modifier.weight(1f)
          )
          Spacer(Modifier.width(12.dp))
          Button(
            enabled = !connecting.value,
            colors = ButtonDefaults.buttonColors(backgroundColor = AmberGold, contentColor = CameraChromeOnGold),
            onClick = {
              if (link == null) return@Button
              connecting.value = true
              scope.launch {
                val handled = onQrCode(link)
                if (handled) {
                  onClose()
                } else {
                  connecting.value = false
                  detectedLink.value = null
                }
              }
            }
          ) {
            Text(stringResource(MR.strings.quick_camera_connect))
          }
        }
      }
    }
  }
}

@SuppressLint("UnsafeOptInUsageError")
private fun imageProxyToGrayU8(img: ImageProxy): GrayU8? {
  val image = img.image ?: return null
  val outImg = GrayU8()
  ConvertCameraImage.imageToBoof(image, ColorFormat.GRAY, outImg, null)
  return outImg
}
