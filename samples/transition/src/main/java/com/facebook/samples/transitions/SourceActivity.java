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
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.facebook.drawee.view.SimpleDraweeView;

public class SourceActivity extends Activity {

  private SimpleDraweeView mSimpleDraweeView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.drawee_source);
    mSimpleDraweeView = (SimpleDraweeView) findViewById(R.id.image);
    mSimpleDraweeView.setImageURI("res:/" + R.drawable.test_image);
  }

  public void startTransition(View view) {
    Intent intent = new Intent(this, DestinationActivity.class);
    final String transitionName = getString(R.string.transition_name);
    final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
          this,
          mSimpleDraweeView,
          transitionName);
    startActivity(intent, options.toBundle());
  }
}
