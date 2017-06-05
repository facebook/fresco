package com.facebook.drawee.interfaces;

import android.graphics.drawable.Drawable;

/**
 * 提供给外部的监听器
 * Created by Linhh on 16/12/13.
 */

public interface FrescoPainterDraweeInterceptor {
  Drawable onSetPlaceholderImage(int resourceId);
  Drawable onSetRetryImage(int resourceId);
  Drawable onSetFailureImage(int resourceId);
  Drawable onSetProgressBarImage(int resourceId);
  Drawable onSetBackground(int resourceId);
}
