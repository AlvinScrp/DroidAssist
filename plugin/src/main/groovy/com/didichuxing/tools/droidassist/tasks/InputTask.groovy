package com.didichuxing.tools.droidassist.tasks

import com.a.plugin.cc.Coverage
import com.android.build.api.transform.QualifiedContent
import com.didichuxing.tools.droidassist.DroidAssistContext
import com.didichuxing.tools.droidassist.DroidAssistExecutor.BuildContext
import com.didichuxing.tools.droidassist.ex.DroidAssistBadStatementException
import com.didichuxing.tools.droidassist.ex.DroidAssistException
import com.didichuxing.tools.droidassist.ex.DroidAssistNotFoundException
import com.didichuxing.tools.droidassist.util.IOUtils
import com.didichuxing.tools.droidassist.util.Logger
import javassist.CannotCompileException
import javassist.CtClass
import javassist.NotFoundException
import org.apache.commons.io.FileUtils

/**
 * Interface to process QualifiedContent.
 *
 * <p> It provides the ability to handle classes, see {@link #executeClass}
 */
abstract class InputTask<T extends QualifiedContent> implements Runnable {

    public static final String DOT_CLASS = ".class"
    public static final String DOT_JAR = ".jar"

    DroidAssistContext context
    BuildContext buildContext
    TaskInput<T> taskInput
    File temporaryDir

    static class TaskInput<T> {
        T input
        File dest
        boolean incremental
    }

    InputTask(
            DroidAssistContext context,
            BuildContext buildContext,
            TaskInput<T> taskInput) {
        this.context = context
        this.buildContext = buildContext
        this.taskInput = taskInput
        temporaryDir = ensureTemporaryDir()
    }

    @Override
    final void run() {
        try {
            Logger.info("execute ${inputType}: ${IOUtils.getPath(taskInput.input.file)}")
            execute()
        } catch (DroidAssistException e) {
            throw e
        } catch (Throwable e) {
            throw new DroidAssistException("Execution failed for " +
                    "input:${IOUtils.getPath(taskInput.input.file)}", e)
        }
    }

    abstract void execute()

    abstract String getInputType()

    File ensureTemporaryDir() {
        def dir = new File(
                "${buildContext.temporaryDir}/" +
                        "${inputType}/" +
                        "${taskInput.input.name.replace(":", "-")}")
        if (dir.exists()) {
            if (dir.isDirectory()) {
                org.apache.commons.io.FileUtils.cleanDirectory(dir)
            } else if (dir.isFile()) {
                FileUtils.forceDelete(dir)
            }
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    boolean executeClass(String className, File directory) {
        println("executeClass:${className}")
        if (className.contains("module-info") || className.contains("META-INF.")) {
            return false
        }
        buildContext.totalCounter.incrementAndGet()
        CtClass inputClass = null
        def transformers = context.transformers.findAll {
            it.classAllowed(className)
        }

//        if (transformers.isEmpty()) {
//            return false
//        }

        inputClass = context.classPool.getOrNull(className)
        if (inputClass == null) {
            return false
        }

//        def needCoverage = Coverage.matches(className)
//        if (needCoverage) {
//            def classBytes = inputClass.toBytecode()
//            def instrumentBytes = Coverage.instrument(className, classBytes)
//            InputStream sbs = new ByteArrayInputStream(instrumentBytes);
//            inputClass.detach()
//            inputClass = context.classPool.makeClass(sbs)
//            if (inputClass.isFrozen()) {
//                inputClass.defrost()
//            }
//        }

        transformers.each {
            try {
                it.performTransform(inputClass, className)
            } catch (Exception e) {
                throw new DroidAssistException(
                        "Transform failed for class: ${className}" +
                                " with  exception: ${e.cause?.message}", e)
            }
        }


//        println("inputClass.needCoverage:${needCoverage}")
//        println("inputClass.modified:${inputClass.modified}")
//        if (needCoverage || inputClass.modified) {
//            buildContext.affectedCounter.incrementAndGet()
//            inputClass.writeFile(directory.absolutePath)
//            return true
//        }


        def needCoverage = Coverage.matches(className)
        if (needCoverage) {
            def classBytes = inputClass.toBytecode()
            Coverage.instrumentAndSave(className, classBytes, directory)
            println("inputClass.needCoverage:${needCoverage}")
            buildContext.affectedCounter.incrementAndGet()
            return true
        }
        else if (inputClass.modified) {
            println("inputClass.modified:${inputClass.modified}")
            buildContext.affectedCounter.incrementAndGet()
            inputClass.writeFile(directory.absolutePath)
            return true
        }
//        return false
//        println("inputClass.modified:${inputClass.modified}")
        buildContext.affectedCounter.incrementAndGet()
        inputClass.writeFile(directory.absolutePath)
        return true
    }
}
