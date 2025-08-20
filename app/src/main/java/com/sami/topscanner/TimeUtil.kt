package com.sami.topscanner

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtil {
    fun formatMillisToDisplay(ms: Long): String {
        if (ms <= 0L) return ""
        val sdf = SimpleDateFormat("d-M-yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(ms))
    }
}