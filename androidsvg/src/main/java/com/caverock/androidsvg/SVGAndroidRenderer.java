/*
   Copyright 2013 Paul LeBeau, Cave Rock Software Ltd.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.caverock.androidsvg;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.util.Base64;
import android.util.Log;

import com.caverock.androidsvg.SVG.Box;
import com.caverock.androidsvg.SVG.ClipPath;
import com.caverock.androidsvg.SVG.Colour;
import com.caverock.androidsvg.SVG.CurrentColor;
import com.caverock.androidsvg.SVG.GradientElement;
import com.caverock.androidsvg.SVG.GradientSpread;
import com.caverock.androidsvg.SVG.Length;
import com.caverock.androidsvg.SVG.Line;
import com.caverock.androidsvg.SVG.Marker;
import com.caverock.androidsvg.SVG.NotDirectlyRendered;
import com.caverock.androidsvg.SVG.PaintReference;
import com.caverock.androidsvg.SVG.PathDefinition;
import com.caverock.androidsvg.SVG.PathInterface;
import com.caverock.androidsvg.SVG.Pattern;
import com.caverock.androidsvg.SVG.Rect;
import com.caverock.androidsvg.SVG.SolidColor;
import com.caverock.androidsvg.SVG.Stop;
import com.caverock.androidsvg.SVG.Style;
import com.caverock.androidsvg.SVG.Style.FontStyle;
import com.caverock.androidsvg.SVG.Style.TextAnchor;
import com.caverock.androidsvg.SVG.Style.TextDecoration;
import com.caverock.androidsvg.SVG.Style.VectorEffect;
import com.caverock.androidsvg.SVG.SvgContainer;
import com.caverock.androidsvg.SVG.SvgElement;
import com.caverock.androidsvg.SVG.SvgElementBase;
import com.caverock.androidsvg.SVG.SvgLinearGradient;
import com.caverock.androidsvg.SVG.SvgObject;
import com.caverock.androidsvg.SVG.SvgPaint;
import com.caverock.androidsvg.SVG.SvgRadialGradient;
import com.caverock.androidsvg.SVG.TextContainer;
import com.caverock.androidsvg.SVG.TextSequence;
import com.caverock.androidsvg.SVG.Unit;

/**
 * The rendering part of AndroidSVG.
 * <p>
 * All interaction with AndroidSVG is via the SVG class.  You may ignore this class.
 * 
 * @hide
 */

public class SVGAndroidRenderer
{
   private static final String  TAG = "SVGAndroidRenderer";

   private Canvas   canvas;
   private Box      canvasViewPort;
   // dots per inch. Needed for accurate conversion of length values that have real world units,
   // such as "cm".
   private float    dpi;
   private boolean  directRenderingMode;

   // Renderer state
   private SVG                  document;
   private RendererState        state;
   private Stack<RendererState> stateStack;  // Keeps track of render state as we render
   
   // Keep track of element stack while rendering.

   // The 'render parent' for elements like Symbol cf. file parent
   private Stack<SvgContainer>  parentStack;
   // Keeps track of current transform as we descend into element tree
   private Stack<Matrix>        matrixStack;

   // Canvas stack for when we are processing mask elements
   private Stack<Canvas>  canvasStack;
   private Stack<Bitmap>  bitmapStack;


   private static final float  BEZIER_ARC_FACTOR = 0.5522847498f;

   // The feColorMatrix luminance-to-alpha coefficient. Used for <mask>s.
   // Using integer arithmetic for a little extra speed.
   private static final int  LUMINANCE_FACTOR_SHIFT = 15;
   private static final int  LUMINANCE_TO_ALPHA_RED = (int)(0.2125f
           * (1 << LUMINANCE_FACTOR_SHIFT));
   private static final int  LUMINANCE_TO_ALPHA_GREEN = (int)(0.7154f
           * (1 << LUMINANCE_FACTOR_SHIFT));
   private static final int  LUMINANCE_TO_ALPHA_BLUE = (int)(0.0721f
           * (1 << LUMINANCE_FACTOR_SHIFT));

   private static final String DEFAULT_FONT_FAMILY = "sans-serif";

   private static HashSet<String>  supportedFeatures = null;


   private class RendererState implements Cloneable
   {
      public Style    style;
      public boolean  hasFill;
      public boolean  hasStroke;
      public Paint    fillPaint;
      public Paint    strokePaint;
      public SVG.Box  viewPort;
      public SVG.Box  viewBox;
      public boolean  spacePreserve;

      // Set when we doing direct rendering.
      public boolean  directRendering;


      public RendererState()
      {
         fillPaint = new Paint();
         fillPaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG
                 | Paint.SUBPIXEL_TEXT_FLAG);
         fillPaint.setStyle(Paint.Style.FILL);
         fillPaint.setTypeface(Typeface.DEFAULT);

         strokePaint = new Paint();
         strokePaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG
                 | Paint.SUBPIXEL_TEXT_FLAG);
         strokePaint.setStyle(Paint.Style.STROKE);
         strokePaint.setTypeface(Typeface.DEFAULT);

         style = Style.getDefaultStyle();
      }

      @Override
      protected Object  clone()
      {
         RendererState obj;
         try
         {
            obj = (RendererState) super.clone();
            obj.style = (Style) style.clone();
            obj.fillPaint = new Paint(fillPaint);
            obj.strokePaint = new Paint(strokePaint);
            return obj;
         }
         catch (CloneNotSupportedException e)
         {
            throw new InternalError(e.toString());
         }
      }

   }


   private void  resetState()
   {
      state = new RendererState();
      stateStack = new Stack<RendererState>();

      // Initialise the style state properties like Paints etc using a fresh instance of Style
      updateStyle(state, Style.getDefaultStyle());

      state.viewPort = this.canvasViewPort;

      state.spacePreserve = false;
      state.directRendering = this.directRenderingMode;

      // Push a copy of the state with 'default' style, so that inherit works for top level objects
      stateStack.push((RendererState) state.clone());   // Manual push here - don't use statePush();

      // Initialise the stacks used for mask handling
      canvasStack = new Stack<Canvas>();
      bitmapStack = new Stack<Bitmap>();

      // Keep track of element stack while rendering.
      // The 'render parent' for some elements (eg <use> references)
      // is different from its DOM parent.
      matrixStack = new Stack<Matrix>();
      parentStack = new Stack<SvgContainer>();
   }


   /*
    * Create a new renderer instance.
    *
    * @param canvas the canvas to draw to.
    * @param viewPort the default viewport to be rendered into.
    * For example the dimensions of the bitmap.
    * @param defaultDPI the DPI setting to use when converting real-world units
    * such as centimetres.
    */

   protected SVGAndroidRenderer(Canvas canvas, SVG.Box viewPort, float defaultDPI)
   {
      this.canvas = canvas;
      this.dpi = defaultDPI;
      this.canvasViewPort = viewPort;
   }


   protected float  getDPI()
   {
      return dpi;
   }


   protected float  getCurrentFontSize()
   {
      return state.fillPaint.getTextSize();
   }


   protected float  getCurrentFontXHeight()
   {
      // The CSS3 spec says to use 0.5em if there is no way to determine true x-height;
      return state.fillPaint.getTextSize() / 2f;
   }


   /*
    * Get the current view port in user units.
    *
    */
   protected SVG.Box  getCurrentViewPortInUserUnits()
   {
      if (state.viewBox != null)
         return state.viewBox;
      else
         return state.viewPort;
   }


   /*
    * Render the whole document.
    */
   protected void  renderDocument(SVG document, Box viewBox, PreserveAspectRatio positioning,
                                  boolean directRenderingMode)
   {
      this.document = document;
      this.directRenderingMode = directRenderingMode;

      SVG.Svg  rootObj = document.getRootElement();

      if (rootObj == null) {
         warn("Nothing to render. Document is empty.");
         return;
      }

      // Initialise the state
      resetState();

      checkXMLSpaceAttribute(rootObj);

      // Render the document
      render(rootObj, rootObj.width, rootObj.height,
             (viewBox != null) ? viewBox : rootObj.viewBox,
             (positioning != null) ? positioning : rootObj.preserveAspectRatio);
   }


   //==============================================================================
   // Render dispatcher


   private void  render(SVG.SvgObject obj)
   {
      if (obj instanceof NotDirectlyRendered)
         return;

      // Save state
      statePush();

      checkXMLSpaceAttribute(obj);

      if (obj instanceof SVG.Svg) {
         render((SVG.Svg) obj);
      } else if (obj instanceof SVG.Use) {
         render((SVG.Use) obj);
      } else if (obj instanceof SVG.Switch) {
         render((SVG.Switch) obj);
      } else if (obj instanceof SVG.Group) {
         render((SVG.Group) obj);
      } else if (obj instanceof SVG.Image) {
         render((SVG.Image) obj);
      } else if (obj instanceof SVG.Path) {
         render((SVG.Path) obj);
      } else if (obj instanceof SVG.Rect) {
         render((SVG.Rect) obj);
      } else if (obj instanceof SVG.Circle) {
         render((SVG.Circle) obj);
      } else if (obj instanceof SVG.Ellipse) {
         render((SVG.Ellipse) obj);
      } else if (obj instanceof SVG.Line) {
         render((SVG.Line) obj);
      } else if (obj instanceof SVG.Polygon) {
         render((SVG.Polygon) obj);
      } else if (obj instanceof SVG.PolyLine) {
         render((SVG.PolyLine) obj);
      } else if (obj instanceof SVG.Text) {
         render((SVG.Text) obj);
      }

      // Restore state
      statePop();
   }


   //==============================================================================


   private void  renderChildren(SvgContainer obj, boolean isContainer)
   {
      if (isContainer) {
         parentPush(obj);
      }

      for (SVG.SvgObject child: obj.getChildren()) {
         render(child);
      }

      if (isContainer) {
         parentPop();
      }
   }


   //==============================================================================


   private void  statePush()
   {
      // Save matrix and clip
      canvas.save();
      // Save style state
      stateStack.push(state);
      state = (RendererState) state.clone();
   }


   private void  statePop()
   {
      // Restore matrix and clip
      canvas.restore();
      // Restore style state
      state = stateStack.pop();
   }


   //==============================================================================


   @SuppressWarnings("deprecation")
   private void  parentPush(SvgContainer obj)
   {
      parentStack.push(obj);
      matrixStack.push(canvas.getMatrix());
   }


   private void  parentPop()
   {
      parentStack.pop();
      matrixStack.pop();
   }


   //==============================================================================


   private void updateStyleForElement(RendererState state, SvgElementBase obj)
   {
      boolean  isRootSVG = (obj.parent == null);
      state.style.resetNonInheritingProperties(isRootSVG);

      // Apply the styles defined by style attributes on the element
      if (obj.baseStyle != null)
         updateStyle(state, obj.baseStyle);

      // Apply the styles from any CSS files or <style> elements
      if (document.hasCSSRules())
      {
         for (CSSParser.Rule rule: document.getCSSRules())
         {
            if (CSSParser.ruleMatch(rule.selector, obj)) {
               updateStyle(state, rule.style);
            }
         }
      }

      // Apply the styles defined by the 'style' attribute. They have the highest precedence.
      if (obj.style != null)
         updateStyle(state, obj.style);
   }


   /*
    * Check and update xml:space handling.
    */
   private void checkXMLSpaceAttribute(SVG.SvgObject obj)
   {
      if (!(obj instanceof SvgElementBase))
        return;

      SvgElementBase bobj = (SvgElementBase) obj;
      if (bobj.spacePreserve != null)
         state.spacePreserve = bobj.spacePreserve;
   }


   /*
    * Fill a path with either the given paint, or if a pattern is set, with the pattern.
    */
   private void doFilledPath(SvgElement obj, Path path)
   {
      // First check for pattern fill. It requires special handling.
      if (state.style.fill instanceof SVG.PaintReference)
      {
         SVG.SvgObject  ref = document.resolveIRI(((SVG.PaintReference) state.style.fill).href);
         if (ref instanceof SVG.Pattern) {
            SVG.Pattern  pattern = (SVG.Pattern)ref;
            fillWithPattern(obj, path, pattern);
            return;
         }
      }

      // Otherwise do a normal fill
      canvas.drawPath(path, state.fillPaint);
   }


   @SuppressWarnings("deprecation")
   private void  doStroke(Path path)
   {
      // TODO handle degenerate subpaths properly

      if (state.style.vectorEffect == VectorEffect.NonScalingStroke)
      {
         // For non-scaling-stroke, the stroke width is not transformed along with the path.
         // It will be rendered at the same width no matter how the document contents
         // are transformed.

         // First step: get the current canvas matrix
         Matrix  currentMatrix = canvas.getMatrix();
         // Transform the path using this transform
         Path  transformedPath = new Path();
         path.transform(currentMatrix, transformedPath);
         // Reset the current canvas transform completely
         canvas.setMatrix(new Matrix());

         // If there is a shader (such as a gradient), we need to update its transform also
         Shader  shader = state.strokePaint.getShader();
         Matrix  currentShaderMatrix = new Matrix();
         if (shader != null) {
            shader.getLocalMatrix(currentShaderMatrix);
            Matrix  newShaderMatrix = new Matrix(currentShaderMatrix);
            newShaderMatrix.postConcat(currentMatrix);
            shader.setLocalMatrix(newShaderMatrix);
         }

         // Render the transformed path. The stroke width used will be in unscaled device units.
         canvas.drawPath(transformedPath, state.strokePaint);

         // Return the current canvas transform to what it was before all this happened         
         canvas.setMatrix(currentMatrix);
         // And reset the shader matrix also
         if (shader != null)
            shader.setLocalMatrix(currentShaderMatrix);
      }
      else
      {
         canvas.drawPath(path, state.strokePaint);
      }
   }


   //==============================================================================


   private static void  warn(String format, Object... args)
   {
      Log.w(TAG, String.format(format, args));
   }


   private static void  error(String format, Object... args)
   {
      Log.e(TAG, String.format(format, args));
   }


   private static void  debug(String format, Object... args)
   {
      if (LibConfig.DEBUG)
         Log.d(TAG, String.format(format, args));
   }


   private static void  info(String format, Object... args)
   {
      Log.i(TAG, String.format(format, args));
   }


   //==============================================================================
   // Renderers for each element type


   private void render(SVG.Svg obj)
   {
      render(obj, obj.width, obj.height);
   }


   // When referenced by a <use> element, it's width and height take precedence
   // over the ones in the <svg> object.
   private void render(SVG.Svg obj, SVG.Length width, SVG.Length height)
   {
      render(obj, width, height, obj.viewBox, obj.preserveAspectRatio);
   }


   // When called from renderDocument, we pass in our own viewBox.
   // If rendering the whole document, it will be rootObj.viewBox.  When rendering a view
   // it will be the viewBox from the <view> element.
   private void render(SVG.Svg obj, SVG.Length width, SVG.Length height, Box viewBox,
                       PreserveAspectRatio positioning)
   {
      debug("Svg render");

      if ((width != null && width.isZero()) ||
          (height != null && height.isZero()))
         return;

      // "If attribute 'preserveAspectRatio' is not specified,
      // then the effect is as if a value of xMidYMid meet were specified."
      if (positioning == null)
         positioning = (obj.preserveAspectRatio != null) ? obj.preserveAspectRatio
                 : PreserveAspectRatio.LETTERBOX;

      updateStyleForElement(state, obj);

      if (!display())
         return;

      // <svg> elements establish a new viewport.
      float  _x = 0f;
      float  _y = 0f;
      if (obj.parent != null)  // Ignore x,y for root <svg> element
      {
         _x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
         _y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
      }
         
      Box  viewPortUser = getCurrentViewPortInUserUnits();
      float  _w = (width != null) ? width.floatValueX(this) : viewPortUser.width;  // default 100%
      float  _h = (height != null) ? height.floatValueY(this) : viewPortUser.height;
      state.viewPort = new SVG.Box(_x, _y, _w, _h);

      if (!state.style.overflow) {
         setClipRect(state.viewPort.minX, state.viewPort.minY, state.viewPort.width,
                 state.viewPort.height);
      }

      checkForClipPath(obj, state.viewPort);

      if (viewBox != null) {
         canvas.concat(calculateViewBoxTransform(state.viewPort, viewBox, positioning));
         state.viewBox = obj.viewBox;  // Note: definitely obj.viewBox here. Not viewBox parameter.
      } else {
         canvas.translate(_x, _y);
      }

      boolean  compositing = pushLayer();

      // Action the viewport-fill property (if set)
      viewportFill();

      renderChildren(obj, true);

      if (compositing)
         popLayer(obj);

      updateParentBoundingBox(obj);
   }


   //==============================================================================


   private void render(SVG.Group obj)
   {
      debug("Group render");

      updateStyleForElement(state, obj);

      if (!display())
         return;

      if (obj.transform != null) {
         canvas.concat(obj.transform);
      }

      checkForClipPath(obj);

      boolean  compositing = pushLayer();

      renderChildren(obj, true);

      if (compositing)
         popLayer(obj);

      updateParentBoundingBox(obj);
   }


   //==============================================================================


   /*
    * Called by an object to update it's parent's bounding box.
    *
    * This operation is made more tricky because the childs bbox is in the child's coordinate space,
    * but the parent needs it in the parent's coordinate space.
    */
   @SuppressWarnings("deprecation")
   private void updateParentBoundingBox(SvgElement obj)
   {
      if (obj.parent == null)
         // skip this if obj is root element
         return;
      if (obj.boundingBox == null)
         // empty bbox, possibly as a result of a badly defined element (eg bad use reference etc)
         return;

      // Convert the corners of the child bbox to world space
      Matrix  m = new Matrix();
      // Get the inverse of the child transform
      if (matrixStack.peek().invert(m)) {
         float[] pts = {obj.boundingBox.minX, obj.boundingBox.minY,
                        obj.boundingBox.maxX(), obj.boundingBox.minY,
                        obj.boundingBox.maxX(), obj.boundingBox.maxY(),
                        obj.boundingBox.minX, obj.boundingBox.maxY()};
         // Now concatenate the parent's matrix to create a child-to-parent transform
         m.preConcat(canvas.getMatrix());
         m.mapPoints(pts);
         // Finally, find the bounding box of the transformed points
         RectF  rect = new RectF(pts[0], pts[1], pts[0], pts[1]);
         for (int i=2; i<=6; i+=2) {
            if (pts[i] < rect.left) rect.left = pts[i];
            if (pts[i] > rect.right) rect.right = pts[i]; 
            if (pts[i+1] < rect.top) rect.top = pts[i+1];
            if (pts[i+1] > rect.bottom) rect.bottom = pts[i+1]; 
         }
         // Update the parent bounding box with the transformed bbox
         SvgElement  parent = (SvgElement) parentStack.peek();
         if (parent.boundingBox == null)
            parent.boundingBox = Box.fromLimits(rect.left, rect.top, rect.right, rect.bottom);
         else
            parent.boundingBox.union(Box.fromLimits(rect.left, rect.top, rect.right, rect.bottom));
      }
   }


   //==============================================================================


   private boolean  pushLayer()
   {
      if (!requiresCompositing())
         return false;

      // Custom version of statePush() that also saves the layer
      canvas.saveLayerAlpha(null, clamp255(state.style.opacity), Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);

      // Save style state
      stateStack.push(state);
      state = (RendererState) state.clone();

      if (state.style.mask != null && state.directRendering) {
         SVG.SvgObject  ref = document.resolveIRI(state.style.mask);
         // Check the we are referencing a mask element
         if (ref == null || !(ref instanceof SVG.Mask)) {
            // This is an invalid mask reference - disable this object's mask
            error("Mask reference '%s' not found", state.style.mask);
            state.style.mask = null;
            return true;
         }
         // We now need to replace the canvas with one
         // onto which we draw the content that is getting masked
         canvasStack.push(canvas);
         duplicateCanvas();
      }

      return true;
   }


   private void  popLayer(SvgElement obj)
   {
      // If this is masked content, apply the mask now
      if (state.style.mask != null && state.directRendering) {
         // The masked content has been drawn, now we have to render the mask to a separate canvas
         SVG.SvgObject  ref = document.resolveIRI(state.style.mask);
         duplicateCanvas();
         renderMask((SVG.Mask) ref, obj);
         
         Bitmap  maskedContent = processMaskBitmaps();
         
         // Retrieve the real canvas
         canvas = canvasStack.pop();
         canvas.save();
         // Reset the canvas matrix so that we can draw the maskedContent
         // exactly over the top of the root bitmap
         canvas.setMatrix(new Matrix());
         canvas.drawBitmap(maskedContent, 0, 0, state.fillPaint);
         maskedContent.recycle();
         canvas.restore();
      }

      statePop();
   }


   private boolean requiresCompositing()
   {
      if (state.style.mask != null && !state.directRendering)
         warn("Masks are not supported when using getPicture()");

      return (state.style.opacity < 1.0f) ||
             (state.style.mask != null && state.directRendering);
   }


   @SuppressWarnings("deprecation")
   private void duplicateCanvas()
   {
      try {
         Bitmap  newBM = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(),
                 Bitmap.Config.ARGB_8888);
         bitmapStack.push(newBM);
         Canvas  newCanvas = new Canvas(newBM);
         newCanvas.setMatrix(canvas.getMatrix());
         canvas = newCanvas;
      } catch (OutOfMemoryError e) {
         error("Not enough memory to create temporary bitmaps for mask processing");
         throw e;
      }
   }


   private Bitmap  processMaskBitmaps()
   {
      // Retrieve the rendered mask
      Bitmap  mask = bitmapStack.pop();
      // Retrieve the rendered content to which the mask is to be applied
      Bitmap  maskedContent = bitmapStack.pop();
      // Convert the mask bitmap to an alpha channel and multiply it to the content
      // We will process the bitmaps in a row-wise fashion to save memory.
      // It doesn't seem to be be significantly slower than doing it all at once.
      int    w = mask.getWidth();
      int    h = mask.getHeight();
      int[]  maskBuf = new int[w];
      int[]  maskedContentBuf = new int[w];
      for (int y=0; y<h; y++)
      {
         mask.getPixels(maskBuf, 0, w, 0, y, w, 1);
         maskedContent.getPixels(maskedContentBuf, 0, w, 0, y, w, 1);
         for (int x=0; x<w; x++)
         {
            int  px = maskBuf[x];
            int  b = px & 0xff;
            int  g = (px >> 8) & 0xff;
            int  r = (px >> 16) & 0xff;
            int  a = (px >> 24) & 0xff;
            if (a == 0) {
               // Shortcut for transparent mask pixels
               maskedContentBuf[x] = 0;
               continue;
            }
            int  maskAlpha = (r * LUMINANCE_TO_ALPHA_RED + g * LUMINANCE_TO_ALPHA_GREEN + b
                    * LUMINANCE_TO_ALPHA_BLUE) * a / (255 << LUMINANCE_FACTOR_SHIFT);
            int  content = maskedContentBuf[x];
            int  contentAlpha = (content >> 24) & 0xff;
            contentAlpha = (contentAlpha * maskAlpha) / 255;
            maskedContentBuf[x] = (content & 0x00ffffff) | (contentAlpha << 24);
         }
         maskedContent.setPixels(maskedContentBuf, 0, w, 0, y, w, 1);
      }
      mask.recycle();
      return maskedContent;
   }


   //==============================================================================


   /*
    * Find the first child of the switch that passes the feature tests and render only that child.
    */
   private void render(SVG.Switch obj)
   {
      debug("Switch render");

      updateStyleForElement(state, obj);

      if (!display())
         return;

      if (obj.transform != null) {
         canvas.concat(obj.transform);
      }

      checkForClipPath(obj);

      boolean  compositing = pushLayer();

      renderSwitchChild(obj);

      if (compositing)
         popLayer(obj);

      updateParentBoundingBox(obj);
   }


   private void  renderSwitchChild(SVG.Switch obj)
   {
      String                   deviceLanguage = Locale.getDefault().getLanguage();
      SVGExternalFileResolver  fileResolver = document.getFileResolver();

      ChildLoop:
      for (SVG.SvgObject child: obj.getChildren())
      {
         // Ignore any objects that don't belong in a <switch>
         if (!(child instanceof SVG.SvgConditional)) {
            continue;
         }
         SVG.SvgConditional  condObj = (SVG.SvgConditional) child;

         // We don't support extensions
         if (condObj.getRequiredExtensions() != null) {
            continue;
         }
         // Check language
         Set<String>  syslang = condObj.getSystemLanguage();
         if (syslang != null && (syslang.isEmpty() || !syslang.contains(deviceLanguage))) {
            continue;
         }
         // Check features
         Set<String>  reqfeat = condObj.getRequiredFeatures();
         if (reqfeat != null) {
            if (supportedFeatures == null)
               initialiseSupportedFeaturesMap();
            if (reqfeat.isEmpty() || !supportedFeatures.containsAll(reqfeat)) {
               continue;
            }
         }
         // Check formats (MIME types)
         Set<String>  reqfmts = condObj.getRequiredFormats();
         if (reqfmts != null) {
            if (reqfmts.isEmpty() || fileResolver==null)
               continue;
            for (String mimeType: reqfmts) {
               if (!fileResolver.isFormatSupported(mimeType))
                  continue ChildLoop;
            }
         }
         // Check formats (MIME types)
         Set<String>  reqfonts = condObj.getRequiredFonts();
         if (reqfonts != null) {
            if (reqfonts.isEmpty() || fileResolver==null)
               continue;
            for (String fontName: reqfonts) {
               if (fileResolver.resolveFont(fontName, state.style.fontWeight,
                       String.valueOf(state.style.fontStyle)) == null)
                  continue ChildLoop;
            }
         }
         
         // All checks passed!  Render this one element and exit
         render(child);
         break;
      }
   }


   private static synchronized void  initialiseSupportedFeaturesMap()
   {
      supportedFeatures = new HashSet<String>();

      // SVG features this SVG implementation supports
      // Actual feature strings have the prefix: FEATURE_STRING_PREFIX (see above)
      // NO indicates feature will probable not ever be implemented
      // NYI indicates support is in progress, or is planned
      
      // Feature sets that represent sets of other feature strings (ie a group of features strings)
      //supportedFeatures.add("SVG");                       // NO
      //supportedFeatures.add("SVGDOM");                    // NO
      //supportedFeatures.add("SVG-static");                // NO
      //supportedFeatures.add("SVGDOM-static");             // NO
      //supportedFeatures.add("SVG-animation");             // NO
      //supportedFeatures.add("SVGDOM-animation");          // NO 
      //supportedFeatures.add("SVG-dynamic");               // NO
      //supportedFeatures.add("SVGDOM-dynamic");            // NO

      // Individual features
      //supportedFeatures.add("CoreAttribute");             // NO
      supportedFeatures.add("Structure");                   // YES (although desc title and metadata
                                                            // are ignored)
      supportedFeatures.add("BasicStructure");              // YES (although desc title and metadata
                                                            // are ignored)
      //supportedFeatures.add("ContainerAttribute");        // NO (filter related. NYI)
      supportedFeatures.add("ConditionalProcessing");       // YES
      supportedFeatures.add("Image");                       // YES (bitmaps only - not SVG files)
      supportedFeatures.add("Style");                       // YES
      supportedFeatures.add("ViewportAttribute");           // YES
      supportedFeatures.add("Shape");                       // YES
      //supportedFeatures.add("Text");                      // NO
      supportedFeatures.add("BasicText");                   // YES
      supportedFeatures.add("PaintAttribute");              // YES (except color-interpolation
                                                            // and color-rendering)
      supportedFeatures.add("BasicPaintAttribute");         // YES (except color-rendering)
      supportedFeatures.add("OpacityAttribute");            // YES
      //supportedFeatures.add("GraphicsAttribute");         // NO     
      supportedFeatures.add("BasicGraphicsAttribute");      // YES
      supportedFeatures.add("Marker");                      // YES
      //supportedFeatures.add("ColorProfile");              // NO
      supportedFeatures.add("Gradient");                    // YES
      supportedFeatures.add("Pattern");                     // YES
      supportedFeatures.add("Clip");                        // YES
      supportedFeatures.add("BasicClip");                   // YES
      supportedFeatures.add("Mask");                        // YES
      //supportedFeatures.add("Filter");                    // NO
      //supportedFeatures.add("BasicFilter");               // NO
      //supportedFeatures.add("DocumentEventsAttribute");   // NO
      //supportedFeatures.add("GraphicalEventsAttribute");  // NO
      //supportedFeatures.add("AnimationEventsAttribute");  // NO
      //supportedFeatures.add("Cursor");                    // NO
      //supportedFeatures.add("Hyperlinking");              // NO
      //supportedFeatures.add("XlinkAttribute");            // NO
      //supportedFeatures.add("ExternalResourcesRequired"); // NO
      supportedFeatures.add("View");                        // YES
      //supportedFeatures.add("Script");                    // NO
      //supportedFeatures.add("Animation");                 // NO
      //supportedFeatures.add("Font");                      // NO
      //supportedFeatures.add("BasicFont");                 // NO
      //supportedFeatures.add("Extensibility");             // NO

      // SVG 1.0 features - all are too general and include things we are not likely to ever support
      // If we ever do support these, we'll need to change how FEATURE_STRING_PREFIX is used.
      //supportedFeatures.add("org.w3c.svg");
      //supportedFeatures.add("org.w3c.dom.svg");
      //supportedFeatures.add("org.w3c.svg.static");
      //supportedFeatures.add("org.w3c.dom.svg.static");
      //supportedFeatures.add("org.w3c.svg.animation");
      //supportedFeatures.add("org.w3c.dom.svg.animation");
      //supportedFeatures.add("org.w3c.svg.dynamic");
      //supportedFeatures.add("org.w3c.dom.svg.dynamic");
      //supportedFeatures.add("org.w3c.svg.all");
      //supportedFeatures.add("org.w3c.dom.svg.all" );
   }


   //==============================================================================


   private void render(SVG.Use obj)
   {
      debug("Use render");

      if ((obj.width != null && obj.width.isZero()) ||
          (obj.height != null && obj.height.isZero()))
         return;

      updateStyleForElement(state, obj);

      if (!display())
         return;

      // Locate the referenced object
      SVG.SvgObject  ref = obj.document.resolveIRI(obj.href);
      if (ref == null) {
         error("Use reference '%s' not found", obj.href);
         return;
      }

      if (obj.transform != null) {
         canvas.concat(obj.transform);
      }

      // We handle the x,y,width,height attributes by adjusting the transform
      Matrix m = new Matrix();
      float _x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
      float _y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
      m.preTranslate(_x, _y);
      canvas.concat(m);

      checkForClipPath(obj);

      boolean  compositing = pushLayer();

      parentPush(obj);

      if (ref instanceof SVG.Svg)
      {
         statePush();
         SVG.Svg  svgElem = (SVG.Svg) ref;
         Length _w = (obj.width != null) ? obj.width : svgElem.width;
         Length _h = (obj.height != null) ? obj.height : svgElem.height;
         render(svgElem, _w, _h);
         statePop();
      }
      else if (ref instanceof SVG.Symbol)
      {
         Length _w = (obj.width != null) ? obj.width : new Length(100, Unit.percent);
         Length _h = (obj.height != null) ? obj.height : new Length(100, Unit.percent);
         statePush();
         render((SVG.Symbol) ref, _w, _h);
         statePop();
      }
      else
      {
         render(ref);
      }

      parentPop();

      if (compositing)
         popLayer(obj);

      updateParentBoundingBox(obj);
   }


   //==============================================================================


   private void render(SVG.Path obj)
   {
      debug("Path render");

      if (obj.d == null)
         return;

      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;
      if (!state.hasStroke && !state.hasFill)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      Path  path = (new PathConverter(obj.d)).getPath();

      if (obj.boundingBox == null) {
         obj.boundingBox = calculatePathBounds(path);
      }
      updateParentBoundingBox(obj);

      checkForGradientsAndPatterns(obj);
      checkForClipPath(obj);
      
      boolean  compositing = pushLayer();

      if (state.hasFill) {
         path.setFillType(getFillTypeFromState());
         doFilledPath(obj, path);
      }
      if (state.hasStroke)
         doStroke(path);

      renderMarkers(obj);

      if (compositing)
         popLayer(obj);
   }


   private Box  calculatePathBounds(Path path)
   {
      RectF  pathBounds = new RectF();
      path.computeBounds(pathBounds, true);
      return new Box(pathBounds.left, pathBounds.top, pathBounds.width(), pathBounds.height());
   }


   //==============================================================================


   private void render(SVG.Rect obj)
   {
      debug("Rect render");

      if (obj.width == null || obj.height == null || obj.width.isZero() || obj.height.isZero())
         return;

      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      Path  path = makePathAndBoundingBox(obj);
      updateParentBoundingBox(obj);

      checkForGradientsAndPatterns(obj);
      checkForClipPath(obj);

      boolean  compositing = pushLayer();

      if (state.hasFill)
         doFilledPath(obj, path);
      if (state.hasStroke)
         doStroke(path);


      if (compositing)
         popLayer(obj);
   }


   //==============================================================================


   private void render(SVG.Circle obj)
   {
      debug("Circle render");

      if (obj.r == null || obj.r.isZero())
         return;

      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      Path  path = makePathAndBoundingBox(obj);
      updateParentBoundingBox(obj);

      checkForGradientsAndPatterns(obj);
      checkForClipPath(obj);

      boolean  compositing = pushLayer();

      if (state.hasFill)
         doFilledPath(obj, path);
      if (state.hasStroke)
         doStroke(path);

      if (compositing)
         popLayer(obj);
   }


   //==============================================================================


   private void render(SVG.Ellipse obj)
   {
      debug("Ellipse render");

      if (obj.rx == null || obj.ry == null || obj.rx.isZero() || obj.ry.isZero())
         return;

      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      Path  path = makePathAndBoundingBox(obj);
      updateParentBoundingBox(obj);

      checkForGradientsAndPatterns(obj);
      checkForClipPath(obj);

      boolean  compositing = pushLayer();

      if (state.hasFill)
         doFilledPath(obj, path);
      if (state.hasStroke)
         doStroke(path);

      if (compositing)
         popLayer(obj);
   }


   //==============================================================================


   private void render(SVG.Line obj)
   {
      debug("Line render");

      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;
      if (!state.hasStroke)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      Path  path = makePathAndBoundingBox(obj);
      updateParentBoundingBox(obj);

      checkForGradientsAndPatterns(obj);
      checkForClipPath(obj);

      boolean  compositing = pushLayer();

      doStroke(path);

      renderMarkers(obj);

      if (compositing)
         popLayer(obj);
   }


   private List<MarkerVector>  calculateMarkerPositions(SVG.Line obj)
   {
      float _x1, _y1, _x2, _y2;
      _x1 = (obj.x1 != null) ? obj.x1.floatValueX(this) : 0f;
      _y1 = (obj.y1 != null) ? obj.y1.floatValueY(this) : 0f;
      _x2 = (obj.x2 != null) ? obj.x2.floatValueX(this) : 0f;
      _y2 = (obj.y2 != null) ? obj.y2.floatValueY(this) : 0f;

      List<MarkerVector>  markers = new ArrayList<MarkerVector>(2);
      markers.add(new MarkerVector(_x1, _y1, (_x2-_x1), (_y2-_y1)));
      markers.add(new MarkerVector(_x2, _y2, (_x2-_x1), (_y2-_y1)));
      return markers;
   }


   //==============================================================================


   private void render(SVG.PolyLine obj)
   {
      debug("PolyLine render");

      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;
      if (!state.hasStroke && !state.hasFill)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      int  numPoints = obj.points.length;
      if (numPoints < 2)
         return;

      Path  path = makePathAndBoundingBox(obj);
      updateParentBoundingBox(obj);

      checkForGradientsAndPatterns(obj);
      checkForClipPath(obj);
      
      boolean  compositing = pushLayer();

      if (state.hasFill)
         doFilledPath(obj, path);
      if (state.hasStroke)
         doStroke(path);

      renderMarkers(obj);

      if (compositing)
         popLayer(obj);
   }


   private List<MarkerVector>  calculateMarkerPositions(SVG.PolyLine obj)
   {
      int  numPoints = obj.points.length; 

      if (numPoints < 2)
         return null;

      List<MarkerVector>  markers = new ArrayList<MarkerVector>();
      MarkerVector        lastPos = new MarkerVector(obj.points[0], obj.points[1], 0, 0);
      float               x = 0, y = 0;

      for (int i=2; i<numPoints; i+=2) {
         x = obj.points[i];
         y = obj.points[i+1];
         lastPos.add(x, y);
         markers.add(lastPos);
         MarkerVector  newPos = new MarkerVector(x, y, x-lastPos.x, y-lastPos.y);
         lastPos = newPos;
      }

      // Deal with last point
      if (obj instanceof SVG.Polygon) {
         if (x != obj.points[0] && y != obj.points[1]) {
            x = obj.points[0];
            y = obj.points[1];
            lastPos.add(x, y);
            markers.add(lastPos);
            // Last marker point needs special handling because its orientation depends
            // on the orientation of the very first segment of the path
            MarkerVector  newPos = new MarkerVector(x, y, x-lastPos.x, y-lastPos.y);
            newPos.add(markers.get(0));
            markers.add(newPos);
            markers.set(0, newPos);  // Start marker is the same
         }
      } else {
         markers.add(lastPos);
      }
      return markers;
   }


   //==============================================================================


   private void render(SVG.Polygon obj)
   {
      debug("Polygon render");

      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;
      if (!state.hasStroke && !state.hasFill)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      int  numPoints = obj.points.length;
      if (numPoints < 2)
         return;

      Path  path = makePathAndBoundingBox(obj);
      updateParentBoundingBox(obj);

      checkForGradientsAndPatterns(obj);
      checkForClipPath(obj);
      
      boolean  compositing = pushLayer();

      if (state.hasFill)
         doFilledPath(obj, path);
      if (state.hasStroke)
         doStroke(path);

      renderMarkers(obj);

      if (compositing)
         popLayer(obj);
   }


   //==============================================================================


   private void render(SVG.Text obj)
   {
      debug("Text render");

      updateStyleForElement(state, obj);

      if (!display())
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      // Get the first coordinate pair from the lists in the x and y properties.
      float  x = (obj.x == null || obj.x.size() == 0) ? 0f : obj.x.get(0).floatValueX(this);
      float  y = (obj.y == null || obj.y.size() == 0) ? 0f : obj.y.get(0).floatValueY(this);
      float  dx = (obj.dx == null || obj.dx.size() == 0) ? 0f : obj.dx.get(0).floatValueX(this);
      float  dy = (obj.dy == null || obj.dy.size() == 0) ? 0f : obj.dy.get(0).floatValueY(this);

      // Handle text alignment
      Style.TextAnchor  anchor = getAnchorPosition();
      if (anchor != Style.TextAnchor.Start) {
         float  textWidth = calculateTextWidth(obj);
         if (anchor == Style.TextAnchor.Middle) {
            x -= (textWidth / 2);
         } else {
            x -= textWidth;  // 'End' (right justify)
         }
      }

      if (obj.boundingBox == null) {
         TextBoundsCalculator  proc = new TextBoundsCalculator(x, y);
         enumerateTextSpans(obj, proc);
         obj.boundingBox = new Box(proc.bbox.left, proc.bbox.top, proc.bbox.width(),
                 proc.bbox.height());
      }
      updateParentBoundingBox(obj);

      checkForGradientsAndPatterns(obj);
      checkForClipPath(obj);
      
      boolean  compositing = pushLayer();

      enumerateTextSpans(obj, new PlainTextDrawer(x + dx, y + dy));

      if (compositing)
         popLayer(obj);
   }


   private Style.TextAnchor  getAnchorPosition()
   {
      if (state.style.direction == Style.TextDirection.LTR
              || state.style.textAnchor == TextAnchor.Middle)
         return state.style.textAnchor;

      // Handle RTL case where Start and End are reversed
      return (state.style.textAnchor == TextAnchor.Start) ? TextAnchor.End : TextAnchor.Start;
   }


   private class  PlainTextDrawer extends TextProcessor
   {
      public float x;
      public float y;

      public PlainTextDrawer(float x, float y)
      {
         this.x = x;
         this.y = y;
      }

      @Override
      public void processText(String text)
      {
         debug("TextSequence render");

         if (visible())
         {
            if (state.hasFill)
               canvas.drawText(text, x, y, state.fillPaint);
            if (state.hasStroke)
               canvas.drawText(text, x, y, state.strokePaint);
         }

         // Update the current text position
         x += state.fillPaint.measureText(text);
      }
   }


   //==============================================================================
   // Text sequence enumeration


   private abstract class  TextProcessor
   {
      public boolean  doTextContainer(TextContainer obj)
      {
         return true;
      }

      public abstract void  processText(String text);
   }


   /*
    * Given a text container, recursively visit its children invoking the TextDrawer
    * handler for each segment of text found.
    */
   private void enumerateTextSpans(TextContainer obj, TextProcessor textprocessor)
   {
      if (!display())
         return;

      Iterator<SvgObject>  iter = obj.children.iterator();
      boolean              isFirstChild = true;

      while (iter.hasNext())
      {
         SvgObject  child = iter.next();

         if (child instanceof SVG.TextSequence) {
            textprocessor.processText(textXMLSpaceTransform(((SVG.TextSequence) child).text,
                    isFirstChild, !iter.hasNext() /*isLastChild*/));
         } else {
            processTextChild(child, textprocessor);
         }
         isFirstChild = false;
      }
   }


   private void  processTextChild(SVG.SvgObject obj, TextProcessor textprocessor)
   {
      // Ask the processor implementation if it wants to process this object
      if (!textprocessor.doTextContainer((SVG.TextContainer) obj))
         return;

      if (obj instanceof SVG.TextPath)
      {
         // Save state
         statePush();

         renderTextPath((SVG.TextPath) obj);

         // Restore state
         statePop();
      }
      else if (obj instanceof SVG.TSpan)
      {
         debug("TSpan render");

         // Save state
         statePush();

         SVG.TSpan tspan = (SVG.TSpan) obj; 

         updateStyleForElement(state, tspan);

         if (display())
         {
            // Get the first coordinate pair from the lists in the x and y properties.
            float x=0, y=0, dx=0, dy=0;
            if (textprocessor instanceof PlainTextDrawer) {
               x = (tspan.x == null || tspan.x.size() == 0) ? ((PlainTextDrawer) textprocessor).x
                       : tspan.x.get(0).floatValueX(this);
               y = (tspan.y == null || tspan.y.size() == 0) ? ((PlainTextDrawer) textprocessor).y
                       : tspan.y.get(0).floatValueY(this);
               dx = (tspan.dx == null || tspan.dx.size() == 0) ? 0f
                       : tspan.dx.get(0).floatValueX(this);
               dy = (tspan.dy == null || tspan.dy.size() == 0) ? 0f
                       : tspan.dy.get(0).floatValueY(this);
            }

            checkForGradientsAndPatterns((SvgElement) tspan.getTextRoot());

            if (textprocessor instanceof PlainTextDrawer) {
               ((PlainTextDrawer) textprocessor).x = x + dx;
               ((PlainTextDrawer) textprocessor).y = y + dy;
            }

            boolean  compositing = pushLayer();

            enumerateTextSpans(tspan, textprocessor);

            if (compositing)
               popLayer(tspan);
         }

         // Restore state
         statePop();
      }
      else if  (obj instanceof SVG.TRef)
      {
         // Save state
         statePush();

         SVG.TRef tref = (SVG.TRef) obj; 

         updateStyleForElement(state, tref);

         if (display())
         {
            checkForGradientsAndPatterns((SvgElement) tref.getTextRoot());

            // Locate the referenced object
            SVG.SvgObject  ref = obj.document.resolveIRI(tref.href);
            if (ref != null && (ref instanceof TextContainer))
            {
               StringBuilder  str = new StringBuilder();
               extractRawText((TextContainer) ref, str);
               if (str.length() > 0) {
                  textprocessor.processText(str.toString());
               }
            }
            else
            {
               error("Tref reference '%s' not found", tref.href);
            }
         }

         // Restore state
         statePop();
      }
   }


   //==============================================================================


   private void renderTextPath(SVG.TextPath obj)
   {
      debug("TextPath render");

      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;

      SVG.SvgObject  ref = obj.document.resolveIRI(obj.href);
      if (ref == null)
      {
         error("TextPath reference '%s' not found", obj.href);
         return;
      }

      SVG.Path     pathObj = (SVG.Path) ref;
      Path         path = (new PathConverter(pathObj.d)).getPath();

      if (pathObj.transform != null)
         path.transform(pathObj.transform);

      PathMeasure  measure = new PathMeasure(path, false);

      float  startOffset = (obj.startOffset != null)
              ? obj.startOffset.floatValue(this, measure.getLength()) : 0f;

      // Handle text alignment
      Style.TextAnchor  anchor = getAnchorPosition();
      if (anchor != Style.TextAnchor.Start) {
         float  textWidth = calculateTextWidth(obj);
         if (anchor == Style.TextAnchor.Middle) {
            startOffset -= (textWidth / 2);
         } else {
            startOffset -= textWidth;  // 'End' (right justify)
         }
      }

      checkForGradientsAndPatterns((SvgElement) obj.getTextRoot());
      
      boolean  compositing = pushLayer();

      enumerateTextSpans(obj, new PathTextDrawer(path, startOffset, 0f));

      if (compositing)
         popLayer(obj);
   }


   private class  PathTextDrawer extends PlainTextDrawer
   {
      private Path   path;

      public PathTextDrawer(Path path, float x, float y)
      {
         super(x, y);
         this.path = path;
      }

      @Override
      public void processText(String text)
      {
         if (visible())
         {
            if (state.hasFill)
               canvas.drawTextOnPath(text, path, x, y, state.fillPaint);
            if (state.hasStroke)
               canvas.drawTextOnPath(text, path, x, y, state.strokePaint);
         }

         // Update the current text position
         x += state.fillPaint.measureText(text);
      }
   }


   //==============================================================================


   /*
    * Calculate the approximate width of this line of text.
    * To simplify, we will ignore font changes and just assume that all the text
    * uses the current font.
    */
   private float  calculateTextWidth(TextContainer parentTextObj)
   {
      TextWidthCalculator  proc = new TextWidthCalculator();
      enumerateTextSpans(parentTextObj, proc);
      return proc.x;
   }

   private class  TextWidthCalculator extends TextProcessor
   {
      public float x = 0;

      @Override
      public void processText(String text)
      {
         x += state.fillPaint.measureText(text);
      }
   }


   //==============================================================================


   /*
    * Use the TextDrawer process to determine the bounds of a <text> element
    */
   private class  TextBoundsCalculator extends TextProcessor
   {
      float  x;
      float  y;
      RectF  bbox = new RectF();

      public TextBoundsCalculator(float x, float y)
      {
         this.x = x;
         this.y = y;
      }

      @Override
      public boolean doTextContainer(TextContainer obj)
      {
         if (obj instanceof SVG.TextPath)
         {
            // Since we cheat a bit with our textPath rendering, we need
            // to cheat a bit with our bbox calculation.
            SVG.TextPath  tpath = (SVG.TextPath) obj;
            SVG.SvgObject  ref = obj.document.resolveIRI(tpath.href);
            if (ref == null) {
               error("TextPath path reference '%s' not found", tpath.href);
               return false;
            }
            SVG.Path  pathObj = (SVG.Path) ref;
            Path      path = (new PathConverter(pathObj.d)).getPath();
            if (pathObj.transform != null)
               path.transform(pathObj.transform);
            RectF     pathBounds = new RectF();
            path.computeBounds(pathBounds, true);
            bbox.union(pathBounds);
            return false;
         }
         return true;
      }

      @Override
      public void processText(String text)
      {
         if (visible())
         {
            android.graphics.Rect  rect = new android.graphics.Rect();
            // Get text bounding box (for offset 0)
            state.fillPaint.getTextBounds(text, 0, text.length(), rect);
            RectF  textbounds = new RectF(rect);
            // Adjust bounds to offset at text position
            textbounds.offset(x, y);
            // Merge with accumulated bounding box
            bbox.union(textbounds);
         }

         // Update the current text position
         x += state.fillPaint.measureText(text);
      }
   }


   /*
    * Extract the raw text from a TextContainer. Used by <tref> handler code.
    */
   private void  extractRawText(TextContainer parent, StringBuilder str)
   {
      Iterator<SvgObject>  iter = parent.children.iterator();
      boolean              isFirstChild = true;

      while (iter.hasNext())
      {
         SvgObject  child = iter.next();

         if (child instanceof TextContainer) {
            extractRawText((TextContainer) child, str);
         } else if (child instanceof TextSequence) {
            str.append(textXMLSpaceTransform(((TextSequence) child).text, isFirstChild,
                    !iter.hasNext() /*isLastChild*/));
         }
         isFirstChild = false;
      }
   }
 

   //==============================================================================


   // Process the text string according to the xml:space rules
   private String  textXMLSpaceTransform(String text, boolean isFirstChild, boolean isLastChild)
   {
      if (state.spacePreserve)  // xml:space = "preserve"
         return text.replaceAll("[\\n\\t]", " ");

      // xml:space = "default"
      text = text.replaceAll("\\n", "");
      text = text.replaceAll("\\t", " ");
      //text = text.trim();
      if (isFirstChild)
         text = text.replaceAll("^\\s+",  "");
      if (isLastChild)
         text = text.replaceAll("\\s+$",  "");
      return text.replaceAll("\\s{2,}", " ");
   }


   //==============================================================================


   private void render(SVG.Symbol obj, SVG.Length width, SVG.Length height)
   {
      debug("Symbol render");

      if ((width != null && width.isZero()) ||
          (height != null && height.isZero()))
         return;

      // "If attribute 'preserveAspectRatio' is not specified,
      // then the effect is as if a value of xMidYMid meet were specified."
      PreserveAspectRatio  positioning = (obj.preserveAspectRatio != null)
              ? obj.preserveAspectRatio : PreserveAspectRatio.LETTERBOX;

      updateStyleForElement(state, obj);

      float  _w = (width != null) ? width.floatValueX(this) : state.viewPort.width;
      float  _h = (height != null) ? height.floatValueX(this) : state.viewPort.height;
      state.viewPort = new SVG.Box(0, 0, _w, _h);

      if (!state.style.overflow) {
         setClipRect(state.viewPort.minX, state.viewPort.minY, state.viewPort.width,
                 state.viewPort.height);
      }

      if (obj.viewBox != null) {
         canvas.concat(calculateViewBoxTransform(state.viewPort, obj.viewBox, positioning));
         state.viewBox = obj.viewBox;
      }
      
      boolean  compositing = pushLayer();

      renderChildren(obj, true);

      if (compositing)
         popLayer(obj);

      updateParentBoundingBox(obj);
   }


   //==============================================================================


   private void render(SVG.Image obj)
   {
      debug("Image render");

      if (obj.width == null || obj.width.isZero() ||
          obj.height == null || obj.height.isZero())
         return;

      if (obj.href == null)
         return;

      // "If attribute 'preserveAspectRatio' is not specified,
      // then the effect is as if a value of xMidYMid meet were specified."
      PreserveAspectRatio  positioning = (obj.preserveAspectRatio != null)
              ? obj.preserveAspectRatio : PreserveAspectRatio.LETTERBOX;

      // Locate the referenced image
      Bitmap  image = checkForImageDataURL(obj.href);
      if (image == null)
      {
         SVGExternalFileResolver  fileResolver = document.getFileResolver();
         if (fileResolver == null)
            return;

         image = fileResolver.resolveImage(obj.href);
      }
      if (image == null) {
         error("Could not locate image '%s'", obj.href);
         return;
      }

      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;

      if (obj.transform != null) {
         canvas.concat(obj.transform);
      }

      float  _x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
      float  _y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
      float  _w = obj.width.floatValueX(this);
      float  _h = obj.height.floatValueX(this);
      state.viewPort = new SVG.Box(_x, _y, _w, _h);

      if (!state.style.overflow) {
         setClipRect(state.viewPort.minX, state.viewPort.minY, state.viewPort.width,
                 state.viewPort.height);
      }

      obj.boundingBox = new SVG.Box(0,  0,  image.getWidth(), image.getHeight());
      canvas.concat(calculateViewBoxTransform(state.viewPort, obj.boundingBox, positioning));

      updateParentBoundingBox(obj);

      checkForClipPath(obj);

      boolean  compositing = pushLayer();

      viewportFill();

      canvas.drawBitmap(image, 0, 0, new Paint());

      if (compositing)
         popLayer(obj);
   }


   //==============================================================================


   /*
    * Check for an decode an image encoded in a data URL.
    * We don't handle all permutations of data URLs. Only base64 ones.
    */
   private Bitmap  checkForImageDataURL(String url)
   {
      if (!url.startsWith("data:"))
         return null;
      if (url.length() < 14)
         return null;

      int  comma = url.indexOf(',');
      if (comma == -1 || comma < 12)
         return null;
      if (!";base64".equals(url.substring(comma-7, comma)))
         return null;
      byte[]  imageData = Base64.decode(url.substring(comma+1), Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(imageData, 0,  imageData.length);
   }


   private boolean  display()
   {
      if (state.style.display != null)
        return state.style.display;
      return true;
   }


   private boolean  visible()
   {
      if (state.style.visibility != null)
        return state.style.visibility;
      return true;
   }


   /*
    * Calculate the transform required to fit the supplied viewBox into the current viewPort.
    * See spec section 7.8 for an explanation of how this works.
    * 
    * aspectRatioRule determines where the graphic is placed in the viewPort when aspect ration
    *    is kept.  xMin means left justified, xMid is centred, xMax is right justified etc.
    * slice determines whether we see the whole image or not. True fill the whole viewport.
    *    If slice is false, the image will be "letter-boxed".
    * 
    * Note values in the two Box parameters whould be in user units. If you pass values
    * that are in "objectBoundingBox" space, you will get incorrect results.
    */
   private Matrix calculateViewBoxTransform(Box viewPort, Box viewBox,
                                            PreserveAspectRatio positioning)
   {
      Matrix m = new Matrix();

      if (positioning == null || positioning.getAlignment() == null)
         return m;

      float  xScale = viewPort.width / viewBox.width;
      float  yScale = viewPort.height / viewBox.height;
      float  xOffset = -viewBox.minX;
      float  yOffset = -viewBox.minY;

      // 'none' means scale both dimensions to fit the viewport
      if (positioning.equals(PreserveAspectRatio.STRETCH))
      {
         m.preTranslate(viewPort.minX, viewPort.minY);
         m.preScale(xScale, yScale);
         m.preTranslate(xOffset, yOffset);
         return m;
      }

      // Otherwise, the aspect ratio of the image is kept.
      // What scale are we going to use?
      float  scale = (positioning.getScale() == PreserveAspectRatio.Scale.Slice)
              ? Math.max(xScale,  yScale) : Math.min(xScale,  yScale);
      // What size will the image end up being? 
      float  imageW = viewPort.width / scale;
      float  imageH = viewPort.height / scale;
      // Determine final X position
      switch (positioning.getAlignment())
      {
         case XMidYMin:
         case XMidYMid:
         case XMidYMax:
            xOffset -= (viewBox.width - imageW) / 2;
            break;
         case XMaxYMin:
         case XMaxYMid:
         case XMaxYMax:
            xOffset -= (viewBox.width - imageW);
            break;
         default:
            // nothing to do 
            break;
      }
      // Determine final Y position
      switch (positioning.getAlignment())
      {
         case XMinYMid:
         case XMidYMid:
         case XMaxYMid:
            yOffset -= (viewBox.height - imageH) / 2;
            break;
         case XMinYMax:
         case XMidYMax:
         case XMaxYMax:
            yOffset -= (viewBox.height - imageH);
            break;
         default:
            // nothing to do 
            break;
      }

      m.preTranslate(viewPort.minX, viewPort.minY);
      m.preScale(scale, scale);
      m.preTranslate(xOffset, yOffset);
      return m;
   }


   private boolean  isSpecified(Style style, long flag)
   {
      return (style.specifiedFlags & flag) != 0;
   }


   /*
    * Updates the global style state with the style defined by the current object.
    * Will also update the current paints etc where appropriate.
    */
   private void updateStyle(RendererState state, Style style)
   {
      // Now update each style property we know about
      if (isSpecified(style, SVG.SPECIFIED_COLOR))
      {
         state.style.color = style.color;
      }

      if (isSpecified(style, SVG.SPECIFIED_OPACITY))
      {
         state.style.opacity = style.opacity;
      }

      if (isSpecified(style, SVG.SPECIFIED_FILL))
      {
         state.style.fill = style.fill;
         state.hasFill = (style.fill != null);
      }

      if (isSpecified(style, SVG.SPECIFIED_FILL_OPACITY))
      {
         state.style.fillOpacity = style.fillOpacity;
      }

      // If either fill or its opacity has changed, update the fillPaint
      if (isSpecified(style, SVG.SPECIFIED_FILL | SVG.SPECIFIED_FILL_OPACITY
              | SVG.SPECIFIED_COLOR | SVG.SPECIFIED_OPACITY))
      {
         setPaintColour(state, true, state.style.fill);
      }

      if (isSpecified(style, SVG.SPECIFIED_FILL_RULE))
      {
         state.style.fillRule = style.fillRule;
      }


      if (isSpecified(style, SVG.SPECIFIED_STROKE))
      {
         state.style.stroke = style.stroke;
         state.hasStroke = (style.stroke != null);
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_OPACITY))
      {
         state.style.strokeOpacity = style.strokeOpacity;
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE | SVG.SPECIFIED_STROKE_OPACITY
              | SVG.SPECIFIED_COLOR | SVG.SPECIFIED_OPACITY))
      {
         setPaintColour(state, false, state.style.stroke);
      }

      if (isSpecified(style, SVG.SPECIFIED_VECTOR_EFFECT))
      {
         state.style.vectorEffect = style.vectorEffect;
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_WIDTH))
      {
         state.style.strokeWidth = style.strokeWidth;
         state.strokePaint.setStrokeWidth(state.style.strokeWidth.floatValue(this));
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_LINECAP))
      {
         state.style.strokeLineCap = style.strokeLineCap;
         switch (style.strokeLineCap)
         {
            case Butt:
               state.strokePaint.setStrokeCap(Paint.Cap.BUTT);
               break;
            case Round:
               state.strokePaint.setStrokeCap(Paint.Cap.ROUND);
               break;
            case Square:
               state.strokePaint.setStrokeCap(Paint.Cap.SQUARE);
               break;
            default:
               break;
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_LINEJOIN))
      {
         state.style.strokeLineJoin = style.strokeLineJoin;
         switch (style.strokeLineJoin)
         {
            case Miter:
               state.strokePaint.setStrokeJoin(Paint.Join.MITER);
               break;
            case Round:
               state.strokePaint.setStrokeJoin(Paint.Join.ROUND);
               break;
            case Bevel:
               state.strokePaint.setStrokeJoin(Paint.Join.BEVEL);
               break;
            default:
               break;
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_MITERLIMIT))
      {
         state.style.strokeMiterLimit = style.strokeMiterLimit;
         state.strokePaint.setStrokeMiter(style.strokeMiterLimit);
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_DASHARRAY))
      {
         state.style.strokeDashArray = style.strokeDashArray;
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_DASHOFFSET))
      {
         state.style.strokeDashOffset = style.strokeDashOffset;
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_DASHARRAY | SVG.SPECIFIED_STROKE_DASHOFFSET))
      {
         // Either the dash array or dash offset has changed.
         if (state.style.strokeDashArray == null)
         {
            state.strokePaint.setPathEffect(null);
         }
         else
         {
            float  intervalSum = 0f;
            int    n = state.style.strokeDashArray.length;
            // SVG dash arrays can be odd length, whereas Android dash arrays must have
            // an even length.
            // So we solve the problem by doubling the array length.
            int    arrayLen = (n % 2==0) ? n : n*2;
            float[] intervals = new float[arrayLen];
            for (int i=0; i<arrayLen; i++) {
               intervals[i] = state.style.strokeDashArray[i % n].floatValue(this);
               intervalSum += intervals[i];
            }
            if (intervalSum == 0f) {
               state.strokePaint.setPathEffect(null);
            } else {
               float offset = state.style.strokeDashOffset.floatValue(this);
               if (offset < 0) {
                  // SVG offsets can be negative. Not sure if Android ones can be.
                  // Just in case we will convert it.
                  offset = intervalSum + (offset % intervalSum);
               }
               state.strokePaint.setPathEffect( new DashPathEffect(intervals, offset) );
            }
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_FONT_SIZE))
      {
         float  currentFontSize = getCurrentFontSize();
         state.style.fontSize = style.fontSize;
         state.fillPaint.setTextSize(style.fontSize.floatValue(this, currentFontSize));
         state.strokePaint.setTextSize(style.fontSize.floatValue(this, currentFontSize));
      }

      if (isSpecified(style, SVG.SPECIFIED_FONT_FAMILY))
      {
         state.style.fontFamily = style.fontFamily;
      }

      if (isSpecified(style, SVG.SPECIFIED_FONT_WEIGHT))
      {
         // Font weights are 100,200...900
         if (style.fontWeight == Style.FONT_WEIGHT_LIGHTER && state.style.fontWeight > 100)
            state.style.fontWeight -= 100;
         else if (style.fontWeight == Style.FONT_WEIGHT_BOLDER && state.style.fontWeight < 900)
            state.style.fontWeight += 100;
         else
            state.style.fontWeight = style.fontWeight;
      }

      if (isSpecified(style, SVG.SPECIFIED_FONT_STYLE))
      {
         state.style.fontStyle = style.fontStyle;
      }

      // If typeface, weight or style has changed, update the paint typeface
      if (isSpecified(style, SVG.SPECIFIED_FONT_FAMILY | SVG.SPECIFIED_FONT_WEIGHT
              | SVG.SPECIFIED_FONT_STYLE))
      {
         SVGExternalFileResolver  fileResolver = null;
         Typeface  font = null;

         if (state.style.fontFamily != null && document != null) {
            fileResolver = document.getFileResolver();

            for (String fontName: state.style.fontFamily) {
               font = checkGenericFont(fontName, state.style.fontWeight, state.style.fontStyle);
               if (font == null && fileResolver != null) {
                  font = fileResolver.resolveFont(fontName, state.style.fontWeight,
                          String.valueOf(state.style.fontStyle));
               }
               if (font != null)
                  break;
            }
         }
         if (font == null) {
            // Fall back to default font
            font = checkGenericFont(DEFAULT_FONT_FAMILY, state.style.fontWeight,
                    state.style.fontStyle);
         }
         state.fillPaint.setTypeface(font);
         state.strokePaint.setTypeface(font);
      }

      if (isSpecified(style, SVG.SPECIFIED_TEXT_DECORATION))
      {
         state.style.textDecoration = style.textDecoration;
         state.fillPaint.setStrikeThruText(style.textDecoration == TextDecoration.LineThrough);
         state.fillPaint.setUnderlineText(style.textDecoration == TextDecoration.Underline);
         // There is a bug in Android <= JELLY_BEAN (16) that causes stroked underlines to
         // not be drawn properly. See bug (39511). This has been fixed in JELLY_BEAN_MR1 (4.2)
         if (android.os.Build.VERSION.SDK_INT >= 17) {
            state.strokePaint.setStrikeThruText(style.textDecoration == TextDecoration.LineThrough);
            state.strokePaint.setUnderlineText(style.textDecoration == TextDecoration.Underline);
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_DIRECTION))
      {
         state.style.direction = style.direction;
      }

      if (isSpecified(style, SVG.SPECIFIED_TEXT_ANCHOR))
      {
         state.style.textAnchor = style.textAnchor;
      }

      if (isSpecified(style, SVG.SPECIFIED_OVERFLOW))
      {
         state.style.overflow = style.overflow;
      }

      if (isSpecified(style, SVG.SPECIFIED_MARKER_START))
      {
         state.style.markerStart = style.markerStart;
      }

      if (isSpecified(style, SVG.SPECIFIED_MARKER_MID))
      {
         state.style.markerMid = style.markerMid;
      }

      if (isSpecified(style, SVG.SPECIFIED_MARKER_END))
      {
         state.style.markerEnd = style.markerEnd;
      }

      if (isSpecified(style, SVG.SPECIFIED_DISPLAY))
      {
         state.style.display = style.display;
      }

      if (isSpecified(style, SVG.SPECIFIED_VISIBILITY))
      {
         state.style.visibility = style.visibility;
      }

      if (isSpecified(style, SVG.SPECIFIED_CLIP))
      {
         state.style.clip = style.clip;
      }

      if (isSpecified(style, SVG.SPECIFIED_CLIP_PATH))
      {
         state.style.clipPath = style.clipPath;
      }

      if (isSpecified(style, SVG.SPECIFIED_CLIP_RULE))
      {
         state.style.clipRule = style.clipRule;
      }

      if (isSpecified(style, SVG.SPECIFIED_MASK))
      {
         state.style.mask = style.mask;
      }

      if (isSpecified(style, SVG.SPECIFIED_STOP_COLOR))
      {
         state.style.stopColor = style.stopColor;
      }

      if (isSpecified(style, SVG.SPECIFIED_STOP_OPACITY))
      {
         state.style.stopOpacity = style.stopOpacity;
      }

      if (isSpecified(style, SVG.SPECIFIED_VIEWPORT_FILL))
      {
         state.style.viewportFill = style.viewportFill;
      }

      if (isSpecified(style, SVG.SPECIFIED_VIEWPORT_FILL_OPACITY))
      {
         state.style.viewportFillOpacity = style.viewportFillOpacity;
      }

   }


   private void  setPaintColour(RendererState state, boolean isFill, SvgPaint paint)
   {
      float  paintOpacity = (isFill) ? state.style.fillOpacity : state.style.strokeOpacity;
      int    col;
      if (paint instanceof SVG.Colour) {
         col = ((SVG.Colour) paint).colour;
      } else if (paint instanceof CurrentColor) {
         col = state.style.color.colour;
      } else {
         return;
      }
      col = clamp255(paintOpacity) << 24 | col;
      if (isFill)
         state.fillPaint.setColor(col);
      else
         state.strokePaint.setColor(col);
   }


   private Typeface  checkGenericFont(String fontName, Integer fontWeight, FontStyle fontStyle)
   {
      Typeface font = null;
      int      typefaceStyle;

      boolean  italic = (fontStyle == Style.FontStyle.Italic);
      typefaceStyle = (fontWeight > 500) ? (italic ? Typeface.BOLD_ITALIC : Typeface.BOLD)
                                         : (italic ? Typeface.ITALIC : Typeface.NORMAL);

      if (fontName.equals("serif")) {
         font = Typeface.create(Typeface.SERIF, typefaceStyle);
      } else if (fontName.equals("sans-serif")) {
         font = Typeface.create(Typeface.SANS_SERIF, typefaceStyle);
      } else if (fontName.equals("monospace")) {
         font = Typeface.create(Typeface.MONOSPACE, typefaceStyle);
      } else if (fontName.equals("cursive")) {
         font = Typeface.create(Typeface.SANS_SERIF, typefaceStyle);
      } else if (fontName.equals("fantasy")) {
         font = Typeface.create(Typeface.SANS_SERIF, typefaceStyle);
      }
      return font;
   }


   private int  clamp255(float val)
   {
      int  i = (int)(val * 256f);
      return (i<0) ? 0 : (i>255) ? 255 : i;
   }


   private Path.FillType  getFillTypeFromState()
   {
      if (state.style.fillRule == null)
         return Path.FillType.WINDING;
      switch (state.style.fillRule)
      {
         case EvenOdd:
            return Path.FillType.EVEN_ODD;
         case NonZero:
         default:
            return Path.FillType.WINDING;
      }
   }


   private void  setClipRect(float minX, float minY, float width, float height)
   {
      float  left = minX;
      float  top = minY;
      float  right = minX + width;
      float  bottom = minY + height;

      if (state.style.clip != null) {
         left += state.style.clip.left.floatValueX(this);
         top += state.style.clip.top.floatValueY(this);
         right -= state.style.clip.right.floatValueX(this);
         bottom -= state.style.clip.bottom.floatValueY(this);
      }

      canvas.clipRect(left, top, right, bottom);
   }


   /*
    * Viewport fill colour. A new feature in SVG 1.2.
    */
   private void  viewportFill()
   {
      int    col;
      if (state.style.viewportFill instanceof SVG.Colour) {
         col = ((SVG.Colour) state.style.viewportFill).colour;
      } else if (state.style.viewportFill instanceof CurrentColor) {
         col = state.style.color.colour;
      } else {
         return;
      }
      if (state.style.viewportFillOpacity != null)
         col = clamp255(state.style.viewportFillOpacity) << 24 | col;

      canvas.drawColor(col);
   }


   //==============================================================================

   /*
    *  Convert an internal PathDefinition to an android.graphics.Path object
    */
   private class  PathConverter implements PathInterface
   {
      Path   path = new Path();
      float  lastX, lastY;
      
      public PathConverter(PathDefinition pathDef)
      {
         if (pathDef == null)
            return;
         pathDef.enumeratePath(this);
      }

      public Path  getPath()
      {
         return path;
      }

      @Override
      public void moveTo(float x, float y)
      {
         path.moveTo(x, y);
         lastX = x;
         lastY = y;
      }

      @Override
      public void lineTo(float x, float y)
      {
         path.lineTo(x, y);
         lastX = x;
         lastY = y;
      }

      @Override
      public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3)
      {
         path.cubicTo(x1, y1, x2, y2, x3, y3);
         lastX = x3;
         lastY = y3;
      }

      @Override
      public void quadTo(float x1, float y1, float x2, float y2)
      {
         path.quadTo(x1, y1, x2, y2);
         lastX = x2;
         lastY = y2;
      }

      @Override
      public void arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag,
                        boolean sweepFlag, float x, float y)
      {
         SVGAndroidRenderer.arcTo(lastX, lastY, rx, ry, xAxisRotation, largeArcFlag,
                 sweepFlag, x, y, this);
         lastX = x;
         lastY = y;
      }

      @Override
      public void close()
      {
         path.close();
      }
         
   }


   //=========================================================================
   // Handling of Arcs

   /*
    * SVG arc representation uses "endpoint parameterisation"
    * where we specify the endpoint of the arc.
    * This is to be consistent with the other path commands.
    * However we need to convert this to "centre point parameterisation" in order
    * to calculate the arc. Handily, the SVG spec provides all the required maths
    * in section "F.6 Elliptical arc implementation notes".
    * 
    * Some of this code has been borrowed from the Batik library (Apache-2 license).
    *
    * Note: the original version of this code used doubles. This version uses floats
    * because of some sort Android JIT(?) bug. See Issue #62.
    */

   private static void arcTo(float lastX, float lastY, float rx, float ry, float angle,
                             boolean largeArcFlag, boolean sweepFlag, float x, float y,
                             PathInterface pather)
   {
      if (lastX == x && lastY == y) {
         // If the endpoints (x, y) and (x0, y0) are identical, then this
         // is equivalent to omitting the elliptical arc segment entirely.
         // (behaviour specified by the spec)
         return;
      }

      // Handle degenerate case (behaviour specified by the spec)
      if (rx == 0 || ry == 0) {
         pather.lineTo(x, y);
         return;
      }

      // Sign of the radii is ignored (behaviour specified by the spec)
      rx = Math.abs(rx);
      ry = Math.abs(ry);

      // Convert angle from degrees to radians
      float angleRad = (float) Math.toRadians(angle % 360.0);
      float cosAngle = (float) Math.cos(angleRad);
      float sinAngle = (float) Math.sin(angleRad);
      
      // We simplify the calculations by transforming the arc so that the origin is at the
      // midpoint calculated above followed by a rotation to line up the coordinate axes
      // with the axes of the ellipse.

      // Compute the midpoint of the line between the current and the end point
      float dx2 = (lastX - x) / 2.0f;
      float dy2 = (lastY - y) / 2.0f;

      // Step 1 : Compute (x1', y1') - the transformed start point
      float x1 = (cosAngle * dx2 + sinAngle * dy2);
      float y1 = (-sinAngle * dx2 + cosAngle * dy2);

      float rx_sq = rx * rx;
      float ry_sq = ry * ry;
      float x1_sq = x1 * x1;
      float y1_sq = y1 * y1;

      // Check that radii are large enough.
      // If they are not, the spec says to scale them up so they are.
      // This is to compensate for potential rounding errors/differences
      // between SVG implementations.
      float radiiCheck = x1_sq / rx_sq + y1_sq / ry_sq;
      if (radiiCheck > 1) {
         rx = (float) Math.sqrt(radiiCheck) * rx;
         ry = (float) Math.sqrt(radiiCheck) * ry;
         rx_sq = rx * rx;
         ry_sq = ry * ry;
      }

      // Step 2 : Compute (cx1, cy1) - the transformed centre point
      float sign = (largeArcFlag == sweepFlag) ? -1 : 1;
      float sq = ((rx_sq * ry_sq) - (rx_sq * y1_sq) - (ry_sq * x1_sq)) / ((rx_sq * y1_sq)
              + (ry_sq * x1_sq));
      sq = (sq < 0) ? 0 : sq;
      float coef = (float) (sign * Math.sqrt(sq));
      float cx1 = coef * ((rx * y1) / ry);
      float cy1 = coef * -((ry * x1) / rx);

      // Step 3 : Compute (cx, cy) from (cx1, cy1)
      float sx2 = (lastX + x) / 2.0f;
      float sy2 = (lastY + y) / 2.0f;
      float cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
      float cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

      // Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
      float ux = (x1 - cx1) / rx;
      float uy = (y1 - cy1) / ry;
      float vx = (-x1 - cx1) / rx;
      float vy = (-y1 - cy1) / ry;
      float p, n;

      // Compute the angle start
      n = (float) Math.sqrt((ux * ux) + (uy * uy));
      p = ux; // (1 * ux) + (0 * uy)
      sign = (uy < 0) ? -1.0f : 1.0f;
      float angleStart = (float) Math.toDegrees(sign * Math.acos(p / n));

      // Compute the angle extent
      n = (float) Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
      p = ux * vx + uy * vy;
      sign = (ux * vy - uy * vx < 0) ? -1.0f : 1.0f;
      double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
      if (!sweepFlag && angleExtent > 0) {
         angleExtent -= 360f;
      } else if (sweepFlag && angleExtent < 0) {
         angleExtent += 360f;
      }
      angleExtent %= 360f;
      angleStart %= 360f;

      // Many elliptical arc implementations including the Java2D and Android ones, only
      // support arcs that are axis aligned.  Therefore we need to substitute the arc
      // with bezier curves.  The following method call will generate the beziers for
      // a unit circle that covers the arc angles we want.
      float[]  bezierPoints = arcToBeziers(angleStart, angleExtent);

      // Calculate a transformation matrix that will move and scale these bezier points
      // to the correct location.
      Matrix m = new Matrix();
      m.postScale(rx, ry);
      m.postRotate(angle);
      m.postTranslate(cx, cy);
      m.mapPoints(bezierPoints);

      // The last point in the bezier set should match exactly the last coord pair
      // in the arc (ie: x,y). But considering all the mathematical manipulation we have been doing,
      // it is bound to be off by a tiny fraction. Experiments show that it can be
      // up to around 0.00002.  So why don't we just set it to exactly what it ought to be.
      bezierPoints[bezierPoints.length-2] = x;
      bezierPoints[bezierPoints.length-1] = y;

      // Final step is to add the bezier curves to the path
      for (int i=0; i<bezierPoints.length; i+=6)
      {
         pather.cubicTo(bezierPoints[i], bezierPoints[i+1], bezierPoints[i+2], bezierPoints[i+3],
                 bezierPoints[i+4], bezierPoints[i+5]);
      }
   }


   /*
    * Generate the control points and endpoints for a set of bezier curves that match
    * a circular arc starting from angle 'angleStart' and sweep the angle 'angleExtent'.
    * The circle the arc follows will be centred on (0,0) and have a radius of 1.0.
    * 
    * Each bezier can cover no more than 90 degrees, so the arc will be divided evenly
    * into a maximum of four curves.
    * 
    * The resulting control points will later be scaled and rotated to match the final
    * arc required.
    * 
    * The returned array has the format [x0,y0, x1,y1,...] and excludes the start point
    * of the arc.
    */
   private static float[]  arcToBeziers(double angleStart, double angleExtent)
   {
      int    numSegments = (int) Math.ceil(Math.abs(angleExtent) / 90.0);
      
      angleStart = Math.toRadians(angleStart);
      angleExtent = Math.toRadians(angleExtent);
      float  angleIncrement = (float) (angleExtent / numSegments);
      
      // The length of each control point vector is given by the following formula.
      double  controlLength = 4.0 / 3.0 * Math.sin(angleIncrement / 2.0)
              / (1.0 + Math.cos(angleIncrement / 2.0));
      
      float[] coords = new float[numSegments * 6];
      int     pos = 0;

      for (int i=0; i<numSegments; i++)
      {
         double  angle = angleStart + i * angleIncrement;
         // Calculate the control vector at this angle
         double  dx = Math.cos(angle);
         double  dy = Math.sin(angle);
         // First control point
         coords[pos++]   = (float) (dx - controlLength * dy);
         coords[pos++] = (float) (dy + controlLength * dx);
         // Second control point
         angle += angleIncrement;
         dx = Math.cos(angle);
         dy = Math.sin(angle);
         coords[pos++] = (float) (dx + controlLength * dy);
         coords[pos++] = (float) (dy - controlLength * dx);
         // Endpoint of bezier
         coords[pos++] = (float) dx;
         coords[pos++] = (float) dy;
      }
      return coords;
   }


   //==============================================================================
   // Marker handling
   //==============================================================================


   private class MarkerVector
   {
      public float x, y, dx=0f, dy=0f;

      public MarkerVector(float x, float y, float dx, float dy)
      {
         this.x = x;
         this.y = y;
         // normalise direction vector
         double  len = Math.sqrt( dx*dx + dy*dy );
         if (len != 0) {
            this.dx = (float) (dx / len);
            this.dy = (float) (dy / len);
         }
      }

      public void add(float x, float y)
      {
         // In order to get accurate angles, we have to normalise
         // all vectors before we add them.  As long as they are
         // all the same length, the angles will work out correctly.
         float dx = (x - this.x);
         float dy = (y - this.y);
         double  len = Math.sqrt( dx*dx + dy*dy );
         if (len != 0) {
            this.dx += (float) (dx / len);
            this.dy += (float) (dy / len);
         }
      }

      public void add(MarkerVector v2)
      {
         this.dx += v2.dx;
         this.dy += v2.dy;
      }

      @Override
      public String toString()
      {
         return "("+x+","+y+" "+dx+","+dy+")";
      }
   }
   

   /*
    *  Calculates the positions and orientations of any markers that should be placed
    *  on the given path.
    */
   private class  MarkerPositionCalculator implements PathInterface
   {
      private List<MarkerVector>  markers = new ArrayList<MarkerVector>();
      private float               startX, startY;
      private MarkerVector        lastPos = null;
      private boolean             startArc = false, normalCubic = true;
      private int                 subpathStartIndex = -1;
      private boolean             closepathReAdjustPending;

      
      public MarkerPositionCalculator(PathDefinition pathDef)
      {
         if (pathDef == null)
            return;

         // Generate and add markers for the first N-1 points
         pathDef.enumeratePath(this);

         if (closepathReAdjustPending) {
            // Now correct the start and end marker points of the subpath.
            // They should both be oriented as if this was a midpoint (ie sum the vectors).
            lastPos.add(markers.get(subpathStartIndex));
            // Overwrite start marker. Other (end) marker will be written on exit
            // or at start of next subpath.
            markers.set(subpathStartIndex,  lastPos);
            closepathReAdjustPending = false;
         }
         // Add the marker for the pending last point
         if (lastPos != null) {
            markers.add(lastPos);
         }
      }

      public List<MarkerVector>  getMarkers()
      {
         return markers;
      }

      @Override
      public void moveTo(float x, float y)
      {
         if (closepathReAdjustPending) {
            // Now correct the start and end marker points of the subpath.
            // They should both be oriented as if this was a midpoint (ie sum the vectors).
            lastPos.add(markers.get(subpathStartIndex));
            // Overwrite start marker. Other (end) marker will be written on exit
            // or at start of next subpath.
            markers.set(subpathStartIndex,  lastPos);
            closepathReAdjustPending = false;
         }
         if (lastPos != null) {
            markers.add(lastPos);
         }
         startX = x;
         startY = y;
         lastPos = new MarkerVector(x, y, 0, 0);
         subpathStartIndex = markers.size();
      }

      @Override
      public void lineTo(float x, float y)
      {
         lastPos.add(x, y);
         markers.add(lastPos);
         MarkerVector  newPos = new MarkerVector(x, y, x-lastPos.x, y-lastPos.y);
         lastPos = newPos;
         closepathReAdjustPending = false;
      }

      @Override
      public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3)
      {
         if (normalCubic || startArc) {
            lastPos.add(x1, y1);
            markers.add(lastPos);
            startArc = false;
         }
         MarkerVector  newPos = new MarkerVector(x3, y3, x3-x2, y3-y2);
         lastPos = newPos;
         closepathReAdjustPending = false;
      }

      @Override
      public void quadTo(float x1, float y1, float x2, float y2)
      {
         lastPos.add(x1, y1);
         markers.add(lastPos);
         MarkerVector  newPos = new MarkerVector(x2, y2, x2-x1, y2-y1);
         lastPos = newPos;
         closepathReAdjustPending = false;
      }

      @Override
      public void arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag,
                        boolean sweepFlag, float x, float y)
      {
         // We'll piggy-back on the arc->bezier conversion to get our start and end vectors
         startArc = true;
         normalCubic = false;
         SVGAndroidRenderer.arcTo(lastPos.x, lastPos.y, rx, ry, xAxisRotation,
                 largeArcFlag, sweepFlag, x, y, this);
         normalCubic = true;
         closepathReAdjustPending = false;
      }

      @Override
      public void close()
      {
         markers.add(lastPos);
         lineTo(startX, startY);
         // We may need to readjust the first and last markers on this subpath so that
         // the orientation is a sum of the inward and outward vectors.
         // But this only happens if the path ends or the next subpath starts with a Move.
         // See description of "orient" attribute in section 11.6.2.
         closepathReAdjustPending = true;
      }
         
   }


   private void  renderMarkers(SVG.GraphicsElement obj)
   {
      if (state.style.markerStart == null && state.style.markerMid == null
              && state.style.markerEnd == null)
         return;

      SVG.Marker  _markerStart = null;
      SVG.Marker  _markerMid = null;
      SVG.Marker  _markerEnd = null;

      if (state.style.markerStart != null) {
         SVG.SvgObject  ref = obj.document.resolveIRI(state.style.markerStart);
         if (ref != null)
            _markerStart = (SVG.Marker) ref;
         else
            error("Marker reference '%s' not found", state.style.markerStart);
      }

      if (state.style.markerMid != null) {
         SVG.SvgObject  ref = obj.document.resolveIRI(state.style.markerMid);
         if (ref != null)
            _markerMid = (SVG.Marker) ref;
         else
            error("Marker reference '%s' not found", state.style.markerMid);
      }

      if (state.style.markerEnd != null) {
         SVG.SvgObject  ref = obj.document.resolveIRI(state.style.markerEnd);
         if (ref != null)
            _markerEnd = (SVG.Marker) ref;
         else
            error("Marker reference '%s' not found", state.style.markerEnd);
      }

      List<MarkerVector>  markers = null;
      if (obj instanceof SVG.Path)
         markers = (new MarkerPositionCalculator(((SVG.Path) obj).d)).getMarkers();
      else if (obj instanceof SVG.Line)
         markers = calculateMarkerPositions((SVG.Line) obj);
      else // PolyLine and Polygon
         markers = calculateMarkerPositions((SVG.PolyLine) obj);
      
      if (markers == null)
         return;

      int  markerCount = markers.size();
      if (markerCount == 0)
         return;

      // We don't want the markers to inherit themselves as markers,
      // otherwise we get infinite recursion.
      state.style.markerStart = state.style.markerMid = state.style.markerEnd = null;

      if (_markerStart != null)
         renderMarker(_markerStart, markers.get(0));

      if (_markerMid != null)
      {
         for (int i=1; i<(markerCount-1); i++) {
            renderMarker(_markerMid, markers.get(i));
         }
      }

      if (_markerEnd != null)
         renderMarker(_markerEnd, markers.get(markerCount-1));
   }


   /*
    * Render the given marker type at the given position
    */
   private void renderMarker(Marker marker, MarkerVector pos)
   {
      float  angle = 0f;
      float  unitsScale;

      statePush();

      // Calculate vector angle
      if (marker.orient != null)
      {
         if (Float.isNaN(marker.orient))  // Indicates "auto"
         {
            if (pos.dx != 0 || pos.dy != 0) {
               angle = (float) Math.toDegrees( Math.atan2(pos.dy, pos.dx) );
            }
         } else {
            angle = marker.orient;
         }
      }
      // Calculate units scale
      unitsScale = marker.markerUnitsAreUser ? 1f : state.style.strokeWidth.floatValue(dpi);

      // "Properties inherit into the <marker> element from its ancestors; properties do not
      // inherit from the element referencing the <marker> element." (sect 11.6.2)
      state = findInheritFromAncestorState(marker);

      Matrix m = new Matrix();
      m.preTranslate(pos.x, pos.y);
      m.preRotate(angle);
      m.preScale(unitsScale, unitsScale);
      // Scale and/or translate the marker to fit in the marker viewPort
      float _refX = (marker.refX != null) ? marker.refX.floatValueX(this) : 0f;
      float _refY = (marker.refY != null) ? marker.refY.floatValueY(this) : 0f;
      float _markerWidth = (marker.markerWidth != null)
              ? marker.markerWidth.floatValueX(this) : 3f;
      float _markerHeight = (marker.markerHeight != null)
              ? marker.markerHeight.floatValueY(this) : 3f;

      if (marker.viewBox != null)
      {
         // We now do a simplified version of calculateViewBoxTransform().  For now we will
         // ignore the alignment setting because refX and refY have to be aligned with the
         // marker position, and alignment would complicate the calculations.
         float xScale, yScale;

         xScale = _markerWidth / marker.viewBox.width;
         yScale = _markerHeight / marker.viewBox.height;

         // If we are keeping aspect ratio, then set both scales to the appropriate value
         // depending on 'slice'
         PreserveAspectRatio  positioning = (marker.preserveAspectRatio != null)
                 ? marker.preserveAspectRatio :  PreserveAspectRatio.LETTERBOX;
         if (!positioning.equals(PreserveAspectRatio.STRETCH))
         {
            float  aspectScale = (positioning.getScale() == PreserveAspectRatio.Scale.Slice)
                    ? Math.max(xScale,  yScale) : Math.min(xScale,  yScale);
            xScale = yScale = aspectScale;
         }

         //m.preTranslate(viewPort.minX, viewPort.minY);
         m.preTranslate(-_refX * xScale, -_refY * yScale);
         canvas.concat(m);

         // Now we need to take account of alignment setting, because it affects the
         // size and position of the clip rectangle.
         float  imageW = marker.viewBox.width * xScale;
         float  imageH = marker.viewBox.height * yScale;
         float  xOffset = 0f;
         float  yOffset = 0f;
         switch (positioning.getAlignment())
         {
            case XMidYMin:
            case XMidYMid:
            case XMidYMax:
               xOffset -= (_markerWidth - imageW) / 2;
               break;
            case XMaxYMin:
            case XMaxYMid:
            case XMaxYMax:
               xOffset -= (_markerWidth - imageW);
               break;
            default:
               // nothing to do 
                  break;
         }
         // Determine final Y position
         switch (positioning.getAlignment())
         {
            case XMinYMid:
            case XMidYMid:
            case XMaxYMid:
               yOffset -= (_markerHeight - imageH) / 2;
               break;
            case XMinYMax:
            case XMidYMax:
            case XMaxYMax:
               yOffset -= (_markerHeight - imageH);
               break;
            default:
               // nothing to do 
               break;
         }

         if (!state.style.overflow) {
            setClipRect(xOffset, yOffset, _markerWidth, _markerHeight);
         }

         m.reset();
         m.preScale(xScale, yScale);
         canvas.concat(m);
      }
      else
      {
         // No viewBox provided

         m.preTranslate(-_refX, -_refY);
         canvas.concat(m);

         if (!state.style.overflow) {
            setClipRect(0, 0, _markerWidth, _markerHeight);
         }
      }

      boolean  compositing = pushLayer();

      renderChildren(marker, false);

      if (compositing)
         popLayer(marker);

      statePop();
   }


   /*
    * Determine an elements style based on it's ancestors in the tree rather than
    * it's render time ancestors.
    */
   private RendererState  findInheritFromAncestorState(SvgObject obj)
   {
      RendererState newState = new RendererState();
      updateStyle(newState, Style.getDefaultStyle());
      return findInheritFromAncestorState(obj, newState);
   }


   private RendererState  findInheritFromAncestorState(SvgObject obj, RendererState newState)
   {
      List<SvgElementBase>    ancestors = new ArrayList<SvgElementBase>();

      // Traverse up the document tree adding element styles to a list.
      while (true) {
         if (obj instanceof SvgElementBase) {
            ancestors.add(0, (SvgElementBase) obj);
         }
         if (obj.parent == null)
            break;
         obj = (SvgObject) obj.parent;
      }
      
      // Now apply the ancestor styles in reverse order to a fresh RendererState object
      for (SvgElementBase ancestor: ancestors)
         updateStyleForElement(newState, ancestor);

      // Caller may also need a valid viewBox in order to calculate percentages
      newState.viewBox = document.getRootElement().viewBox;
      if (newState.viewBox == null) {
         newState.viewBox = this.canvasViewPort;
      }

      // May also need a base viewport
      newState.viewPort = this.canvasViewPort;

      // Set the directRendering mode based on what the current state has set
      newState.directRendering = state.directRendering;

      return newState;
   }


   //==============================================================================
   // Gradients
   //==============================================================================


   /*
    * Check for gradient fills or strokes on this object.  These are always relative
    * to the object, so can't be preconfigured. They have to be initialised at the
    * time each object is rendered.
    */
   private void  checkForGradientsAndPatterns(SvgElement obj)
   {
      if (state.style.fill instanceof PaintReference) {
         decodePaintReference(true, obj.boundingBox, (PaintReference) state.style.fill);
      }
      if (state.style.stroke instanceof PaintReference) {
         decodePaintReference(false, obj.boundingBox, (PaintReference) state.style.stroke);
      }
   }


   /*
    * Takes a PaintReference object and generates an appropriate Android Shader object from it.
    */
   private void  decodePaintReference(boolean isFill, Box boundingBox, PaintReference paintref)
   {
      SVG.SvgObject  ref = document.resolveIRI(paintref.href);
      if (ref == null)
      {
         error("%s reference '%s' not found", (isFill ? "Fill":"Stroke"), paintref.href);
         if (paintref.fallback != null) {
            setPaintColour(state, isFill, paintref.fallback);
         } else {
            if (isFill)
               state.hasFill = false;
            else
               state.hasStroke = false;
         }
         return;
      }
      if (ref instanceof SvgLinearGradient)
         makeLinearGradient(isFill, boundingBox, (SvgLinearGradient) ref);
      if (ref instanceof SvgRadialGradient)
         makeRadialGradient(isFill, boundingBox, (SvgRadialGradient) ref);
      if (ref instanceof SolidColor)
         setSolidColor(isFill, (SolidColor) ref);
      //if (ref instanceof SVG.Pattern) {}  // May be needed later if/when we do direct rendering
   }


   private void  makeLinearGradient(boolean isFill, Box boundingBox, SvgLinearGradient gradient)
   {
      if (gradient.href != null)
         fillInChainedGradientFields(gradient, gradient.href);

      boolean  userUnits = (gradient.gradientUnitsAreUser != null && gradient.gradientUnitsAreUser);
      Paint    paint = isFill ? state.fillPaint : state.strokePaint;

      float  _x1,_y1,_x2,_y2;
      if (userUnits)
      {
          Box  viewPortUser = getCurrentViewPortInUserUnits();
         _x1 = (gradient.x1 != null) ? gradient.x1.floatValueX(this): 0f;
         _y1 = (gradient.y1 != null) ? gradient.y1.floatValueY(this): 0f;
         _x2 = (gradient.x2 != null) ? gradient.x2.floatValueX(this): viewPortUser.width; // 100%
         _y2 = (gradient.y2 != null) ? gradient.y2.floatValueY(this): 0f;
      }
      else
      {
         _x1 = (gradient.x1 != null) ? gradient.x1.floatValue(this, 1f): 0f;
         _y1 = (gradient.y1 != null) ? gradient.y1.floatValue(this, 1f): 0f;
         _x2 = (gradient.x2 != null) ? gradient.x2.floatValue(this, 1f): 1f;
         _y2 = (gradient.y2 != null) ? gradient.y2.floatValue(this, 1f): 0f;
      }

      // Push the state
      statePush();

      // Set the style for the gradient (inherits from its own ancestors, not from callee's state)
      state = findInheritFromAncestorState(gradient);

      // Calculate the gradient transform matrix
      Matrix m = new Matrix();
      if (!userUnits)
      {
         m.preTranslate(boundingBox.minX, boundingBox.minY);
         m.preScale(boundingBox.width, boundingBox.height);
      }
      if (gradient.gradientTransform != null)
      {
         m.preConcat(gradient.gradientTransform);
      }

      // Create the colour and position arrays for the shader
      int    numStops = gradient.children.size();
      if (numStops == 0) {
         // If there are no stops defined, we are to treat it as paint = 'none' (see spec 13.2.4)
         statePop();
         if (isFill)
            state.hasFill = false;
         else
            state.hasStroke = false;
         return;
      }

      int[]  colours = new int[numStops];
      float[]  positions = new float[numStops];
      int  i = 0;
      float  lastOffset = -1;
      for (SvgObject child: gradient.children)
      {
         Stop  stop = (Stop) child;
         if (i == 0 || stop.offset >= lastOffset) {
            positions[i] = stop.offset;
            lastOffset = stop.offset;
         } else {
            // Each offset must be equal or greater than the last one.
            // If it doesn't we need to replace it with the previous value.
            positions[i] = lastOffset;
         }

         statePush();

         updateStyleForElement(state, stop);
         Colour col = (SVG.Colour) state.style.stopColor;
         if (col == null)
            col = Colour.BLACK;
         colours[i] = clamp255(state.style.stopOpacity) << 24 | col.colour;
         i++;

         statePop();
      }

      // If gradient vector is zero length, we instead fill with last stop colour
      if ((_x1 == _x2 && _y1 == _y2) || numStops == 1) {
         statePop();
         paint.setColor(colours[numStops - 1]);
         return;
      }

      // Convert spreadMethod->TileMode
      TileMode  tileMode = TileMode.CLAMP;
      if (gradient.spreadMethod != null)
      {
         if (gradient.spreadMethod == GradientSpread.reflect)
            tileMode = TileMode.MIRROR;
         else if (gradient.spreadMethod == GradientSpread.repeat)
            tileMode = TileMode.REPEAT;
      }
      
      statePop();

      // Create shader instance
      LinearGradient  gr = new LinearGradient(_x1, _y1, _x2, _y2, colours, positions, tileMode); 
      gr.setLocalMatrix(m);
      paint.setShader(gr);
   }


   private void  makeRadialGradient(boolean isFill, Box boundingBox, SvgRadialGradient gradient)
   {
      if (gradient.href != null)
         fillInChainedGradientFields(gradient, gradient.href);

      boolean  userUnits = (gradient.gradientUnitsAreUser != null && gradient.gradientUnitsAreUser);
      Paint    paint = isFill ? state.fillPaint : state.strokePaint;

      float  _cx,_cy,_r;
      if (userUnits)
      {
         SVG.Length  fiftyPercent = new SVG.Length(50f, Unit.percent);
         _cx = (gradient.cx != null) ? gradient.cx.floatValueX(this)
                 : fiftyPercent.floatValueX(this);
         _cy = (gradient.cy != null) ? gradient.cy.floatValueY(this)
                 : fiftyPercent.floatValueY(this);
         _r = (gradient.r != null) ? gradient.r.floatValue(this): fiftyPercent.floatValue(this);
      }
      else
      {
         _cx = (gradient.cx != null) ? gradient.cx.floatValue(this, 1f): 0.5f;
         _cy = (gradient.cy != null) ? gradient.cy.floatValue(this, 1f): 0.5f;
         _r = (gradient.r != null) ? gradient.r.floatValue(this, 1f): 0.5f;
      }
      // fx and fy are ignored because Android RadialGradient doesn't support a
      // 'focus' point that is different from cx,cy.

      // Push the state
      statePush();

      // Set the style for the gradient (inherits from its own ancestors, not from callee's state)
      state = findInheritFromAncestorState(gradient);

      // Calculate the gradient transform matrix
      Matrix m = new Matrix();
      if (!userUnits)
      {
         m.preTranslate(boundingBox.minX, boundingBox.minY);
         m.preScale(boundingBox.width, boundingBox.height);
      }
      if (gradient.gradientTransform != null)
      {
         m.preConcat(gradient.gradientTransform);
      }

      // Create the colour and position arrays for the shader
      int    numStops = gradient.children.size();
      if (numStops == 0) {
         // If there are no stops defined, we are to treat it as paint = 'none' (see spec 13.2.4)
         statePop();
         if (isFill)
            state.hasFill = false;
         else
            state.hasStroke = false;
         return;
      }

      int[]  colours = new int[numStops];
      float[]  positions = new float[numStops];
      int  i = 0;
      float  lastOffset = -1;
      for (SvgObject child: gradient.children)
      {
         Stop  stop = (Stop) child;
         if (i == 0 || stop.offset >= lastOffset) {
            positions[i] = stop.offset;
            lastOffset = stop.offset;
         } else {
            // Each offset must be equal or greater than the last one.
            // If it doesn't we need to replace it with the previous value.
            positions[i] = lastOffset;
         }

         statePush();

         updateStyleForElement(state, stop);
         Colour col = (SVG.Colour) state.style.stopColor;
         if (col == null)
            col = Colour.BLACK;
         colours[i] = clamp255(state.style.stopOpacity) << 24 | col.colour;
         i++;

         statePop();
      }

      // If gradient radius is zero, we instead fill with last stop colour
      if (_r == 0 || numStops == 1) {
         statePop();
         paint.setColor(colours[numStops - 1]);
         return;
      }

      // Convert spreadMethod->TileMode
      TileMode  tileMode = TileMode.CLAMP;
      if (gradient.spreadMethod != null)
      {
         if (gradient.spreadMethod == GradientSpread.reflect)
            tileMode = TileMode.MIRROR;
         else if (gradient.spreadMethod == GradientSpread.repeat)
            tileMode = TileMode.REPEAT;
      }

      statePop();

      // Create shader instance
      RadialGradient  gr = new RadialGradient(_cx, _cy, _r, colours, positions, tileMode); 
      gr.setLocalMatrix(m);
      paint.setShader(gr);
   }


   /*
    * Any unspecified fields in this gradient can be 'borrowed' from another
    * gradient specified by the href attribute.
    */
   private void fillInChainedGradientFields(GradientElement gradient, String href)
   {
      // Locate the referenced object
      SVG.SvgObject  ref = gradient.document.resolveIRI(href);
      if (ref == null) {
         // Non-existent
         warn("Gradient reference '%s' not found", href);
         return;
      }
      if (!(ref instanceof GradientElement)) {
         error("Gradient href attributes must point to other gradient elements");
         return;
      }
      if (ref == gradient) {
         error("Circular reference in gradient href attribute '%s'", href);
         return;
      }

      GradientElement  grRef = (GradientElement) ref;

      if (gradient.gradientUnitsAreUser == null)
         gradient.gradientUnitsAreUser = grRef.gradientUnitsAreUser;
      if (gradient.gradientTransform == null)
         gradient.gradientTransform = grRef.gradientTransform;
      if (gradient.spreadMethod == null)
         gradient.spreadMethod = grRef.spreadMethod;
      if (gradient.children.isEmpty())
         gradient.children = grRef.children;

      try
      {
         if (gradient instanceof SvgLinearGradient) {
            fillInChainedGradientFields((SvgLinearGradient) gradient, (SvgLinearGradient) ref);
         } else {
            fillInChainedGradientFields((SvgRadialGradient) gradient, (SvgRadialGradient) ref);
         }
      }
      catch (ClassCastException e) { /* expected - do nothing */ }

      if (grRef.href != null)
         fillInChainedGradientFields(gradient, grRef.href);
   }


   private void fillInChainedGradientFields(SvgLinearGradient gradient, SvgLinearGradient grRef)
   {
      if (gradient.x1 == null)
         gradient.x1 = grRef.x1;
      if (gradient.y1 == null)
         gradient.y1 = grRef.y1;
      if (gradient.x2 == null)
         gradient.x2 = grRef.x2;
      if (gradient.y2 == null)
         gradient.y2 = grRef.y2;
   }


   private void fillInChainedGradientFields(SvgRadialGradient gradient, SvgRadialGradient grRef)
   {
      if (gradient.cx == null)
         gradient.cx = grRef.cx;
      if (gradient.cy == null)
         gradient.cy = grRef.cy;
      if (gradient.r == null)
         gradient.r = grRef.r;
      if (gradient.fx == null)
         gradient.fx = grRef.fx;
      if (gradient.fy == null)
         gradient.fy = grRef.fy;
   }


   private void setSolidColor(boolean isFill, SolidColor ref)
   {
      // Make a Style object that has fill or stroke color values set
      // depending on the value of isFill.
      if (isFill)
      {
        if (isSpecified(ref.baseStyle, SVG.SPECIFIED_SOLID_COLOR))
        {
           state.style.fill = ref.baseStyle.solidColor;
           state.hasFill = (ref.baseStyle.solidColor != null);
        }

        if (isSpecified(ref.baseStyle, SVG.SPECIFIED_SOLID_OPACITY))
        {
           state.style.fillOpacity = ref.baseStyle.solidOpacity;
        }

        // If either fill or its opacity has changed, update the fillPaint
        if (isSpecified(ref.baseStyle, SVG.SPECIFIED_SOLID_COLOR | SVG.SPECIFIED_SOLID_OPACITY))
        {
           setPaintColour(state, isFill, state.style.fill);
        }
      }
      else
      {
        if (isSpecified(ref.baseStyle, SVG.SPECIFIED_SOLID_COLOR))
        {
           state.style.stroke = ref.baseStyle.solidColor;
           state.hasStroke = (ref.baseStyle.solidColor != null);
        }

        if (isSpecified(ref.baseStyle, SVG.SPECIFIED_SOLID_OPACITY))
        {
           state.style.strokeOpacity = ref.baseStyle.solidOpacity;
        }

        // If either fill or its opacity has changed, update the fillPaint
        if (isSpecified(ref.baseStyle, SVG.SPECIFIED_SOLID_COLOR | SVG.SPECIFIED_SOLID_OPACITY))
        {
           setPaintColour(state, isFill, state.style.stroke);
        }
      }
      
   }


   //==============================================================================
   // Clip paths
   //==============================================================================


   private void  checkForClipPath(SvgElement obj)
   {
      checkForClipPath(obj, obj.boundingBox);
   }


   private void  checkForClipPath(SvgElement obj, Box boundingBox)
   {
      if (state.style.clipPath == null)
         return;

      // Locate the referenced object
      SVG.SvgObject  ref = obj.document.resolveIRI(state.style.clipPath);
      if (ref == null) {
         error("ClipPath reference '%s' not found", state.style.clipPath);
         return;
      }

      ClipPath  clipPath = (ClipPath) ref;

      // An empty clipping path will completely clip away the element (sect 14.3.5).
      if (clipPath.children.isEmpty()) {
         canvas.clipRect(0, 0, 0, 0);
         return;
      }

      boolean  userUnits = (clipPath.clipPathUnitsAreUser == null || clipPath.clipPathUnitsAreUser);

      if ((obj instanceof SVG.Group) && !userUnits) {
         warn("<clipPath clipPathUnits=\"objectBoundingBox\"> is not supported when referenced " +
                 "from container elements (like %s)", obj.getClass().getSimpleName());
         return;
      }

      clipStatePush();

      if (!userUnits)
      {
         Matrix m = new Matrix();
         m.preTranslate(boundingBox.minX, boundingBox.minY);
         m.preScale(boundingBox.width, boundingBox.height);
         canvas.concat(m);
      }
      if (clipPath.transform != null)
      {
         canvas.concat(clipPath.transform);
      }

      // "Properties inherit into the <clipPath> element from its ancestors; properties do not
      // inherit from the element referencing the <clipPath> element." (sect 14.3.5)
      state = findInheritFromAncestorState(clipPath);

      checkForClipPath(clipPath);

      Path  combinedPath = new Path();
      for (SvgObject child: clipPath.children)
      {
         addObjectToClip(child, true, combinedPath, new Matrix());
      }
      canvas.clipPath(combinedPath);

      clipStatePop();
   }


   private void addObjectToClip(SvgObject obj, boolean allowUse, Path combinedPath,
                                Matrix combinedPathMatrix)
   {
      if (!display())
         return;

      // Save state
      clipStatePush();

      if (obj instanceof SVG.Use) {
         if (allowUse) {
            addObjectToClip((SVG.Use) obj, combinedPath, combinedPathMatrix);
         } else {
            error("<use> elements inside a <clipPath> cannot reference another <use>");
         }
      } else if (obj instanceof SVG.Path) {
         addObjectToClip((SVG.Path) obj, combinedPath, combinedPathMatrix);
      } else if (obj instanceof SVG.Text) {
         addObjectToClip((SVG.Text) obj, combinedPath, combinedPathMatrix);
      } else if (obj instanceof SVG.GraphicsElement) {
         addObjectToClip((SVG.GraphicsElement) obj, combinedPath, combinedPathMatrix);
      } else {
         error("Invalid %s element found in clipPath definition", obj.getClass().getSimpleName());
      }

      // Restore state
      clipStatePop();
   }


   // The clip state push and pop methods only save the matrix.
   // The normal push/pop save the clip region also which would
   // destroy the clip region we are trying to build.
   private void  clipStatePush()
   {
      // Save matrix and clip
      canvas.save(Canvas.MATRIX_SAVE_FLAG);
      // Save style state
      stateStack.push(state);
      state = (RendererState) state.clone();
   }


   private void  clipStatePop()
   {
      // Restore matrix and clip
      canvas.restore();
      // Restore style state
      state = stateStack.pop();
   }


   private Path.FillType  getClipRuleFromState()
   {
      if (state.style.clipRule == null)
         return Path.FillType.WINDING;
      switch (state.style.clipRule)
      {
         case EvenOdd:
            return Path.FillType.EVEN_ODD;
         case NonZero:
         default:
            return Path.FillType.WINDING;
      }
   }


   private void addObjectToClip(SVG.Path obj, Path combinedPath, Matrix combinedPathMatrix)
   {
      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;

      if (obj.transform != null)
         combinedPathMatrix.preConcat(obj.transform);

      Path  path = (new PathConverter(obj.d)).getPath();

      if (obj.boundingBox == null) {
         obj.boundingBox = calculatePathBounds(path);
      }
      checkForClipPath(obj);

      //path.setFillType(getClipRuleFromState());
      combinedPath.setFillType(getClipRuleFromState());
      combinedPath.addPath(path, combinedPathMatrix);
   }


   private void addObjectToClip(SVG.GraphicsElement obj, Path combinedPath,
                                Matrix combinedPathMatrix)
   {
      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;

      if (obj.transform != null)
         combinedPathMatrix.preConcat(obj.transform);

      Path  path;
      if (obj instanceof SVG.Rect)
         path = makePathAndBoundingBox((SVG.Rect) obj);
      else if (obj instanceof SVG.Circle)
         path = makePathAndBoundingBox((SVG.Circle) obj);
      else if (obj instanceof SVG.Ellipse)
         path = makePathAndBoundingBox((SVG.Ellipse) obj);
      else if (obj instanceof SVG.PolyLine)
         path = makePathAndBoundingBox((SVG.PolyLine) obj);
      else
         return;

      checkForClipPath(obj);

      combinedPath.setFillType(path.getFillType());
      combinedPath.addPath(path, combinedPathMatrix);
   }


   private void addObjectToClip(SVG.Use obj, Path combinedPath, Matrix combinedPathMatrix)
   {
      updateStyleForElement(state, obj);

      if (!display())
         return;
      if (!visible())
         return;

      if (obj.transform != null)
         combinedPathMatrix.preConcat(obj.transform);

      // Locate the referenced object
      SVG.SvgObject  ref = obj.document.resolveIRI(obj.href);
      if (ref == null) {
         error("Use reference '%s' not found", obj.href);
         return;
      }

      checkForClipPath(obj);
      
      addObjectToClip(ref, false, combinedPath, combinedPathMatrix);
   }


   private void addObjectToClip(SVG.Text obj, Path combinedPath, Matrix combinedPathMatrix)
   {
      updateStyleForElement(state, obj);

      if (!display())
         return;

      if (obj.transform != null)
         combinedPathMatrix.preConcat(obj.transform);

      // Get the first coordinate pair from the lists in the x and y properties.
      float  x = (obj.x == null || obj.x.size() == 0) ? 0f : obj.x.get(0).floatValueX(this);
      float  y = (obj.y == null || obj.y.size() == 0) ? 0f : obj.y.get(0).floatValueY(this);
      float  dx = (obj.dx == null || obj.dx.size() == 0) ? 0f : obj.dx.get(0).floatValueX(this);
      float  dy = (obj.dy == null || obj.dy.size() == 0) ? 0f : obj.dy.get(0).floatValueY(this);

      // Handle text alignment
      if (state.style.textAnchor != Style.TextAnchor.Start) {
         float  textWidth = calculateTextWidth(obj);
         if (state.style.textAnchor == Style.TextAnchor.Middle) {
            x -= (textWidth / 2);
         } else {
            x -= textWidth;  // 'End' (right justify)
         }
      }

      if (obj.boundingBox == null) {
         TextBoundsCalculator  proc = new TextBoundsCalculator(x, y);
         enumerateTextSpans(obj, proc);
         obj.boundingBox = new Box(proc.bbox.left, proc.bbox.top, proc.bbox.width(),
                 proc.bbox.height());
      }
      checkForClipPath(obj);

      Path  textAsPath = new Path();
      enumerateTextSpans(obj, new PlainTextToPath(x + dx, y + dy, textAsPath));

      combinedPath.setFillType(getClipRuleFromState());
      combinedPath.addPath(textAsPath, combinedPathMatrix);
   }


   private class  PlainTextToPath extends TextProcessor
   {
      public float   x;
      public float   y;
      public Path    textAsPath;

      public PlainTextToPath(float x, float y, Path textAsPath)
      {
         this.x = x;
         this.y = y;
         this.textAsPath = textAsPath;
      }

      @Override
      public boolean doTextContainer(TextContainer obj)
      {
         if (obj instanceof SVG.TextPath)
         {
            warn("Using <textPath> elements in a clip path is not supported.");
            return false;
         }
         return true;
      }

      @Override
      public void processText(String text)
      {
         if (visible())
         {
            //state.fillPaint.getTextPath(text, 0, text.length(), x, y, textAsPath);
            Path spanPath = new Path();
            state.fillPaint.getTextPath(text, 0, text.length(), x, y, spanPath);
            textAsPath.addPath(spanPath);
         }

         // Update the current text position
         x += state.fillPaint.measureText(text);
      }
   }


   //==============================================================================
   // Convert the different shapes to paths
   //==============================================================================


   private Path  makePathAndBoundingBox(Line obj)
   {
      float x1 = (obj.x1 == null) ? 0 : obj.x1.floatValueX(this);
      float y1 = (obj.y1 == null) ? 0 : obj.y1.floatValueY(this);
      float x2 = (obj.x2 == null) ? 0 : obj.x2.floatValueX(this);
      float y2 = (obj.y2 == null) ? 0 : obj.y2.floatValueY(this);

      if (obj.boundingBox == null) {
         obj.boundingBox = new Box(Math.min(x1, y1), Math.min(y1, y2), Math.abs(x2-x1),
                 Math.abs(y2-y1));
      }

      Path  p = new Path();
      p.moveTo(x1, y1);
      p.lineTo(x2, y2);
      return p;
   }


   private Path  makePathAndBoundingBox(Rect obj)
   {
      float x, y, w, h, rx, ry;

      if (obj.rx == null && obj.ry == null) {
         rx = 0;
         ry = 0;
      } else if (obj.rx == null) {
         rx = ry = obj.ry.floatValueY(this);
      } else if (obj.ry == null) {
         rx = ry = obj.rx.floatValueX(this);
      } else {
         rx = obj.rx.floatValueX(this);
         ry = obj.ry.floatValueY(this);
      }
      rx = Math.min(rx, obj.width.floatValueX(this) / 2f);
      ry = Math.min(ry, obj.height.floatValueY(this) / 2f);
      x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
      y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
      w = obj.width.floatValueX(this);
      h = obj.height.floatValueY(this);

      if (obj.boundingBox == null) {
         obj.boundingBox = new Box(x, y, w, h);
      }

      float  right = x + w;
      float  bottom = y + h;

      Path  p = new Path();
      if (rx == 0 || ry == 0)
      {
         // Simple rect
         p.moveTo(x, y);
         p.lineTo(right, y);
         p.lineTo(right, bottom);
         p.lineTo(x, bottom);
         p.lineTo(x, y);
      }
      else
      {
         // Rounded rect
         
         // Bexier control point lengths for a 90 degress arc
         float  cpx = rx * BEZIER_ARC_FACTOR;
         float  cpy = ry * BEZIER_ARC_FACTOR;

         p.moveTo(x, y+ry);
         p.cubicTo(x, y+ry-cpy, x+rx-cpx, y, x+rx, y);
         p.lineTo(right-rx, y);
         p.cubicTo(right-rx+cpx, y, right, y+ry-cpy, right, y+ry);
         p.lineTo(right, bottom-ry);
         p.cubicTo(right, bottom-ry+cpy, right-rx+cpx, bottom, right-rx, bottom);
         p.lineTo(x+rx, bottom);
         p.cubicTo(x+rx-cpx, bottom, x, bottom-ry+cpy, x, bottom-ry);
         p.lineTo(x, y+ry);
      }
      p.close();
      return p;
   }


   private Path makePathAndBoundingBox(SVG.Circle obj)
   {
      float  cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
      float  cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
      float  r = obj.r.floatValue(this);

      float  left = cx - r;
      float  top = cy - r;
      float  right = cx + r;
      float  bottom = cy + r;

      if (obj.boundingBox == null) {
         obj.boundingBox = new Box(left, top, r*2, r*2);
      }

      float  cp = r * BEZIER_ARC_FACTOR;

      Path  p = new Path();
      p.moveTo(cx, top);
      p.cubicTo(cx+cp, top, right, cy-cp, right, cy);
      p.cubicTo(right, cy+cp, cx+cp, bottom, cx, bottom);
      p.cubicTo(cx-cp, bottom, left, cy+cp, left, cy);
      p.cubicTo(left, cy-cp, cx-cp, top, cx, top);
      p.close();
      return p;
   }


   private Path makePathAndBoundingBox(SVG.Ellipse obj)
   {
      float  cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
      float  cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
      float  rx = obj.rx.floatValueX(this);
      float  ry = obj.ry.floatValueY(this);

      float  left = cx - rx;
      float  top = cy - ry;
      float  right = cx + rx;
      float  bottom = cy + ry;

      if (obj.boundingBox == null) {
         obj.boundingBox = new Box(left, top, rx*2, ry*2);
      }

      float  cpx = rx * BEZIER_ARC_FACTOR;
      float  cpy = ry * BEZIER_ARC_FACTOR;

      Path  p = new Path();
      p.moveTo(cx, top);
      p.cubicTo(cx+cpx, top, right, cy-cpy, right, cy);
      p.cubicTo(right, cy+cpy, cx+cpx, bottom, cx, bottom);
      p.cubicTo(cx-cpx, bottom, left, cy+cpy, left, cy);
      p.cubicTo(left, cy-cpy, cx-cpx, top, cx, top);
      p.close();
      return p;
   }


   private Path makePathAndBoundingBox(SVG.PolyLine obj)
   {
      Path  path = new Path();

      path.moveTo(obj.points[0], obj.points[1]);
      for (int i=2; i<obj.points.length; i+=2) {
         path.lineTo(obj.points[i], obj.points[i+1]);
      }
      if (obj instanceof SVG.Polygon)
         path.close();

      if (obj.boundingBox == null) {
         obj.boundingBox = calculatePathBounds(path);
      }

      path.setFillType(getClipRuleFromState());
      return path;
   }


   //==============================================================================
   // Pattern fills
   //==============================================================================


   /*
    * Fill a path with a pattern by setting the path as a clip path and
    * drawing the pattern element as a repeating tile inside it.
    */
   private void  fillWithPattern(SvgElement obj, Path path, Pattern pattern)
   {
      boolean      patternUnitsAreUser = (pattern.patternUnitsAreUser != null
              && pattern.patternUnitsAreUser);
      float        x, y, w, h;
      float        originX, originY;

      if (pattern.href != null)
         fillInChainedPatternFields(pattern, pattern.href);

      if (patternUnitsAreUser)
      {
         x = (pattern.x != null) ? pattern.x.floatValueX(this): 0f;
         y = (pattern.y != null) ? pattern.y.floatValueY(this): 0f;
         w = (pattern.width != null) ? pattern.width.floatValueX(this): 0f;
         h = (pattern.height != null) ? pattern.height.floatValueY(this): 0f;
      }
      else
      {
         // Convert objectBoundingBox space to user space
         x = (pattern.x != null) ? pattern.x.floatValue(this, 1f): 0f;
         y = (pattern.y != null) ? pattern.y.floatValue(this, 1f): 0f;
         w = (pattern.width != null) ? pattern.width.floatValue(this, 1f): 0f;
         h = (pattern.height != null) ? pattern.height.floatValue(this, 1f): 0f;
         x = obj.boundingBox.minX + x * obj.boundingBox.width;
         y = obj.boundingBox.minY + y * obj.boundingBox.height;
         w *= obj.boundingBox.width;
         h *= obj.boundingBox.height;
      }
      if (w == 0 || h == 0)
         return;

      // "If attribute 'preserveAspectRatio' is not specified,
      // then the effect is as if a value of xMidYMid meet were specified."
      PreserveAspectRatio  positioning = (pattern.preserveAspectRatio != null)
              ? pattern.preserveAspectRatio : PreserveAspectRatio.LETTERBOX;

      // Push the state
      statePush();
      // Set path as the clip region
      canvas.clipPath(path);

      // Set the style for the pattern (inherits from its own ancestors, not from callee's state)
      RendererState  baseState = new RendererState();
      updateStyle(baseState, Style.getDefaultStyle());
      baseState.style.overflow = false;    // By default patterns do not overflow
      state = findInheritFromAncestorState(pattern, baseState);

      // The bounds of the area we need to cover with pattern to ensure that our shape is filled
      Box  patternArea = obj.boundingBox;
      // Apply the patternTransform
      if (pattern.patternTransform != null)
      {
         canvas.concat(pattern.patternTransform);
         
         // A pattern transform will affect the area we need to cover with the pattern.
         // So we need to alter the area bounding rectangle.
         Matrix inverse = new Matrix();
         if (pattern.patternTransform.invert(inverse)) {
            float[] pts = {obj.boundingBox.minX, obj.boundingBox.minY,
                           obj.boundingBox.maxX(), obj.boundingBox.minY,
                           obj.boundingBox.maxX(), obj.boundingBox.maxY(),
                           obj.boundingBox.minX, obj.boundingBox.maxY()};
            inverse.mapPoints(pts);
            // Find the bounding box of the shape created by the inverse transform 
            RectF  rect = new RectF(pts[0], pts[1], pts[0], pts[1]);
            for (int i=2; i<=6; i+=2) {
               if (pts[i] < rect.left) rect.left = pts[i]; 
               if (pts[i] > rect.right) rect.right = pts[i]; 
               if (pts[i+1] < rect.top) rect.top = pts[i+1]; 
               if (pts[i+1] > rect.bottom) rect.bottom = pts[i+1]; 
            }
            patternArea = new Box(rect.left, rect.top, rect.right-rect.left, rect.bottom-rect.top);
         }
      }
      // Calculate the pattern origin
      originX = x + (float) Math.floor((patternArea.minX - x) / w) * w;
      originY = y + (float) Math.floor((patternArea.minY - y) / h) * h;
      // For each Y step, then each X step
      float  right = patternArea.maxX();
      float  bottom = patternArea.maxY();
      Box    stepViewBox = new Box(0,0,w,h);
      for (float stepY = originY; stepY < bottom; stepY += h)
      {
         for (float stepX = originX; stepX < right; stepX += w)
         {
            stepViewBox.minX = stepX;
            stepViewBox.minY = stepY;
            // Push the state
            statePush();
            // Set pattern clip rectangle if appropriate
            if (!state.style.overflow) {
               setClipRect(stepViewBox.minX,
                       stepViewBox.minY,
                       stepViewBox.width,
                       stepViewBox.height);
            }
            // Calculate and set the viewport for each instance of the pattern
            if (pattern.viewBox != null)
            {
               canvas.concat(calculateViewBoxTransform(stepViewBox, pattern.viewBox, positioning));
            }
            else
            {
               boolean  patternContentUnitsAreUser = (pattern.patternContentUnitsAreUser == null
                       || pattern.patternContentUnitsAreUser);
               // Simple translate of pattern to step position
               canvas.translate(stepX, stepY);
               if (!patternContentUnitsAreUser) {
                  canvas.scale(obj.boundingBox.width, obj.boundingBox.height);
               }
            }

            boolean  compositing = pushLayer();

            // Render the pattern
            for (SVG.SvgObject child: pattern.children) {
               render(child);
            }

            if (compositing)
               popLayer(pattern);

            // Pop the state
            statePop();
         }
      }
      // Pop the state
      statePop();
   }


   /*
    * Any unspecified fields in this pattern can be 'borrowed' from another
    * pattern specified by the href attribute.
    */
   private void fillInChainedPatternFields(Pattern pattern, String href)
   {
      // Locate the referenced object
      SVG.SvgObject  ref = pattern.document.resolveIRI(href);
      if (ref == null) {
         // Non-existent
         warn("Pattern reference '%s' not found", href);
         return;
      }
      if (!(ref instanceof Pattern)) {
         error("Pattern href attributes must point to other pattern elements");
         return;
      }
      if (ref == pattern) {
         error("Circular reference in pattern href attribute '%s'", href);
         return;
      }

      Pattern  pRef = (Pattern) ref;

      if (pattern.patternUnitsAreUser == null)
         pattern.patternUnitsAreUser = pRef.patternUnitsAreUser;
      if (pattern.patternContentUnitsAreUser == null)
         pattern.patternContentUnitsAreUser = pRef.patternContentUnitsAreUser;
      if (pattern.patternTransform == null)
         pattern.patternTransform = pRef.patternTransform;
      if (pattern.x == null)
         pattern.x = pRef.x;
      if (pattern.y == null)
         pattern.y = pRef.y;
      if (pattern.width == null)
         pattern.width = pRef.width;
      if (pattern.height == null)
         pattern.height = pRef.height;
      // attributes from superclasses
      if (pattern.children.isEmpty())
         pattern.children = pRef.children;
      if (pattern.viewBox == null)
         pattern.viewBox = pRef.viewBox;
      if (pattern.preserveAspectRatio == null) {
         pattern.preserveAspectRatio = pRef.preserveAspectRatio;
      }

      if (pRef.href != null)
         fillInChainedPatternFields(pattern, pRef.href);
   }


   //==============================================================================
   // Masks
   //==============================================================================


   /*
    * Render the contents of a mask element.
    */
   private void  renderMask(SVG.Mask mask, SvgElement obj)
   {
      debug("Mask render");

      boolean      maskUnitsAreUser = (mask.maskUnitsAreUser != null && mask.maskUnitsAreUser);
      float        x, y, w, h;

      if (maskUnitsAreUser)
      {
         w = (mask.width != null) ? mask.width.floatValueX(this): obj.boundingBox.width;
         h = (mask.height != null) ? mask.height.floatValueY(this): obj.boundingBox.height;
         x = (mask.x != null) ? mask.x.floatValueX(this)
                 : (float)(obj.boundingBox.minX - 0.1 * obj.boundingBox.width);
         y = (mask.y != null) ? mask.y.floatValueY(this)
                 : (float)(obj.boundingBox.minY - 0.1 * obj.boundingBox.height);
      }
      else
      {
         // Convert objectBoundingBox space to user space
         x = (mask.x != null) ? mask.x.floatValue(this, 1f): -0.1f;
         y = (mask.y != null) ? mask.y.floatValue(this, 1f): -0.1f;
         w = (mask.width != null) ? mask.width.floatValue(this, 1f): 1.2f;
         h = (mask.height != null) ? mask.height.floatValue(this, 1f): 1.2f;
         x = obj.boundingBox.minX + x * obj.boundingBox.width;
         y = obj.boundingBox.minY + y * obj.boundingBox.height;
         w *= obj.boundingBox.width;
         h *= obj.boundingBox.height;
      }
      if (w == 0 || h == 0)
         return;

      // Push the state
      statePush();

      state = findInheritFromAncestorState(mask);
      // Set the style for the pattern (inherits from its own ancestors, not from callee's state)
      // The 'opacity', 'filter' and 'display' properties
      // do not apply to the 'mask' element" (sect 14.4)
      // Next line is not actually needed since we aren't calling
      // pushLayer() here. Kept for future reference.
      state.style.opacity = 1f;
      //state.style.filter = null;

      boolean  maskContentUnitsAreUser = (mask.maskContentUnitsAreUser == null
              || mask.maskContentUnitsAreUser);
      if (!maskContentUnitsAreUser) {
         canvas.translate(obj.boundingBox.minX, obj.boundingBox.minY);
         canvas.scale(obj.boundingBox.width, obj.boundingBox.height);
      }

      // Render the mask
      renderChildren(mask, false);

      // Pop the state
      statePop();
   }


}
