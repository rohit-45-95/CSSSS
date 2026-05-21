package com.craftstudio.launcher.anim

import android.animation.Animator
import android.view.View

abstract class BaseAnimator {
    abstract fun getAnimators(target: View): Array<Animator>
}