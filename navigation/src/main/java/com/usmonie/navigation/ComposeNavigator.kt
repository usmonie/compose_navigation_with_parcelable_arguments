package com.usmonie.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.usmonie.navigation.ComposeNavigator.Destination

/**
 * Navigator that navigates through [Composable]s. Every destination using this Navigator must
 * set a valid [Composable] by setting it directly on an instantiated [Destination] or calling
 * [composable].
 */
@Navigator.Name("composable")
public class ComposeNavigator : Navigator<Destination>() {

    /**
     * Get the map of transitions currently in progress from the [state].
     */
    internal val transitionsInProgress get() = state.transitionsInProgress

    /**
     * Get the back stack from the [state].
     */
    internal val backStack get() = state.backStack

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        entries.forEach { entry ->
            state.pushWithTransition(entry)
        }
    }

    override fun createDestination(): Destination {
        return Destination(this) { }
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.popWithTransition(popUpTo, savedState)
    }

    /**
     * Callback that removes the given [NavBackStackEntry] from the [map of the transitions in
     * progress][transitionsInProgress]. This should be called in conjunction with [navigate] and
     * [popBackStack] as those call are responsible for adding entries to [transitionsInProgress].
     *
     * Failing to call this method could result in entries being prevented from reaching their
     * final [Lifecycle.State]}.
     */
    internal fun onTransitionComplete(entry: NavBackStackEntry) {
        state.markTransitionComplete(entry)
    }

    /**
     * NavDestination specific to [ComposeNavigator]
     */
    @NavDestination.ClassType(Composable::class)
    public class Destination(
        navigator: ComposeNavigator,
        internal val content: @Composable (arguments: Bundle?) -> Unit
    ) : NavDestination(navigator)

    internal companion object {
        internal const val NAME = "composable"
    }
}
