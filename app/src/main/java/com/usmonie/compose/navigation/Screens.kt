package com.usmonie.compose.navigation

import com.usmonie.navigation.Extra
import com.usmonie.navigation.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class Screens(
    override val route: String,
    override val extras: List<Pair<String, Extra>>? = null
) : Screen(route, extras) {

    object Home : Screens("home", )
    data class ArgNav(val arg: Arg) : Screens("arg_nav", listOf("arg_key" to arg))
}

@Parcelize
class Arg : Extra