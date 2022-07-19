/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.callercontext;

import android.os.Parcel;
import android.os.Parcelable;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Context chain that adds additional metadata about the current product and infra that's active.
 * These can be chained together, which allows us to identify a given product / infra.
 *
 * <p>Each element in the chain is a simple tag / value pair. Tags can be arbitrary strings that can
 * be used to filter later on. For example, for product metadata, use {@link #TAG_PRODUCT}, for
 * infra {@link #TAG_INFRA} and for both (e.g. if you'd like to report the parent surface, like feed
 * or profile) {@link #TAG_PRODUCT_AND_INFRA}.
 *
 * <p>The context chain can be serialized and used for logging. An example for a serialized chain:
 * "pi:FEED/p:feature_x/i:HeaderComponentSpec/p:profile_photo/i:MyFrescoImage"
 */
public class ContextChain implements Parcelable {

  public static final String TAG_PRODUCT = "p";
  public static final String TAG_INFRA = "i";
  public static final String TAG_PRODUCT_AND_INFRA = TAG_PRODUCT + TAG_INFRA;

  private static final char PARENT_SEPARATOR = '/';

  private final String mTag;
  private final String mName;
  private final int mLevel;
  private final @Nullable ContextChain mParent;

  // Allows setting arbitrary key:value String pairs without polluting Context Chain names.
  private @Nullable Map<String, Object> mExtraData;

  private @Nullable String mSerializedString;

  public ContextChain(
      final String tag,
      final String name,
      final @Nullable Map<String, String> extraData,
      final @Nullable ContextChain parent) {
    mTag = tag;
    mName = name;
    mLevel = parent != null ? parent.mLevel + 1 : 0;
    mParent = parent;

    Map<String, Object> parentExtraData = null;
    if (parent != null) {
      parentExtraData = parent.getExtraData();
    }
    if (parentExtraData != null) {
      mExtraData = new HashMap<>(parentExtraData);
    }

    if (extraData != null) {
      if (mExtraData == null) {
        mExtraData = new HashMap<>();
      }
      mExtraData.putAll(extraData);
    }
  }

  public ContextChain(final String tag, final String name, final @Nullable ContextChain parent) {
    this(tag, name, null, parent);
  }

  protected ContextChain(Parcel in) {
    mTag = in.readString();
    mName = in.readString();
    mLevel = in.readInt();
    mParent = in.readParcelable(ContextChain.class.getClassLoader());
  }

  public String getName() {
    return mName;
  }

  public String getTag() {
    return mTag;
  }

  @Nullable
  public Map<String, Object> getExtraData() {
    return mExtraData;
  }

  @Nullable
  public ContextChain getParent() {
    return mParent;
  }

  /** Get the top level context chain, which can be the current context chain itself. */
  public ContextChain getRootContextChain() {
    return mParent == null ? this : mParent.getRootContextChain();
  }

  public @Nullable String getStringExtra(String key) {
    if (mExtraData == null) {
      return null;
    }
    Object val = mExtraData.get(key);
    return val == null ? null : String.valueOf(val);
  }

  public void putObjectExtra(String key, Object value) {
    if (mExtraData == null) {
      mExtraData = new HashMap<>();
    }
    mExtraData.put(key, value);
  }

  @Override
  public String toString() {
    if (mSerializedString == null) {
      mSerializedString = mTag + ":" + mName;
      if (mParent != null) {
        mSerializedString = mParent.toString() + PARENT_SEPARATOR + mSerializedString;
      }
    }
    return mSerializedString;
  }

  public String[] toStringArray() {
    String[] result = new String[mLevel + 1];
    ContextChain current = this;
    for (int i = mLevel; i >= 0; i--) {
      Preconditions.checkNotNull(current, "ContextChain level mismatch, this should not happen.");
      result[i] = current.mTag + ":" + current.mName;
      current = current.mParent;
    }
    return result;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ContextChain other = (ContextChain) obj;
    return Objects.equal(mTag, other.mTag)
        && Objects.equal(mName, other.mName)
        && mLevel == other.mLevel
        && (mParent == other.mParent || (mParent != null && mParent.equals(other.mParent)));
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mTag != null ? mTag.hashCode() : 0);
    result = 31 * result + (mName != null ? mName.hashCode() : 0);
    result = 31 * result + mLevel;
    result = 31 * result + (mParent != null ? mParent.hashCode() : 0);
    return result;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(mTag);
    dest.writeString(mName);
    dest.writeInt(mLevel);
    dest.writeParcelable(mParent, flags);
  }

  public static final Creator<ContextChain> CREATOR =
      new Creator<ContextChain>() {
        @Override
        public ContextChain createFromParcel(Parcel in) {
          return new ContextChain(in);
        }

        @Override
        public ContextChain[] newArray(int size) {
          return new ContextChain[size];
        }
      };
}
