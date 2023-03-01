package com.a.plugin.utils;

import com.android.SdkConstants;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class JarUtil {

    public static void instrumentJarToJar(File inputJarFile, File outputJarFile, IZipEntryHandler handler) {
        ZipOutputStream jarOutputStream = null;
        try {
            Files.deleteIfExists(outputJarFile.toPath());
            ZipFile inZipFile = new ZipFile(inputJarFile);
            jarOutputStream =
                    new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputJarFile)));
            jarOutputStream.setLevel(Deflater.NO_COMPRESSION);

            Enumeration<? extends ZipEntry> entries = inZipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InputStream inputStream = inZipFile.getInputStream(entry);
                String entryName = entry.getName();
                byte[] entryBytes = readFully(inputStream);
                if (classMatcher(entryName)) {
                    byte[] classBytes = handler.inject(inZipFile, entry, toClassName(entryName));
                    if (classBytes != null) {
                        entryBytes = classBytes;
                    }
                }
                saveEntryToJar(entryName, entryBytes, jarOutputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jarOutputStream != null) {
                try {
                    jarOutputStream.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    public static void saveEntryToJar(
            String entryName,
            byte[] byteArray,
            ZipOutputStream jarOutputStream
    ) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        entry.setTime(0);
        jarOutputStream.putNextEntry(entry);
        jarOutputStream.write(byteArray);
        jarOutputStream.closeEntry();
        jarOutputStream.flush();
    }

    public static byte[] readFully(final InputStream is) throws IOException {
        final byte[] buf = new byte[1024];
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            final int r = is.read(buf);
            if (r == -1) {
                break;
            }
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    public static boolean classMatcher(String entryName) {
        String lowerCase = entryName.toLowerCase();
        if (!lowerCase.endsWith(SdkConstants.DOT_CLASS)) {
            return false;
        }

        if (lowerCase.equals("module-info.class")
                || lowerCase.endsWith("/module-info.class")) {
            return false;
        }

        if (lowerCase.startsWith("/meta-inf/") || lowerCase.startsWith("meta-inf/")) {
            return false;
        }
        return true;
    }

    /**
     *
     * @param entryName maybe like  com/a/b/C.class
     * @return
     */
    public static String toClassName(String entryName) {
        String className = entryName.replace("/", ".");
        className = FilenameUtils.removeExtension(className);
        return className;
    }


}
