package com.lzr.screenshot.engine;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.os.AsyncTaskCompat;

import com.lzr.screenshot.utils.BitmapUtils;
import com.lzr.screenshot.utils.DensityUtil;
import com.lzr.screenshot.utils.FileUtils;
import com.lzr.screenshot.utils.ThreadManager;

import java.lang.ref.SoftReference;
import java.util.ArrayList;

public class Shotter {

    private final SoftReference<Context> mRefContext;
    private ImageReader mImageReader;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private OnShotListener mOnShotListener;
    private ArrayList<Integer> cachedPHash;


    public Shotter(Context context, int reqCode, Intent data) {
        this.mRefContext = new SoftReference<>(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mMediaProjection == null)
                mMediaProjection = getMediaProjectionManager().getMediaProjection(reqCode, data);
            mImageReader = ImageReader.newInstance(
                    DensityUtil.getScreenWidth(),
                    DensityUtil.getScreenHeight(),
                    PixelFormat.RGBA_8888,//此处必须和下面 buffer处理一致的格式 ，RGB_565在一些机器上出现兼容问题。
                    1);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void virtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                DensityUtil.getScreenWidth(),
                DensityUtil.getScreenHeight(),
                Resources.getSystem().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void startScreenShot(OnShotListener onShotListener) {
        mOnShotListener = onShotListener;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            virtualDisplay();
            final Image image = mImageReader.acquireLatestImage();
            /**
             * Java的线程池中封装了阻塞队列，当活动线程超过核心线程时，线程将会在阻塞队列里排队，
             * 因此使用线程池缓存任务队列
             */
            ThreadManager.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    //取到屏幕图片后的核心业务逻辑
                    AsyncTaskCompat.executeParallel(new SaveTask(), image);
                }
            });

        }
    }

    public class SaveTask extends AsyncTask<Image, Void, Bitmap> {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected Bitmap doInBackground(Image... params) {
            boolean alike = false;
            if (params == null || params.length < 1 || params[0] == null) {
                return null;
            }
            Image image = params[0];
            if (image == null) return null;

            //把截屏获取的image转成bitmap类型
            Bitmap bitmap = BitmapUtils.image2Bitmap(image);
            //获取phash值
            ArrayList<Integer> pHashArray = BitmapUtils.pHash(bitmap);
            //和缓存的phash做对比，如果两张图片相似，则不保存
            if (cachedPHash != null && cachedPHash.size() != 0) {
                alike = BitmapUtils.isAlike(cachedPHash, pHashArray);
            }
            cachedPHash = pHashArray;
            if (!alike) {
                //获取phash作为文件名保存到本地
                String pHashString = BitmapUtils.getPHashString(pHashArray);
                FileUtils.saveBitmap(getContext(), bitmap, pHashString);
            }
            return null;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
            }
            if (mOnShotListener != null) {
                mOnShotListener.onFinish();
            }

        }
    }

    private MediaProjectionManager getMediaProjectionManager() {
        return (MediaProjectionManager) getContext().getSystemService(
                Context.MEDIA_PROJECTION_SERVICE);
    }

    private Context getContext() {
        return mRefContext.get();
    }

    public interface OnShotListener {
        void onFinish();
    }
}
