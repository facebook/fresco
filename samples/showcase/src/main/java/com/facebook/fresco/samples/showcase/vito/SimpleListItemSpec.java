/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.fresco.samples.showcase.vito;

import static com.facebook.yoga.YogaAlign.CENTER;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.START;

import android.net.Uri;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.vito.litho.FrescoVitoImage;
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
      ImageOptions.create()
          .placeholderRes(R.color.placeholder_color)
          .build();

  private static final ImageOptions PROFILE_IMAGE_OPTIONS =
      ImageOptions.extend(IMAGE_OPTIONS)
          .round(RoundingOptions.asCircle())
          .build();

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
                    FrescoVitoImage.create(c)
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
        .child(
            FrescoVitoImage.create(c)
            .uri(mainPicture)
            .imageOptions(IMAGE_OPTIONS))
        .build();
  }
}
