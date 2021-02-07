/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.net.Uri;
import com.facebook.fresco.samples.showcase.ShowcaseApplication;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;

@GroupSectionSpec
public class SimpleListSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(final SectionContext c) {
    return Children.create()
        .child(
            DataDiffSection.<Data>create(c)
                .data(generateData(c, 100))
                .renderEventHandler(SimpleListSection.onRender(c)))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(final SectionContext c, @FromEvent Data model) {
    return ComponentRenderInfo.create()
        .component(
            SimpleListItem.create(c)
                .profilePicture(model.profilePicture)
                .mainPicture(model.mainPicture)
                .title(model.title))
        .build();
  }

  private static List<Data> generateData(final SectionContext c, int count) {
    ImageUriProvider uris = ShowcaseApplication.Companion.getImageUriProvider();
    ArrayList<Data> data = new ArrayList<>(count);
    for (int i = 1; i <= count; i++) {
      data.add(
          new Data(
              uris.createSampleUri(ImageUriProvider.ImageSize.S),
              uris.createSampleUri(ImageUriProvider.ImageSize.M),
              "Photo " + i));
    }
    return data;
  }

  static class Data {
    final Uri profilePicture;
    final Uri mainPicture;
    final String title;

    Data(Uri profilePicture, Uri mainPicture, String title) {
      this.profilePicture = profilePicture;
      this.mainPicture = mainPicture;
      this.title = title;
    }
  }
}
