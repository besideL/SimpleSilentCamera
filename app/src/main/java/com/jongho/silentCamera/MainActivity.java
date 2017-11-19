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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jongho.silentCamera.CameraRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.jongho.silentCamera.R;
import com.mommoo.permission.MommooPermission;
import com.mommoo.permission.listener.OnPermissionDenied;
import com.mommoo.permission.listener.OnPermissionGranted;
import com.mommoo.permission.repository.DenyInfo;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private CameraRenderer renderer;
    private TextureView textureView;
    private Button captureButton;
    private Button toggleButton;
    private Button folderButton;
    private SeekBar zoomSeekBar;
    private float mDist;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.main);


        textureView = (TextureView) findViewById(R.id.textureView);
        captureButton = (Button) findViewById(R.id.capture_button);
        toggleButton = (Button) findViewById(R.id.toggle_button);
        folderButton = (Button) findViewById(R.id.folder_button);
        zoomSeekBar = (SeekBar) findViewById(R.id.zoom_seekBar);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: captureButton");
                Bitmap bitmap = textureView.getBitmap();
                new SaveImageTask().execute(bitmap);
            }
        });

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: toggleButton");
                renderer.cameraChange();
            }
        });

        folderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: folderButton");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "content://media/internal/images/media/"));
                startActivity(intent);
            }
        });

        zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.i(TAG, "onProgressChanged" + progress);
                seekBar.setVisibility(View.VISIBLE);
                renderer.seekBarZoom(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            setupCameraPreviewView();

        } else {

            new MommooPermission.Builder(this)
                    .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                    .setOnPermissionDenied(new OnPermissionDenied() {
                        @Override
                        public void onDenied(List<DenyInfo> deniedPermissionList) {
                            for (DenyInfo denyInfo : deniedPermissionList) {
                                System.out.println("isDenied : " + denyInfo.getPermission() + " , " +
                                        "userNeverSeeChecked : " + denyInfo.isUserNeverAskAgainChecked());
                            }
                        }
                    })
                    .setPreNoticeDialogData("Pre-Notice", "Please accept all permission to using this app")
                    .setOfferGrantPermissionData("Move To App Setup", "1. Touch the 'SETUP'\n" +
                            "2. Touch the 'Permission' tab\n" +
                            "3. Grant all permissions by dragging toggle button")
                    .setOnPermissionGranted(new OnPermissionGranted() {
                        @Override
                        public void onGranted(List<String> permissionList) {
                            Log.i(TAG, "onGranted");
                            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                setupCameraPreviewView();
                            }

                        }
                    })
                    .build()
                    .checkPermissions();

        }
    }

    private void setupCameraPreviewView() {
        Log.i(TAG, "setupCameraPreviewView");

        renderer = new CameraRenderer(this);
        assert textureView != null;
        textureView.setSurfaceTextureListener(renderer);

        textureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                if (event.getPointerCount() > 1) {
                    // handle multi-touch events
                    if (action == MotionEvent.ACTION_POINTER_DOWN) {
                        Log.i(TAG, "onTouch: ACTION_POINTER_DOWN");
                        mDist = renderer.getFingerSpacing(event);
                    } else if (action == MotionEvent.ACTION_MOVE) {
                        Log.i(TAG, "onTouch: ACTION_MOVE");
                        renderer.cancelAutoFocusCamera();
                        mDist = renderer.touchZoom(event, mDist);
                        zoomSeekBar.setProgress(renderer.getZoom());

                        zoomSeekBar.setVisibility(View.VISIBLE);

                    }
                } else {
                    // handle single touch events
                    if (action == MotionEvent.ACTION_UP) {
                        Log.i(TAG, "onTouch: ACTION_UP");

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                zoomSeekBar.setVisibility(View.INVISIBLE);
                            }
                        }, 2000);


                    }
                }

                return true;
            }
        });

        textureView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Log.i(TAG, "onLayoutChange");
                renderer.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight());
            }
        });
    }


    private void refreshGallery(File file) {
        Log.i(TAG, "refreshGallery");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    private class SaveImageTask extends AsyncTask<Bitmap, Boolean, Boolean> {
        @Override
        protected Boolean doInBackground(Bitmap... bitmaps) {
            Log.i(TAG, "SaveImageTask.doInBackground");
            FileOutputStream outputStream = null;

            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File(sdCard.getAbsolutePath() + "/JonghoCam");
                dir.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File imageFile = new File(dir, fileName);

                outputStream = new FileOutputStream(imageFile);

                bitmaps[0].compress(Bitmap.CompressFormat.PNG, 0, outputStream);
                outputStream.flush();
                outputStream.close();
                refreshGallery(imageFile);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Log.i(TAG, "SaveImageTask.onPostExecute");
            if ( aBoolean == false ) {
                Toast.makeText(MainActivity.this , "save failed!", Toast.LENGTH_SHORT ).show();
            } else {
                Toast.makeText(MainActivity.this , "save success!", Toast.LENGTH_SHORT ).show();
            }
        }
    }
}
