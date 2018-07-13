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
package com.facebook.fresco.samples.showcase;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.facebook.fresco.samples.showcase.drawee.DraweeHierarchyFragment;
import com.facebook.fresco.samples.showcase.drawee.DraweeMediaPickerFragment;
import com.facebook.fresco.samples.showcase.drawee.DraweeRecyclerViewFragment;
import com.facebook.fresco.samples.showcase.drawee.DraweeRotationFragment;
import com.facebook.fresco.samples.showcase.drawee.DraweeRoundedCornersFragment;
import com.facebook.fresco.samples.showcase.drawee.DraweeScaleTypeFragment;
import com.facebook.fresco.samples.showcase.drawee.DraweeSimpleFragment;
import com.facebook.fresco.samples.showcase.drawee.DraweeSpanSimpleTextFragment;
import com.facebook.fresco.samples.showcase.drawee.RetainingDataSourceSupplierFragment;
import com.facebook.fresco.samples.showcase.drawee.transition.DraweeTransitionFragment;
import com.facebook.fresco.samples.showcase.imageformat.color.ImageFormatColorFragment;
import com.facebook.fresco.samples.showcase.imageformat.datauri.ImageFormatDataUriFragment;
import com.facebook.fresco.samples.showcase.imageformat.gif.ImageFormatGifFragment;
import com.facebook.fresco.samples.showcase.imageformat.keyframes.ImageFormatKeyframesFragment;
import com.facebook.fresco.samples.showcase.imageformat.override.ImageFormatOverrideExample;
import com.facebook.fresco.samples.showcase.imageformat.pjpeg.ImageFormatProgressiveJpegFragment;
import com.facebook.fresco.samples.showcase.imageformat.svg.ImageFormatSvgFragment;
import com.facebook.fresco.samples.showcase.imageformat.webp.ImageFormatWebpFragment;
import com.facebook.fresco.samples.showcase.imagepipeline.ImagePipelineBitmapFactoryFragment;
import com.facebook.fresco.samples.showcase.imagepipeline.ImagePipelineNotificationFragment;
import com.facebook.fresco.samples.showcase.imagepipeline.ImagePipelinePostProcessorFragment;
import com.facebook.fresco.samples.showcase.imagepipeline.ImagePipelinePrefetchFragment;
import com.facebook.fresco.samples.showcase.imagepipeline.ImagePipelineQualifiedResourceFragment;
import com.facebook.fresco.samples.showcase.imagepipeline.ImagePipelineRegionDecodingFragment;
import com.facebook.fresco.samples.showcase.imagepipeline.ImagePipelineResizingFragment;
import com.facebook.fresco.samples.showcase.imagepipeline.PartialRequestFragment;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.samples.showcase.misc.WelcomeFragment;
import com.facebook.fresco.samples.showcase.settings.SettingsFragment;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {

  private static final int INITIAL_NAVDRAWER_ITEM_ID = R.id.nav_welcome;

  private static final String KEY_SELECTED_NAVDRAWER_ITEM_ID = "selected_navdrawer_item_id";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.setDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    if (savedInstanceState == null) {
      int selectedItem = PreferenceManager.getDefaultSharedPreferences(this).getInt(
          KEY_SELECTED_NAVDRAWER_ITEM_ID,
          INITIAL_NAVDRAWER_ITEM_ID);
      handleNavigationItemClick(selectedItem);
      navigationView.setCheckedItem(selectedItem);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    maybeShowUriOverrideReminder();
  }

  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    // the support toolbar should probably do this by default
    final TypedArray styles =
        obtainStyledAttributes(R.style.AppTheme_Toolbar, new int[]{R.attr.colorControlNormal});
    try {
      int tintColor = styles.getColor(0, Color.BLACK);
      for (int i = 0; i < menu.size(); i++) {
        Drawable icon = menu.getItem(i).getIcon();
        if (icon != null) {
          DrawableCompat.setTint(icon, tintColor);
        }
      }
    } finally {
      styles.recycle();
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_settings) {
      showFragment(new SettingsFragment());
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    handleNavigationItemClick(item.getItemId());
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  private void handleNavigationItemClick(int itemId) {
    ShowcaseFragment fragment;
    switch (itemId) {
      // Drawee
      case R.id.nav_drawee_simple:
        fragment = new DraweeSimpleFragment();
        break;
      case R.id.nav_drawee_media_picker:
        fragment = new DraweeMediaPickerFragment();
        break;
      case R.id.nav_drawee_scaletype:
        fragment = new DraweeScaleTypeFragment();
        break;
      case R.id.nav_drawee_span_simple:
        fragment = new DraweeSpanSimpleTextFragment();
        break;
      case R.id.nav_drawee_rounded_corners:
        fragment = new DraweeRoundedCornersFragment();
        break;
      case R.id.nav_drawee_hierarchy:
        fragment = new DraweeHierarchyFragment();
        break;
      case R.id.nav_drawee_rotation:
        fragment = new DraweeRotationFragment();
        break;
      case R.id.nav_drawee_recycler:
        fragment = new DraweeRecyclerViewFragment();
        break;
      case R.id.nav_drawee_transition:
        fragment = new DraweeTransitionFragment();
        break;
      case R.id.nav_drawee_retaining_supplier:
        fragment = new RetainingDataSourceSupplierFragment();
        break;

      // Imagepipline
      case R.id.nav_imagepipeline_notification:
        fragment = new ImagePipelineNotificationFragment();
        break;
      case R.id.nav_imagepipeline_postprocessor:
        fragment = new ImagePipelinePostProcessorFragment();
        break;
      case R.id.nav_imagepipeline_prefetch:
        fragment = new ImagePipelinePrefetchFragment();
        break;
      case R.id.nav_imagepipeline_resizing:
        fragment = new ImagePipelineResizingFragment();
        break;
      case R.id.nav_imagepipeline_qualified_resource:
        fragment = new ImagePipelineQualifiedResourceFragment();
        break;
      case R.id.nav_imagepipeline_partial_request:
        fragment = new PartialRequestFragment();
        break;
      case R.id.nav_imagepipeline_bitmap_factory:
        fragment = new ImagePipelineBitmapFactoryFragment();
        break;
      case R.id.nav_imagepipeline_region_decoding:
        fragment = new ImagePipelineRegionDecodingFragment();
        break;

      // Image Formats
      case R.id.nav_format_pjpeg:
        fragment = new ImageFormatProgressiveJpegFragment();
        break;
      case R.id.nav_format_color:
        fragment = new ImageFormatColorFragment();
        break;
      case R.id.nav_format_gif:
        fragment = new ImageFormatGifFragment();
        break;
      case R.id.nav_format_webp:
        fragment = new ImageFormatWebpFragment();
        break;
      case R.id.nav_format_svg:
        fragment = new ImageFormatSvgFragment();
        break;
      case R.id.nav_format_keyframes:
        fragment = new ImageFormatKeyframesFragment();
        break;
      case R.id.nav_format_override:
        fragment = new ImageFormatOverrideExample();
        break;
      case R.id.nav_format_datauri:
        fragment = new ImageFormatDataUriFragment();
        break;

      // More
      case R.id.nav_welcome:
        fragment = new WelcomeFragment();
        break;
      case R.id.nav_action_settings:
        fragment = new SettingsFragment();
        break;
      default:
        // Default to the welcome fragment
        fragment = new WelcomeFragment();
    }
    showFragment(fragment);

    // Save the item if it's not the settings fragment
    if (itemId != R.id.nav_action_settings) {
      PreferenceManager.getDefaultSharedPreferences(this)
          .edit()
          .putInt(KEY_SELECTED_NAVDRAWER_ITEM_ID, itemId)
          .apply();
    }
  }

  /**
   * Utility method to display a specific Fragment. If the tag is not null we add a backstack
   *
   * @param fragment The Fragment to add
   */
  private void showFragment(ShowcaseFragment fragment) {
    final FragmentTransaction fragmentTransaction = getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.content_main, (Fragment) fragment);
    if (fragment.getBackstackTag() != null) {
      fragmentTransaction.addToBackStack(fragment.getBackstackTag());
    }
    fragmentTransaction.commit();

    setTitle(fragment.getTitleId());
  }

  private void maybeShowUriOverrideReminder() {
    if (ImageUriProvider.getInstance(this).getUriOverride() == null) {
      return;
    }
    final Snackbar snackbar = Snackbar.make(
        findViewById(R.id.content_main),
        R.string.snackbar_uri_override_reminder_text,
        Snackbar.LENGTH_LONG);
    snackbar.setAction(
        R.string.snackbar_uri_override_reminder_change_button,
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            showFragment(new SettingsFragment());
          }
        });
    snackbar.show();
  }
}
