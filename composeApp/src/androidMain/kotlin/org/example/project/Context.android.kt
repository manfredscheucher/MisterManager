package org.example.project

import android.content.Context

private var applicationContext: Context? = null

fun setContext(context: Context) {
    applicationContext = context
}

actual fun getContext(): Any? = applicationContext
