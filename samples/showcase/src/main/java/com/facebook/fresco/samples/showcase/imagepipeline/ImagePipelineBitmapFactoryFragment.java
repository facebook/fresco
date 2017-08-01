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
package com.facebook.fresco.samples.showcase.imagepipeline;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;

/**
 * Fragment demonstrating how to use the PlatformBitmapFactory. It also demonstrates how to manage
 * a {@link CloseableReference} in another component.
 *
 * Most notably, one MUST close it when the outer component is being destroyed.
 */
public class ImagePipelineBitmapFactoryFragment extends BaseShowcaseFragment {

  private enum CreateOptions {
    BASIC(R.string.imagepipeline_bitmap_factory_case_create_basic),
    CROPPED(R.string.imagepipeline_bitmap_factory_case_create_cropped),
    SCALED(R.string.imagepipeline_bitmap_factory_case_create_scaled),
    TRANSFORMED(R.string.imagepipeline_bitmap_factory_case_create_transformed);

    final int descriptionResId;

    CreateOptions(int descriptionResId) {
      this.descriptionResId = descriptionResId;
    }
  }

  private ImageView mImageView;
  private Spinner mSpinner;

  private PlatformBitmapFactory mPlatformBitmapFactory;

  /**
   * The PlatformBitmapFactory calls require initial input to work with. We use the following member
   * Bitmap for this. In a real work example, the pixel data would e.g. come from a decoder.
   */
  private CloseableReference<Bitmap> mOriginalBitmap;

  /**
   * Reference to the currently displayed bitmap
   */
  private CloseableReference<Bitmap> mDisplayedBitmap;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPlatformBitmapFactory = Fresco.getImagePipelineFactory().getPlatformBitmapFactory();
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_imagepipeline_bitmap_factory, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    mImageView = view.findViewById(R.id.drawee_view);
    mSpinner = view.findViewById(R.id.spinner);

    final CreateOptionsAdapter adapter = new CreateOptionsAdapter();
    mSpinner.setAdapter(adapter);
    mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final CreateOptions spinnerEntry = (CreateOptions) adapter.getItem(position);
        initDisplayedBitmap(spinnerEntry);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();

    // restore bitmaps
    mOriginalBitmap = createRainbowBitmap();
    if (mSpinner != null) {
      initDisplayedBitmap((CreateOptions) mSpinner.getSelectedItem());
    }
  }

  @Override
  public void onStop() {
    super.onStop();

    // close bitmaps
    mImageView.setImageBitmap(null);
    CloseableReference.closeSafely(mOriginalBitmap);
    CloseableReference.closeSafely(mDisplayedBitmap);
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_rotation_title;
  }

  /**
   * Creates a new bitmap with a HSV map at value=1
   */
  private CloseableReference<Bitmap> createRainbowBitmap() {
    final int w = 256;
    final int h = 256;
    mOriginalBitmap = mPlatformBitmapFactory.createBitmap(w, h, Bitmap.Config.ARGB_8888);

    final int[] colors = new int[w * h];
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        final float hue = 360f * j / (float) w;
        final float saturation = 2f * (h - i) / (float) h;
        final float value = 1;
        colors[i * h + j] = Color.HSVToColor(255, new float[]{hue, saturation, value});
      }
    }
    mOriginalBitmap.get().setPixels(colors, 0, w, 0, 0, w, h);

    return mOriginalBitmap;
  }

  private void initDisplayedBitmap(CreateOptions createOptions) {
    final CloseableReference<Bitmap> oldDisplayedReference = mDisplayedBitmap;

    final int originalW = mOriginalBitmap.get().getWidth();
    final int originalH = mOriginalBitmap.get().getHeight();

    switch (createOptions) {
      case BASIC:
        mDisplayedBitmap = mPlatformBitmapFactory.createBitmap(mOriginalBitmap.get());
        break;
      case CROPPED:
        mDisplayedBitmap = mPlatformBitmapFactory.createBitmap(
            mOriginalBitmap.get(),
            0,
            0,
            originalW / 2,
            originalH / 2);
        break;
      case SCALED:
        mDisplayedBitmap = mPlatformBitmapFactory.createScaledBitmap(
            mOriginalBitmap.get(),
            4,
            4,
            true);
        break;
      case TRANSFORMED:
        mDisplayedBitmap = mPlatformBitmapFactory.createBitmap(
            mOriginalBitmap.get(),
            0,
            0,
            originalW,
            originalH,
            getMatrix(2f, 3f, 60.0f),
            true);
        break;
    }

    mImageView.setImageBitmap(mDisplayedBitmap.get());
    CloseableReference.closeSafely(oldDisplayedReference);
  }

  private static Matrix getMatrix(float skewX, float skewY, float degrees) {
    Matrix matrix = new Matrix();
    matrix.postSkew(skewX, skewY);
    matrix.postRotate(degrees);
    return matrix;
  }

  private static class CreateOptionsAdapter extends BaseAdapter {

    CreateOptions[] SPINNER_ENTRIES = new CreateOptions[]{
        CreateOptions.BASIC,
        CreateOptions.CROPPED,
        CreateOptions.SCALED,
        CreateOptions.TRANSFORMED};

    @Override
    public int getCount() {
      return SPINNER_ENTRIES.length;
    }

    @Override
    public Object getItem(int position) {
      return SPINNER_ENTRIES[position];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

      final View view = convertView != null
          ? convertView
          : layoutInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);

      final TextView textView = view.findViewById(android.R.id.text1);
      textView.setText(SPINNER_ENTRIES[position].descriptionResId);

      return view;
    }
  }
}
