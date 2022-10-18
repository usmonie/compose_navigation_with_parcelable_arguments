package com.usmonie.compose.navigation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.usmonie.compose.navigation.ui.theme.ComposeNavigationTheme
import com.usmonie.navigation.NavExtrasHost
import com.usmonie.navigation.composable
import com.usmonie.navigation.rememberNavExtrasController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeNavigationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val controller = rememberNavExtrasController(Screens.Home)

                    NavExtrasHost(navController = controller) {
                        composable(Screens.Home.route) {
                            Button(onClick = {
                                controller.navigate(Screens.ArgNav(Arg().apply {
                                    Log.d("PRE_ROUTING", hashCode().toString())
                                }))
                            }) {

                            }
                        }

                        composable("arg_nav") {
                            Column {
                                val arg = it?.getParcelable<Arg>("arg_key")
                                Text(arg.hashCode().toString())
                            }
                        }
                    }
                }
            }
        }
    }
}