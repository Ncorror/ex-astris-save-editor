package com.exa.save;

import android.os.ParcelFileDescriptor;

interface IShizukuFileService {
    void destroy() = 16777114;
    int getRemoteUid() = 1;
    String[] findSaveFiles() = 2;
    ParcelFileDescriptor openRead(String path) = 3;
    ParcelFileDescriptor openWrite(String path) = 4;
    boolean exists(String path) = 5;
    ParcelFileDescriptor openAtomicWrite(String path) = 6;
    void commitAtomicWrite(String path) = 7;
    void abortAtomicWrite(String path) = 8;
}
