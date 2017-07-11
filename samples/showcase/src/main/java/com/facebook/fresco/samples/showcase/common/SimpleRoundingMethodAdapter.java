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

import java.util.ArrayList;
import java.util.List;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.facebook.drawee.generic.RoundingParams;

public class SimpleRoundingMethodAdapter extends BaseAdapter {

  public static SimpleRoundingMethodAdapter createForAllRoundingMethods() {
    List<Entry> entries = new ArrayList<>();
    entries.add(new Entry(RoundingParams.RoundingMethod.OVERLAY_COLOR, "overlay_color"));
    entries.add(new Entry(RoundingParams.RoundingMethod.BITMAP_ONLY, "bitmap_only"));
    if (Build.VERSION.SDK_INT >= 21) {
      entries.add(new Entry(RoundingParams.RoundingMethod.OUTLINE, "outline"));
    }
    return new SimpleRoundingMethodAdapter(entries);
  }

  private final List<Entry> mEntries;

  private SimpleRoundingMethodAdapter(List<Entry> entries) {
    mEntries = entries;
  }

  @Override
  public int getCount() {
    return mEntries.size();
  }

  @Override
  public Object getItem(int position) {
    return mEntries.get(position);
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
    textView.setText(mEntries.get(position).description);

    return view;
  }

  public static class Entry {

    public final RoundingParams.RoundingMethod roundingMethod;
    public final String description;

    private Entry(
        RoundingParams.RoundingMethod roundingMethod,
        String description) {
      this.roundingMethod = roundingMethod;
      this.description = description;
    }
  }
}
