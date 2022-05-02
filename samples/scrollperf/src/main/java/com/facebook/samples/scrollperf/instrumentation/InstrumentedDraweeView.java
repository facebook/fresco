/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.instrumentation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import androidx.annotation.Nullable;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.interfaces.SimpleDraweeControllerBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.util.DraweeUtil;

/** {@link SimpleDraweeView} with instrumentation. */
public class InstrumentedDraweeView extends SimpleDraweeView implements Instrumented {

  private Instrumentation mInstrumentation;
  private Config mConfig;
  private ControllerListener<Object> mListener;

  public InstrumentedDraweeView(Context context, GenericDraweeHierarchy hierarchy, Config config) {
    super(context, hierarchy);
    this.mConfig = config;
    init();
  }

  private void init() {
    mInstrumentation = new Instrumentation(this);
    if (mConfig.instrumentationEnabled) {
      mListener =
          new BaseControllerListener<Object>() {
            @Override
            public void onSubmit(String id, Object callerContext) {
              mInstrumentation.onStart();
            }

            @Override
            public void onFinalImageSet(
                String id, @Nullable Object imageInfo, @Nullable Animatable animatable) {
              mInstrumentation.onSuccess();
            }

            @Override
            public void onFailure(String id, Throwable throwable) {
              mInstrumentation.onFailure();
            }

            @Override
            public void onRelease(String id) {
              mInstrumentation.onCancellation();
            }
          };
    }
    DraweeUtil.setBgColor(this, mConfig);
  }

  @Override
  public void initInstrumentation(String tag, PerfListener perfListener) {
    if (mConfig.instrumentationEnabled) {
      mInstrumentation.init(tag, perfListener);
    }
  }

  @Override
  public void onDraw(final Canvas canvas) {
    super.onDraw(canvas);
    if (mConfig.instrumentationEnabled) {
      mInstrumentation.onDraw(canvas);
    }
  }

  @Override
  public void setImageURI(Uri uri, @Nullable Object callerContext) {
    SimpleDraweeControllerBuilder controllerBuilder =
        getControllerBuilder()
            .setUri(uri)
            .setCallerContext(callerContext)
            .setOldController(getController());
    if (mConfig.instrumentationEnabled
        && controllerBuilder instanceof AbstractDraweeControllerBuilder) {
      ((AbstractDraweeControllerBuilder<?, ?, ?, ?>) controllerBuilder)
          .setControllerListener(mListener);
    }
    setController(controllerBuilder.build());
  }

  public void setListener(AbstractDraweeControllerBuilder controllerBuilder) {
    if (mConfig.instrumentationEnabled) {
      controllerBuilder.setControllerListener(mListener);
    }
  }

  public ControllerListener<Object> getListener() {
    return mListener;
  }
}
