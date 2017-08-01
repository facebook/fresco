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
package com.facebook.samples.scrollperf;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.facebook.samples.scrollperf.fragments.MainFragment;
import com.facebook.samples.scrollperf.fragments.SettingsFragment;
import com.facebook.samples.scrollperf.util.SizeUtil;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SizeUtil.initSizeData(this);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle(R.string.app_name);
    setSupportActionBar(toolbar);
    if (savedInstanceState == null) {
      final MainFragment mainFragment = new MainFragment();
      getSupportFragmentManager().beginTransaction()
              .add(R.id.anchor_point, mainFragment, MainFragment.TAG)
              .commit();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_settings) {
      final SettingsFragment settingsFragment = new SettingsFragment();
      getSupportFragmentManager().beginTransaction()
              .replace(R.id.anchor_point, settingsFragment, SettingsFragment.TAG)
              .addToBackStack(SettingsFragment.TAG)
              .commit();
    }
    return super.onOptionsItemSelected(item);
  }
}
