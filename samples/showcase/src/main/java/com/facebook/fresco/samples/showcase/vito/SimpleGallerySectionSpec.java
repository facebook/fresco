/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.ShowcaseApplication;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.vito.litho.FrescoVitoImage2;
import com.facebook.fresco.vito.litho.FrescoVitoTapToRetryImage;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSourceProvider;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.LongClickEvent;
import com.facebook.litho.TouchEvent;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.yoga.YogaEdge;
import java.util.List;

@GroupSectionSpec
public class SimpleGallerySectionSpec {

  private static final String TAG = "SimpleGallerySection";

  private static final ImageOptions IMAGE_OPTIONS =
      ImageOptions.create().placeholderRes(R.color.placeholder_color).build();

  @OnCreateChildren
  static Children onCreateChildren(final SectionContext c) {
    List<Uri> data =
        ShowcaseApplication.Companion.getImageUriProvider()
            .getRandomSampleUris(ImageUriProvider.ImageSize.M, 500);

    return Children.create()
        .child(
            DataDiffSection.<Uri>create(c)
                .data(data)
                .renderEventHandler(SimpleGallerySection.onRender(c)))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(
      final SectionContext c,
      @Prop(optional = true) boolean enableTapToRetry,
      @Prop(optional = true) boolean isInitialTapToLoad,
      @FromEvent Uri model) {
    Component.Builder image;
    if (enableTapToRetry) {
      image =
          FrescoVitoTapToRetryImage.create(c)
              .imageSource(ImageSourceProvider.forUri(model))
              .imageOptions(IMAGE_OPTIONS)
              .retryImageRes(R.drawable.ic_retry_black_48dp)
              .retryImageScaleType(ScalingUtils.ScaleType.CENTER_INSIDE)
              .isInitialTapToLoad(isInitialTapToLoad)
              .imageClickHandler(SimpleGallerySection.onClickEvent(c))
              .imageTouchHandler(SimpleGallerySection.onTouchEvent(c))
              .imageLongClickHandler(SimpleGallerySection.onLongClickEvent(c));

    } else {
      image =
          FrescoVitoImage2.create(c)
              .uri(model)
              .imageOptions(IMAGE_OPTIONS)
              .clickHandler(SimpleGallerySection.onClickEvent(c))
              .touchHandler(SimpleGallerySection.onTouchEvent(c))
              .longClickHandler(SimpleGallerySection.onLongClickEvent(c));
    }
    return ComponentRenderInfo.create().component(image.paddingDip(YogaEdge.ALL, 2)).build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(SectionContext c, @FromEvent View view) {
    android.util.Log.d(TAG, "Gallery item clicked " + view);
  }

  @OnEvent(LongClickEvent.class)
  static boolean onLongClickEvent(SectionContext c, @FromEvent View view) {
    android.util.Log.d(TAG, "Gallery item long clicked " + view);
    return true;
  }

  @OnEvent(TouchEvent.class)
  static boolean onTouchEvent(
      SectionContext c, @FromEvent View view, @FromEvent MotionEvent motionEvent) {
    android.util.Log.d(TAG, "Gallery item touched " + view);
    return false;
  }
}
