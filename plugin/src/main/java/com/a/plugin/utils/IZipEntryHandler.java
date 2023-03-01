package com.a.plugin.utils;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

public interface IZipEntryHandler {

    byte[] inject(ZipFile inZipFile,ZipEntry entry,String className);
}
