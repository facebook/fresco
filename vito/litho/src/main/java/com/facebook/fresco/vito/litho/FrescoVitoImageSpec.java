/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho;

import android.content.Context;
import android.net.Uri;
import androidx.core.util.ObjectsCompat;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoController;
import com.facebook.fresco.vito.core.FrescoDrawable;
import com.facebook.fresco.vito.core.FrescoState;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.DefaultFrescoContext;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.MountingType;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import com.facebook.litho.utils.MeasureUtils;
import javax.annotation.Nullable;

@MountSpec(isPureRender = true)
public class FrescoVitoImageSpec {

  @PropDefault protected static final float imageAspectRatio = 1f;

  @OnCreateMountContent(mountingType = MountingType.DRAWABLE)
  static FrescoDrawable onCreateMountContent(Context c) {
    return new FrescoDrawable(false);
  }

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context,
      StateValue<FrescoState> lastFrescoState,
      @Prop final Uri uri,
      @Prop(optional = true) final ImageOptions imageOptions,
      @Prop(optional = true) final FrescoContext frescoContext,
      @Prop(optional = true) final Object callerContext,
      @Prop(optional = true) final ImageListener imageListener) {
    lastFrescoState.set(
        getController(frescoContext)
            .createState(
                uri,
                imageOptions != null ? imageOptions : ImageOptions.defaults(),
                callerContext,
                context.getResources(),
                imageListener));
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop(optional = true, resType = ResType.FLOAT) float imageAspectRatio) {
    MeasureUtils.measureWithAspectRatio(widthSpec, heightSpec, imageAspectRatio, size);
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext context,
      @Prop final Uri uri,
      @Prop(optional = true) final @Nullable ImageOptions imageOptions,
      @Prop(optional = true) final FrescoContext frescoContext,
      @Prop(optional = true) final Object callerContext,
      @Prop(optional = true) final ImageListener imageListener,
      @State(canUpdateLazily = true) final FrescoState lastFrescoState,
      Output<FrescoState> frescoState) {
    FrescoState maybeNewFrescoState =
        getController(frescoContext)
            .onPrepare(
                lastFrescoState,
                uri,
                imageOptions != null ? imageOptions : ImageOptions.defaults(),
                callerContext,
                context.getResources(),
                imageListener);
    if (lastFrescoState != maybeNewFrescoState) {
      FrescoVitoImage.lazyUpdateLastFrescoState(context, maybeNewFrescoState);
    }
    frescoState.set(maybeNewFrescoState);
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      final FrescoDrawable frescoDrawable,
      @Prop(optional = true) final FrescoContext frescoContext,
      @FromPrepare final FrescoState frescoState) {
    frescoState.setFrescoDrawable(frescoDrawable);
    getController(frescoContext).onAttach(frescoState);
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext context,
      FrescoDrawable frescoDrawable,
      @Prop(optional = true) final FrescoContext frescoContext,
      @FromPrepare final FrescoState frescoState) {
    frescoState.setFrescoDrawable(frescoDrawable);
    getController(frescoContext).onDetach(frescoState);
  }

  @OnBoundsDefined
  static void onBoundDefined(
      ComponentContext c, ComponentLayout layout, @FromPrepare final FrescoState frescoState) {
    frescoState.setTargetWidthPx(layout.getWidth());
    frescoState.setTargetHeightPx(layout.getHeight());
  }

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(
      @Prop Diff<Uri> uri,
      @Prop(optional = true) Diff<ImageOptions> imageOptions,
      @Prop(optional = true) Diff<FrescoContext> frescoContext,
      @Prop(optional = true, resType = ResType.FLOAT) Diff<Float> imageAspectRatio) {
    return !ObjectsCompat.equals(uri.getPrevious(), uri.getNext())
        || !ObjectsCompat.equals(imageOptions.getPrevious(), imageOptions.getNext())
        || !ObjectsCompat.equals(imageAspectRatio.getPrevious(), imageAspectRatio.getNext())
        || !ObjectsCompat.equals(frescoContext.getPrevious(), frescoContext.getNext());
  }

  static FrescoContext resolveContext(@Nullable FrescoContext contextOverride) {
    if (contextOverride != null) {
      return contextOverride;
    }
    return DefaultFrescoContext.get();
  }

  static FrescoController getController(@Nullable FrescoContext contextOverride) {
    return resolveContext(contextOverride).getController();
  }
}
