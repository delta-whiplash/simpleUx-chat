package chat.simplex.common.views.helpers

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp

val AccountCircleFilled: ImageVector
  get() {
    if (_accountCircleFilled != null) {
      return _accountCircleFilled!!
    }
    _accountCircleFilled = materialIcon(name = "Filled.AccountCircle") {
      materialPath {
        moveTo(12.0f, 2.0f)
        curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
        reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
        reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
        reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
        close()
        moveTo(12.0f, 5.0f)
        curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
        reflectiveCurveToRelative(-1.34f, 3.0f, -3.0f, 3.0f)
        reflectiveCurveToRelative(-3.0f, -1.34f, -3.0f, -3.0f)
        reflectiveCurveToRelative(1.34f, -3.0f, 3.0f, -3.0f)
        close()
        moveTo(12.0f, 19.2f)
        curveToRelative(-2.5f, 0.0f, -4.71f, -1.28f, -6.0f, -3.22f)
        curveToRelative(0.03f, -1.99f, 4.0f, -3.08f, 6.0f, -3.08f)
        curveToRelative(1.99f, 0.0f, 5.97f, 1.09f, 6.0f, 3.08f)
        curveToRelative(-1.29f, 1.94f, -3.5f, 3.22f, -6.0f, 3.22f)
        close()
      }
    }
    return _accountCircleFilled!!
  }

private var _accountCircleFilled: ImageVector? = null

val SupervisedUserCircleFilled: ImageVector
  get() {
    if (_supervisedUserCircleFilled != null) {
      return _supervisedUserCircleFilled!!
    }
    _supervisedUserCircleFilled = materialIcon(name = "Filled.SupervisedUserCircle") {
      materialPath {
        moveTo(11.99f, 2.0f)
        curveToRelative(-5.52f, 0.0f, -10.0f, 4.48f, -10.0f, 10.0f)
        reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
        reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
        reflectiveCurveToRelative(-4.48f, -10.0f, -10.0f, -10.0f)
        close()
        moveTo(15.6f, 8.34f)
        curveToRelative(1.07f, 0.0f, 1.93f, 0.86f, 1.93f, 1.93f)
        curveToRelative(0.0f, 1.07f, -0.86f, 1.93f, -1.93f, 1.93f)
        curveToRelative(-1.07f, 0.0f, -1.93f, -0.86f, -1.93f, -1.93f)
        curveToRelative(-0.01f, -1.07f, 0.86f, -1.93f, 1.93f, -1.93f)
        close()
        moveTo(9.6f, 6.76f)
        curveToRelative(1.3f, 0.0f, 2.36f, 1.06f, 2.36f, 2.36f)
        curveToRelative(0.0f, 1.3f, -1.06f, 2.36f, -2.36f, 2.36f)
        reflectiveCurveToRelative(-2.36f, -1.06f, -2.36f, -2.36f)
        curveToRelative(0.0f, -1.31f, 1.05f, -2.36f, 2.36f, -2.36f)
        close()
        moveTo(9.6f, 15.89f)
        verticalLineToRelative(3.75f)
        curveToRelative(-2.4f, -0.75f, -4.3f, -2.6f, -5.14f, -4.96f)
        curveToRelative(1.05f, -1.12f, 3.67f, -1.69f, 5.14f, -1.69f)
        curveToRelative(0.53f, 0.0f, 1.2f, 0.08f, 1.9f, 0.22f)
        curveToRelative(-1.64f, 0.87f, -1.9f, 2.02f, -1.9f, 2.68f)
        close()
        moveTo(11.99f, 20.0f)
        curveToRelative(-0.27f, 0.0f, -0.53f, -0.01f, -0.79f, -0.04f)
        verticalLineToRelative(-4.07f)
        curveToRelative(0.0f, -1.42f, 2.94f, -2.13f, 4.4f, -2.13f)
        curveToRelative(1.07f, 0.0f, 2.92f, 0.39f, 3.84f, 1.15f)
        curveToRelative(-1.17f, 2.97f, -4.06f, 5.09f, -7.45f, 5.09f)
        close()
      }
    }
    return _supervisedUserCircleFilled!!
  }

private var _supervisedUserCircleFilled: ImageVector? = null

val ChevronBackVector: ImageVector
  get() {
    if (_chevronBack != null) return _chevronBack!!
    _chevronBack = ImageVector.Builder(
      name = "ChevronBack",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      addPath(
        pathData = PathParser().parsePathString("M15.5 19l-7-7 7-7").toNodes(),
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
      )
    }.build()
    return _chevronBack!!
  }
private var _chevronBack: ImageVector? = null

val MediaGalleryVector: ImageVector
  get() {
    if (_mediaGallery != null) return _mediaGallery!!
    _mediaGallery = ImageVector.Builder(
      name = "MediaGallery",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      addPath(
        pathData = PathParser().parsePathString("M4 6.5C4 5.12 5.12 4 6.5 4h10c1.38 0 2.5 1.12 2.5 2.5v1.2H6.5C4.9 7.7 3.7 8.9 3.7 10.5v6H6.5v1H6.5C4.9 17.5 4 16.38 4 14.8V6.5z").toNodes(),
        fill = SolidColor(Color.White),
        fillAlpha = 0.5f
      )
      addPath(
        pathData = PathParser().parsePathString("M7.5 8h11.5c1.1 0 2 .9 2 2v10c0 1.1-.9 2-2 2H7.5c-1.1 0-2-.9-2-2V10c0-1.1.9-2 2-2z").toNodes(),
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
      )
      addPath(
        pathData = PathParser().parsePathString("M10.5 12.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z").toNodes(),
        fill = SolidColor(Color.White)
      )
      addPath(
        pathData = PathParser().parsePathString("M6.5 19l4.5-5 3.5 3.5 2.5-2.5 3.5 4").toNodes(),
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.6f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
      )
    }.build()
    return _mediaGallery!!
  }
private var _mediaGallery: ImageVector? = null

val AttachmentClipVector: ImageVector
  get() {
    if (_attachmentClip != null) return _attachmentClip!!
    _attachmentClip = ImageVector.Builder(
      name = "AttachmentClip",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      addPath(
        pathData = PathParser().parsePathString("M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48").toNodes(),
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
      )
    }.build()
    return _attachmentClip!!
  }
private var _attachmentClip: ImageVector? = null

val ModernMicVector: ImageVector
  get() {
    if (_modernMic != null) return _modernMic!!
    _modernMic = ImageVector.Builder(
      name = "ModernMic",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      addPath(
        pathData = PathParser().parsePathString("M12 2a3.2 3.2 0 0 0-3.2 3.2v6.6a3.2 3.2 0 0 0 6.4 0V5.2A3.2 3.2 0 0 0 12 2z").toNodes(),
        fill = SolidColor(Color.White)
      )
      addPath(
        pathData = PathParser().parsePathString("M18.5 10.5v1.3a6.5 6.5 0 0 1-13 0v-1.3M12 18.3v3.7M8.5 22h7").toNodes(),
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
      )
    }.build()
    return _modernMic!!
  }
private var _modernMic: ImageVector? = null

val BoltFilled: ImageVector
  get() {
    if (_boltFilled != null) return _boltFilled!!
    _boltFilled = ImageVector.Builder(
      name = "Filled.Bolt",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      addPath(
        pathData = PathParser().parsePathString("M13 2L3.5 13.5h7L9.5 22 20.5 10.5h-7L13 2z").toNodes(),
        fill = SolidColor(Color.White),
        stroke = SolidColor(Color.White),
        strokeLineWidth = 0.8f,
        strokeLineJoin = StrokeJoin.Round
      )
    }.build()
    return _boltFilled!!
  }

private var _boltFilled: ImageVector? = null

val MoreVertFilled: ImageVector
  get() {
    if (_moreVertFilled != null) return _moreVertFilled!!
    _moreVertFilled = ImageVector.Builder(
      name = "Filled.MoreVert",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      addPath(
        pathData = PathParser().parsePathString("M12 5.5a2 2 0 1 0 0-4 2 2 0 0 0 0 4zm0 8.5a2 2 0 1 0 0-4 2 2 0 0 0 0 4zm0 8.5a2 2 0 1 0 0-4 2 2 0 0 0 0 4z").toNodes(),
        fill = SolidColor(Color.White)
      )
    }.build()
    return _moreVertFilled!!
  }

private var _moreVertFilled: ImageVector? = null
