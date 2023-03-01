package com.didichuxing.tools.droidassist.tasks

import com.a.plugin.utils.IZipEntryHandler
import com.a.plugin.utils.JarUtil
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.Status
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

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class JarInputTask extends InputTask<JarInput> {

    JarInputTask(
            DroidAssistContext context,
            BuildContext buildContext,
            TaskInput<JarInput> taskInput) {
        super(context, buildContext, taskInput)
    }

    @Override
    String getInputType() {
        return "jar"
    }

    /**
     * If jar is changed, reprocess jar and otherwise skip.
     *
     * <p> See for details {@link JarInputTask#executeClass}
     */
    void execute() {
        JarInput input = taskInput.input
        File inputJar = input.file
        if (taskInput.incremental) {
            if (input.status != Status.NOTCHANGED) {
                Logger.info("Jar incremental build: \ninput:${ taskInput.input.name} \npath: ${IOUtils.getPath(inputJar)} \ndest:${taskInput.dest} \nstatus:${input.status}")
                FileUtils.deleteQuietly(taskInput.dest)
            } else {
                Logger.info("${IOUtils.getPath(inputJar)} not changed, skip.")
                return
            }
        }

        if (input.status != Status.REMOVED) {
//            def written = false

//            ZipUtils.collectAllClassesFromJar(inputJar).forEach {
//                written = executeClass(it, temporaryDir) || written
//            }
//            if (written) {
//                ZipUtils.zipAppend(inputJar, taskInput.dest, temporaryDir)
//            } else {
//                FileUtils.copyFile(inputJar, taskInput.dest)
//            }

            JarUtil.instrumentJarToJar(inputJar,taskInput.dest,new IZipEntryHandler() {
                @Override
                byte[] inject(ZipFile inZipFile, ZipEntry entry,String className) {
                    byte[] bytes = executeClass(className, temporaryDir)
                    return bytes
                }
            })
        }
    }

    byte[] executeClass(String className, File directory) {
        buildContext.totalCounter.incrementAndGet()
        CtClass inputClass = null
        def transformers = context.transformers.findAll {
            it.classAllowed(className)
        }

        if (transformers.isEmpty()) {
            return null
        }

        inputClass = context.classPool.getOrNull(className)
        if (inputClass == null) {
            return null
        }

        transformers.each {
            try {
                it.performTransform(inputClass, className)
            } catch (NotFoundException e) {
                throw new DroidAssistNotFoundException(
                        "Transform failed for class: ${className}" +
                                " with not found exception: ${e.cause?.message}", e)
            } catch (CannotCompileException e) {
                throw new DroidAssistBadStatementException(
                        "Transform failed for class: ${className} " +
                                "with compile error: ${e.cause?.message}", e)
            } catch (Throwable e) {
                throw new DroidAssistException(
                        "Transform failed for class: ${className} " +
                                "with error: ${e.cause?.message}", e)
            }
        }

        if (inputClass.modified) {
            buildContext.affectedCounter.incrementAndGet()
//            inputClass.writeFile(directory.absolutePath)
            return inputClass.toBytecode()
        }
        return null
    }
}
