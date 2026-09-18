package com.exa.save

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

/**
 * libsu RootService backend. The binder itself still enforces the Ex Astris
 * Android/data allow-list, despite this process running with UID 0.
 */
class RootFileService : RootService() {
    private val binder = PrivilegedFileBinder()

    override fun onBind(intent: Intent): IBinder = binder
}
