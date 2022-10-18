package com.usmonie.navigation

import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.Navigator
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.createGraph
import androidx.navigation.get
import com.usmonie.navigation.ComposeNavigator.Destination
import kotlinx.coroutines.flow.map

@Composable
fun NavExtrasHost(
    navController: NavExtrasHostController,
    modifier: Modifier = Modifier,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    NavHost(
        navController,
        remember(route, navController.startDestination.route, builder) {
            navController.createGraph(navController.startDestination.route, route, builder)
        },
        modifier
    )
}

@Composable
public fun NavHost(
    navController: NavExtrasHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "NavHost requires a ViewModelStoreOwner to be provided via LocalViewModelStoreOwner"
    }
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onBackPressedDispatcher = onBackPressedDispatcherOwner?.onBackPressedDispatcher

    // Setup the navController with proper owners
    navController.setLifecycleOwner(lifecycleOwner)
    navController.setViewModelStore(viewModelStoreOwner.viewModelStore)
    if (onBackPressedDispatcher != null) {
        navController.setOnBackPressedDispatcher(onBackPressedDispatcher)
    }
    // Ensure that the NavController only receives back events while
    // the NavHost is in composition
    DisposableEffect(navController) {
        navController.enableOnBackPressed(true)
        onDispose {
            navController.enableOnBackPressed(false)
        }
    }

    // Then set the graph
    navController.graph = graph

    val saveableStateHolder = rememberSaveableStateHolder()

    // Find the ComposeNavigator, returning early if it isn't found
    // (such as is the case when using TestNavHostController)
    val composeNavigator = navController.navigatorProvider.get<Navigator<out NavDestination>>(
        ComposeNavigator.NAME
    ) as? ComposeNavigator ?: return
    val visibleEntries by remember(navController.visibleEntries) {
        navController.visibleEntries.map {
            it.filter { entry ->
                entry.destination.navigatorName == ComposeNavigator.NAME
            }
        }
    }.collectAsState(emptyList())
    val screensBackStack by navController.currentScreensBackStack.collectAsState()

    val backStackEntry = visibleEntries.lastOrNull()

    var initialCrossfade by remember { mutableStateOf(true) }
    if (backStackEntry != null) {
        // while in the scope of the composable, we provide the navBackStackEntry as the
        // ViewModelStoreOwner and LifecycleOwner
        Crossfade(backStackEntry.id, modifier) {
            val lastEntry = visibleEntries.last { entry ->
                it == entry.id
            }
            // We are disposing on a Unit as we only want to dispose when the CrossFade completes
            DisposableEffect(Unit) {
                if (initialCrossfade) {
                    // There's no animation for the initial crossfade,
                    // so we can instantly mark the transition as complete
                    visibleEntries.forEach { entry ->
                        composeNavigator.onTransitionComplete(entry)
                    }
                    initialCrossfade = false
                }
                onDispose {
                    visibleEntries.forEach { entry ->
                        composeNavigator.onTransitionComplete(entry)
                    }
                }
            }

            lastEntry.LocalOwnersProvider(saveableStateHolder) {
                val lastScreenExtras = screensBackStack[lastEntry.destination.route]?.extras
                val newLastEntryArguments = if (!lastScreenExtras.isNullOrEmpty()) {
                    (lastEntry.arguments ?: Bundle()).putExtras(lastScreenExtras)
                } else {
                    lastEntry.arguments
                }

                (lastEntry.destination as Destination).content(newLastEntryArguments)
            }
        }
    }

    val dialogNavigator = navController.navigatorProvider.get<Navigator<out NavDestination>>(
        DialogNavigator.NAME
    ) as? DialogNavigator ?: return

    // Show any dialog destinations
    DialogHost(dialogNavigator)
}

private fun Bundle.putExtras(extras: List<Pair<String, Extra>>): Bundle {
    extras.forEach {
        putParcelable(it.first, it.second)
    }

    return this
}

/**
 * Add the [Composable] to the [NavGraphBuilder]
 *
 * @param route route for the destination
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param content composable for the destination
 */
public fun NavGraphBuilder.composable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable (Bundle?) -> Unit
) {
    addDestination(
        ComposeNavigator.Destination(provider[ComposeNavigator::class], content).apply {
            this.route = route
            arguments.forEach { (argumentName, argument) ->
                addArgument(argumentName, argument)
            }
            deepLinks.forEach { deepLink ->
                addDeepLink(deepLink)
            }
        }
    )
}