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

### Executors

When subscribing to a data source, an executor must be provided. The purpose of executors is to execute runnables (in our case the subscriber callback methods) on a specific thread and with specific policy.
Fresco provides several [executors] (https://github.com/facebook/fresco/tree/0f3d52318631f2125e080d2a19f6fa13a31efb31/fbcore/src/main/java/com/facebook/common/executors) and one should carefully choose which one to be used:

* If you need to do any UI stuff from your callback (accessing views, drawables, etc.), you must use `UiThreadExecutorService.getInstance()`. Android view system is not thread safe and is only to be accessed from the main thread (the UI thread).
* If the callback is lightweight, and does not do any UI related stuff, you can simply use `CallerThreadExecutor.getInstance()`. This executor executes runnables on the caller's thread. Depending on what is the calling thread, callback may be executed either on the UI or a background thread. There are no guarantees which thread it is going to be and because of that this executor should be used with great caution. And again, only for lightweight non-UI related stuff. 
* If you need to do some expensive non-UI related work (database access, disk read/write, or any other slow operation), this should NOT be done either with `CallerThreadExecutor` nor with the `UiThreadExecutorService`, but with one of the background thread executors. See [DefaultExecutorSupplier.forBackgroundTasks] (https://github.com/facebook/fresco/blob/0f3d52318631f2125e080d2a19f6fa13a31efb31/imagepipeline/src/main/java/com/facebook/imagepipeline/core/DefaultExecutorSupplier.java) for an example implementation.

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

If you want to deviate from the example above and assign the `CloseableReference` to another variable somewhere else, you can. Just be sure to [follow the rules](closeable-references.html).

