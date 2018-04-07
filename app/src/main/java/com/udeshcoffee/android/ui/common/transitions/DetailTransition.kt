package com.udeshcoffee.android.ui.common.transitions

import android.os.Build
import android.support.annotation.RequiresApi
import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.TransitionSet

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class DetailsTransition: TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER
        addTransition(ChangeBounds())
                .addTransition(ChangeTransform())
                .addTransition(ChangeImageTransform())
    }
}