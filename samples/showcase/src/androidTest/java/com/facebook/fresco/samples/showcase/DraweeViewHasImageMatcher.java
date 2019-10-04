/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase;

import android.view.View;
import androidx.test.espresso.matcher.BoundedMatcher;
import com.facebook.drawee.view.SimpleDraweeView;
import org.hamcrest.Description;

class DraweeViewHasImageMatcher {

  static BoundedMatcher<View, SimpleDraweeView> hasImage() {
    return new BoundedMatcher<View, SimpleDraweeView>(SimpleDraweeView.class) {
      @Override
      public void describeTo(Description description) {
        description.appendText("has image");
      }

      @Override
      public boolean matchesSafely(SimpleDraweeView draweeView) {
        return draweeView.getHierarchy().hasImage();
      }
    };
  }
}
