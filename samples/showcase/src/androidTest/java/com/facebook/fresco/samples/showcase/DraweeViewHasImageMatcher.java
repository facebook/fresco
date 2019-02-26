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
