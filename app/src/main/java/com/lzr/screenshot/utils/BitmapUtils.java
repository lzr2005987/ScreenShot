package com.lzr.screenshot.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.Image;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by Administrator on 2018/6/2 0002.
 */

public class BitmapUtils {
    private static final int DCT_SIZE = 8;
    private static final int DIF_NO = 10;

    private static Mat getMat(Drawable drawable) {
        Bitmap bitmap = BitmapUtils.drawableToBitmap(drawable);
        Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Mat mat = new Mat(bitmapCopy.getHeight(), bitmapCopy.getWidth(), CvType.CV_32FC1);
        Utils.bitmapToMat(bitmapCopy, mat);
        /**
         * 20180602 lizheren
         * cvType分为depth和channel两部分，channel随bitmap的类型，convertTo方法仅能改变depth
         * 默认的bitmap的channel为C4（ARGB），depth为CV_8U，如果要把channel转换为C1必须使图片变为灰度图
         * dct离散余弦变换要求矩阵格式是CV_32FC1或CV_64FC1
         */
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY);
        mat.convertTo(mat, CvType.CV_32F);
        return mat;
    }

    private static Mat getMat(Bitmap bitmap) {
        Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Mat mat = new Mat(bitmapCopy.getHeight(), bitmapCopy.getWidth(), CvType.CV_32FC1);
        Utils.bitmapToMat(bitmapCopy, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY);
        mat.convertTo(mat, CvType.CV_32F);
        return mat;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;

    }

    public static ArrayList<Integer> pHash(Drawable drawable) {
        return pHash(getMat(drawable));
    }

    public static ArrayList<Integer> pHash(Bitmap bitmap) {
        return pHash(getMat(bitmap));
    }

    public static ArrayList<Integer> pHash(Context context, int resId) {
        return pHash(context.getResources().getDrawable(resId));
    }

    public static ArrayList<Integer> pHash(Mat mat) {
        ArrayList<Double> dctArray = new ArrayList<>();
        ArrayList<Integer> result = new ArrayList<>();
        double average = 0;
        Mat pHashMat = new Mat(new Size(DCT_SIZE, DCT_SIZE), CvType.CV_32FC1);
        Imgproc.resize(mat, mat, new Size(DCT_SIZE, DCT_SIZE));

        Core.dct(mat, pHashMat);
        for (int i = 0; i < DCT_SIZE; i++) {
            for (int j = 0; j < DCT_SIZE; j++) {
                /**
                 * 20180602 lizheren
                 * 这里返回的double数组的长度取决于cvType的channel，灰度图的通道数为1，所以double长度是1
                 * dct算法仅能支持灰度图
                 */
                double[] dct = pHashMat.get(i, j);
                dctArray.add(dct[0]);
                average += dct[0] / (DCT_SIZE * DCT_SIZE);
            }
        }

        for (int i = 0; i < 64; i++) {
            if (dctArray.get(i) >= average) result.add(1);
            else result.add(0);
        }
        return result;
    }

    public static String getPHashString(ArrayList<Integer> pHash) {
        StringBuffer pHashString = new StringBuffer();
        for (int i = 0; i < pHash.size(); i++) {
            pHashString.append(pHash.get(i));
        }
        return pHashString.toString();
    }

    public static int compareHash(ArrayList<Integer> pHash1, ArrayList<Integer> pHash2) {
        int number = 0;
        if (pHash1 == null || pHash2 == null || pHash1.size() != pHash2.size()) return -1;
        else {
            for (int i = 0; i < pHash1.size(); i++) {
                if (!pHash1.get(i).equals(pHash2.get(i))) number++;
            }
            return number;
        }
    }

    public static boolean isAlike(ArrayList<Integer> pHash1, ArrayList<Integer> pHash2) {
        int dif = compareHash(pHash1, pHash2);
        return dif <= DIF_NO;
    }

    public static Bitmap image2Bitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        //每个像素的间距
        int pixelStride = planes[0].getPixelStride();
        //总的间距
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height,
                Bitmap.Config.ARGB_8888);//虽然这个色彩比较费内存但是 兼容性更好
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        image.close();
        return bitmap;
    }
}
