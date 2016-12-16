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
package com.facebook.fresco.samples.showcase.drawee;

import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.R;

/**
 * Simple drawee fragment that illustrates different scale types
 */
public class DraweeScaleTypeFragment extends Fragment {

  private static final Uri URI_1 =
      Uri.parse("http://frescolib.org/static/sample-images/animal_a_m.jpg");
  private static final Uri URI_2 =
      Uri.parse("http://frescolib.org/static/sample-images/animal_d_m.jpg");

  private static final Entry[] SPINNER_ENTRIES = new Entry[]{
      new Entry(ScaleType.CENTER, "center", null),
      new Entry(ScaleType.CENTER_CROP, "center_crop", null),
      new Entry(ScaleType.CENTER_INSIDE, "center_inside", null),
      new Entry(ScaleType.FIT_CENTER, "fit_center", null),
      new Entry(ScaleType.FIT_START, "fit_start", null),
      new Entry(ScaleType.FIT_END, "fit_end", null),
      new Entry(ScaleType.FIT_XY, "fit_xy", null),
      new Entry(ScaleType.FOCUS_CROP, "focus_crop (0, 0)", new PointF(0, 0)),
      new Entry(ScaleType.FOCUS_CROP, "focus_crop (1, 0.5)", new PointF(1, 0.5f))
  };

  private SimpleDraweeView mDraweeTop1;
  private SimpleDraweeView mDraweeTop2;
  private SimpleDraweeView mDraweeMain;
  private Spinner mSpinner;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_drawee_scale_type, container, false);

    mDraweeTop1 = (SimpleDraweeView) view.findViewById(R.id.drawee_view_top_1);
    mDraweeTop2 = (SimpleDraweeView) view.findViewById(R.id.drawee_view_top_2);
    mDraweeMain = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    mSpinner = (Spinner) view.findViewById(R.id.spinner);

    mDraweeTop1.setImageURI(URI_1);
    mDraweeTop1.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        changeMainDraweeUri(URI_1);
      }
    });

    mDraweeTop2.setImageURI(URI_2);
    mDraweeTop2.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        changeMainDraweeUri(URI_2);
      }
    });

    changeMainDraweeUri(URI_1);

    mSpinner.setAdapter(new SimpleScaleTypeAdapter());
    mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final Entry spinnerEntry = SPINNER_ENTRIES[position];
        changeMainDraweeScaleType(spinnerEntry.scaleType, spinnerEntry.focusPoint);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    mSpinner.setSelection(0);

    return view;
  }

  private void changeMainDraweeUri(Uri uri) {
    mDraweeMain.setImageURI(uri);
  }

  private void changeMainDraweeScaleType(ScaleType scaleType, @Nullable PointF focusPoint) {
    final GenericDraweeHierarchy hierarchy = mDraweeMain.getHierarchy();
    hierarchy.setActualImageScaleType(scaleType);
    hierarchy.setActualImageFocusPoint(focusPoint != null ? focusPoint : new PointF(0.5f, 0.5f));
  }

  private class SimpleScaleTypeAdapter extends BaseAdapter {

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
      final LayoutInflater layoutInflater = getLayoutInflater(null);

      final View view = convertView != null
          ? convertView
          : layoutInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);

      final TextView textView = (TextView) view.findViewById(android.R.id.text1);
      textView.setText(SPINNER_ENTRIES[position].description);

      return view;
    }
  }

  private static class Entry {

    public final ScaleType scaleType;
    public final String description;
    public final PointF focusPoint;

    public Entry(
        ScaleType scaleType,
        String description,
        PointF focusPoint) {
      this.scaleType = scaleType;
      this.description = description;
      this.focusPoint = focusPoint;
    }
  }
}
