package com.usmonie.navigation

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NavExtrasHostController(context: Context, val startDestination: Screen) :
    NavHostController(context) {

    private val _currentScreensBackStack: MutableStateFlow<MutableMap<String, Screen>> =
        MutableStateFlow(mutableMapOf(startDestination.route to startDestination))

    public val currentScreensBackStack: StateFlow<Map<String, Screen>> =
        _currentScreensBackStack.asStateFlow()

    override fun popBackStack(): Boolean {
        _currentScreensBackStack.update { screensMap ->
            screensMap.apply { remove(currentDestination?.route) }
        }
        return super.popBackStack()
    }

    fun navigate(screen: Screen, navOptions: NavOptions? = null) {
        _currentScreensBackStack.update { it.apply { put(screen.route, screen) } }
        navigate(route = screen.route, navOptions = navOptions)
    }

    companion object {
        private const val SCREENS_BACK_STACK_KEY = "SCREENS_BACK_STACK"
    }
}

/**
 * Creates a NavHostController that handles the adding of the [ComposeNavigator] and
 * [DialogNavigator]. Additional [Navigator] instances can be passed through [navigators] to
 * be applied to the returned NavController. Note that each [Navigator] must be separately
 * remembered before being passed in here: any changes to those inputs will cause the
 * NavController to be recreated.
 *
 * @see NavHost
 */
@Composable
public fun rememberNavExtrasController(
    startDestination: Screen,
    vararg navigators: Navigator<out NavDestination>
): NavExtrasHostController {
    val context = LocalContext.current
    return rememberSaveable(
        inputs = navigators,
        saver = NavExtrasControllerSaver(context, startDestination)
    ) {
        createNavExtrasController(context, startDestination)
    }.apply {
        for (navigator in navigators) {
            navigatorProvider.addNavigator(navigator)
        }
    }
}

private fun createNavExtrasController(context: Context, startDestination: Screen) =
    NavExtrasHostController(context, startDestination = startDestination).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        navigatorProvider.addNavigator(DialogNavigator())
    }

/**
 * Saver to save and restore the NavController across config change and process death.
 */
private fun NavExtrasControllerSaver(
    context: Context,
    startDestination: Screen
): Saver<NavExtrasHostController, *> = Saver<NavExtrasHostController, Bundle>(
    save = { it.saveState() },
    restore = { createNavExtrasController(context, startDestination).apply { restoreState(it) } }
)
