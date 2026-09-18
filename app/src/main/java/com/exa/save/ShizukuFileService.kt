package com.exa.save

import android.content.Context
import androidx.annotation.Keep

/** Runs inside Shizuku/Sui with shell or root identity. */
@Keep
class ShizukuFileService() : PrivilegedFileBinder() {

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : this()
}
