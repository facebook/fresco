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
package com.facebook.fresco.samples.showcase.common;

import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.facebook.drawee.drawable.ScalingUtils;

public class SimpleScaleTypeAdapter extends BaseAdapter {

  private static final Entry[] SPINNER_ENTRIES = new Entry[]{
      new Entry(ScalingUtils.ScaleType.CENTER, "center", null),
      new Entry(ScalingUtils.ScaleType.CENTER_CROP, "center_crop", null),
      new Entry(ScalingUtils.ScaleType.CENTER_INSIDE, "center_inside", null),
      new Entry(ScalingUtils.ScaleType.FIT_CENTER, "fit_center", null),
      new Entry(ScalingUtils.ScaleType.FIT_START, "fit_start", null),
      new Entry(ScalingUtils.ScaleType.FIT_END, "fit_end", null),
      new Entry(ScalingUtils.ScaleType.FIT_XY, "fit_xy", null),
      new Entry(ScalingUtils.ScaleType.FOCUS_CROP, "focus_crop (0, 0)", new PointF(0, 0)),
      new Entry(ScalingUtils.ScaleType.FOCUS_CROP, "focus_crop (1, 0.5)", new PointF(1, 0.5f))
  };

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

    final TextView textView = (TextView) view.findViewById(android.R.id.text1);
    textView.setText(SPINNER_ENTRIES[position].description);

    return view;
  }

  public static class Entry {

    public final ScalingUtils.ScaleType scaleType;
    public final String description;
    public final @Nullable PointF focusPoint;

    private Entry(
        ScalingUtils.ScaleType scaleType,
        String description,
        @Nullable PointF focusPoint) {
      this.scaleType = scaleType;
      this.description = description;
      this.focusPoint = focusPoint;
    }
  }
}
