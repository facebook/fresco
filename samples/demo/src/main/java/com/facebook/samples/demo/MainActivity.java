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

package com.facebook.samples.demo;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

public class MainActivity extends Activity {

  private ImageView mBaselineJpegView;
  private SimpleDraweeView mProgressiveJpegView;
  private SimpleDraweeView mStaticWebpView;
  private SimpleDraweeView mAlphaWebpView;
  private SimpleDraweeView mAnimatedGifView;
  private SimpleDraweeView mAnimatedWebpView;
  private SimpleDraweeView mOneLoopAnimatedWebpView;
  private SimpleDraweeView mDataWebpView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    FLog.setMinimumLoggingLevel(FLog.VERBOSE);
    Set<RequestListener> listeners = new HashSet<>();
    listeners.add(new RequestLoggingListener());
    ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
        .setRequestListeners(listeners)
        .setBitmapsConfig(Bitmap.Config.ARGB_8888)
        .build();
    Fresco.initialize(this, config);
    setContentView(R.layout.activity_main);

    mBaselineJpegView = (SimpleDraweeView) findViewById(R.id.baseline_jpeg);
    mProgressiveJpegView = (SimpleDraweeView) findViewById(R.id.progressive_jpeg);
    mStaticWebpView = (SimpleDraweeView) findViewById(R.id.static_webp);
    mAlphaWebpView = (SimpleDraweeView) findViewById(R.id.alpha_webp);
    mAnimatedGifView = (SimpleDraweeView) findViewById(R.id.animated_gif);
    mAnimatedWebpView = (SimpleDraweeView) findViewById(R.id.animated_webp);
    mOneLoopAnimatedWebpView = (SimpleDraweeView) findViewById(R.id.one_loop_animated_webp);
    mDataWebpView = (SimpleDraweeView) findViewById(R.id.data_webp);

    mBaselineJpegView.setImageURI(Uri.parse("https://www.gstatic.com/webp/gallery/1.sm.jpg"));

    Uri uri = Uri.parse("http://pooyak.com/p/progjpeg/jpegload.cgi?o=1");
    ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
        .setProgressiveRenderingEnabled(true)
        .build();
    DraweeController controller = Fresco.newDraweeControllerBuilder()
        .setImageRequest(request)
        .build();
    mProgressiveJpegView.setController(controller);

    mStaticWebpView.setImageURI(Uri.parse("https://www.gstatic.com/webp/gallery/2.sm.webp"));

    mAlphaWebpView.setImageURI(Uri.parse("http://frescolib.org/static/translucent.webp"));

    DraweeController animatedGifController = Fresco.newDraweeControllerBuilder()
        .setAutoPlayAnimations(true)
        .setUri(Uri.parse("https://s3.amazonaws.com/giphygifs/media/4aBQ9oNjgEQ2k/giphy.gif"))
        .build();
    mAnimatedGifView.setController(animatedGifController);

    DraweeController animatedWebpController = Fresco.newDraweeControllerBuilder()
        .setAutoPlayAnimations(true)
        .setUri(Uri.parse("https://www.gstatic.com/webp/animated/1.webp"))
        .build();
    mAnimatedWebpView.setController(animatedWebpController);

    DraweeController oneLoopWebpController = Fresco.newDraweeControllerBuilder()
        .setAutoPlayAnimations(true)
        .setUri(Uri.parse("https://dl.dropboxusercontent.com/u/6610969/webp_180_example.webp"))
        .build();
    mOneLoopAnimatedWebpView.setController(oneLoopWebpController);

    mDataWebpView.setImageURI(Uri.parse("data:image/webp;base64," +
        "UklGRjgCAABXRUJQVlA4WAoAAAAQAAAAFwAAFwAAQUxQSC0AAAABJ6AgbQNm1+EkTnRExDkWahpJgRIk" +
        "oOCVgAKy/mVdSxvR/wyEHWJ49xCbCAcAVlA4IOQBAACQCgCdASoYABgAPlEkjkWjoiEUBAA4BQS2AE6Z" +
        "Qjgbyv8ZuWF3B3ANsBdoHoAeWj+s3wleTHcua7PMAYrNOLPHFqAbpKGWe8x3KqHen7YXTMnmq/c9GqBt" +
        "ZtuQ0AAA/r031iZbkliICmd/QSg0OjEWbX/nv8v+g4UDPpobcehywI6oypX8hbuzcQndgaVt0zW5DiZP" +
        "6Ueo/21IPqsuRm1WyZHL3bJIFStwH8BOWif7xVniUiHwD5HwW8AXIZiq2maDmyIvxn4a0fetR+flTrt/" +
        "5/Vq3BVTeorYBHMN7L09DE9xDW/2+dj45/mCe9vjNUGRpT5EJhV8jDz/ZxPixLvN9Tl5iPD/neh1RCl6" +
        "AOcx3JudnAseXqvm8dEtF+rA40Bg881EW88XwU1oXf/5RY/4ToF9NwcXPLC/AodLaAFPpiXt+C6cFDIj" +
        "+uqi12PWFO+p7jn1P+sjCpbP/OBdHIoez8Rp6nslBEiFG19LKqv6dkGzLKtvt9dRIpz2sef2JFUVB+v+" +
        "hvMMmQ4o6d8aMTGuv/4wZxogl/n/k3g83NO3bBnf7/TL8Baf97pQw43+FVR0hXfpvD0k51yE35e2jNF/" +
        "98Uv3fAfPXw0T8irZQR4r1/ktG5xrypg/aKDooBtoI5aQAAA"));
  }
}
