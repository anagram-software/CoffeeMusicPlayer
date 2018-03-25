package com.udeshcoffee.android.utils

import java.text.Collator
import java.util.*

/**
* Created by Udathari on 7/25/2017.
*/

val collator : Collator = Collator.getInstance(Locale.getDefault())

fun compareLong(a : Long, b : Long) : Int{
    return if (a < b) -1 else if (a == b) 0 else 1
}

fun compareInt(a : Int, b : Int) : Int{
    return if (a < b) -1 else if (a == b) 0 else 1
}

fun compareString(a : String?, b : String?) : Int{
    return when {
        a == null -> -1
        b == null -> 1
        a == b -> 0
        else -> collator.compare(a , b)
    }
}