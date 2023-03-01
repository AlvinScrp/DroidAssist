package com.didichuxing.tools.test;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.jacoco.agent.rt.RT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class JacocoHelpe {

    /**
     * 生成executionData
     */
    public static void generateCoverageFile(Context context) {
        String dir = context.getExternalCacheDir().getAbsolutePath();

        OutputStream out = null;
        try {
            File file = new File(dir + "/aa.ec");
            Log.d("alvin", file.getAbsolutePath());
            if (!file.exists()) {
                file.createNewFile();

            }
            out = new FileOutputStream(file, false);

//            out?.write(
//                agent.javaClass.getMethod(
//                    "getExecutionData",
//                    Boolean::class.javaPrimitiveType
//                ).invoke(agent, false) as ByteArray
//            )
            out.write(RT.getAgent().getExecutionData(false));
            Log.i("alvin", "generateCoverageFile write success");
            Toast.makeText(context, "success!!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.i("alvin", "generateCoverageFile Exception:" + e);
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
