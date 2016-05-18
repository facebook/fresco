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

package com.facebook.samples.transitions;

import android.app.Activity;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.TransitionSet;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.DraweeTransform;
import com.facebook.drawee.view.SimpleDraweeView;

public class DestinationActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.drawee_destination);

    SimpleDraweeView simpleDraweeView = (SimpleDraweeView) findViewById(R.id.image);
    simpleDraweeView.setImageURI("res:/" + R.drawable.test_image);

    getWindow().setSharedElementEnterTransition(DraweeTransform.createTransitionSet(
            ScalingUtils.ScaleType.CENTER_CROP, ScalingUtils.ScaleType.CENTER_INSIDE));
    getWindow().setSharedElementReturnTransition(DraweeTransform.createTransitionSet(
            ScalingUtils.ScaleType.CENTER_INSIDE, ScalingUtils.ScaleType.CENTER_CROP));
  }
}