package com.pglvee.compressutils;

import android.Manifest;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.pglvee.lib_compress.CompressUtils;

import java.io.File;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "compress";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.compress).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivityPermissionsDispatcher.compressWithPermissionCheck(MainActivity.this);
            }
        });
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void compress(){
        File oldFile = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsoluteFile(),
                "IMG_1567413460211.jpg"
        );
        File newFile = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsoluteFile(),
                "compress_BB5.jpg"
        );
        long start = System.currentTimeMillis();
//        CompressUtils.newInstance().size(1080,1920).src(oldFile).dst(newFile).quality(90).maxScale(6f).image();
        CompressUtils.newInstance().size(640).src(oldFile).dst(newFile).max(20*1024).crop(6f).thumbnail();
        Log.e(TAG, "compress time : "+(System.currentTimeMillis()-start)+" ms, image file size : "+newFile.length());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

}
