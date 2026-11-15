package com.unibud.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.unibud.app.ui.screens.AudioStudioScreen
import com.unibud.app.ui.screens.ControlScreen
import com.unibud.app.ui.screens.DevicesScreen
import com.unibud.app.ui.screens.HomeScreen
import com.unibud.app.ui.screens.InfoScreen
import com.unibud.app.ui.screens.SettingsScreen
import com.unibud.app.ui.screens.SplashScreen
import com.unibud.app.ui.screens.WelcomeScreen
import com.unibud.app.ui.theme.UniBudTheme
import com.unibud.app.ui.theme.appBackground
import kotlinx.coroutines.delay

private enum class Stage { SPLASH, WELCOME, MAIN }

/** Keeps content readable in landscape and on foldables instead of stretching edge to edge. */
private val ContentMaxWidth = 640.dp

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    DEVICES("Devices", Icons.Rounded.Bluetooth),
    CONTROL("Control", Icons.Rounded.Tune),
    AUDIO("Audio", Icons.Rounded.GraphicEq),
    INFO("Info", Icons.Rounded.Info),
}

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val saved by vm.savedDevices.collectAsStateWithLifecycle()
    val presets by vm.customPresets.collectAsStateWithLifecycle()

    val systemDark = isSystemInDarkTheme()
    val dark = when (settings?.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> systemDark
    }

    // Make status and navigation bar icons follow the app theme, not only the phone's theme.
    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(dark) {
        val bars = if (dark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        activity?.enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        onDispose { }
    }
    val background = remember(dark) { appBackground(dark) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        vm.onPermissionChanged(result.isNotEmpty() && result.values.all { it })
    }
    val requestPermission = { permissionLauncher.launch(BluetoothPermissions.required()) }

    var splashDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1200)
        splashDone = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onResume() }

    UniBudTheme(dark) {
        Box(Modifier.fillMaxSize().background(background)) {
            val current = settings
            val stage = when {
                current == null || !splashDone -> Stage.SPLASH
                !current.onboarded -> Stage.WELCOME
                else -> Stage.MAIN
            }
            AnimatedContent(
                targetState = stage,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
                label = "stage",
            ) { target ->
                when (target) {
                    Stage.SPLASH -> SplashScreen()
                    Stage.WELCOME -> WelcomeScreen(onStart = {
                        vm.completeOnboarding()
                        if (!BluetoothPermissions.hasAll(vm.getApplication<android.app.Application>())) requestPermission()
                    })
                    Stage.MAIN -> MainShell(vm, state, current ?: AppSettings(), saved, presets, requestPermission)
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
    saved: List<com.unibud.app.core.model.SavedDevice>,
    presets: List<com.unibud.app.core.audio.EqPreset>,
    requestPermission: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.events.collect { snackbar.showSnackbar(it) } }
    BackHandler(enabled = showSettings) { showSettings = false }

    val selectedSaved = saved.firstOrNull { it.address == state.selected?.address }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = !showSettings && tab == t,
                        onClick = { showSettings = false; tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (showSettings) "Profile and settings" else tab.label,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showSettings = !showSettings }) {
                    Icon(Icons.Rounded.Person, contentDescription = "Profile and settings")
                }
            }
            AnimatedContent(
                targetState = if (showSettings) null else tab,
                modifier = Modifier.widthIn(max = ContentMaxWidth),
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 24 }) togetherWith fadeOut(tween(160))
                },
                label = "tab",
            ) { target ->
                when (target) {
                    null -> SettingsScreen(settings, state.permissionGranted, vm::setTheme, requestPermission)
                    Tab.HOME -> HomeScreen(
                        state = state,
                        displayName = selectedSaved?.displayName,
                        onGoToDevices = { tab = Tab.DEVICES },
                        onRequestPermission = requestPermission,
                        onExecute = vm::execute,
                    )
                    Tab.DEVICES -> DevicesScreen(
                        state = state,
                        saved = saved,
                        onScan = vm::scan,
                        onRequestPermission = requestPermission,
                        onPair = vm::pair,
                        onInspect = { device -> vm.select(device); tab = Tab.INFO },
                    )
                    Tab.CONTROL -> ControlScreen(state.report, vm::execute)
                    Tab.AUDIO -> AudioStudioScreen(state.report, presets, vm::savePreset, vm::deletePreset, vm::execute)
                    Tab.INFO -> InfoScreen(state, selectedSaved, vm::rename, vm::forget, vm::refreshReport)
                }
            }
        }
    }
}
