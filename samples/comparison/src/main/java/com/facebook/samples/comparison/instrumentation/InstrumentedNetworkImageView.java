/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.instrumentation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.android.volley.toolbox.NetworkImageView;
import com.facebook.samples.comparison.R;

/**
 * {@link NetworkImageView} that notifies its instance of {@link Instrumentation} whenever an image
 * request lifecycle event happens.
 */
public class InstrumentedNetworkImageView extends NetworkImageView implements Instrumented {

  private final Instrumentation mInstrumentation;

  public InstrumentedNetworkImageView(final Context context) {
    super(context);
    mInstrumentation = new Instrumentation(this);
  }

  @Override
  public void initInstrumentation(final String tag, final PerfListener perfListener) {
    mInstrumentation.init(tag, perfListener);
    // we don't have a better estimate on when to call onStart, so do it here.
    mInstrumentation.onStart();
  }

  @Override
  public void onDraw(final Canvas canvas) {
    super.onDraw(canvas);
    mInstrumentation.onDraw(canvas);
  }

  @Override
  public void setImageBitmap(final Bitmap bm) {
    // bm == null in couple of situations like
    // - detaching from window
    // - cleaning up previous request
    if (bm != null) {
      mInstrumentation.onSuccess();
    }
    super.setImageBitmap(bm);
  }

  public void setImageResource(int resourceId) {
    if (resourceId == R.color.placeholder) {
      // ignore
    } else if (resourceId == R.color.error) {
      mInstrumentation.onFailure();
    } else {
      throw new IllegalArgumentException("Unrecognized resourceId");
    }
    super.setImageResource(resourceId);
  }
}
