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

package com.facebook.samples.uriapp;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;

public class MainActivity extends Activity {
  private SimpleDraweeView mSimpleDraweeView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    FLog.setMinimumLoggingLevel(FLog.VERBOSE);
    Set<RequestListener> listeners = new HashSet<>();
    listeners.add(new RequestLoggingListener());
    ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
        .setRequestListeners(listeners)
        .build();
    Fresco.initialize(this, config);
    setContentView(R.layout.activity_main);

    mSimpleDraweeView = (SimpleDraweeView) findViewById(R.id.simple_drawee_view);

    EditText editText = (EditText) findViewById(R.id.uri_edit_text);
    editText.setOnEditorActionListener(
        new TextView.OnEditorActionListener() {
          @Override
          public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            final boolean isEnterKeyDown = (actionId == EditorInfo.IME_NULL) &&
                    (event.getAction() == KeyEvent.ACTION_DOWN);
            if (isEnterKeyDown || actionId == EditorInfo.IME_ACTION_DONE) {
              updateImageUri(Uri.parse(v.getText().toString()));
            }
            return false;
          }
        });
  }

  private void updateImageUri(Uri uri) {
    DraweeController controller = Fresco.newDraweeControllerBuilder()
            .setUri(uri)
            .setAutoPlayAnimations(true)
            .build();
    mSimpleDraweeView.setController(controller);
  }
}
