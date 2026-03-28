package com.darksok.canvaslauncher.defaultscreen

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darksok.canvaslauncher.R
import com.darksok.canvaslauncher.core.model.ui.resolveDarkTheme
import com.darksok.canvaslauncher.core.ui.theme.CanvasLauncherTheme
import com.darksok.canvaslauncher.settings.SettingsActivity

@Composable
fun DefaultRoute(
    modifier: Modifier = Modifier,
    viewModel: DefaultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = uiState.themeMode.resolveDarkTheme(isSystemDark)
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val roleManager = remember(context) { context.homeRoleManagerOrNull() }
    val requestDefaultLauncherLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        val stillNotDefault = !roleManager.isHomeRoleHeldSafely()
        if (stillNotDefault) {
            context.openHomeSettingsOrChooser()
        }
    }
    var revealContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        revealContent = true
    }

    CanvasLauncherTheme(darkTheme = darkTheme, lightPalette = uiState.lightPalette, darkPalette = uiState.darkPalette) {
        SystemBarsContrastEffect(darkTheme = darkTheme)
        Scaffold(
            modifier = modifier.fillMaxSize(),
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            ),
                        ),
                    )
                    .padding(padding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    AnimatedVisibility(
                        visible = revealContent,
                        enter = fadeIn(animationSpec = tween(durationMillis = 600)) +
                            slideInVertically(initialOffsetY = { it / 5 }, animationSpec = tween(600)),
                    ) {
                        IntroHero()
                    }

                    AnimatedVisibility(
                        visible = revealContent,
                        enter = fadeIn(animationSpec = tween(durationMillis = 900)) +
                            slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(900)),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(id = R.string.default_intro_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(id = R.string.default_intro_body),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = revealContent,
                        enter = fadeIn(animationSpec = tween(durationMillis = 1100)),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val action = DefaultLauncherSetupPlanner.decideAction(
                                        sdkInt = Build.VERSION.SDK_INT,
                                        roleManagerAvailable = roleManager.canRequestHomeRoleSafely(),
                                        roleHeld = roleManager.isHomeRoleHeldSafely(),
                                    )
                                    when (action) {
                                        DefaultLauncherSetupAction.REQUEST_ROLE -> {
                                            val requestIntent = roleManager.createHomeRoleIntentSafely()
                                            if (requestIntent != null && activity != null) {
                                                runCatching {
                                                    requestDefaultLauncherLauncher.launch(requestIntent)
                                                }.onFailure {
                                                    context.openHomeSettingsOrChooser()
                                                }
                                            } else {
                                                context.openHomeSettingsOrChooser()
                                            }
                                        }

                                        DefaultLauncherSetupAction.OPEN_HOME_SETTINGS -> {
                                            context.openHomeSettingsOrChooser()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(text = stringResource(id = R.string.default_make_launcher_button))
                            }
                            OutlinedButton(
                                onClick = { context.openSettingsScreen() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(text = stringResource(id = R.string.default_open_settings_button))
                            }
                            OutlinedButton(
                                onClick = { context.openHomeScreen() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(text = stringResource(id = R.string.default_open_canvas_button))
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.default_feature_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(id = R.string.default_feature_line_one),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                            )
                            Text(
                                text = stringResource(id = R.string.default_feature_line_two),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                            )
                            Text(
                                text = stringResource(id = R.string.default_feature_line_three),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroHero() {
    val transition = rememberInfiniteTransition(label = "hero-pulse")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero-scale",
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero-glow",
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(132.dp)
            .scale(scale)
            .clip(RoundedCornerShape(32.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = glowAlpha),
                        Color.Transparent,
                    ),
                ),
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = size.minDimension * 0.28f,
            )
            drawCircle(
                color = primaryColor,
                radius = size.minDimension * 0.12f,
            )
        }
        Text(
            text = stringResource(id = R.string.default_brand_label),
            style = MaterialTheme.typography.titleMedium,
            color = onPrimaryColor,
            modifier = Modifier
                .background(
                    color = primaryColor.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SystemBarsContrastEffect(darkTheme: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}

private fun Context.openHomeSettingsOrChooser() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.onFailure {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }.onFailure {
            Toast.makeText(this, getString(R.string.default_open_settings_failed), Toast.LENGTH_SHORT).show()
        }
    }
}

private fun Context.openSettingsScreen() {
    runCatching {
        startActivity(
            Intent(this, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.onFailure {
        Toast.makeText(this, getString(R.string.default_open_settings_failed), Toast.LENGTH_SHORT).show()
    }
}

private fun Context.openHomeScreen() {
    runCatching {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.onFailure {
        Toast.makeText(this, getString(R.string.default_open_home_failed), Toast.LENGTH_SHORT).show()
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Context.homeRoleManagerOrNull(): RoleManager? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    return getSystemService(RoleManager::class.java)
}

private fun RoleManager?.canRequestHomeRoleSafely(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || this == null) return false
    return HomeRoleApi29.canRequest(this)
}

private fun RoleManager?.isHomeRoleHeldSafely(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || this == null) return false
    return HomeRoleApi29.isHeld(this)
}

private fun RoleManager?.createHomeRoleIntentSafely(): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || this == null) return null
    return HomeRoleApi29.createRequestIntent(this)
}

@RequiresApi(Build.VERSION_CODES.Q)
private object HomeRoleApi29 {
    fun canRequest(roleManager: RoleManager): Boolean {
        return roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    }

    fun isHeld(roleManager: RoleManager): Boolean {
        return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    }

    fun createRequestIntent(roleManager: RoleManager): Intent {
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }
}

