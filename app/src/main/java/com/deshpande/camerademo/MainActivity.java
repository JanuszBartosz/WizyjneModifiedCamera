package com.deshpande.camerademo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Button takePictureButton;
    private ImageView imageView;
    private Uri file;
    private String horizontalViewAngle;
    private String focalPlaneXResolution;
    private String focalPlaneYResolution;
    private int focalPlaneResolutionUnit;
    private String pixelXDimension;
    private String pixelYDimension;

    private void getCameraInfo(int camNum) {
        SizeF sensorSize = new SizeF(0, 0);
        Size pixelDimension = new Size(0, 0);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = manager.getCameraIdList();
            if (cameraIds.length > camNum) {
                CameraCharacteristics character = manager.getCameraCharacteristics(cameraIds[camNum]);
                sensorSize = character.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                pixelDimension = character.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
            }
        } catch (CameraAccessException e) {
            Log.e("YourLogString", e.getMessage(), e);
        }
        focalPlaneXResolution = convertDecimalToFraction(sensorSize.getWidth() / 10);
        focalPlaneYResolution = convertDecimalToFraction(sensorSize.getHeight() / 10);
        focalPlaneResolutionUnit = 0x00000003;
        pixelXDimension = String.valueOf(pixelDimension.getWidth());
        pixelYDimension = String.valueOf(pixelDimension.getHeight());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Camera camera = null;
        try {
            camera = Camera.open();
            this.horizontalViewAngle = String.valueOf(Math.toRadians(camera.getParameters().getHorizontalViewAngle()));
            Log.i("Param", horizontalViewAngle);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (camera != null) {
                camera.release();
                camera = null;
            }
        }
        getCameraInfo(0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePictureButton = (Button) findViewById(R.id.button_image);
        imageView = (ImageView) findViewById(R.id.imageview);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            takePictureButton.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                takePictureButton.setEnabled(true);
            }
        }
    }

    public void takePicture(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = Uri.fromFile(getOutputMediaFile());

        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);

        startActivityForResult(intent, 100);
    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("CameraDemo", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                try {
                    Log.i("Angle", horizontalViewAngle);
                    Log.i("FocalPlaneResolutionU", String.valueOf(focalPlaneResolutionUnit));
                    Log.i("FocalPlaneXResolution", focalPlaneXResolution);
                    Log.i("FocalPlaneYResolution", focalPlaneYResolution);
                    Log.i("file", file.getPath());
                    ExifInterface exifInterface = new ExifInterface(file.getPath());
                    exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, horizontalViewAngle);
                    exifInterface.setAttribute(ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT, String.valueOf(focalPlaneResolutionUnit));
                    exifInterface.setAttribute(ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION, focalPlaneXResolution);
                    exifInterface.setAttribute(ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION, focalPlaneYResolution);
                    exifInterface.saveAttributes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageURI(file);
                galleryAddPic();
            }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        mediaScanIntent.setData(file);
        this.sendBroadcast(mediaScanIntent);
    }

    private String convertDecimalToFraction(double x) {
        if (x < 0) {
            return "-" + convertDecimalToFraction(-x);
        }
        double tolerance = 1.0E-6;
        double h1 = 1;
        double h2 = 0;
        double k1 = 0;
        double k2 = 1;
        double b = x;
        do {
            double a = Math.floor(b);
            double aux = h1;
            h1 = a * h1 + h2;
            h2 = aux;
            aux = k1;
            k1 = a * k1 + k2;
            k2 = aux;
            b = 1 / (b - a);
        } while (Math.abs(x - h1 / k1) > x * tolerance);

        return h1 + "/" + k1;
    }
}
