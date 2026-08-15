package chat.simplex.common.views.ux.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.CreateProfile
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.usersettings.UserProfilesView
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch

@Composable
fun ProfileSwitcherOverlay(
  chatModel: ChatModel,
  show: Boolean,
  onDismiss: () -> Unit,
  onNavigateToProfile: () -> Unit
) {
  val isDark = isInDarkTheme()
  val scope = rememberCoroutineScope()

  if (show && appPlatform.isAndroid) {
    BackHandler {
      onDismiss()
    }
  }

  AnimatedVisibility(
    visible = show,
    enter = fadeIn(animationSpec = tween(200)),
    exit = fadeOut(animationSpec = tween(150)),
    modifier = Modifier.zIndex(1000f)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.55f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) {
          onDismiss()
        }
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 14.dp)
          .padding(bottom = 82.dp),
        contentAlignment = Alignment.BottomCenter
      ) {
        AnimatedVisibility(
          visible = show,
          enter = scaleIn(
            initialScale = 0.82f,
            transformOrigin = TransformOrigin(0.85f, 1f),
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)
          ) + fadeIn(animationSpec = tween(200)) + slideInVertically(
            initialOffsetY = { it / 5 },
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)
          ),
          exit = scaleOut(
            targetScale = 0.82f,
            transformOrigin = TransformOrigin(0.85f, 1f),
            animationSpec = tween(150)
          ) + fadeOut(animationSpec = tween(150)) + slideOutVertically(
            targetOffsetY = { it / 5 },
            animationSpec = tween(150)
          )
        ) {
          val cardShape = RoundedCornerShape(26.dp)
          val cardBg = if (isDark) Color(0xF7182232) else Color(0xFCFFFFFF)
          val cardBorder = if (isDark) Color(0x4DE2B755) else Color(0x33D97706)
          val goldAccent = if (isDark) Color(0xFFE2B755) else Color(0xFFD97706)

          Surface(
            modifier = Modifier
              .widthIn(max = 420.dp)
              .fillMaxWidth()
              .shadow(elevation = 18.dp, shape = cardShape)
              .clip(cardShape)
              .border(width = 1.dp, color = cardBorder, shape = cardShape)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) { /* consume clicks inside card */ },
            shape = cardShape,
            color = cardBg
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            ) {
              // ── Header ───────────────────────────────────────────
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(CircleShape)
                      .background(goldAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      painter = painterResource(MR.images.ic_manage_accounts),
                      contentDescription = null,
                      tint = goldAccent,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Column {
                    Text(
                      text = "Profils & Identités",
                      style = TextStyle(
                        fontFamily = Inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                      )
                    )
                    Text(
                      text = "Basculez d'identité en un clic",
                      style = TextStyle(
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                      )
                    )
                  }
                }

                IconButton(
                  onClick = onDismiss,
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x2AFFFFFF) else Color(0x14000000))
                ) {
                  Icon(
                    painter = painterResource(MR.images.ic_close),
                    contentDescription = "Fermer",
                    tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              Spacer(Modifier.height(14.dp))
              Divider(
                color = if (isDark) Color(0x22FFFFFF) else Color(0x14000000),
                thickness = 0.5.dp
              )
              Spacer(Modifier.height(10.dp))

              // ── Profiles List ─────────────────────────────────────
              val users = remember(chatModel.users.toList()) {
                chatModel.users.map { it.user }.sortedByDescending { it.activeUser }
              }

              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(users) { user ->
                  val isActive = user.activeUser
                  val itemShape = RoundedCornerShape(16.dp)

                  val itemBg = if (isActive) {
                    if (isDark) goldAccent.copy(alpha = 0.16f) else goldAccent.copy(alpha = 0.12f)
                  } else {
                    if (isDark) Color(0x1F334155) else Color(0x0D0F172A)
                  }

                  val itemBorder = if (isActive) {
                    goldAccent.copy(alpha = 0.55f)
                  } else {
                    if (isDark) Color(0x1FFFFFFF) else Color(0x10000000)
                  }

                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(itemShape)
                      .background(itemBg)
                      .border(width = 1.dp, color = itemBorder, shape = itemShape)
                      .clickable {
                        onDismiss()
                        if (isActive) {
                          onNavigateToProfile()
                        } else {
                          scope.launch {
                            withBGApi {
                              chatModel.controller.showProgressIfNeeded {
                                ModalManager.closeAllModalsEverywhere()
                                chatModel.controller.changeActiveUser(user.remoteHostId, user.userId, null)
                              }
                            }
                          }
                        }
                      }
                      .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(
                      modifier = Modifier.weight(1f),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                      // Avatar
                      Box(
                        modifier = Modifier
                          .size(42.dp)
                          .clip(CircleShape)
                          .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) goldAccent else (if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)),
                            shape = CircleShape
                          ),
                        contentAlignment = Alignment.Center
                      ) {
                        if (user.image != null) {
                          ProfileImage(
                            image = user.image,
                            size = 42.dp
                          )
                        } else {
                          Box(
                            modifier = Modifier
                              .fillMaxSize()
                              .background(
                                Brush.linearGradient(
                                  if (isActive) listOf(goldAccent, goldAccent.copy(alpha = 0.8f))
                                  else if (isDark) listOf(Color(0xFF475569), Color(0xFF334155))
                                  else listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))
                                )
                              ),
                            contentAlignment = Alignment.Center
                          ) {
                            Text(
                              text = (user.displayName.take(1).ifEmpty { "?" }).uppercase(),
                              style = TextStyle(
                                fontFamily = Inter,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) (if (isDark) Color(0xFF0F172A) else Color.White) else Color.White
                              )
                            )
                          }
                        }
                      }

                      // Name & Bio
                      Column(modifier = Modifier.weight(1f)) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                          Text(
                            text = user.displayName.ifEmpty { "Sans nom" },
                            style = TextStyle(
                              fontFamily = Inter,
                              fontSize = 14.sp,
                              fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                              color = if (isActive) goldAccent else (if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A))
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }

                        val subtitle = when {
                          user.fullName.isNotBlank() -> user.fullName
                          !user.shortDescr.isNullOrBlank() -> user.shortDescr ?: ""
                          else -> "Profil local"
                        }

                        Text(
                          text = subtitle,
                          style = TextStyle(
                            fontFamily = Inter,
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                          ),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }

                    // Active badge or arrow
                    if (isActive) {
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(10.dp))
                          .background(goldAccent.copy(alpha = 0.22f))
                          .border(0.5.dp, goldAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                          .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          Icon(
                            painter = painterResource(MR.images.ic_done_filled),
                            contentDescription = "Actif",
                            tint = goldAccent,
                            modifier = Modifier.size(12.dp)
                          )
                          Text(
                            text = "Actif",
                            style = TextStyle(
                              fontFamily = Inter,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold,
                              color = goldAccent
                            )
                          )
                        }
                      }
                    } else {
                      Icon(
                        painter = painterResource(MR.images.ic_arrow_forward),
                        contentDescription = "Basculez",
                        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }

              Spacer(Modifier.height(14.dp))
              Divider(
                color = if (isDark) Color(0x22FFFFFF) else Color(0x14000000),
                thickness = 0.5.dp
              )
              Spacer(Modifier.height(14.dp))

              // ── Action Buttons ────────────────────────────────────
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                // Button 1: + Nouveau profil
                Button(
                  onClick = {
                    onDismiss()
                    ModalManager.center.showModalCloseable { close ->
                      CreateProfile(chatModel, close)
                    }
                  },
                  modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp),
                  shape = RoundedCornerShape(14.dp),
                  colors = ButtonDefaults.buttonColors(
                    backgroundColor = goldAccent,
                    contentColor = if (isDark) Color(0xFF0F172A) else Color.White
                  ),
                  elevation = ButtonDefaults.elevation(defaultElevation = 2.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                  Icon(
                    painter = painterResource(MR.images.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isDark) Color(0xFF0F172A) else Color.White
                  )
                  Spacer(Modifier.width(6.dp))
                  Text(
                    text = "Nouveau profil",
                    style = TextStyle(
                      fontFamily = Inter,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isDark) Color(0xFF0F172A) else Color.White
                    )
                  )
                }

                // Button 2: Gérer tous les profils
                OutlinedButton(
                  onClick = {
                    onDismiss()
                    ModalManager.start.showCustomModal(keyboardCoversBar = false) { close ->
                      val search = rememberSaveable { mutableStateOf("") }
                      val profileHidden = rememberSaveable { mutableStateOf(false) }
                      ModalView(
                        { close() },
                        showSearch = true,
                        searchAlwaysVisible = true,
                        onSearchValueChanged = { search.value = it },
                        content = {
                          UserProfilesView(chatModel, search, profileHidden) { block ->
                            block()
                          }
                        }
                      )
                    }
                  },
                  modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                  shape = RoundedCornerShape(14.dp),
                  colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (isDark) Color(0x331E293B) else Color(0x80F1F5F9),
                    contentColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                  ),
                  border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                  Icon(
                    painter = painterResource(MR.images.ic_settings),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                  )
                  Spacer(Modifier.width(6.dp))
                  Text(
                    text = "Gérer",
                    style = TextStyle(
                      fontFamily = Inter,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                    )
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
