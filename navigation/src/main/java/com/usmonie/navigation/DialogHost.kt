package com.usmonie.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.LocalOwnersProvider
import com.usmonie.navigation.DialogNavigator.Destination

/**
 * Show each [Destination] on the [DialogNavigator]'s back stack as a [Dialog].
 *
 * Note that [NavHost] will call this for you; you do not need to call it manually.
 */
@Composable
public fun DialogHost(dialogNavigator: DialogNavigator) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val dialogBackStack by dialogNavigator.backStack.collectAsState()
    val visibleBackStack = rememberVisibleList(dialogBackStack)
    visibleBackStack.PopulateVisibleList(dialogBackStack)

    visibleBackStack.forEach { backStackEntry ->
        val destination = backStackEntry.destination as Destination
        Dialog(
            onDismissRequest = { dialogNavigator.dismiss(backStackEntry) },
            properties = destination.dialogProperties
        ) {
            DisposableEffect(backStackEntry) {
                onDispose {
                    dialogNavigator.onTransitionComplete(backStackEntry)
                }
            }

            // while in the scope of the composable, we provide the navBackStackEntry as the
            // ViewModelStoreOwner and LifecycleOwner
            backStackEntry.LocalOwnersProvider(saveableStateHolder) {
                destination.content(backStackEntry.arguments)
            }
        }
    }
}

@Composable
internal fun MutableList<NavBackStackEntry>.PopulateVisibleList(
    transitionsInProgress: Collection<NavBackStackEntry>
) {
    transitionsInProgress.forEach { entry ->
        DisposableEffect(entry.lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                // ON_START -> add to visibleBackStack, ON_STOP -> remove from visibleBackStack
                if (event == Lifecycle.Event.ON_START) {
                    // We want to treat the visible lists as Sets but we want to keep
                    // the functionality of mutableStateListOf() so that we recompose in response
                    // to adds and removes.
                    if (!contains(entry)) {
                        add(entry)
                    }
                }
                if (event == Lifecycle.Event.ON_STOP) {
                    remove(entry)
                }
            }
            entry.lifecycle.addObserver(observer)
            onDispose {
                entry.lifecycle.removeObserver(observer)
            }
        }
    }
}

@Composable
internal fun rememberVisibleList(transitionsInProgress: Collection<NavBackStackEntry>) =
    remember(transitionsInProgress) {
        mutableStateListOf<NavBackStackEntry>().also {
            it.addAll(
                transitionsInProgress.filter { entry ->
                    entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                }
            )
        }
    }
