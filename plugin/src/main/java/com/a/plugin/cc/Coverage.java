package com.a.plugin.cc;

import com.google.common.io.Files;

import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.internal.InputStreams;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class Coverage {

    public static boolean matches(String className) {
//        return className.contains("MainActivity");
        return CoverageConfig.matches(className);
    }

    public static boolean instrumentAndSave(String className, byte[] classBytes, File directory) {
        try {

            if (className.endsWith(".class")) {
                className = className.substring(0, className.indexOf(".class"));
            }
            byte[] instrumented = instrument(className, classBytes);

            File outputFile = new File(directory, className.replace(".", File.separator) + ".class");
            Files.createParentDirs(outputFile);
            Files.write(instrumented, outputFile);
            return true;
//            return instrumented;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    public static boolean instrumentAndSave(String className, File classFile, File inputDir, File destDir) {
        try {
            FileInputStream inputStream = new FileInputStream(classFile);
            byte[] classBytes = InputStreams.readFully(inputStream);
            return instrumentAndSave(className, classBytes, destDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    public static byte[] instrument(String className, byte[] classBytes) {
        try {

            Instrumenter instrumenter = new Instrumenter(new OfflineInstrumentationAccessGenerator());
            byte[] instrumented = instrumenter.instrument(classBytes, className + " instrument");
            return instrumented;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classBytes;

    }

//    public static void instrumentJarToJar(File inputJarFile,File outputJarFile ) {

}
