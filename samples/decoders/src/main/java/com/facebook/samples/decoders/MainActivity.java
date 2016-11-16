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
package com.facebook.samples.decoders;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Get the Drawee view
    SimpleDraweeView drawee1 = (SimpleDraweeView) findViewById(R.id.drawee1);

    // Set a simple custom color resource as the image.
    // The format of custom_color1 is <color>#4CAF50</color>
    drawee1.setImageURI(UriUtil.getUriForResourceId(R.raw.custom_color1));

    // Get another Drawee view and set another custom image resource
    SimpleDraweeView drawee2 = (SimpleDraweeView) findViewById(R.id.drawee2);
    drawee2.setImageURI(UriUtil.getUriForResourceId(R.raw.custom_color2));
  }
}
