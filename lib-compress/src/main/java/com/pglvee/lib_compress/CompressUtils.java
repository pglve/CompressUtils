package com.pglvee.lib_compress;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CompressUtils {

    private static final String TAG = "compress-jni";
    private String inputFilePath;
    private String outputFilePath;
    private int width;
    private int height;
    private int quality;
    private long maxSize;
    private int angle;
    private Bitmap bitmap;
    private String tempInputFilePath;
    private String tempOutFilePath;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("light");
    }

    public static CompressUtils newInstance() {
        return new CompressUtils();
    }

    public CompressUtils size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public CompressUtils size(int size) {
        this.width = size;
        this.height = size;
        return this;
    }

    public CompressUtils quality(int quality) {
        this.quality = quality;
        return this;
    }

    public CompressUtils src(File inputFile) {
        this.inputFilePath = inputFile.getAbsolutePath();
        return this;
    }

    public CompressUtils src(String inputFilePath) {
        this.inputFilePath = inputFilePath;
        return this;
    }

    public CompressUtils src(Bitmap bitmap) {
        this.bitmap = bitmap;
        return this;
    }

    public CompressUtils src(byte[] data) {
        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsoluteFile(),
                System.currentTimeMillis() + ".jpg");
        ImageUtils.byte2File(data, file);
        this.tempInputFilePath = file.getPath();
        this.inputFilePath = file.getPath();
        return this;
    }

    public CompressUtils dst(File outputFile) {
        this.outputFilePath = outputFile.getAbsolutePath();
        return this;
    }

    public CompressUtils dst(String outputFilePath) {
        this.outputFilePath = outputFilePath;
        return this;
    }

    public byte[] dst() {
        byte[] data = new byte[0];
        if (!TextUtils.isEmpty(tempOutFilePath)) {
            data = ImageUtils.file2byte(tempOutFilePath);
            new File(tempOutFilePath).delete();
        }
        return data;
    }

    public CompressUtils rotate() {
        if (!TextUtils.isEmpty(this.inputFilePath)) {
            if (ImageUtils.getMimeType(this.inputFilePath).equals("image/jpeg")) {
                angle = ImageUtils.readPictureDegree(this.inputFilePath);
            }
        }
        return this;
    }

    public CompressUtils rotate(int angle) {
        this.angle = angle;
        return this;
    }

    public CompressUtils max(long maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public synchronized CompressUtils image() {
        int w, h;
        float scale;
        boolean recycle = false;
        if (bitmap == null) {
            recycle = true;
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
            BitmapFactory.decodeFile(inputFilePath, newOpts);
            newOpts.inJustDecodeBounds = false;
            w = newOpts.outWidth;
            h = newOpts.outHeight;
            scale = ImageUtils.getOptionScale(w, h, width, height);
            newOpts.inSampleSize = ImageUtils.getOptionSample(scale);
            bitmap = BitmapFactory.decodeFile(inputFilePath, newOpts);
        } else {
            w = bitmap.getWidth();
            h = bitmap.getHeight();
            scale = ImageUtils.getOptionScale(w, h, width, height);
        }
        if (w == 0 || h == 0)
            return this;
        if (scale > 1)
            bitmap = Bitmap.createScaledBitmap(bitmap, (int) (w / scale), (int) (h / scale), true);
        if (angle > 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate(angle, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        String tempFile = ImageUtils.getTempFile();
        try {
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(outputFilePath))
            imageCompress(tempFile, outputFilePath, quality);
        else {
            tempOutFilePath = ImageUtils.getTempFile();
            imageCompress(tempFile, tempOutFilePath, quality);
        }
        new File(tempFile).delete();
        if (!TextUtils.isEmpty(tempInputFilePath)) new File(tempInputFilePath).delete();
        try {
            if (recycle && bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public synchronized CompressUtils thumbnail() {
        int w, h;
        float scale;
        boolean recycle = false;
        if (bitmap == null) {
            recycle = true;
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
            BitmapFactory.decodeFile(inputFilePath, newOpts);
            newOpts.inJustDecodeBounds = false;
            w = newOpts.outWidth;
            h = newOpts.outHeight;
            scale = ImageUtils.getOptionScale(w, h, width, height);
            newOpts.inSampleSize = ImageUtils.getOptionSample(scale);
            bitmap = BitmapFactory.decodeFile(inputFilePath, newOpts);
        } else {
            w = bitmap.getWidth();
            h = bitmap.getHeight();
            scale = ImageUtils.getOptionScale(w, h, width, height);
        }
        if (w == 0 || h == 0)
            return this;
        if (scale > 1)
            bitmap = Bitmap.createScaledBitmap(bitmap, (int) (w / scale), (int) (h / scale), true);
        if (angle > 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate(angle, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        ByteArrayOutputStream outputStream;
        outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] jpegBuff = outputStream.toByteArray();
        if (!TextUtils.isEmpty(outputFilePath))
            thumbnailCompress(jpegBuff, outputFilePath, maxSize);
        else {
            tempOutFilePath = ImageUtils.getTempFile();
            thumbnailCompress(jpegBuff, tempOutFilePath, maxSize);
        }
        try {
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(tempInputFilePath)) new File(tempInputFilePath).delete();
        try {
            if (recycle && bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private static native void imageCompress(String inputFile, String outputFile, int quality);

    private static native void thumbnailCompress(byte[] inputData, String outputFile, long maxSize);

}
