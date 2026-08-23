package chat.simplex.common.views.ux.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.util.Log
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  val permissionLauncher = rememberPermissionLauncher { granted -> hasCameraPermission = granted }
  LaunchedEffect(Unit) {
    if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
  }

  val cameraProviderFuture by produceState<ListenableFuture<ProcessCameraProvider>?>(initialValue = null) {
    value = ProcessCameraProvider.getInstance(context)
  }
  val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
  val detectedLink = remember { mutableStateOf<String?>(null) }
  val connecting = remember { mutableStateOf(false) }
  val lastAnalysisAt = remember { longArrayOf(0L) }

  DisposableEffect(lifecycleOwner) {
    onDispose { cameraProviderFuture?.get()?.unbindAll() }
  }

  Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
    if (!hasCameraPermission) {
      Column(
        Modifier.align(Alignment.Center).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(stringResource(MR.strings.enable_camera_access), color = MaterialTheme.colors.onBackground)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
          Text(stringResource(MR.strings.enable_camera_access))
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
        val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
        val detector: QrCodeDetector<GrayU8> = FactoryFiducial.qrcode(null, GrayU8::class.java)

        cameraProviderFuture?.addListener({
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
            cameraProviderFuture?.get()?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture, imageAnalysis)
          } catch (e: Exception) {
            Log.e(TAG, "QuickCameraSheet: ${e.localizedMessage}")
          }
        }, ContextCompat.getMainExecutor(context))
      }

      IconButton(
        onClick = onClose,
        modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp).size(48.dp)
      ) {
        Icon(painterResource(MR.images.ic_close), contentDescription = null, tint = Color.White)
      }

      Box(
        Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 28.dp)
          .size(72.dp)
          .clip(CircleShape)
          .background(Color.White)
          .clickable(enabled = imageCapture.value != null) {
            val capture = imageCapture.value ?: return@clickable
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
      )

      AnimatedVisibility(
        visible = detectedLink.value != null,
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 116.dp, start = 16.dp, end = 16.dp)
      ) {
        val link = detectedLink.value
        Row(
          Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            stringResource(MR.strings.quick_camera_link_detected),
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.weight(1f)
          )
          Spacer(Modifier.width(12.dp))
          Button(
            enabled = !connecting.value,
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
