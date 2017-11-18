/*
 * Copyright 2016 nekocode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jongho.silentCamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.FloatMath;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.TextureView;

import java.io.IOException;


public class CameraRenderer implements TextureView.SurfaceTextureListener {
    private static final String TAG = "CameraRenderer";


    private Context context;
    private SurfaceTexture surfaceTexture;
    private Camera camera;
    private boolean previewing = false;
    private int currentCameraId;


    public CameraRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable");

        if (previewing == true) {
            camera.stopPreview();
            camera.release();
            camera = null;
        } else {

            currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            camera = Camera.open(currentCameraId);

            surfaceTexture = surface;

            try {
                camera.setPreviewTexture(surfaceTexture);
                camera.setDisplayOrientation(90);
                camera.startPreview();
                previewing = true;

            } catch (Exception e) {
                Log.i(TAG, "onSurfaceTextureAvailable.error");
            }
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //Log.i(TAG, "onSurfaceTextureUpdated"); -- callback everytime.
        camera.autoFocus(myAutofocus);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.i(TAG, "onSurfaceTextureDestroyed");

        surfaceTexture = surface;
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        surface.release();

        return true;
    }

    Camera.AutoFocusCallback myAutofocus = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            Log.i(TAG, "onAutoFocus");
        }
    };

    public void cameraChange() {
        if (previewing) {
            camera.stopPreview();
        }
        //NB: if you don't release the current camera before switching, you app will crash
        camera.release();

        //swap the id of the camera to be used
        if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            camera = Camera.open(currentCameraId);
            camera.setDisplayOrientation(90);

        } else {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            camera = Camera.open(currentCameraId);
            camera.setDisplayOrientation(90);
        }

        try {
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public float getFingerSpacing(MotionEvent event) {
        Log.i(TAG, "getFingerSpacing");
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x*x + y*y);
    }

    public float handleZoom(MotionEvent event, float origDist) {
        Camera.Parameters params = camera.getParameters();
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > origDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < origDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }

        params.setZoom(zoom);
        origDist = newDist;
        camera.setParameters(params);
        return origDist;
    }

    public void cancelAutoFocusCamera() {
        camera.cancelAutoFocus();
    }
}
