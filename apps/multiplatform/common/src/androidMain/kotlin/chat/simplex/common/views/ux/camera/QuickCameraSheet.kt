package chat.simplex.common.views.ux.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import boofcv.abst.fiducial.QrCodeDetector
import boofcv.alg.color.ColorFormat
import boofcv.android.ConvertCameraImage
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.struct.image.GrayU8
import chat.simplex.common.helpers.APPLICATION_ID
import chat.simplex.common.platform.TAG
import chat.simplex.common.platform.androidAppContext
import chat.simplex.common.platform.showToast
import chat.simplex.common.platform.tmpDir
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import com.google.common.util.concurrent.ListenableFuture
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Single always-on camera screen for the SimpleUX central quick-access button:
// the shutter is always available, and EVERY code entering the frame surfaces
// a confirmation card with its decoded content - SimpleX link (connect flow),
// URL (explicit "open in browser" tap) or plain text (copy / share into
// SimpleX) - instead of being silently dropped. Auto-detect, tap to act,
// never auto-navigate on a bare scan.
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
  onQrCode: suspend (String) -> Boolean,
  onTextShared: (String) -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current
  val clipboard = LocalClipboardManager.current

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
    if (uri != null) {
      onPhotoCaptured(uri)
      // ShareListView renders behind this full-screen modal, so the sheet must
      // step aside or the hand-off looks like a no-op (shutter had the same
      // issue - found on emulator, 2026-08-29).
      onClose()
    }
  }

  val cameraProviderFuture by produceState<ListenableFuture<ProcessCameraProvider>?>(initialValue = null) {
    value = ProcessCameraProvider.getInstance(context)
  }
  val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
  val camera = remember { mutableStateOf<Camera?>(null) }
  val torchOn = remember { mutableStateOf(false) }
  val detectedRaw = remember { mutableStateOf<String?>(null) }
  val detectedContent = remember { mutableStateOf<QrContent?>(null) }
  val connecting = remember { mutableStateOf(false) }
  val lastAnalysisAt = remember { longArrayOf(0L) }
  val reticulePulse = remember { Animatable(1f) }

  val hasFlashUnit = camera.value?.cameraInfo?.hasFlashUnit() == true

  fun dismissCard() {
    detectedRaw.value = null
    detectedContent.value = null
  }

  val copyToClipboard: (String) -> Unit = { text ->
    clipboard.setText(AnnotatedString(text))
    showToast(generalGetString(MR.strings.copied))
  }

  val openInBrowser: (String) -> Unit = { url ->
    try {
      // Plain ACTION_VIEW: one tap opens the scanned URL exactly once, in the
      // user's chosen browser. Never auto-opened on a bare scan.
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
      Log.e(TAG, "QuickCameraSheet: no handler for a scanned URL")
    }
  }

  val openAppSettings: () -> Unit = {
    try {
      // Deep-link to this app's page in system settings, where camera access
      // is granted - the only reliable path after a permanent ("don't ask
      // again") denial stops Android from re-showing the runtime dialog.
      context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
      )
    } catch (e: ActivityNotFoundException) {
      Log.e(TAG, "QuickCameraSheet: no handler for app settings")
    }
  }

  DisposableEffect(lifecycleOwner) {
    // Re-check on resume: the user grants the permission in system settings
    // while this sheet is backgrounded, so the result never arrives through
    // the launcher callback - the camera must come alive when they return.
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasCameraPermission =
          ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      cameraProviderFuture?.get()?.unbindAll()
    }
  }

  LaunchedEffect(detectedRaw.value) {
    if (detectedRaw.value == null) return@LaunchedEffect
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
          // Same as the gallery path: close so the share list surfaces.
          onClose()
        }
        override fun onError(exc: ImageCaptureException) {
          Log.e(TAG, "QuickCameraSheet: photo capture failed: ${exc.localizedMessage}")
        }
      }
    )
  }

  Box(Modifier.fillMaxSize().background(CameraChromeCanvas)) {
    // FB-16: system back closes the sheet instead of leaving the app.
    BackHandler { onClose() }

    if (!hasCameraPermission) {
      // The close button sits OUTSIDE the permission branches: a denied state
      // must never strand the user in the sheet (FB-10).
      CameraTopBar(onClose)
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CameraPermissionCard(
          onOpenSettings = openAppSettings,
          onClose = onClose
        )
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
                if (qr != null && qr.message != null && qr.message != detectedRaw.value) {
                  detectedRaw.value = qr.message
                  detectedContent.value = classifyQrContent(qr.message)
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

      // Universal QR routing: EVERY detected code surfaces its decoded
      // content here (SimpleX link, URL or plain text) and waits for an
      // explicit tap - never auto-navigates on a bare scan.
      AnimatedVisibility(
        visible = detectedContent.value != null,
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 132.dp, start = 16.dp, end = 16.dp)
      ) {
        val content = detectedContent.value
        if (content != null) {
          QrResultCard(
            content = content,
            connecting = connecting.value,
            onConnect = {
              val raw = detectedRaw.value ?: return@QrResultCard
              connecting.value = true
              scope.launch {
                val handled = onQrCode(raw)
                if (handled) {
                  onClose()
                } else {
                  connecting.value = false
                  dismissCard()
                }
              }
            },
            onOpenUrl = openInBrowser,
            onCopy = copyToClipboard,
            onShare = onTextShared,
            onDismiss = ::dismissCard
          )
        }
      }

      // FB-16: composed LAST so the close button draws above the fullscreen
      // camera preview - the PreviewView used to cover it entirely, leaving
      // the granted path with no visible way out.
      CameraTopBar(onClose)
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
