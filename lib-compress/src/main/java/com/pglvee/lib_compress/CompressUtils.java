package com.pglvee.lib_compress;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedOutputStream;
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
    private Bitmap bitmap;
    private String tempInputFilePath;

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
        byte2File(data, file);
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

    public CompressUtils max(long maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    private int getOptionSample(final float scale) {
        if (scale < 2) return 1;
        else if (scale >= 2f && scale < 4f) return 2;
        else if (scale >= 4f && scale < 8f) return 4;
        else if (scale >= 8f && scale < 16) return 8;
        else return 16;
    }

    private float getOptionScale(int oW, int oH, int w, int h) {
        if (w == 0 || h == 0 || oW == 0 || oH == 0) return 1f;
        int ol = Math.max(oW, oH);
        int s = Math.max(w, h);
        if (ol <= s) return 1f;
        else return (float) ol / (float) s;
    }

    private String getTempFile() {
        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsoluteFile(),
                System.currentTimeMillis() + ".jpg");
        return file.getAbsolutePath();
    }

    private void byte2File(byte[] buf, File file) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(buf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public synchronized void image() {
        int w, h;
        float scale;
        if (bitmap == null) {
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
            BitmapFactory.decodeFile(inputFilePath, newOpts);
            newOpts.inJustDecodeBounds = false;
            w = newOpts.outWidth;
            h = newOpts.outHeight;
            scale = getOptionScale(w, h, width, height);
            newOpts.inSampleSize = getOptionSample(scale);
            bitmap = BitmapFactory.decodeFile(inputFilePath, newOpts);
        } else {
            w = bitmap.getWidth();
            h = bitmap.getHeight();
            scale = getOptionScale(w, h, width, height);
        }
        if (w == 0 || h == 0)
            return;
        if (scale > 1)
            bitmap = Bitmap.createScaledBitmap(bitmap, (int) (w / scale), (int) (h / scale), true);
        String tempFile = getTempFile();
        try {
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageCompress(tempFile, outputFilePath, quality);
        new File(tempFile).delete();
        if (!TextUtils.isEmpty(tempInputFilePath)) new File(tempInputFilePath).delete();
    }

    public synchronized void thumbnail() {
        int w, h;
        float scale;
        if (bitmap == null) {
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
            BitmapFactory.decodeFile(inputFilePath, newOpts);
            newOpts.inJustDecodeBounds = false;
            w = newOpts.outWidth;
            h = newOpts.outHeight;
            scale = getOptionScale(w, h, width, height);
            newOpts.inSampleSize = getOptionSample(scale);
            bitmap = BitmapFactory.decodeFile(inputFilePath, newOpts);
        } else {
            w = bitmap.getWidth();
            h = bitmap.getHeight();
            scale = getOptionScale(w, h, width, height);
        }
        if (w == 0 || h == 0)
            return;
        if (scale > 1)
            bitmap = Bitmap.createScaledBitmap(bitmap, (int) (w / scale), (int) (h / scale), true);
        ByteArrayOutputStream outputStream;
        outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] jpegBuff = outputStream.toByteArray();
        thumbnailCompress(jpegBuff, outputFilePath, maxSize);
        try {
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(tempInputFilePath)) new File(tempInputFilePath).delete();
    }

    private static native void imageCompress(String inputFile, String outputFile, int quality);

    private static native void thumbnailCompress(byte[] inputData, String outputFile, long maxSize);

}
