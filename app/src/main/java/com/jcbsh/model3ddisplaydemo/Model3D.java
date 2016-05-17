package com.jcbsh.model3ddisplaydemo;

import android.content.Context;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

/**
 * Created by JCBSH on 20/04/2016.
 */
public class Model3D {

    private static final int BYTE_PER_FLOAT = Float.SIZE / Byte.SIZE;
    private static final int BYTE_PER_INT = Integer.SIZE / Byte.SIZE;
    private static final int MAX_TASK_COUNT = Runtime.getRuntime().availableProcessors() * 2 + 1;

    private final String vertexShaderCode =
            "attribute vec4 Position;" +
                    "attribute vec4 SourceColor;" +
                    "varying vec4 DestinationColor;" +
                    "uniform mat4 uMVPMatrix;" +
                    "void main() {" +
                    "  DestinationColor = SourceColor;" +
                    "  gl_Position = uMVPMatrix * Position;" +
                    "}";

    private final String fragmentShaderCode =
            "varying lowp vec4 DestinationColor;" +
                    "void main() {" +
                    "   gl_FragColor = DestinationColor;" +
                    "}";

    private double mModelCenterX = 0;
    private double mModelCenterY = 0;
    private double mModelCenterZ = 0;

    private int mValidVertexCount = 0;

    private int mProgramHandle;

    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    private static final int MAX_NUM_OF_VERTEX  = 683459;
    private int mProcessedVertexCount = 0;

    private int[] mGPUBuffers;

    private FloatBuffer mVertexBuffer;
    private static final int COORDS_PER_VERTEX = 3;
    private final int VertexStride = COORDS_PER_VERTEX * BYTE_PER_FLOAT;

    private FloatBuffer mColorBuffer;
    private static final int COLORS_PER_VERTEX = 4;
    private final int ColorStride = COLORS_PER_VERTEX * BYTE_PER_FLOAT;

    private IntBuffer mIndicesBuffer;

    private Object mSyncObject = new Object();
    private int mTaskCount = 0;

    public static final int SINGLE_THREAD = 0;
    public static final int MULTI_THREAD = 1;
    public static int sMode = MULTI_THREAD;



//    smallest x pos: -91.870   biggest x pos:  106.99
//    smallest y pos: -64.057   biggest y pos:   70.58
//    smallest z pos:  173.777  biggest z pos:  253.40

    // Use to access and set the view transformation
    public Model3D(Context context) {

        float[] vertexArray = new float[MAX_NUM_OF_VERTEX * COORDS_PER_VERTEX];
        float[] colorArray = new float[MAX_NUM_OF_VERTEX * COLORS_PER_VERTEX];
        int[] indicesArray = new int[MAX_NUM_OF_VERTEX];

        int vertexCount = 0;
        BufferedReader reader = null;
        long start =  System.currentTimeMillis();
        try {

            //////////------------------------//////////


            if (sMode == MULTI_THREAD) {
                // Open and read the file into a StringBuilder
                InputStream in = context.getAssets().open("assassin.ply");
                reader = new BufferedReader(new InputStreamReader(in));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    // Line breaks are omitted and irrelevant
                    String[] strings = line.split(" ");
                    if (strings.length >= 6) break;

                }

                ArrayList<String> strings = new ArrayList<String>();
                do {
                    if (vertexCount >= MAX_NUM_OF_VERTEX) break;
                    strings.add(line);
                    boolean b = (vertexCount + 1 == MAX_NUM_OF_VERTEX);
                    if (b || strings.size() >= 10000) {
                        synchronized (mSyncObject) {
                            while (mTaskCount > MAX_TASK_COUNT) {
                                try {
                                    mSyncObject.wait(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        MyTask task = new MyTask();
                        ++mTaskCount;
                        task.setArrays(vertexArray, colorArray, indicesArray);
                        task.setVertexNum(vertexCount - strings.size() + 1);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, strings);
                        } else {
                            task.execute(strings);
                        }
                        //task.execute(strings);
                        strings = new ArrayList<String>();
                    }
                    vertexCount++;
                } while ((line = reader.readLine()) != null);
                ///////---------------------------/////////
            } else {

                InputStream in = context.getAssets().open("assassin.ply");
                reader = new BufferedReader(new InputStreamReader(in));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    // Line breaks are omitted and irrelevant
                    String[] strings = line.split(" ");

                    if (vertexCount >= MAX_NUM_OF_VERTEX) break;
                    if (strings.length >= 6) {
                        //Log.d("Model3D", line);
                        int i = vertexCount * 3;
                        if (!strings[0].equalsIgnoreCase("nan")) {
                            ++mValidVertexCount;

                            vertexArray[i] = Float.parseFloat(strings[0]);
                            mModelCenterX += vertexArray[i];

                            vertexArray[i + 1] = Float.parseFloat(strings[1]);
                            mModelCenterY += vertexArray[i + 1];

                            vertexArray[i + 2] = Float.parseFloat(strings[2]);
                            mModelCenterZ += vertexArray[i + 2];
                        }

                        i = vertexCount * 4;
                        colorArray[i] = Float.parseFloat(strings[3]) / 255.0f;
                        colorArray[i + 1] = Float.parseFloat(strings[4]) / 255.0f;
                        colorArray[i + 2] = Float.parseFloat(strings[5]) / 255.0f;
                        colorArray[i + 3] = 1.0f;
                        indicesArray[vertexCount] = vertexCount;
                        ++vertexCount;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }


        if (sMode == MULTI_THREAD) {
            synchronized (mSyncObject) {
                while (mProcessedVertexCount != MAX_NUM_OF_VERTEX) {
                    try {
                        mSyncObject.wait(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                mModelCenterX = mModelCenterX / mValidVertexCount;
                mModelCenterY = mModelCenterY / mValidVertexCount;
                mModelCenterZ = mModelCenterZ / mValidVertexCount;
            }
        } else {
            mModelCenterX = mModelCenterX / mValidVertexCount;
            mModelCenterY = mModelCenterY / mValidVertexCount;
            mModelCenterZ = mModelCenterZ / mValidVertexCount;

        }

        long timeTaken =  System.currentTimeMillis() -  start;
        Log.d("Model3D", "time taken : " + timeTaken + "ms");

        mVertexBuffer = ByteBuffer.allocateDirect(vertexArray.length * BYTE_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(vertexArray);
        // set the buffer to read the first coordinate
        mVertexBuffer.position(0);

        mColorBuffer = ByteBuffer.allocateDirect(colorArray.length * BYTE_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColorBuffer.put(colorArray);
        mColorBuffer.position(0);

        mIndicesBuffer = ByteBuffer.allocateDirect(indicesArray.length * BYTE_PER_INT).order(ByteOrder.nativeOrder()).asIntBuffer();
        mIndicesBuffer.put(indicesArray);
        mIndicesBuffer.position(0);

    }

    public void initGl() {
        compileShaders();
        setupVBOs();
    }

    public float getModelCenterX() {
        return (float) mModelCenterX;
    }

    public float getModelCenterY() {
        return (float) mModelCenterY;
    }

    public float getModelCenterZ() {
        return (float) mModelCenterZ;
    }

    private void compileShaders() {

        // 1
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // 2
        mProgramHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgramHandle, vertexShader);
        GLES20.glAttachShader(mProgramHandle, fragmentShader);
        GLES20.glLinkProgram(mProgramHandle);

        // 4

    }

    private void setupVBOs () {

        mGPUBuffers = new int[2];
        GLES20.glGenBuffers(2, mGPUBuffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGPUBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertexBuffer.capacity() * BYTE_PER_FLOAT, mVertexBuffer, GLES20.GL_STATIC_DRAW);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGPUBuffers[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mColorBuffer.capacity() * BYTE_PER_FLOAT, mColorBuffer, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);



    }

    public void draw(float[] mvpMatrix) {


        GLES20.glUseProgram(mProgramHandle);
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "SourceColor");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGPUBuffers[0]);

        GLES20.glEnableVertexAttribArray(mPositionHandle);


        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                VertexStride, 0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGPUBuffers[1]);
        GLES20.glEnableVertexAttribArray(mColorHandle);


        GLES20.glVertexAttribPointer(mColorHandle, COLORS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                ColorStride, 0);

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);


 //       GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 3);


        GLES20.glDrawElements(GLES20.GL_POINTS, mIndicesBuffer.capacity(),
                GLES20.GL_UNSIGNED_INT, mIndicesBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mColorHandle);




    }


    private class MyTask extends AsyncTask<ArrayList<String>,Void,Void> {

        private int vertexNum;
        private float[] vertexArray, colorArray;
        private int[] indicesArray;
        private double modelCenterX = 0;
        private double modelCenterY = 0;
        private double modelCenterZ = 0;
        private int validVertexCount = 0;

        private ArrayList<String> lines;
        @Override
        protected Void doInBackground(ArrayList<String>... params) {
            lines = params[0];
            for (String line:lines) {
                String[] strings = line.split(" ");
                //Log.d("Model3D", line);
                int i = vertexNum * 3;
                boolean flag = true;
                if (!strings[0].equalsIgnoreCase("nan")) {
                    ++validVertexCount;

                    vertexArray[i] = Float.parseFloat(strings[0]);
                    modelCenterX +=  vertexArray[i];

                    vertexArray[i + 1] = Float.parseFloat(strings[1]);
                    modelCenterY +=  vertexArray[i + 1];

                    vertexArray[i + 2] = Float.parseFloat(strings[2]);
                    modelCenterZ +=  vertexArray[i + 2];
                }
                i = vertexNum * 4;
                colorArray[i] = Float.parseFloat(strings[3]) /255.0f;
                colorArray[i + 1] = Float.parseFloat(strings[4]) /255.0f;
                colorArray[i + 2] = Float.parseFloat(strings[5]) /255.0f;
                colorArray[i + 3] = 1.0f;

                indicesArray[vertexNum] = vertexNum;
                ++vertexNum;
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mModelCenterX +=  modelCenterX;
            mModelCenterY +=  modelCenterY;
            mModelCenterZ +=  modelCenterZ;
            mValidVertexCount += validVertexCount;
            mProcessedVertexCount += lines.size();
            --mTaskCount;

        }

        public void setArrays(float[] vertexArray, float[] colorArray, int[] indicesArray) {
            this.vertexArray = vertexArray;
            this.colorArray = colorArray;
            this.indicesArray = indicesArray;
        }

        public void setVertexNum(int vertexNum) {
            this.vertexNum = vertexNum;
        }
    }


    public int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
}
