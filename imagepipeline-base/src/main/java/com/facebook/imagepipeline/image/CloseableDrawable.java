package com.facebook.imagepipeline.image;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imageutils.BitmapUtil;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * {@link CloseableImage} that wraps a drawable.
 */
@ThreadSafe
public class CloseableDrawable extends CloseableImage {

    @GuardedBy("this")
    private CloseableReference<Drawable> mDrawableReference;

    private Drawable mDrawable;

    public CloseableDrawable(Drawable drawable, ResourceReleaser<Drawable> resourceReleaser) {
        mDrawable = drawable;
        mDrawableReference = CloseableReference.of(
                mDrawable,
                Preconditions.checkNotNull(resourceReleaser));
    }

    /**
     * Gets the underlying drawable.
     * Note: some Android drawable resources like xml, vector drawable, adaptive icon can be only decoded
     * to drawable not bitmap
     * @return the underlying drawable
     */
    public Drawable getUnderlyingDrawable() {
        return mDrawable;
    }

    @Override
    public int getSizeInBytes() {

        if (mDrawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable)mDrawable).getBitmap();
            return BitmapUtil.getSizeInBytes(bitmap);
        }
        // 4 bytes per pixel for ARGB_8888 Bitmaps is something of a reasonable approximation. If
        // there are no intrinsic bounds, we can fall back just to 1.
        return Math.max(1, getWidth() * getHeight() * 4);
    }

    @Override
    public void close() {
        CloseableReference<Drawable> reference = detachDrawableReference();
        if (reference != null) {
            reference.close();
        }
    }

    private synchronized CloseableReference<Drawable> detachDrawableReference() {
        CloseableReference<Drawable> reference = mDrawableReference;
        mDrawableReference = null;
        mDrawable = null;
        return reference;
    }

    @Override
    public boolean isClosed() {
        return mDrawableReference == null;
    }

    @Override
    public int getWidth() {
        return mDrawable == null ? 0 : Math.max(0, mDrawable.getIntrinsicWidth());
    }

    @Override
    public int getHeight() {
        return mDrawable == null ? 0 : Math.max(0, mDrawable.getIntrinsicHeight());
    }
}
