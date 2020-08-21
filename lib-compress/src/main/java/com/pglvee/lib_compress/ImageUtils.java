package com.pglvee.lib_compress;

import android.media.ExifInterface;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    static int getOptionSample(final float scale) {
        if (scale < 2) return 1;
        else if (scale >= 2f && scale < 4f) return 2;
        else if (scale >= 4f && scale < 8f) return 4;
        else if (scale >= 8f && scale < 16) return 8;
        else return 16;
    }

    static float getOptionScale(int oW, int oH, int w, int h) {
        if (w == 0 || h == 0 || oW == 0 || oH == 0) return 1f;
        int ol = Math.max(oW, oH);
        int s = Math.max(w, h);
        if (ol <= s) return 1f;
        else return (float) ol / (float) s;
    }

    static int[] getOptionCrop(int oW, int oH, float maxScale) {
        if (oW == 0 || oH == 0) return new int[]{0, 0, 0, 0};
        int oWC = oW;
        int oHC = oH;
        int xOffset = 0;
        int yOffset = 0;
        if(maxScale > 0){
            if((float)oW/oH > maxScale){
                oWC = (int) (oH*maxScale);
                xOffset = (oW-oWC)/2;
            }else if((float)oH/oW > maxScale){
                oHC = (int) (oW*maxScale);
                yOffset = (oH-oHC)/2;
            }
        }
        return new int[]{oWC, oHC, xOffset, yOffset};
    }

    static String getTempFile() {
        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsoluteFile(),
                "temp"+System.currentTimeMillis() + ".jpg");
        return file.getAbsolutePath();
    }

    private static String bytesToHexString(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }
        return bytesToHexString(src, src.length);
    }
    private static String bytesToHexString(byte[] src, int length) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString().toUpperCase();
    }

    public static String getMimeType(String path) {
        // 读取文件的前几个字节来判断图片格式
        // https://www.jianshu.com/p/3266fc93b9f1
        byte[] b = new byte[4];
        try (FileInputStream fis = new FileInputStream(path)){
            fis.read(b, 0, b.length);
            String type = bytesToHexString(b);
            if (type.contains("FFD8FF")) {
                return "image/jpeg";
            } else if (type.contains("89504E47")) {
                return "image/png";
            } else if (type.contains("47494638")) {
                return "image/gif";
            } else if (type.contains("424D")) {
                return "image/x-ms-bmp";
            } else if (type.contains("52494646")) {
                return "image/webp";
            } else {
                return "image/*";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "*/*";
    }

    public static String getMimeType(byte[] data){
        String prefix = bytesToHexString(data, 4);
        if(prefix == null) return "image/*";
        String type = prefix.toUpperCase();
        if (type.contains("FFD8FF")) {
            return "image/jpeg";
        } else if (type.contains("89504E47")) {
            return "image/png";
        } else if (type.contains("47494638")) {
            return "image/gif";
        } else if (type.contains("424D")) {
            return "image/x-ms-bmp";
        } else if (type.contains("52494646")) {
            return "image/webp";
        } else {
            return "image/*";
        }
    }

    static void byte2File(byte[] buf, File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)){
            bos.write(buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static byte[] file2byte(String filePath) {
        byte[] buffer = null;
        File file = new File(filePath);
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()){
            byte[] b = new byte[8 * 1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static int readPictureDegree(String path) {
        if(TextUtils.isEmpty(path) || !"image/jpeg".equals(getMimeType(path))) return 0;
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    degree = 0;
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return degree;
    }

}
