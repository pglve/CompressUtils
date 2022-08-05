package com.pglvee.compressutils;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.pglvee.lib_compress.CompressUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "compress";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ActivityResultLauncher<String> register = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                File file = uri2File(result);
                if (file.exists())
                    compress(file);

            }
        });
        findViewById(R.id.compress).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register.launch("image/*");
            }
        });
    }

    private File uri2File(Uri uri) {
        File targetFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "original.jpg");
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(targetFile)) {
            int read;
            byte[] buffer = new byte[64 * 1024];
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return targetFile;
    }

    private void compress(File targetFile) {
        long start = System.currentTimeMillis();
        File destinationFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "destination.jpg");
        CompressUtils.newInstance().size(1920).src(targetFile).dst(destinationFile).max(200 * 1024).crop(6f).thumbnail();
        Log.e(TAG, "compress time : " + (System.currentTimeMillis() - start) + " ms, image file size : " + destinationFile.length());
    }

}
