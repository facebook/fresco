/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import bolts.Task;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.CacheKeyUtil;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.time.Clock;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.MediaVariations;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

public class MediaVariationsIndexDatabase implements MediaVariationsIndex {

  private static final String TAG = MediaVariationsIndexDatabase.class.getSimpleName();

  private static final String[] PROJECTION = {
      IndexEntry.COLUMN_NAME_CACHE_CHOICE,
      IndexEntry.COLUMN_NAME_CACHE_KEY,
      IndexEntry.COLUMN_NAME_WIDTH,
      IndexEntry.COLUMN_NAME_HEIGHT
  };

  private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + IndexEntry.TABLE_NAME;
  private static final String WHERE_CLAUSE_DATE_BEFORE = IndexEntry.COLUMN_NAME_DATE + " < ?";
  private static final long MILLIS_IN_ONE_DAY = TimeUnit.DAYS.toMillis(1);
  private static final long MILLIS_IN_FIVE_DAYS = TimeUnit.DAYS.toMillis(5);

  @GuardedBy("MediaVariationsIndexDatabase.class")
  private final LazyIndexDbOpenHelper mDbHelper;
  private final Executor mReadExecutor;
  private final Executor mWriteExecutor;
  private final Clock mClock;

  private long mLastTrimTimestamp;

  public MediaVariationsIndexDatabase(
      Context context, Executor readExecutor, Executor writeExecutor, Clock clock) {
    mDbHelper = new LazyIndexDbOpenHelper(context);
    mReadExecutor = readExecutor;
    mWriteExecutor = writeExecutor;
    mClock = clock;
  }

  @Override
  public Task<MediaVariations> getCachedVariants(
      final String mediaId,
      final MediaVariations.Builder mediaVariationsBuilder) {
    try {
      return Task.call(
          new Callable<MediaVariations>() {
            @Override
            public MediaVariations call() throws Exception {
              return getCachedVariantsSync(mediaId, mediaVariationsBuilder);
            }
          },
          mReadExecutor);
    } catch (Exception exception) {
      FLog.w(TAG, exception, "Failed to schedule query task for %s", mediaId);
      return Task.forError(exception);
    }
  }

  @VisibleForTesting
  protected MediaVariations getCachedVariantsSync(
      String mediaId,
      MediaVariations.Builder mediaVariationsBuilder) {
    synchronized (MediaVariationsIndexDatabase.class) {
      SQLiteDatabase db = mDbHelper.getWritableDatabase();
      Cursor c = null;
      try {
        String selection = IndexEntry.COLUMN_NAME_MEDIA_ID + " = ?";
        String[] selectionArgs = {mediaId};

        c = db.query(
            IndexEntry.TABLE_NAME,
            PROJECTION,
            selection,
            selectionArgs,
            null, // groupBy
            null, // having
            null); // orderBy

        if (c.getCount() == 0) {
          return mediaVariationsBuilder.build();
        }

        final int columnIndexCacheKey = c.getColumnIndexOrThrow(IndexEntry.COLUMN_NAME_CACHE_KEY);
        final int columnIndexWidth = c.getColumnIndexOrThrow(IndexEntry.COLUMN_NAME_WIDTH);
        final int columnIndexHeight = c.getColumnIndexOrThrow(IndexEntry.COLUMN_NAME_HEIGHT);
        final int columnIndexCacheChoice =
            c.getColumnIndexOrThrow(IndexEntry.COLUMN_NAME_CACHE_CHOICE);

        while (c.moveToNext()) {
          String cacheChoiceStr = c.getString(columnIndexCacheChoice);

          mediaVariationsBuilder.addVariant(
              Uri.parse(c.getString(columnIndexCacheKey)),
              c.getInt(columnIndexWidth),
              c.getInt(columnIndexHeight),
              TextUtils.isEmpty(cacheChoiceStr)
                  ? null : ImageRequest.CacheChoice.valueOf(cacheChoiceStr));
        }

        return mediaVariationsBuilder.build();
      } catch (SQLException x) {
        FLog.e(TAG, x, "Error reading for %s", mediaId);
        throw x;
      } finally {
        if (c != null) {
          c.close();
        }
      }
    }
  }

  @Override
  public void saveCachedVariant(
      final String mediaId,
      final ImageRequest.CacheChoice cacheChoice,
      final CacheKey cacheKey,
      final EncodedImage encodedImage) {
    mWriteExecutor.execute(new Runnable() {
      @Override
      public void run() {
        saveCachedVariantSync(mediaId, cacheChoice, cacheKey, encodedImage);
      }
    });
  }

  protected void saveCachedVariantSync(
      final String mediaId,
      final ImageRequest.CacheChoice cacheChoice,
      final CacheKey cacheKey,
      final EncodedImage encodedImage) {
    synchronized (MediaVariationsIndexDatabase.class) {
      SQLiteDatabase db = mDbHelper.getWritableDatabase();
      long now = mClock.now();

      try {
        db.beginTransaction();

        ContentValues contentValues = new ContentValues();
        contentValues.put(IndexEntry.COLUMN_NAME_MEDIA_ID, mediaId);
        contentValues.put(IndexEntry.COLUMN_NAME_WIDTH, encodedImage.getWidth());
        contentValues.put(IndexEntry.COLUMN_NAME_HEIGHT, encodedImage.getHeight());
        contentValues.put(IndexEntry.COLUMN_NAME_CACHE_CHOICE, cacheChoice.name());
        contentValues.put(IndexEntry.COLUMN_NAME_CACHE_KEY, cacheKey.getUriString());
        contentValues
            .put(IndexEntry.COLUMN_NAME_RESOURCE_ID, CacheKeyUtil.getFirstResourceId(cacheKey));
        contentValues.put(IndexEntry.COLUMN_NAME_DATE, now);

        db.replaceOrThrow(IndexEntry.TABLE_NAME, null, contentValues);

        // Now remove old records from the table
        if (mLastTrimTimestamp <= now - MILLIS_IN_ONE_DAY) {
          db.delete(
              IndexEntry.TABLE_NAME,
              WHERE_CLAUSE_DATE_BEFORE,
              new String[] {Long.toString(now - MILLIS_IN_FIVE_DAYS)});
          mLastTrimTimestamp = now;
        }

        db.setTransactionSuccessful();
      } catch (Exception x) {
        FLog.e(TAG, x, "Error writing for %s", mediaId);
      } finally {
        try {
          db.endTransaction();
        } catch (SQLiteException e) {
          // Some devices (like the Advan S4Z) throws a SQLiteException when endTransaction()
          // is called. Supposedly due to no disk space although this would be more likely to
          // cause an exception during the insert.
        }
      }
    }
  }

  private static final class IndexEntry implements BaseColumns {

    public static final String TABLE_NAME = "media_variations_index";
    public static final String COLUMN_NAME_MEDIA_ID = "media_id";
    public static final String COLUMN_NAME_WIDTH = "width";
    public static final String COLUMN_NAME_HEIGHT = "height";
    public static final String COLUMN_NAME_CACHE_CHOICE = "cache_choice";
    public static final String COLUMN_NAME_CACHE_KEY = "cache_key";
    public static final String COLUMN_NAME_RESOURCE_ID = "resource_id";
    public static final String COLUMN_NAME_DATE = "date";
  }

  private static class LazyIndexDbOpenHelper {

    private final Context mContext;
    private @Nullable IndexDbOpenHelper mIndexDbOpenHelper;

    private LazyIndexDbOpenHelper(Context context) {
      mContext = context;
    }

    public synchronized SQLiteDatabase getWritableDatabase() {
      if (mIndexDbOpenHelper == null) {
        mIndexDbOpenHelper = new IndexDbOpenHelper(mContext);
      }
      return mIndexDbOpenHelper.getWritableDatabase();
    }
  }

  private static class IndexDbOpenHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "FrescoMediaVariationsIndex.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String SQL_CREATE_ENTRIES =
        "CREATE TABLE " + IndexEntry.TABLE_NAME + " (" +
            IndexEntry._ID + " INTEGER PRIMARY KEY," +
            IndexEntry.COLUMN_NAME_MEDIA_ID + TEXT_TYPE + "," +
            IndexEntry.COLUMN_NAME_WIDTH + INTEGER_TYPE + "," +
            IndexEntry.COLUMN_NAME_HEIGHT + INTEGER_TYPE + "," +
            IndexEntry.COLUMN_NAME_CACHE_CHOICE + TEXT_TYPE + "," +
            IndexEntry.COLUMN_NAME_CACHE_KEY + TEXT_TYPE + "," +
            IndexEntry.COLUMN_NAME_RESOURCE_ID + TEXT_TYPE + " UNIQUE," +
            IndexEntry.COLUMN_NAME_DATE + INTEGER_TYPE + " )";
    private static final String SQL_CREATE_INDEX =
        "CREATE INDEX index_media_id ON " + IndexEntry.TABLE_NAME + " (" +
            IndexEntry.COLUMN_NAME_MEDIA_ID + ")";

    public IndexDbOpenHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.beginTransaction();
      try {
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_INDEX);
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.beginTransaction();
      try {
        db.execSQL(SQL_DROP_TABLE);
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
      onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      onUpgrade(db, oldVersion, newVersion);
    }
  }
}
