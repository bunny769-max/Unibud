package com.unibud.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unibud.app.core.bluetooth.BluetoothPermissions
import com.unibud.app.core.model.AppSettings
import com.unibud.app.core.model.ThemeMode
import com.unibud.app.ui.components.NeoTopBar
import com.unibud.app.ui.screens.AudioStudioScreen
import com.unibud.app.ui.screens.ControlCenterScreen
import com.unibud.app.ui.screens.DevicesScreen
import com.unibud.app.ui.screens.HomeScreen
import com.unibud.app.ui.screens.InfoScreen
import com.unibud.app.ui.screens.ProfileScreen
import com.unibud.app.ui.screens.SignInScreen
import com.unibud.app.ui.screens.SplashScreen
import com.unibud.app.ui.theme.Neo
import com.unibud.app.ui.theme.UniBudTheme
import kotlinx.coroutines.delay

private enum class Stage { SPLASH, SIGN_IN, MAIN }

/** Main pages. CONTROL is reached from Home and has no tab of its own, like in the design. */
private enum class Page(val title: String) {
    HOME("Home"), DEVICES("Devices"), AUDIO("Audio Studio"), INFO("Device Information"),
    PROFILE("Profile and Settings"), CONTROL("Control Center"),
}

private class TabItem(val page: Page, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Page.HOME, "Home", Icons.Rounded.Home),
    TabItem(Page.DEVICES, "Devices", Icons.Rounded.Devices),
    TabItem(Page.AUDIO, "Audio", Icons.Rounded.GraphicEq),
    TabItem(Page.INFO, "Info", Icons.Rounded.Info),
    TabItem(Page.PROFILE, "Profile", Icons.Rounded.Person),
)

/** Keeps content readable in landscape and on foldables instead of stretching edge to edge. */
private val ContentMaxWidth = 640.dp

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val user by vm.authUser.collectAsStateWithLifecycle()
    val saved by vm.savedDevices.collectAsStateWithLifecycle()
    val presets by vm.customPresets.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val systemDark = isSystemInDarkTheme()
    val dark = when (settings?.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> systemDark
    }

    // Make status and navigation bar icons follow the app theme, not only the phone's theme.
    val activity = context as? ComponentActivity
    DisposableEffect(dark) {
        val bars = if (dark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        activity?.enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        onDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        vm.onPermissionChanged(result.isNotEmpty() && result.values.all { it })
    }
    val requestPermission = { permissionLauncher.launch(BluetoothPermissions.required()) }

    var splashDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1900)
        splashDone = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onResume() }

    UniBudTheme(dark) {
        val colors = Neo.colors
        Box(Modifier.fillMaxSize().background(colors.background)) {
            val current = settings
            val stage = when {
                current == null || !splashDone -> Stage.SPLASH
                !current.onboarded -> Stage.SIGN_IN
                else -> Stage.MAIN
            }
            AnimatedContent(
                targetState = stage,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
                label = "stage",
            ) { target ->
                when (target) {
                    Stage.SPLASH -> SplashScreen()
                    Stage.SIGN_IN -> SignInScreen(
                        busy = state.signingIn,
                        configured = vm.authConfigured,
                        onGoogle = {
                            vm.signIn(context) {
                                vm.completeOnboarding()
                                if (!BluetoothPermissions.hasAll(context)) requestPermission()
                            }
                        },
                        onSkip = {
                            vm.completeOnboarding()
                            if (!BluetoothPermissions.hasAll(context)) requestPermission()
                        },
                    )
                    Stage.MAIN -> MainShell(vm, state, current ?: AppSettings(), user, saved, presets, requestPermission)
                }
            }
        }
    }
}

@Composable
private fun MainShell(
    vm: AppViewModel,
    state: AppUiState,
    settings: AppSettings,
    user: com.unibud.app.data.cloud.AuthUser?,
    saved: List<com.unibud.app.core.model.SavedDevice>,
    presets: List<com.unibud.app.core.audio.EqPreset>,
    requestPermission: () -> Unit,
) {
    val c = Neo.colors
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.events.collect { snackbar.showSnackbar(it) } }
    BackHandler(enabled = page != Page.HOME) { page = Page.HOME }

    val selectedSaved = saved.firstOrNull { it.address == state.selected?.address }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = c.bgBottom.copy(alpha = 0.92f)) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = page == tab.page || (page == Page.CONTROL && tab.page == Page.HOME),
                        onClick = { page = tab.page },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White, selectedTextColor = Color.White,
                            indicatorColor = c.violet.copy(alpha = 0.45f),
                            unselectedIconColor = c.textDim, unselectedTextColor = c.textDim,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (page != Page.HOME) {
                Box(Modifier.widthIn(max = ContentMaxWidth)) {
                    NeoTopBar(
                        title = page.title,
                        onBack = { page = Page.HOME },
                        onTrailing = if (page == Page.PROFILE) null else ({ page = Page.PROFILE }),
                    )
                }
            }
            AnimatedContent(
                targetState = page,
                modifier = Modifier.widthIn(max = ContentMaxWidth),
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 24 }) togetherWith fadeOut(tween(160))
                },
                label = "page",
            ) { target ->
                when (target) {
                    Page.HOME -> HomeScreen(
                        state = state,
                        displayName = selectedSaved?.displayName,
                        signedIn = user != null,
                        onProfile = { page = Page.PROFILE },
                        onDevices = { page = Page.DEVICES },
                        onControl = { page = Page.CONTROL },
                        onAudio = { page = Page.AUDIO },
                        onRequestPermission = requestPermission,
                        onExecute = { vm.execute(it) },
                        onVolume = vm::setVolume,
                        onMedia = vm::media,
                    )
                    Page.DEVICES -> DevicesScreen(
                        state = state,
                        saved = saved,
                        onScan = vm::scan,
                        onRequestPermission = requestPermission,
                        onPair = vm::pair,
                        onInspect = { device -> vm.select(device); page = Page.INFO },
                    )
                    Page.CONTROL -> ControlCenterScreen(state.report) { vm.execute(it) }
                    Page.AUDIO -> AudioStudioScreen(state.report, presets, vm::savePreset, vm::deletePreset) { vm.execute(it) }
                    Page.INFO -> InfoScreen(state, selectedSaved, vm::rename, vm::setBrand, vm::forget, vm::refreshReport)
                    Page.PROFILE -> ProfileScreen(
                        user = user,
                        settings = settings,
                        deviceCount = saved.size,
                        permissionGranted = state.permissionGranted,
                        signingIn = state.signingIn,
                        syncing = state.syncing,
                        onSignIn = { vm.signIn(context) },
                        onSignOut = { vm.signOut(context) },
                        onSync = vm::syncNow,
                        onTheme = vm::setTheme,
                        onDevices = { page = Page.DEVICES },
                        onRequestPermission = requestPermission,
                    )
                }
            }
        }
    }
}
