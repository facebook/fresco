/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import static com.facebook.yoga.YogaAlign.CENTER;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.START;

import android.net.Uri;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.vito.litho.FrescoVitoImage2;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

@LayoutSpec
public class SimpleListItemSpec {

  private static final ImageOptions IMAGE_OPTIONS =
      ImageOptions.create().placeholderRes(R.color.placeholder_color).build();

  private static final ImageOptions PROFILE_IMAGE_OPTIONS =
      ImageOptions.extend(IMAGE_OPTIONS).round(RoundingOptions.asCircle()).build();

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop final Uri profilePicture,
      @Prop final Uri mainPicture,
      @Prop final String title) {

    return Column.create(c)
        .paddingDip(ALL, 16)
        .child(
            Row.create(c)
                .child(
                    FrescoVitoImage2.create(c)
                        .uri(profilePicture)
                        .imageOptions(PROFILE_IMAGE_OPTIONS)
                        .widthDip(52))
                .child(
                    Text.create(c)
                        .text(title)
                        .textSizeSp(24)
                        .alignSelf(CENTER)
                        .paddingDip(START, 8))
                .paddingDip(BOTTOM, 8))
        .child(FrescoVitoImage2.create(c).uri(mainPicture).imageOptions(IMAGE_OPTIONS))
        .build();
  }
}
