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

import android.net.Uri;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.ShowcaseApplication;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.vito.litho.FrescoVitoImage;
import com.facebook.fresco.vito.options.ImageOptions;
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
import com.facebook.yoga.YogaEdge;
import java.util.List;

@GroupSectionSpec
public class SimpleGallerySectionSpec {

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
                .renderEventHandler(SimpleListSection.onRender(c)))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(final SectionContext c, @FromEvent Uri model) {
    return ComponentRenderInfo.create()
        .component(
            FrescoVitoImage.create(c)
                .uri(model)
                .imageOptions(IMAGE_OPTIONS)
                .paddingDip(YogaEdge.ALL, 2))
        .build();
  }
}
