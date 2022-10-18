package com.usmonie.navigation

import android.os.Parcelable

abstract class Screen(open val route: String, open val extras: List<Pair<String, Extra>>? = null): Parcelable