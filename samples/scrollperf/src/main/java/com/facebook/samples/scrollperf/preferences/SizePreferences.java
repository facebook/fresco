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
package com.facebook.samples.scrollperf.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.util.SizeUtil;

/**
 * A simple Preference containing a SeekBar in order to select a size
 */
public class SizePreferences extends Preference implements SeekBar.OnSeekBarChangeListener {

  // We always use half of the width as default
  private static final int DEFAULT_SIZE_VALUE = SizeUtil.DISPLAY_WIDTH / 2;

  private SeekBar mSeekBar;

  private TextView mSeekBarValueTextView;
  private TextView mTitleView;

  private int mProgressValue;

  private int mMaxValue;

  public SizePreferences(
      Context context,
      AttributeSet attrs,
      int defStyleAttr,
      int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    setLayoutResource(R.layout.size_preference);
  }

  public SizePreferences(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public SizePreferences(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public void setSeekBarMaxValue(int maxValue) {
    mMaxValue = maxValue;
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);
    // We get the reference to the mSeekBar
    mSeekBar = (SeekBar) holder.findViewById(R.id.size_seek_bar);
    mSeekBar.setMax(mMaxValue);
    mSeekBar.setOnSeekBarChangeListener(this);
    mSeekBarValueTextView = (TextView) holder.findViewById(R.id.seek_bar_value);
    mTitleView = (TextView) holder.findViewById(R.id.title);
    mTitleView.setText(getTitle());
    // This is called after the initial value is set
    mSeekBar.setProgress(mProgressValue);
    updateCurrentValue(mProgressValue);
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    updateCurrentValue(progress);
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {

  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {

  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    super.onSetInitialValue(restorePersistedValue, defaultValue);
    final int valueToDisplay;
    if (restorePersistedValue) {
      valueToDisplay = getPersistedInt(DEFAULT_SIZE_VALUE);
    } else {
      valueToDisplay = (Integer) defaultValue;
    }
    updateCurrentValue(valueToDisplay);
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getInt(index, 0);
  }

  public void updateCurrentValue(int progress) {
    if (shouldPersist()) {
      persistInt(progress);
    }
    if (progress != mProgressValue) {
      mProgressValue = progress;
      notifyChanged();
    }
    if (mSeekBarValueTextView != null) {
      final String valueStr = getContext().getString(R.string.size_label_format, progress);
      mSeekBarValueTextView.setText(valueStr);
    }
  }
}
