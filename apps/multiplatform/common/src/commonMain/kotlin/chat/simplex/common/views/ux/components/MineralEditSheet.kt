package chat.simplex.common.views.ux.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.desktop.ui.tooling.preview.Preview
import chat.simplex.common.ui.theme.*
import chat.simplex.common.ui.theme.isInDarkTheme

/**
 * Luxury Mineral bottom-sheet scaffold for short single-purpose editors (#64).
 *
 * Presentation only: the host owns the M2 `ModalBottomSheetLayout` and its sheet state, and
 * all save/discard semantics. M2 has no dismiss callback, so the host intercepts user-initiated
 * hides via `rememberModalBottomSheetState(confirmValueChange = ...)` and decides whether to
 * save/discard before calling `hide()`; this scaffold only renders the drag handle, title,
 * editor content and the filled gold Save row.
 *
 * Colors: ui/theme tokens only (no raw hex). Text: caller-provided (MR.strings at call sites).
 */
@Composable
fun MineralEditSheet(
    title: String,
    saveTitle: String,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    showSaveRow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isInDarkTheme()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(if (isDark) Slate900 else Slate50)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) Slate600 else Slate300)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Slate50 else Slate900
        )

        Spacer(Modifier.height(12.dp))

        content()

        if (showSaveRow) {
            Spacer(Modifier.height(16.dp))

            // Save row (filled champagne-gold CTA, per the design system's active-gold tokens)
            val saveBackground =
                if (saveEnabled) (if (isDark) ChampagneGold else Amber600)
                else (if (isDark) Slate800 else Slate200)
            val saveContent =
                if (saveEnabled) (if (isDark) Slate900 else Color.White)
                else (if (isDark) Slate500 else Slate400)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(saveBackground)
                    .clickable(enabled = saveEnabled) { onSave() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = saveTitle,
                    color = saveContent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewMineralEditSheet() {
    SimpleXTheme {
        MineralEditSheet(
            title = "Edit group profile",
            saveTitle = "Save",
            saveEnabled = true,
            onSave = {}
        ) {
            Text("Sheet body", color = if (isInDarkTheme()) Slate100 else Slate700)
        }
    }
}
