package chat.simplex.common.views.helpers

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import chat.simplex.common.helpers.toURI
import chat.simplex.common.platform.androidAppContext
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val RECENT_MEDIA_LIMIT = 4

private fun neededMediaPermissions(): List<String> =
  if (Build.VERSION.SDK_INT >= 33) {
    listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
  } else {
    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
  }

private fun hasMediaPermission(): Boolean =
  neededMediaPermissions().all {
    ContextCompat.checkSelfPermission(androidAppContext, it) == PackageManager.PERMISSION_GRANTED
  }

private fun hasCameraPermission(): Boolean =
  ContextCompat.checkSelfPermission(androidAppContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
actual fun AttachmentTopSection(
  sheetVisible: Boolean,
  onCameraOpened: () -> Unit,
  onMediaPicked: (List<URI>) -> Unit,
  hide: () -> Unit
) {
  var granted by remember { mutableStateOf(hasMediaPermission()) }
  // ask for the next permission in the chain from the current launcher's callback
  var pendingLaunch by remember { mutableStateOf<String?>(null) }
  val permissionLauncher = rememberPermissionLauncher { _ ->
    granted = hasMediaPermission()
    pendingLaunch = if (granted) null else neededMediaPermissions().firstOrNull { !isPermissionGranted(it) }
  }
  LaunchedEffect(Unit) {
    if (!granted) pendingLaunch = neededMediaPermissions().firstOrNull { !isPermissionGranted(it) }
  }
  LaunchedEffect(pendingLaunch) {
    pendingLaunch?.let { permissionLauncher.launch(it) }
  }

  var items by remember { mutableStateOf(emptyList<RecentMediaItem>()) }
  LaunchedEffect(granted) {
    if (granted) {
      items = withContext(Dispatchers.IO) { queryRecentGalleryItems() }
    }
  }

  if (!granted || items.isEmpty()) return
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    CameraPreviewTile(
      modifier = Modifier.weight(1f),
      sheetVisible = sheetVisible,
      onClick = onCameraOpened
    )
    Column(
      Modifier.weight(2f),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      items.chunked(2).forEach { rowItems ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          rowItems.forEach { item ->
            RecentMediaThumb(
              item = item,
              modifier = Modifier.weight(1f),
              onClick = {
                onMediaPicked(listOf(Uri.parse(item.uri).toURI()))
                hide()
              }
            )
          }
          repeat(2 - rowItems.size) { Spacer(Modifier.weight(1f)) }
        }
      }
    }
  }
}

private fun isPermissionGranted(permission: String): Boolean =
  ContextCompat.checkSelfPermission(androidAppContext, permission) == PackageManager.PERMISSION_GRANTED

/**
 * #122 phase 2: live viewfinder square. Binds a preview-only camera while the
 * sheet is VISIBLE (sheet content composes eagerly while hidden - binding
 * unconditionally would keep the camera on during the whole chat, the #99
 * perf class). Tap opens the existing full-screen capture flow, which also
 * handles the CAMERA permission request.
 */
@Composable
private fun CameraPreviewTile(modifier: Modifier, sheetVisible: Boolean, onClick: () -> Unit) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraGranted = remember { mutableStateOf(hasCameraPermission()) }
  val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
  val previewView = remember {
    PreviewView(context).apply {
      scaleType = PreviewView.ScaleType.FILL_CENTER
      implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }
  }
  // the camera permission may be granted while the sheet is open (the capture
  // flow's own request); re-check whenever the sheet becomes visible
  LaunchedEffect(sheetVisible) {
    if (sheetVisible) cameraGranted.value = hasCameraPermission()
  }
  var bound by remember { mutableStateOf(false) }
  DisposableEffect(sheetVisible, cameraGranted.value) {
    if (sheetVisible && cameraGranted.value && !bound) {
      try {
        val provider = providerFuture.get()
        val preview = androidx.camera.core.Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        provider.bindToLifecycle(lifecycleOwner, androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA, preview)
        bound = true
      } catch (e: Exception) {
        // camera busy elsewhere (e.g. the full-screen capture flow) - fall back to the static tile
      }
    }
    onDispose {
      if (bound) {
        try {
          providerFuture.get().unbindAll()
        } catch (e: Exception) {
        }
        bound = false
      }
    }
  }

  Box(
    modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colors.onBackground.copy(alpha = 0.06f))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
      ),
    contentAlignment = Alignment.Center
  ) {
    if (cameraGranted.value && sheetVisible) {
      AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    } else {
      Icon(
        painterResource(MR.images.ic_camera_enhance),
        contentDescription = null,
        tint = MaterialTheme.colors.primary,
        modifier = Modifier.size(26.dp)
      )
    }
  }
}

private fun queryRecentGalleryItems(): List<RecentMediaItem> {
  val resolver = androidAppContext.contentResolver
  data class Row(val uri: String, val isVideo: Boolean, val dateAdded: Long, val durationSec: Int)
  val rows = ArrayList<Row>()
  fun collect(externalUri: Uri, isVideo: Boolean) {
    val projection = arrayOf(
      MediaStore.MediaColumns._ID,
      MediaStore.MediaColumns.DATE_ADDED
    ) + if (isVideo) arrayOf(MediaStore.Video.Media.DURATION) else emptyArray()
    resolver.query(externalUri, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
      val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
      val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
      val durCol = if (isVideo) c.getColumnIndex(MediaStore.Video.Media.DURATION) else -1
      while (c.moveToNext()) {
        rows.add(
          Row(
            uri = externalUri.buildUpon().appendPath(c.getLong(idCol).toString()).build().toString(),
            isVideo = isVideo,
            dateAdded = c.getLong(dateCol),
            durationSec = if (durCol >= 0) (c.getLong(durCol) / 1000L).toInt() else 0
          )
        )
      }
    }
  }
  collect(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false)
  collect(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)
  return rows.sortedByDescending { it.dateAdded }
    .take(RECENT_MEDIA_LIMIT)
    .map { RecentMediaItem(it.uri, it.isVideo, it.durationSec) }
}

@Composable
private fun RecentMediaThumb(item: RecentMediaItem, modifier: Modifier, onClick: () -> Unit) {
  val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(item.uri) {
    thumbnail.value = withContext(Dispatchers.IO) { loadMediaThumbnail(item.uri) }
  }
  Box(
    modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colors.onBackground.copy(alpha = 0.06f))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    val bmp = thumbnail.value
    if (bmp != null) {
      Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
    if (item.isVideo) {
      Text(
        shortDurationText(item.durationSec),
        Modifier
          .align(Alignment.BottomEnd)
          .padding(4.dp)
          .clip(RoundedCornerShape(50))
          .background(Color.Black.copy(alpha = 0.5f))
          .padding(horizontal = 5.dp, vertical = 1.dp),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

private fun loadMediaThumbnail(uri: String): ImageBitmap? = try {
  val parsed = Uri.parse(uri)
  // this SDK's android.jar lacks android.graphics.Size, so no
  // ContentResolver.loadThumbnail - ImageDecoder for images, a first frame
  // via MediaMetadataRetriever for videos
  if (isVideoUri(parsed)) {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(androidAppContext, parsed)
    try {
      retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.asImageBitmap()
    } finally {
      retriever.release()
    }
  } else if (Build.VERSION.SDK_INT >= 28) {
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(androidAppContext.contentResolver, parsed)) { decoder, info, _ ->
      val scale = 320f / maxOf(info.size.width, info.size.height)
      decoder.setTargetSize(
        (info.size.width * scale).toInt().coerceAtLeast(1),
        (info.size.height * scale).toInt().coerceAtLeast(1)
      )
    }.asImageBitmap()
  } else {
    null
  }
} catch (e: Exception) {
  null
}

private fun isVideoUri(uri: Uri): Boolean {
  val mime = androidAppContext.contentResolver.getType(uri)
  return mime != null && mime.startsWith("video/")
}

private fun shortDurationText(totalSeconds: Int): String {
  val m = totalSeconds / 60
  val s = totalSeconds % 60
  return "$m:%02d".format(s)
}
