package com.facebook.imagepipeline.drawable;

import android.graphics.drawable.Drawable;

import com.facebook.imagepipeline.image.CloseableImage;

import javax.annotation.Nullable;

public class BaseDrawableFactory implements DrawableFactory {

  @Override
  public boolean supportsImageType(CloseableImage image) {
    return false;
  }

  @Nullable
  @Override
  public Drawable createDrawable(CloseableImage image) {
    return null;
  }

  @Nullable
  @Override
  public Drawable createDrawable(Drawable previousDrawable, CloseableImage image) {
    return null;
  }

  @Override
  public boolean needPreviousDrawable(Drawable previousDrawable, CloseableImage image) {
    return false;
  }
}
