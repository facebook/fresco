---
id: datasources-datasubscribers
title: DataSources and DataSubscribers
layout: docs
permalink: /docs/datasources-datasubscribers.html
prev: using-image-pipeline.html
next: closeable-references.html
---

A [DataSource](../javadoc/reference/com/facebook/datasource/DataSource.html) is, like a Java [Future](http://developer.android.com/reference/java/util/concurrent/Future.html), the result of an asynchronous computation. The different is that, unlike a Future, a DataSource can return you a whole series of results from a single command, not just one.

After submitting an image request, the image pipeline returns a data source. To get a result out if it, you need to use a [DataSubscriber](../javadoc/reference/com/facebook/datasource/DataSubscriber.html).

### To get encoded image...

```java
    DataSource<CloseableReference<PooledByteBuffer>> dataSource =
        mImagePipeline.fetchEncodedImage(imageRequest, CALLER_CONTEXT);

    DataSubscriber<CloseableReference<PooledByteBuffer>> dataSubscriber =
        new BaseDataSubscriber<CloseableReference<PooledByteBuffer>>() {
          @Override
          protected void onNewResultImpl(
              DataSource<CloseableReference<PooledByteBuffer>> dataSource) {
            if (!dataSource.isFinished()) {
              return;
            }
            CloseableReference<PooledByteBuffer> buffRef = dataSource.getResult();
            if (buffRef != null) {
              PooledByteBufferInputStream is = new PooledByteBufferInputStream(buffRef.get());
              try {
                ImageFormat imageFormat = ImageFormatChecker.getImageFormat(is);
                // TODO: write input stream to file
                ...
              } catch (...) {
                ...
              } finally {
                Closeables.closeQuietly(is);
                CloseableReference.closeSafely(buffRef);
              }
            }
          }

          @Override
          protected void onFailureImpl(
              DataSource<CloseableReference<PooledByteBuffer>> dataSource) {
            ...
          }
        };

    dataSource.subscribe(dataSubscriber, executor);
```


### I just want a bitmap...

If your request to the pipeline is for a decoded image - an Android [Bitmap](http://developer.android.com/reference/android/graphics/Bitmap.html), you can take advantage of our easier-to-use [BaseBitmapDataSubscriber](../javadoc/reference/com/facebook/imagepipeline/datasource/BaseBitmapDataSubscriber):

```java
dataSource.subscribe(new BaseBitmapDataSubscriber() {
    @Override
    public void onNewResultImpl(@Nullable Bitmap bitmap) {
	   // You can use the bitmap in only limited ways
      // No need to do any cleanup.
    }
 
    @Override
    public void onFailureImpl(DataSource dataSource) {
      // No cleanup required here.
    }
  },
  executor);
```

A snap to use, right? There are caveats.

You can not use this subscriber for animated images.

You can **not** assign the bitmap to any variable not in the scope of the `onNewResultImpl` method. The reason is that, after the subscriber has finished executing, the image pipeline will recycle the bitmap and free its memory. If you try to draw the bitmap after that, your app will crash with an `IllegalStateException.`

You can still safely pass the Bitmap to an Android [notification](https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#setLargeIcon\(android.graphics.Bitmap\)) or [remote view](http://developer.android.com/reference/android/widget/RemoteViews.html#setImageViewBitmap\(int, android.graphics.Bitmap\)). If Android needs your Bitmap in order to pass it to a system process, it makes a copy of the Bitmap data in ashmem - the same heap used by Fresco. So Fresco's automatic cleanup will work without issue.

### General-purpose solution

If you want to keep the bitmap around, you can't use raw Bitmaps at all. You must make use of [closeable references](closeable-references.html) and the [BaseDataSubscriber](../javadoc/reference/com/facebook/datasource/BaseDataSubscriber.html):

```java
DataSubscriber dataSubscriber =
    new BaseDataSubscriber<CloseableReference<CloseableImage>>() {
  @Override
  public void onNewResultImpl(
      DataSource<CloseableReference<CloseableImage>> dataSource) {
      
    if (!dataSource.isFinished()) {
      FLog.v("Not yet finished - this is just another progressive scan.");
    }  
      
    CloseableReference<CloseableImage> imageReference = dataSource.getResult();
    if (imageReference != null) {
      try {
        CloseableImage image = imageReference.get();
        // do something with the image
      } finally {
        imageReference.close();
      }
    }
  }
  @Override
  public void onFailureImpl(DataSource dataSource) {
    Throwable throwable = dataSource.getFailureCause();
    // handle failure
  }
};

dataSource.subscribe(dataSubscriber, executor);
```

### For executor

```java
    Executor executor = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
```

If you want to deviate from the example above and assign the `CloseableReference` to another variable somewhere else, you can. Just be sure to [follow the rules](closeable-references.html).

