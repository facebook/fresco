package com.facebook.imagepipeline.decoder;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.drawable.SimpleDrawableReleaser;
import com.facebook.imagepipeline.image.CloseableDrawable;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;



/**
 * Decodes images to {@link Drawable}
 *
 */
public class ResourceDrawableDecoder implements ImageDecoder {

    private final Context context;

    public ResourceDrawableDecoder(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public CloseableImage decode(EncodedImage encodedImage, int length, QualityInfo qualityInfo, ImageDecodeOptions options) {
        Uri uri = encodedImage.getImageUri();
        if (uri == null) {
            throw new IllegalArgumentException("Uri is null");
        }

        @DrawableRes int resId = UriUtil.getResourceIdFromUri(context, uri);
        String pkgName = uri.getAuthority();
        Context targetContext = (TextUtils.isEmpty(pkgName) || TextUtils.equals(pkgName, context.getPackageName()))
                ? context : getContextForPackage(uri, pkgName);
        // We can't get a theme from another application.
        Drawable drawable = DrawableDecoderCompat.getDrawable(context, targetContext, resId);
        return new CloseableDrawable(drawable, SimpleDrawableReleaser.getInstance());
    }


    @NonNull
    private Context getContextForPackage(Uri source, String packageName) {
        try {
            return context.createPackageContext(packageName, /*flags=*/ 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException(
                    "Failed to obtain context or unrecognized Uri format for: " + source, e);
        }
    }
}
