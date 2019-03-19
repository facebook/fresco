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
    ImageUriProvider uris = ImageUriProvider.getInstance(c.getAndroidContext());
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
