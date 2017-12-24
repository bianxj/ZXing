package com.bianxj.zxing;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.bianxj.zxing.camera.CameraManager;
import com.bianxj.zxing.view.ViewfinderView;
import com.google.zxing.Result;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * 作者:边贤君
 * 描述:
 * 创建日期:2017/12/23 09:26
 */
public class ScanActivity extends Activity {

    private final static int REQUEST_PERMISSION = 0x01;
    private String[] permissions = new String[]{Manifest.permission.CAMERA};

    private SurfaceView sv_scan;
    private ViewfinderView vfv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        findView();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ( REQUEST_PERMISSION == requestCode ){
            if (PackageManager.PERMISSION_GRANTED != grantResults[0] ){
                if ( ActivityCompat.shouldShowRequestPermissionRationale(this,permissions[0]) ){
                    checkPermission();
                } else {
                    Toast.makeText(this,"请打开照相机权限",Toast.LENGTH_LONG).show();
                }
            } else {
                startScan();
            }
        }
    }

    private void checkPermission(){
        if ( ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ){
            startScan();
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
        }
    }

    private void findView(){
        sv_scan = findViewById(R.id.sv_scan);
        vfv = findViewById(R.id.vfv);
    }

    private SurfaceHolder holder;
    private void initView(){
        holder = sv_scan.getHolder();
        holder.addCallback(callback);
        cameraManager = new CameraManager(getApplicationContext());
    }

    private CameraManager cameraManager;
    private boolean hasSurface;
    private boolean canScan;

    private void startScan(){
        canScan = true;
        if ( !hasSurface ){
            return;
        }
        try {
            if ( !cameraManager.isOpen() ) {
                cameraManager.openDriver(sv_scan.getHolder());
            }
            cameraManager.startPreview();
            cameraManager.requestPreviewFrame(previewCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopScan(){
        canScan = false;
        if ( cameraManager != null ){
            cameraManager.stopPreview();
            cameraManager.closeDriver();
        }
    }

    private CameraManager.PreviewCallback previewCallback = new CameraManager.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera, int width, int height) {
            byte[] rotatedData = new byte[data.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++)
                    rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
            int tmp = width;
            width = height;
            height = tmp;
            Rect rect = cameraManager.getDecodeArea(vfv.getScanRect());

            Result result = cameraManager.decodeQR(rect,rotatedData,width,height);
            if ( result != null ){
                Toast.makeText(ScanActivity.this,result.getText(),Toast.LENGTH_SHORT).show();
//                saveBitmapToSdcard(data);
            } else {
                cameraManager.requestPreviewFrame(previewCallback);
            }
        }
    };

    private void saveBitmapToSdcard(byte[] data){
        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
        try {
            File file = new File(Environment.getExternalStorageDirectory().getPath()+"/test.jpg");
            if ( file.exists() ){
                file.delete();
            }
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            hasSurface = true;
            if ( canScan ){
                startScan();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            hasSurface = false;
        }
    };

}
