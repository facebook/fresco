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
package com.facebook.samples.animation2.utils;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.bitmap.cache.NoOpCache;
import com.facebook.samples.animation2.R;
import com.facebook.samples.animation2.SampleData;
import com.facebook.samples.animation2.bitmap.ExampleBitmapAnimationFactory;
import com.facebook.samples.animation2.color.ExampleColorBackend;
import com.facebook.samples.animation2.local.LocalDrawableAnimationBackend;

/**
 * Animation backend configurator that holds all sample animation backends.
 */
public class SampleAnimationBackendConfigurator {

  private final Spinner mSpinner;
  private final ArrayAdapter<BackendExampleEntry> mArrayAdapter;
  private final BackendChangedListener mBackendChangedListener;
  private final Context mContext;

  public interface BackendChangedListener {
    void onBackendChanged(AnimationBackend backend);
  }

  public SampleAnimationBackendConfigurator(
      Spinner spinner,
      BackendChangedListener backendChangedListener) {
    mSpinner = spinner;
    mBackendChangedListener = backendChangedListener;

    mContext = mSpinner.getContext();
    mArrayAdapter = new ArrayAdapter<>(spinner.getContext(), android.R.layout.simple_spinner_item);
    mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mSpinner.setAdapter(mArrayAdapter);

    addSampleBackends();
    setupBackendSelector();
  }

  private void addSampleBackends() {
    mArrayAdapter.add(createColorExample());
    mArrayAdapter.add(createLocalDrawableExample());
    mArrayAdapter.add(createBitmapExample());
  }

  private BackendExampleEntry createColorExample() {
    return new BackendExampleEntry() {
      @Override
      public AnimationBackend createBackend() {
        return ExampleColorBackend.createSampleColorAnimationBackend(
            mSpinner.getContext().getResources());
      }

      @Override
      public int getTitleResId() {
        return R.string.backend_color;
      }
    };
  }

  private BackendExampleEntry createLocalDrawableExample() {
    return new BackendExampleEntry() {
      @Override
      public AnimationBackend createBackend() {
        return new LocalDrawableAnimationBackend.Builder(mContext.getResources())
            .addDrawableFrame(R.mipmap.ic_alarm)
            .addDrawableFrame(R.mipmap.ic_android)
            .addDrawableFrame(R.mipmap.ic_launcher)
            .loopCount(3)
            .frameDurationMs(500)
            .build();
      }

      @Override
      public int getTitleResId() {
        return R.string.backend_local_drawables;
      }
    };
  }

  private BackendExampleEntry createBitmapExample() {
    return new BackendExampleEntry() {
      @Override
      public AnimationBackend createBackend() {
        // Get the animation duration in ms for each color frame
        final int frameDurationMs = mContext.getResources()
            .getInteger(android.R.integer.config_mediumAnimTime);
        // Create and return the backend
        return ExampleBitmapAnimationFactory.createColorBitmapAnimationBackend(
            SampleData.COLORS,
            frameDurationMs,
            new NoOpCache());
      }

      @Override
      public int getTitleResId() {
        return R.string.backend_bitmap_simple;
      }
    };
  }

  private void setupBackendSelector() {
    mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateAnimationBackend(mArrayAdapter.getItem(position).createBackend());
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {
      }
    });
  }

  private void updateAnimationBackend(AnimationBackend animationBackend) {
    mBackendChangedListener.onBackendChanged(animationBackend);
  }

  private abstract class BackendExampleEntry {

    public abstract AnimationBackend createBackend();

    public abstract int getTitleResId();

    @Override
    public String toString() {
      return mContext.getString(getTitleResId());
    }
  }
}
