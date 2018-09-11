/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.span;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import javax.annotation.Nullable;

/**
 * DraweeSpan text view that can be used to bind to a {@link DraweeSpanStringBuilder} to
 * display images within text strings.
 *
 * You should always use {@link #setDraweeSpanStringBuilder(DraweeSpanStringBuilder)}
 * instead of calling {@link #setText(CharSequence)} and its variations.
 *
 * If you use the normal text view setters, this view will behave exactly like BetterTextView.
 * If you previously set a {@link DraweeSpanStringBuilder} but want to re-use it as a normal
 * text view, you should call {@link #detachCurrentDraweeSpanStringBuilder()} first.
 *
 */
public class SimpleDraweeSpanTextView extends TextView {

  private DraweeSpanStringBuilder mDraweeStringBuilder;
  private boolean mIsAttached = false;

  public SimpleDraweeSpanTextView(Context context) {
    super(context);
  }

  public SimpleDraweeSpanTextView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public SimpleDraweeSpanTextView(Context context, @Nullable AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    mIsAttached = true;
    if (mDraweeStringBuilder != null) {
      mDraweeStringBuilder.onAttachToView(this);
    }
  }

  @Override
  public void onFinishTemporaryDetach() {
    super.onFinishTemporaryDetach();
    mIsAttached = true;
    if (mDraweeStringBuilder != null) {
      mDraweeStringBuilder.onAttachToView(this);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    mIsAttached = false;
    if (mDraweeStringBuilder != null) {
      mDraweeStringBuilder.onDetachFromView(this);
    }
    super.onDetachedFromWindow();
  }

  @Override
  public void onStartTemporaryDetach() {
    mIsAttached = false;
    if (mDraweeStringBuilder != null) {
      mDraweeStringBuilder.onDetachFromView(this);
    }
    super.onStartTemporaryDetach();
  }

  /**
   * Bind the given string builder to this view.
   *
   * @param draweeSpanStringBuilder the builder to attach to
   */
  public void setDraweeSpanStringBuilder(DraweeSpanStringBuilder draweeSpanStringBuilder) {
    // setText will trigger onTextChanged, which will clean up the old draweeSpanStringBuilder
    // if necessary
    setText(draweeSpanStringBuilder, BufferType.SPANNABLE);
    mDraweeStringBuilder = draweeSpanStringBuilder;
    if (mDraweeStringBuilder != null && mIsAttached) {
      mDraweeStringBuilder.onAttachToView(this);
    }
  }

  /**
   * Detaches the currently attached DraweeSpanStringBuilder (if there is one) so that
   * this view can be used as a normal text view instead.
   */
  public void detachCurrentDraweeSpanStringBuilder() {
    if (mDraweeStringBuilder != null) {
      mDraweeStringBuilder.onDetachFromView(this);
    }
    mDraweeStringBuilder = null;
  }

  @Override
  public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter);
    // The text changed, so we might not have a DraweeSpanStringBuilder any more
    // (or a different one). Since all setText methods in TextView are final, we cannot directly
    // hook into the setText calls to handle this in a cleaner way.
    detachCurrentDraweeSpanStringBuilder();
  }
}
