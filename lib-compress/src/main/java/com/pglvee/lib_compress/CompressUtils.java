package com.pglvee.lib_compress;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

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
    private int outWidth;
    private int outHeight;
    private int inWidth;
    private int inHeight;
    private float maxScale;

    // Used to load the 'light' library on application startup.
    static {
        System.loadLibrary("light");
    }

    public static CompressUtils newInstance() {
        return new CompressUtils();
    }

    /**
     * 设置输出图片的宽高
     */
    public CompressUtils size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * 设置输出图片的宽高
     */
    public CompressUtils size(int size) {
        this.width = size;
        this.height = size;
        return this;
    }

    /**
     * 设置输出图片的质量，使用{@link #thumbnail()}方法时无效
     */
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

    /**
     * 设置图片最大比例，超出比例裁剪图片
     */
    public CompressUtils crop(float maxScale) {
        this.maxScale = maxScale;
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

    /**
     * 返回图片压缩之后的宽高
     */
    public int[] outSize() {
        return new int[]{outWidth, outHeight};
    }

    /**
     * 返回图片压缩之前的宽高
     */
    public int[] inSize() {
        return new int[]{inWidth, inHeight};
    }

    /**
     * 自动旋转图片
     */
    public CompressUtils rotate() {
        this.angle = ImageUtils.readPictureDegree(this.inputFilePath);
        return this;
    }

    /**
     * 根据角度旋转图片
     */
    public CompressUtils rotate(int angle) {
        this.angle = angle;
        return this;
    }

    /**
     * 限制输出图片的最大大小，使用{@link #image()}方法时无效
     */
    public CompressUtils max(long maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    /**
     * 生成压缩图方法：通过quality控制质量，无法通过maxSize控制大小
     */
    public synchronized CompressUtils image() {
        long before = SystemClock.uptimeMillis();
        int w, h;
        float scale;
        int[] cropOptions;
        boolean recycle = false;
        if (bitmap == null) {
            recycle = true;
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            BitmapFactory.decodeFile(inputFilePath, newOpts);
            newOpts.inJustDecodeBounds = false;
            w = newOpts.outWidth;
            h = newOpts.outHeight;
            cropOptions = ImageUtils.getOptionCrop(w, h, maxScale);
            scale = ImageUtils.getOptionScale(cropOptions[0], cropOptions[1], width, height);
            newOpts.inSampleSize = ImageUtils.getOptionSample(scale);
            bitmap = BitmapFactory.decodeFile(inputFilePath, newOpts);
        } else {
            w = bitmap.getWidth();
            h = bitmap.getHeight();
            cropOptions = ImageUtils.getOptionCrop(w, h, maxScale);
            scale = ImageUtils.getOptionScale(cropOptions[0], cropOptions[1], width, height);
        }
        if (w == 0 || h == 0 || bitmap == null)
            return this;
        inWidth = w;
        inHeight = h;
        outWidth = (int) (w / scale);
        outHeight = (int) (h / scale);
        if (scale > 1)
            bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);
        if (maxScale > 0)
            bitmap = Bitmap.createBitmap(bitmap, (int) (cropOptions[2] / scale), (int) (cropOptions[3] / scale), (int) (cropOptions[0] / scale), (int) (cropOptions[1] / scale));
        if (angle > 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate(angle, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        outWidth = bitmap.getWidth();
        outHeight = bitmap.getHeight();
        Bitmap outB = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(outB);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        String tempFile = ImageUtils.getTempFile();
        try {
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            outB.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
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
            if (outB != null) {
                outB.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "compress consume time :-" + (SystemClock.uptimeMillis() - before));
        return this;
    }

    /**
     * 生成缩略图方法：通过maxSize控制输出大小，无法通过quality控制质量
     */
    public synchronized CompressUtils thumbnail() {
        long before = SystemClock.uptimeMillis();
        int w, h;
        float scale;
        int[] cropOptions;
        boolean recycle = false;
        if (bitmap == null) {
            recycle = true;
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            BitmapFactory.decodeFile(inputFilePath, newOpts);
            newOpts.inJustDecodeBounds = false;
            w = newOpts.outWidth;
            h = newOpts.outHeight;
            cropOptions = ImageUtils.getOptionCrop(w, h, maxScale);
            scale = ImageUtils.getOptionScale(cropOptions[0], cropOptions[1], width, height);
            newOpts.inSampleSize = ImageUtils.getOptionSample(scale);
            bitmap = BitmapFactory.decodeFile(inputFilePath, newOpts);
        } else {
            w = bitmap.getWidth();
            h = bitmap.getHeight();
            cropOptions = ImageUtils.getOptionCrop(w, h, maxScale);
            scale = ImageUtils.getOptionScale(cropOptions[0], cropOptions[1], width, height);
        }
        if (w == 0 || h == 0 || bitmap == null)
            return this;
        inWidth = w;
        inHeight = h;
        outWidth = (int) (w / scale);
        outHeight = (int) (h / scale);
        if (scale > 1)
            bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);
        if (maxScale > 0)
            bitmap = Bitmap.createBitmap(bitmap, (int) (cropOptions[2] / scale), (int) (cropOptions[3] / scale), (int) (cropOptions[0] / scale), (int) (cropOptions[1] / scale));
        if (angle > 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate(angle, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        outWidth = bitmap.getWidth();
        outHeight = bitmap.getHeight();
        Bitmap outB = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(outB);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        ByteArrayOutputStream outputStream;
        outputStream = new ByteArrayOutputStream();
        outB.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
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
            if (outB != null) {
                outB.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "compress consume time :-" + (SystemClock.uptimeMillis() - before));
        return this;
    }

    private static native void imageCompress(String inputFile, String outputFile, int quality);

    private static native void thumbnailCompress(byte[] inputData, String outputFile, long maxSize);

    /**
     * @param scale scale <-> m/n
     * @param m Scaling factor ： Numerator
     * @param n Scaling factor ： Denominator
     * @param su outSubSample <-> subSample
     * @param subSample the level of chrominance subsampling to be used when generating the JPEG image (see @ref TJSAMP "Chrominance subsampling options".)
     * @param q quality <-> quality
     * @param quality jpegQual the image quality of the generated JPEG image (1 = worst, 100 = best)
     * @param g xform-option(共存) : TJXOPT_GRAY
     * @param hFlip xform-op(互斥) : TJXOP_HFLIP
     * @param vFlip .. : TJXOP_VFLIP
     * @param transpose .. : TJXOP_TRANSPOSE
     * @param transverse .. : TJXOP_TRANSVERSE
     * @param rot90 .. : TJXOP_ROT90
     * @param rot180 .. : TJXOP_ROT180
     * @param rot270 .. : TJXOP_ROT270
     * @param c xform-crop (xform-option : TJXOPT_CROP) :Cropping region <-> w,h,x,y
     * @param c_w ..
     * @param c_h ..
     * @param c_x ..
     * @param c_y ..
     * @param fastUpSample flags(共存) : TJFLAG_FASTUPSAMPLE
     * @param fastDCT .. : TJFLAG_FASTDCT
     * @param accurateDCT .. : TJFLAG_ACCURATEDCT
     * @param input input filename
     * @param output output filename
     */
    protected static native void compress(boolean scale, int m, int n,
                                          boolean su, int subSample,
                                          boolean q, int quality,
                                          boolean g,
                                          boolean hFlip, boolean vFlip, boolean transpose, boolean transverse, boolean rot90, boolean rot180, boolean rot270,
                                          boolean c, int c_w, int c_h, int  c_x, int  c_y,
                                          boolean fastUpSample, boolean fastDCT, boolean accurateDCT,
                                          String input, String output);

}
