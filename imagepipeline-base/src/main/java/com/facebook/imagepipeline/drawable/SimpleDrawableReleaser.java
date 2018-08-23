package com.facebook.imagepipeline.drawable;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.common.references.ResourceReleaser;

/**
 * A releaser that just recycles (frees) drawable bitmap memory immediately.
 */
public class SimpleDrawableReleaser implements ResourceReleaser<Drawable> {

    private static SimpleDrawableReleaser sInstance;

    public static SimpleDrawableReleaser getInstance() {
        if (sInstance == null) {
            sInstance = new SimpleDrawableReleaser();
        }
        return sInstance;
    }

    @Override
    public void release(Drawable value) {
        if (value instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable)value).getBitmap();
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }
}
