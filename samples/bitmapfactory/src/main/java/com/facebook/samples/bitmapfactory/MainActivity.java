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
package com.facebook.samples.bitmapfactory;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;

public class MainActivity extends Activity {

  public static final int BASIC_LOAD = 0;
  public static final int CROPPED_LOAD = 1;
  public static final int TRANSLATED_LOAD = 2;
  public static final int TRANSFORMED_LOAD = 3;
  public static final int SCALED_LOAD = 4;
  public static final int COLOR_ARRAY_LOAD = 5;
  public static final int CROPPED_COLOR_ARRAY_LOAD = 6;
  public static final int RESOURCE_LOAD = 7;

  private ImageView mImageView;
  private TextView mValidityTextView;

  private PlatformBitmapFactory mBitmapFactory;
  private Bitmap mOriginalBitmap;
  private int mOriginalHeight;
  private int mOriginalWidth;
  private float mConversionFactor;
  private CloseableReference<Bitmap> mCurrentBitmapReference;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mBitmapFactory = Fresco.getImagePipelineFactory().getPlatformBitmapFactory();

    mOriginalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);
    mOriginalHeight = mOriginalBitmap.getHeight();
    mOriginalWidth = mOriginalBitmap.getWidth();

    // To set the size of the original image to the screen width
    DisplayMetrics metrics = getResources().getDisplayMetrics();
    mConversionFactor = metrics.widthPixels / (1.0f * mOriginalWidth);

    setupViews();
    loadBasicBitmap();
  }

  public void setupViews() {
    mImageView = (ImageView) findViewById(R.id.image);
    mValidityTextView = (TextView) findViewById(R.id.validity);
    Spinner spinner = (Spinner) findViewById(R.id.spinner);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        this,
        R.array.image_options,
        android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Cropping the central half of the image for testing
        int startX = mOriginalWidth / 4;
        int startY = mOriginalHeight / 4;
        int width = mOriginalWidth / 2;
        int height = mOriginalHeight / 2;

        switch (position) {
          case BASIC_LOAD:
            loadBasicBitmap();
            break;
          case CROPPED_LOAD:
            loadCroppedBitmap(startX, startY, width, height);
            break;
          case TRANSLATED_LOAD:
            loadTranslatedBitmap(startX, startY, width, height, startX, startY);
            break;
          case TRANSFORMED_LOAD:
            loadTransformedBitmap(
                startX,
                startY,
                width,
                height,
                getMatrix(0, 0, 0.5f, 0.5f, 60.0f));
            break;
          case SCALED_LOAD:
            loadScaledBitmap(width, height);
            break;
          case COLOR_ARRAY_LOAD:
            loadFromColorArray();
            break;
          case CROPPED_COLOR_ARRAY_LOAD:
            loadFromCroppedColorArray(startX, startY, width, height);
            break;
          case RESOURCE_LOAD:
          default:
            loadFromResources();
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });
  }

  public void loadFromResources() {
    mImageView.setImageResource(R.drawable.test_image);
    mImageView.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
    mImageView.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
    mValidityTextView.setText(R.string.ignoring_check);
  }

  public void loadBasicBitmap() {
    CloseableReference<Bitmap> bitmapRef = mBitmapFactory.createBitmap(mOriginalBitmap);
    setImageBitmap(bitmapRef);
    checkValid(bitmapRef, Bitmap.createBitmap(mOriginalBitmap));
  }

  public void loadCroppedBitmap(int x, int y, int width, int height) {
    CloseableReference<Bitmap> bitmapRef = mBitmapFactory.createBitmap(
        mOriginalBitmap,
        x,
        y,
        width,
        height);
    setImageBitmap(bitmapRef);
    checkValid(bitmapRef, Bitmap.createBitmap(mOriginalBitmap, x, y, width, height));
  }

  public void loadTranslatedBitmap(int x, int y, int width, int height, int tx, int ty) {
    Matrix matrix = getMatrix(tx, ty, 1, 1, 0);
    CloseableReference<Bitmap> bitmapRef = mBitmapFactory.createBitmap(
        mOriginalBitmap,
        x,
        y,
        width,
        height,
        matrix,
        true);
    setImageBitmap(bitmapRef);
    checkValid(
        bitmapRef,
        Bitmap.createBitmap(mOriginalBitmap, x, y, width, height, matrix, true));
  }

  public void loadTransformedBitmap(int x, int y, int width, int height, Matrix matrix) {
    CloseableReference<Bitmap> bitmapRef = mBitmapFactory.createBitmap(
        mOriginalBitmap,
        x,
        y,
        width,
        height,
        matrix,
        true);
    setImageBitmap(bitmapRef);
    checkValid(
        bitmapRef,
        Bitmap.createBitmap(mOriginalBitmap, x, y, width, height, matrix, true));
  }

  public void loadScaledBitmap(int finalWidth, int finalHeight) {
    CloseableReference<Bitmap> bitmapRef = mBitmapFactory.createScaledBitmap(
        mOriginalBitmap,
        finalWidth,
        finalHeight,
        true);
    setImageBitmap(bitmapRef);
    checkValid(
        bitmapRef,
        Bitmap.createScaledBitmap(mOriginalBitmap, finalWidth, finalHeight, true));
  }

  public void loadFromColorArray() {
    int[] pixels = new int[mOriginalWidth * mOriginalHeight];
    mOriginalBitmap.getPixels(pixels, 0, mOriginalWidth, 0, 0, mOriginalWidth, mOriginalHeight);
    CloseableReference<Bitmap> bitmapRef = mBitmapFactory.createBitmap(
        pixels,
        mOriginalWidth,
        mOriginalHeight,
        mOriginalBitmap.getConfig());
    setImageBitmap(bitmapRef);
    checkValid(
        bitmapRef,
        Bitmap.createBitmap(pixels, mOriginalWidth, mOriginalHeight, mOriginalBitmap.getConfig()));
  }

  public void loadFromCroppedColorArray(int x, int y, int width, int height) {
    int[] pixels = new int[width * height];
    mOriginalBitmap.getPixels(pixels, 0, width, x, y, width, height);
    CloseableReference<Bitmap> bitmapRef = mBitmapFactory.createBitmap(
        pixels,
        width,
        height,
        mOriginalBitmap.getConfig());
    setImageBitmap(bitmapRef);
    checkValid(
        bitmapRef,
        Bitmap.createBitmap(pixels, width, height, mOriginalBitmap.getConfig()));
  }

  public void setImageBitmap(CloseableReference<Bitmap> bitmapReference) {
    // Note: This is not a recommended way to load Bitmap.
    // This sample is intended to test the internal methods, and show how to use them.
    Bitmap bitmap = bitmapReference.get();
    mImageView.setImageBitmap(bitmap);
    mImageView.getLayoutParams().width = (int) (bitmap.getWidth() * mConversionFactor);
    mImageView.getLayoutParams().height = (int) (bitmap.getHeight() * mConversionFactor);

    if (mCurrentBitmapReference != null && mCurrentBitmapReference.isValid()) {
      mCurrentBitmapReference.close();
    }
    mCurrentBitmapReference = bitmapReference;
  }

  /**
   * Checks if the Bitmap created by Fresco's internal methods is same as the Bitmap created by
   * Bitmap class' methods.
   *
   * @param bitmapReference The Bitmap Reference created to check
   * @param reference The Bitmap generated from Bitmap class function
   */
  public void checkValid(CloseableReference<Bitmap> bitmapReference, Bitmap reference) {
    if (Build.VERSION.SDK_INT < 12) {
      mValidityTextView.setText(R.string.ignoring_check);
    } else if (bitmapReference.get().sameAs(reference)) {
      mValidityTextView.setText(R.string.correct_bitmap);
    } else {
      mValidityTextView.setText(R.string.incorrect_bitmap);
    }
  }

  public static Matrix getMatrix(int tx, int ty, float sx, float sy, float degrees) {
    Matrix matrix = new Matrix();
    matrix.setTranslate(tx, ty);
    matrix.setScale(sx, sy);
    matrix.setRotate(degrees);
    return matrix;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mCurrentBitmapReference != null && mCurrentBitmapReference.isValid()) {
      mCurrentBitmapReference.close();
    }
  }
}
