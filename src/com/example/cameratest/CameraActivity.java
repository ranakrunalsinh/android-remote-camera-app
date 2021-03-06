package com.example.cameratest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.example.cameratest.R.id;

@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
public class CameraActivity extends Activity {

    private static class SendPicture extends AsyncTask<byte[], Integer, Long> {

        @Override
        protected Long doInBackground(byte[]... image) {
            try {
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect(new InetSocketAddress("192.168.0.104", 5555), 500);

                OutputStream output = socket.getOutputStream();
                // output.write("hello world".getBytes("UTF-8"));
                output.write(image[0]);
                output.close();
                socket.close();
            } catch (IOException e) {
                Log.d(TAG, "Failed to send image data over network");
            }
            return null;
        }
    }

    /* package */static final String TAG = "Auto-picture-taker";

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CardReaderApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Faild to create storage directory");
                return null;
            }
        }

        // generate file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd__HHmmss", Locale.US).format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    }

    private Camera mCamera;
    private CameraPreview mPreview;

    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions.");
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                sendData(data);
            } catch (IOException e) {
                Log.d(TAG, "Unable to write file.", e);
            } finally {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Interrupted during sleep", e);
                } // show the picture for a few seconds
                mCamera.startPreview();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setupCamera();

        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // add a listener to the capture button
        Button captureButton = (Button) findViewById(id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.autoFocus(new AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        // get an image from the camera
                        camera.takePicture(null, null, mPicture);
                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onRestart() {
        super.onResume();
        performReset();
    }

    @Override
    protected void onResume() {
        super.onResume();
        performReset();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void performReset() {
        if (mCamera == null) {
            setupCamera();
        }
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.removeAllViews();
        preview.addView(mPreview);
    }

    private void sendData(byte[] picture) {
        new SendPicture().execute(picture);
    }

    private void setupCamera() {
        mCamera = Camera.open();
        Camera.Parameters params = mCamera.getParameters();
        if (!params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            throw new RuntimeException("Camera does not have auto-focus!");
        } else {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        mCamera.setParameters(params);
    }

}