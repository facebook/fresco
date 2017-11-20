---
docid: images-in-notifications
title: Images in Notifications
layout: docs
permalink: /docs/images-in-notifications.html
---

If you need to display an image in a notification, you can use the `BaseBitmapDataSubscriber` for requesting a bitmap from the `ImagePipeline`. This is safe to be passed to a notification as the system will parcel it after the `NotificationManager#notify` method. This page explains a full sample on how to do this.

### Step by step

First create an `ImageRequest` with the URI:

```java
ImageRequest imageRequest = ImageRequest.fromUri("http://example.org/user/42/profile.jpg"));
```

Then create a `DataSource` and request the decoded image from the `ImagePipeline`:

```java
ImagePipeline imagePipeline = Fresco.getImagePipeline();
DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, null);
```

As a `DataSource` is similar to a `Future`, we need to add a `DataSubscriber` to handle the result. The `BaseBitmapDataSubscriber` abstracts some of the complexity away when dealing with `Bitmap`:

```java
dataSource.subscribe(
    new BaseBitmapDataSubscriber() {

      @Override
      protected void onNewResultImpl(Bitmap bitmap) {
        displayNotification(bitmap);
      }

      @Override
      protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
        // In general, failing to fetch the image should not keep us from displaying the
        // notification. We proceed without the bitmap.
        displayNotification(null);
      }
    },
    UiThreadImmediateExecutorService.getInstance());
}
```

The `displayNotification(Bitmap)` method then is similar to the 'normal' way to do this on Android:

```java
private void displayNotification(@Nullable Bitmap bitmap) {
  final NotificationCompat.Builder notificationBuilder =
      new NotificationCompat.Builder(getContext())
          .setSmallIcon(R.drawable.ic_done)
          .setLargeIcon(bitmap)
          .setContentTitle("Fresco Says Hello")
          .setContentText("Notification Text ...");

  final NotificationManager notificationManager =
      (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

  notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
}
```

### Full Sample

For the full sample see the `ImagePipelineNotificationFragment` in the showcase app: [ImagePipelineNotificationFragment.java](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/imagepipeline/ImagePipelineNotificationFragment.java)

![Showcase app with a notification](/static/images/docs/02-images-in-notifications-sample.png)
