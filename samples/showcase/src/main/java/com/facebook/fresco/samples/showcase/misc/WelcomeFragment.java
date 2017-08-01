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
package com.facebook.fresco.samples.showcase.misc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;

/**
 * Fragment shown on start-up
 */
public class WelcomeFragment extends BaseShowcaseFragment {

  private static final String URL_DOCUMENTATION = "http://frescolib.org/";
  private static final String URL_GITHUB = "https://github.com/facebook/fresco";

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_welcome, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final SimpleDraweeView draweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    draweeView.setActualImageResource(R.drawable.logo);
    draweeView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final RotateAnimation rotateAnimation = new RotateAnimation(
            0,
            360,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f);
        rotateAnimation.setDuration(1000);
        rotateAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        draweeView.startAnimation(rotateAnimation);
      }
    });

    final Button buttonGitHub = (Button) view.findViewById(R.id.button_github);
    setUriIntent(buttonGitHub, URL_GITHUB);

    final Button buttonDocumentation = (Button) view.findViewById(R.id.button_documentation);
    setUriIntent(buttonDocumentation, URL_DOCUMENTATION);
  }

  private void setUriIntent(final Button button, final String url) {
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
      }
    });
  }

  @Override
  public int getTitleId() {
    return R.string.welcome_title;
  }
}
