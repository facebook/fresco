---
id: gotchas
title: Gotchas
layout: docs
permalink: /docs/gotchas.html
prev: troubleshooting.html
next: using-other-network-layers.html
---

#### Don't use ScrollViews

If you want to scroll through a long list of images, you should use a [RecyclerView](http://developer.android.com/reference/android/support/v7/widget/RecyclerView.html), [ListView](https://developer.android.com/reference/android/widget/ListView.html), or [GridView](https://developer.android.com/reference/android/widget/GridView.html). All of these re-use their child views continually as you scroll through them. Fresco descendant views receive the system events that let them manage memory correctly.

`ScrollView` does not do this. Thus, Fresco views aren't told when they have gone off-screen, and hold onto their image memory until your Fragment or Activity is stopped. Your app will be at a much greater risk of OOMs.

#### Don't downcast

It is tempting to downcast objects returned by Fresco classes into actual objects that appear to give you greater control. At best, this will result in fragile code that gets broken in next release; at worst, it will lead to very subtle bugs.

#### Don't use getTopLevelDrawable

`DraweeHierarchy.getTopLevelDrawable()` should **only** be used by DraweeViews. Client code should almost never interact with it. 

The sole exception is [custom views](writing-custom-views.html). Even there, the top-level drawable should never be downcast. We may change the actual type of the drawable in future releases.

#### Don't re-use DraweeHierarchies

Never call ```DraweeView.setHierarchy``` with the same argument on two different views. Hierarchies are made up of Drawables, and Drawables on Android cannot be shared among multiple views.

#### Don't use Drawables in more than one DraweeHierarchy

This is for the same reason as the above. Drawables cannot be shared in multiple views.

You are completely free, of course, to use the same resourceID in multiple hierarchies and views. Android will create a separate instance of each Drawable for each view.

#### Do not control hierarchy directly

Do not interact with `SettableDraweeHierarchy` methods (`reset`, `setImage`, ...). Those are to be used by controller only.

#### Don't set images directly on a DraweeView

Currently ```DraweeView``` is a subclass of Android's ImageView. This has various methods to set an image (such as setImageBitmap, setImageDrawable)

If you set an image directly, you will completely lose your ```DraweeHierarchy```, and will not get any results from the image pipeline.

#### Don't use ImageView attributes or methods with DraweeView

Any XML attribute or method of ImageView not found in [View](http://developer.android.com/reference/android/view/View.html) will not work on a DraweeView. Typical cases are `scaleType`, `src`, etc. Don't use those. DraweeView has its own counterparts as explained in the other sections of this documentation. Any ImageView attrribute or method will be removed in the upcoming release, so please don't use those.

#### Image is displayed with repeated edges

This is a known limitation when rounding is used. See [Rounding](http://frescolib.org/docs/rounded-corners-and-circles.html#_) for more information and how to workaround.
