package com.jcbsh.model3ddisplaydemo;

import android.app.Fragment;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import javax.microedition.khronos.opengles.GL10;


/**
 * Created by JCBSH on 18/04/2016.
 */
public class Model3DFragment extends Fragment{
    private static final int ZOOM_MODE = 1;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    public static Fragment getInstance() {
        Fragment fragment = new Model3DFragment();
        return fragment;
    }


    private Model3D mModel3D;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

    }

    private GLSurfaceView mGLSurfaceView;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mGLSurfaceView = new MyGLSurfaceView(getActivity(), mBackgroundHandler);
        return mGLSurfaceView;
    }


    public class MyGLSurfaceView extends GLSurfaceView {

        private final MyGLRenderer mRenderer;
        private final float ROTATE_SCALE_FACTOR = 180.0f / 520;
        private final float ZOOM_SCALE_FACTOR = 600;
        private int mFirstPointIndex = 0;
        private float mPreFirstPointX;
        private float mPreFirstPointY;
        private boolean mPreFirstPointInitializedFlag = false;

        private int mSecondPointIndex = 1;
        private float mPreSecondPointX;
        private float mPreSecondPointY;
        private long doubleTapTimeStart;

        public MyGLSurfaceView(Context context, Handler handler){
            super(context);

            // Create an OpenGL ES 2.0 context
            setEGLContextClientVersion(2);

            mRenderer = new MyGLRenderer(context, handler);

            // Set the Renderer for drawing on the GLSurfaceView
            setRenderer(mRenderer);
            // Render the view only when there is a change in the drawing data
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }


        @Override
        public boolean onTouchEvent(MotionEvent e) {
            // MotionEvent reports input details from the touch screen
            // and other input controls. In this case, you are only
            // interested in events where the touch position changed.


            switch (MotionEventCompat.getActionMasked(e)) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    Log.d("onTouchEvent", "ACTION_POINTER_DOWN " + MotionEventCompat.getActionIndex(e));
                    mPreSecondPointX = MotionEventCompat.getX(e, mSecondPointIndex);
                    mPreSecondPointY = MotionEventCompat.getY(e, mSecondPointIndex);
                    {
                        //cancel the rotations caused by when one point contact from two finger been too
                        //close turn into two point contact, which causes coordinate of the First point to suddenly
                        //shift, thus causing rotation. If the rotation is not cancelled it will cause
                        //a jerky effect.
                        mRenderer.setYAngle((0));
                        mRenderer.setXAngle((0));
                        mRenderer.setZAngle((0));
                        mRenderer.zoom(0);
                        mRenderer.translate(0, 0);
                        requestRender();
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    Log.d("onTouchEvent", "ACTION_POINTER_UP " + MotionEventCompat.getActionIndex(e));
                    mPreFirstPointInitializedFlag = false;
                    break;
                case MotionEvent.ACTION_DOWN:
                    mPreFirstPointInitializedFlag = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d("onTouchEvent", "ACTION_MOVE");

                    float x = MotionEventCompat.getX(e, mFirstPointIndex);
                    float y = MotionEventCompat.getY(e, mFirstPointIndex);
                    float dx = x - mPreFirstPointX;
                    float dy = y - mPreFirstPointY;


                    if (mPreFirstPointInitializedFlag) {
                        if (e.getPointerCount() > 1) {
                            //Log.d("onTouchEvent", "ACTION_MOVE 2");
                            float sx = MotionEventCompat.getX(e, mSecondPointIndex);
                            float sy = MotionEventCompat.getY(e, mSecondPointIndex);
                            float dsx = sx - mPreSecondPointX;
                            float dsy = sy - mPreSecondPointY;

                            //int mode = getMode(dsx, dx, dsy, dy);
                            float oldXDis = Math.abs(mPreSecondPointX - mPreFirstPointX);
                            float oldYDis = Math.abs(mPreSecondPointY - mPreFirstPointY);
                            float newXDis = Math.abs(sx - x);
                            float newYDis = Math.abs(sy - y);
                            float deltaXDis = newXDis - oldXDis;
                            float deltaYDis = newYDis - oldYDis;
                            float zRotation = (float) Math.pow((Math.pow((dsx - dx), 2) + Math.pow((dsy - dy), 2)), 0.5);

                            float preDistance = getDistance(mPreSecondPointX, mPreFirstPointX, mPreSecondPointY, mPreFirstPointY);
                            float distance = getDistance(sx, x, sy, y);



                            float firstPointDisplacement = getDistance(mPreFirstPointX, x, mPreFirstPointY, y);
                            float secondPointDisplacement = getDistance(mPreSecondPointX, sx, mPreSecondPointY, sy);
                            float absDeltaDis = Math.abs(preDistance - distance);

                            //checks if the two point are more moving in a circular than just simpling
                            //moving apart (which signals zoom instead),
                            //the check is done in by
                            //1. get the distance between the previous position of the two pointers (tail distance)
                            //2. get the distance between the current position of the two pointers (head distance)
                            //3. get the combined disposition of the two pointer
                            //4. if less than 50% if that combined disposition contribute towards the difference
                            //   between tail dis and head dis, then that show the gesture is more in a circular
                            //   motion.
                            //   if more than 50%, then that shows gesture is more of a zoom
                            boolean isEllipseFlag = absDeltaDis < 0.5 * (firstPointDisplacement + secondPointDisplacement);
                            if (deltaXDis < 0 && deltaYDis > 0 && isEllipseFlag) {
                                //top right quadrant && bottom left quadrant
                                if ((x > sx && y < sy) || sx > x && sy < y) {
                                    rotateAntiClockWise(zRotation);
                                }

                                //top left quadrant && bottom right quadrant
                                if ((x < sx && y < sy) || sx < x && sy < y) {
                                    rotateClockWise(zRotation);
                                }

                            } else if (deltaXDis > 0 && deltaYDis < 0 && isEllipseFlag) {
//                                Log.d("onTouchEvent", "deltaXDis > 0 && deltaYDis < 0");

                                //top right quadrant && bottom left quadrant
                                if ((x > sx && y < sy) || sx > x && sy < y) {
                                    rotateClockWise(zRotation);
                                }

                                //top left quadrant && bottom right quadrant
                                if ((x < sx && y < sy) || sx < x && sy < y) {
                                    rotateAntiClockWise(zRotation);
                                }

                            } else {
                                float deltaDis = distance - preDistance;
                                mRenderer.zoom(deltaDis/ZOOM_SCALE_FACTOR);

                            }


                            float xShift = (dsx + dx) / 2;
                            float yShift = (dsy + dy) / 2;

                            mRenderer.translate(xShift, yShift);


                            mPreSecondPointX = sx;
                            mPreSecondPointY = sy;
                        } else {

//                            Log.d("onTouchEvent", "ACTION_MOVE 1");
//                            Log.d("onTouchEvent", "dx " + dx + " dy " + dy);
//                            Log.d("onTouchEvent", "x " + x + " y " + y);
//                            Log.d("onTouchEvent", "mPreviousX " + mPreviousX + " mPreviousY " + mPreviousY);

                            mRenderer.setYAngle((dx * ROTATE_SCALE_FACTOR));
                            mRenderer.setXAngle((dy * ROTATE_SCALE_FACTOR));

                        }

                    }
                    mPreFirstPointInitializedFlag = true;
                    mPreFirstPointX = x;
                    mPreFirstPointY = y;


                    requestRender();
                    break;

                case MotionEvent.ACTION_UP:
                    Log.d("onTouchEvent", "ACTION_UP e.getDownTime() " + e.getDownTime() + " " + e.getEventTime()
                    + " " + (e.getEventTime() - e.getDownTime()));
                    //check if the touch is a quick tap
                    if (e.getEventTime() - e.getDownTime() < 100) {
                        //Log.d("onTouchEvent", "ACTION_UP time gap " + (e.getDownTime() - doubleTapTimeStart));
                        long doubleTapTimeGap = e.getDownTime() - doubleTapTimeStart;
                        if (doubleTapTimeGap < 100) {
                            mRenderer.reset();
                            requestRender();
                        } else {
                            doubleTapTimeStart = e.getEventTime();
                        }

                    }
                    break;
                default:
                    Log.d("onTouchEvent", "default " + MotionEventCompat.getActionMasked(e));
                    break;

            }

            return true;
        }

        private void rotateAntiClockWise(float zRotation) {
            mRenderer.setZAngle(zRotation * ROTATE_SCALE_FACTOR);
        }


        private void rotateClockWise(float zRotation) {
            mRenderer.setZAngle(-zRotation * ROTATE_SCALE_FACTOR);
        }

        private int getMode(float dsx, float dx, float dsy, float dy) {
            float x = dsx*dx;
            float y = dsy*dy;
            if (x < 0 && y <0) return ZOOM_MODE;
            return 0;
        }
    }

    private float getDistance(float sX, float x, float sY, float y) {

        return (float) Math.pow(Math.pow((sX - x), 2) + Math.pow((sY - y), 2), 0.5);
    }


    @Override
    public void onResume() {
        super.onResume();
        openBackgroundThread();
    }


    @Override
    public void onPause() {
        super.onPause();
        closeBackgroundThread();


    }

    protected void openBackgroundThread() {
        mBackgroundThread =  new HandlerThread("background thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void closeBackgroundThread() {

        mBackgroundThread.quit();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }



    public class MyGLRenderer implements GLSurfaceView.Renderer {

        private final Handler mHandler;
        private Context mContext;

        private float mZoom = 1;
        private float mXShift = 0;
        private float mYShift = 0;
        private boolean mViewMatrixInitializationFlag = true;


        public MyGLRenderer(Context context, Handler handler) {
            mContext = context;
            mHandler = handler;
        }


        public void onDrawFrame(GL10 gl) {
            float[] scratch = new float[16];

            float[] TSR = new float[16];

            // Redraw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
            // Set the camera position (View matrix)
            //Matrix.setLookAtM(mViewMatrix, 0, 0, 3, 0.f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            if (mViewMatrixInitializationFlag) {
                Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -mModel3D.getModelCenterZ()*2, 0, 0, 0f, 0f, 1.0f, 0.0f);
                {
                    Matrix.translateM(mViewMatrix, 0, 0, 0, mModel3D.getModelCenterZ());
                    Matrix.rotateM(mViewMatrix, 0, -90
                            , 0
                            , 0
                            , 1
                    );


                    Matrix.translateM(mViewMatrix, 0, 0, 0, -mModel3D.getModelCenterZ());
                }
                mViewMatrixInitializationFlag = false;
            }

            Log.d("onDrawFrame", "mXAngle " + mXAngle + " mYAngle " + mYAngle);


            Matrix.translateM(mViewMatrix, 0, 0, 0, mModel3D.getModelCenterZ());

            {
                float[] TranslationVector = new float[4];
                TranslationVector[0] = mXShift;
                TranslationVector[1] = -mYShift;
                TranslationVector[2] = 0;
                TranslationVector[3] = 0;

                float[] viewMatrixTransPose = {
                        mViewMatrix[0], mViewMatrix[4], mViewMatrix[8], 0,
                        mViewMatrix[1], mViewMatrix[5], mViewMatrix[9], 0,
                        mViewMatrix[2], mViewMatrix[6], mViewMatrix[10], 0,
                        mViewMatrix[3], mViewMatrix[7], mViewMatrix[11], mViewMatrix[15],
                };


                Matrix.multiplyMV(TranslationVector, 0, viewMatrixTransPose, 0, TranslationVector, 0);

                Matrix.translateM(mViewMatrix, 0, TranslationVector[0], TranslationVector[1], TranslationVector[2]);
                mXShift = 0;
                mYShift = 0;
            }



            Matrix.scaleM(mViewMatrix, 0, mViewMatrix, 0, mZoom, mZoom, mZoom);
            mZoom = 1;

            {
                float[] M = new float[16];
                Matrix.setIdentityM(M, 0);
                Matrix.rotateM(M, 0, mXAngle
                        , 1
                        , 0
                        , 0
                );

                mXAngle = 0;

                M[12] = mViewMatrix[12];
                M[13] = mViewMatrix[13];
                M[14] = mViewMatrix[14];
                mViewMatrix[12] = 0;
                mViewMatrix[13] = 0;
                mViewMatrix[14] = 0;
                Matrix.multiplyMM(mViewMatrix, 0, M, 0, mViewMatrix, 0);
            }


            {
                float[] M = new float[16];
                Matrix.setIdentityM(M, 0);
                Matrix.rotateM(M, 0, mYAngle
                        , 0
                        , 1
                        , 0
                );

                mYAngle = 0;

                M[12] = mViewMatrix[12];
                M[13] = mViewMatrix[13];
                M[14] = mViewMatrix[14];
                mViewMatrix[12] = 0;
                mViewMatrix[13] = 0;
                mViewMatrix[14] = 0;
                Matrix.multiplyMM(mViewMatrix, 0, M, 0, mViewMatrix, 0);
            }


            {
                float[] M = new float[16];
                Matrix.setIdentityM(M, 0);
                Matrix.rotateM(M, 0, mZAngle
                        , 0
                        , 0
                        , 1
                );

                mZAngle = 0;

                M[12] = mViewMatrix[12];
                M[13] = mViewMatrix[13];
                M[14] = mViewMatrix[14];
                mViewMatrix[12] = 0;
                mViewMatrix[13] = 0;
                mViewMatrix[14] = 0;
                Matrix.multiplyMM(mViewMatrix, 0, M, 0, mViewMatrix, 0);
            }


            Matrix.translateM(mViewMatrix, 0, 0, 0, -mModel3D.getModelCenterZ());
            // Calculate the projection and view transformation
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

            Matrix.setIdentityM(TSR, 0);

            // Create a rotation transformation for the triangle


            // Combine the rotation matrix with the projection and camera view
            // Note that the mMVPMatrix factor *must be first* in order
            // for the matrix multiplication product to be correct.
            Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, TSR, 0);

            // Draw triangle
            //mTriangle.draw(scratch);
            mModel3D.draw(scratch);
        }

        private void print(float[] viewMatrix) {
            Log.d("print", String.format("\n " +
                    "%f %f %f %f \n" +
                    "%f %f %f %f \n" +
                    "%f %f %f %f \n" +
                    "%f %f %f %f",
                    viewMatrix[0], viewMatrix[4], viewMatrix[8], viewMatrix[12],
                    viewMatrix[1], viewMatrix[5], viewMatrix[9], viewMatrix[13],
                    viewMatrix[2], viewMatrix[6], viewMatrix[10], viewMatrix[14],
                    viewMatrix[3], viewMatrix[7], viewMatrix[11], viewMatrix[15]));
        }


        @Override
        public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
            GLES20.glClearColor(0.8f, 0.8f, 0.8f, 1.0f);
            //mTriangle = new Triangle();
            if (mModel3D == null) {
                Log.d("onCreateView", "mModel3D == null");
                mModel3D = new Model3D(getActivity());
            }


            mModel3D.initGl();



        }

        private final float[] mMVPMatrix = new float[16];
        private final float[] mProjectionMatrix = new float[16];
        private float[] mViewMatrix = new float[16];

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            Log.d("MyGLRenderer", "onSurfaceChanged");
            GLES20.glViewport(0, 0, width, height);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            //GLES20.glEnable(GLES20.GL_CULL_FACE);


            float h = 4.0f * height / width;
            Matrix.frustumM(mProjectionMatrix, 0, -2.0f/10.0f, 2.0f/10.0f, -h/2.0f/10.0f, h/2.0f/10.0f, 1, 1000000);


//            float ratio = (float) width / height;
//
//            // this projection matrix is applied to object coordinates
//            // in the onDrawFrame() method
//            int factor = 1;
//            Matrix.frustumM(mProjectionMatrix, 0, -ratio*factor, ratio*factor, -1*factor, 1*factor, 1, 1000000);
//            Matrix.orthoM(mProjectionMatrix, 0, -ratio*factor, ratio*factor, -1*factor, 1*factor, 500, 1500);
        }

        public volatile float mXAngle;
        public volatile float mYAngle;
        public volatile float mZAngle;

        public float getXAngle() {
            return mXAngle;
        }
        public void setXAngle(float angle) {
            mXAngle = angle;
        }

        public float getYAngle() {
            return mYAngle;
        }
        public void setYAngle(float angle) {
            mYAngle = angle;
        }

        public float getZAngle() {
            return mZAngle;
        }
        public void setZAngle(float angle) {
            mZAngle = angle;
        }

        public void zoom(float v) {
            mZoom = v + 1;
        }

        public void translate(float xShift, float yShift) {
            mXShift = xShift;
            mYShift = yShift;
        }

        public void reset() {
            mViewMatrixInitializationFlag = true;
        }
    }
}
