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
import java.util.List;
import java.util.Locale;

import org.xml.sax.SAXException;

import android.util.Log;

import com.caverock.androidsvg.SVG.SvgContainer;
import com.caverock.androidsvg.SVG.SvgElementBase;
import com.caverock.androidsvg.SVG.SvgObject;
import com.caverock.androidsvg.SVGParser.TextScanner;

/**
 * A very simple CSS parser that is not very compliant with the CSS spec but
 * hopefully parses almost all the CSS we are likely to strike in an SVG file.
 * The main goals are to (a) be small, and (b) parse the CSS in a Corel Draw SVG file.
 * 
 * @hide
 */
public class CSSParser
{
   private static final String  TAG = "AndroidSVG CSSParser";

   private static final String  ID = "id";
   private static final String  CLASS = "class";

   private MediaType  rendererMediaType = null;

   private boolean  inMediaRule = false;


   public enum MediaType
   {
      all,
      aural,
      braille,
      embossed,
      handheld,
      print,
      projection,
      screen,
      tty,
      tv
   }

   private enum Combinator
   {
      DESCENDANT,  // E F
      CHILD,       // E > F
      FOLLOWS      // E + F
   }

   private enum AttribOp
   {
      EXISTS,     // *[foo]
      EQUALS,     // *[foo=bar]
      INCLUDES,   // *[foo~=bar]
      DASHMATCH,  // *[foo|=bar]
   }

   public static class Attrib
   {
      public String    name = null;
      public AttribOp  operation;
      public String    value = null;
      
      public Attrib(String name, AttribOp op, String value)
      {
         this.name = name;
         this.operation = op;
         this.value = value;
      }
   }

   private static class SimpleSelector
   {
      public Combinator    combinator = null;
      public String        tag = null;       // null means "*"
      public List<Attrib>  attribs = null;
      public List<String>  pseudos = null;

      public SimpleSelector(Combinator combinator, String tag)
      {
         this.combinator = (combinator != null) ? combinator : Combinator.DESCENDANT;
         this.tag = tag;
      }

      public void  addAttrib(String attrName, AttribOp op, String attrValue)
      {
         if (attribs == null)
            attribs = new ArrayList<Attrib>();
         attribs.add(new Attrib(attrName, op, attrValue));
      }

      public void  addPseudo(String pseudo)
      {
         if (pseudos == null)
            pseudos = new ArrayList<String>();
         pseudos.add(pseudo);
      }

      @Override
      public String toString()
      {
         StringBuilder sb = new StringBuilder();
         if (combinator == Combinator.CHILD)
            sb.append("> ");
         else if (combinator == Combinator.FOLLOWS)
            sb.append("+ ");
         sb.append((tag == null) ? "*" : tag);
         if (attribs != null) {
            for (Attrib attr: attribs) {
               sb.append('[').append(attr.name);
               switch(attr.operation) {
                  case EQUALS: sb.append('=').append(attr.value); break;
                  case INCLUDES: sb.append("~=").append(attr.value); break;
                  case DASHMATCH: sb.append("|=").append(attr.value); break;
                  default: break;
               }
               sb.append(']');
            }
         }
         if (pseudos != null) {
            for (String pseu: pseudos)
               sb.append(':').append(pseu);
         }
         return sb.toString();
      }
   }

   public static class  Ruleset
   {
      private List<Rule>  rules = null;

      // Add a rule to the ruleset. The position at which it is inserted
      // is determined by its specificity value.
      public void  add(Rule rule)
      {
         if (this.rules == null)
            this.rules = new ArrayList<Rule>();
         for (int i = 0; i < rules.size(); i++)
         {
            Rule  nextRule = rules.get(i);
            if (nextRule.selector.specificity > rule.selector.specificity) {
               rules.add(i, rule);
               return;
            }
         }
         rules.add(rule);
      }

      public void  addAll(Ruleset rules)
      {
         if (rules.rules == null)
            return;
         if (this.rules == null)
            this.rules = new ArrayList<Rule>(rules.rules.size());
         for (Rule rule: rules.rules) {
            this.rules.add(rule);
         }
      }

      public List<Rule>  getRules()
      {
         return this.rules;
      }

      public boolean  isEmpty()
      {
         return this.rules == null || this.rules.isEmpty();
      }

      @Override
      public String toString()
      {
         if (rules == null)
            return "";
         StringBuilder sb = new StringBuilder();
         for (Rule rule: rules)
            sb.append(rule.toString()).append('\n');
         return sb.toString();
      }
   }


   public static class  Rule
   {
      public Selector   selector = null;
      public SVG.Style  style = null;
      
      public Rule(Selector selector, SVG.Style style)
      {
         this.selector = selector;
         this.style = style;
      }

      @Override
      public String toString()
      {
         StringBuilder sb = new StringBuilder();
         return sb.append(selector).append(" {}").toString();
      }
   }


   public static class Selector
   {
      public List<SimpleSelector>  selector = null;
      public int                   specificity = 0;
      
      public void  add(SimpleSelector part)
      {
         if (this.selector == null)
            this.selector = new ArrayList<SimpleSelector>();
         this.selector.add(part);
      }

      public int size()
      {
         return (this.selector == null) ? 0 : this.selector.size();
      }

      public SimpleSelector get(int i)
      {
         return this.selector.get(i);
      }

      public boolean isEmpty()
      {
         return (this.selector == null) ? true : this.selector.isEmpty();
      }

      // Methods for accumulating a specificity value as SimpleSelector entries are added.
      public void  addedIdAttribute()
      {
         specificity += 10000;
      }

      public void  addedAttributeOrPseudo()
      {
         specificity += 100;
      }

      public void  addedElement()
      {
         specificity += 1;
      }

      @Override
      public String toString()
      {
         StringBuilder  sb = new StringBuilder();
         for (SimpleSelector sel: selector)
            sb.append(sel).append(' ');
         return sb.append('(').append(specificity).append(')').toString();
      }
   }


   //===========================================================================================


   
   public CSSParser(MediaType rendererMediaType)
   {
      this.rendererMediaType = rendererMediaType;
   }


   public Ruleset  parse(String sheet) throws SAXException
   {
      CSSTextScanner  scan = new CSSTextScanner(sheet);
      scan.skipWhitespace();

      return parseRuleset(scan);
   }


   public static boolean mediaMatches(String mediaListStr, MediaType rendererMediaType)
           throws SAXException
   {
      CSSTextScanner  scan = new CSSTextScanner(mediaListStr);
      scan.skipWhitespace();
      List<MediaType>  mediaList = parseMediaList(scan);
      if (!scan.empty())
         throw new SAXException("Invalid @media type list");
      return mediaMatches(mediaList, rendererMediaType);
   }


   //==============================================================================


   private static void  warn(String format, Object... args)
   {
      Log.w(TAG, String.format(format, args));
   }


   /*
   private static void  error(String format, Object... args)
   {
      Log.e(TAG, String.format(format, args));
   }


   private static void  debug(String format, Object... args)
   {
      if (LibConfig.DEBUG)
         Log.d(TAG, String.format(format, args));
   }
   */


   //==============================================================================
   
   
   private static class CSSTextScanner extends TextScanner
   {
      public CSSTextScanner(String input)
      {
         super(input.replaceAll("(?s)/\\*.*?\\*/", ""));  // strip all block comments
      }

      /*
       * Scans for a CSS 'ident' identifier.
       */
      public String  nextIdentifier()
      {
         int  end = scanForIdentifier();
         if (end == position)
            return null;
         String result = input.substring(position, end);
         position = end;
         return result;
      }
         
      private int  scanForIdentifier()
      {
         if (empty())
            return position;
         int  start = position;
         int  lastValidPos = position;

         int  ch = input.charAt(position);
         if (ch == '-')
            ch = advanceChar();
         // nmstart
         if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch == '_'))
         {
            ch = advanceChar();
            // nmchar
            while ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9') || (ch == '-') || (ch == '_')) {
               ch = advanceChar();
            }
            lastValidPos = position;
         }
         position = start;
         return lastValidPos;
      }

      /*
       * Scans for a CSS 'simple selector'.
       * Returns true if it found one.
       * Returns false if there was an error or the input is empty.
       */
      public boolean  nextSimpleSelector(Selector selector) throws SAXException
      {
         if (empty())
            return false;

         int             start = position;
         Combinator      combinator = null;
         SimpleSelector  selectorPart = null;

         if (!selector.isEmpty())
         {
            if (consume('>')) {
               combinator = Combinator.CHILD;
               skipWhitespace();
            } else if (consume('+')) {
               combinator = Combinator.FOLLOWS;
               skipWhitespace();
            }
         }

         if (consume('*')) {
            selectorPart = new SimpleSelector(combinator, null);
         } else {
            String tag = nextIdentifier();
            if (tag != null) {
               selectorPart = new SimpleSelector(combinator, tag);
               selector.addedElement();
            }
         }

         while (!empty())
         {
            if (consume('.'))
            {
               // ".foo" is equivalent to *[class="foo"]
               if (selectorPart == null)
                  selectorPart = new SimpleSelector(combinator, null);
               String  value = nextIdentifier();
               if (value == null)
                  throw new SAXException("Invalid \".class\" selector in <style> element");
               selectorPart.addAttrib(CLASS, AttribOp.EQUALS, value);
               selector.addedAttributeOrPseudo();
               continue;
            }

            if (consume('#'))
            {
               // "#foo" is equivalent to *[id="foo"]
               if (selectorPart == null)
                  selectorPart = new SimpleSelector(combinator, null);
               String  value = nextIdentifier();
               if (value == null)
                  throw new SAXException("Invalid \"#id\" selector in <style> element");
               selectorPart.addAttrib(ID, AttribOp.EQUALS, value);
               selector.addedIdAttribute();
            }

            if (selectorPart == null)
               break;

            // Now check for attribute selection and pseudo selectors   
            if (consume('['))
            {
               skipWhitespace();
               String  attrName = nextIdentifier();
               String  attrValue = null;
               if (attrName == null)
                  throw new SAXException("Invalid attribute selector in <style> element");
               skipWhitespace();
               AttribOp  op = null;
               if (consume('='))
                  op = AttribOp.EQUALS;
               else if (consume("~="))
                  op = AttribOp.INCLUDES;
               else if (consume("|="))
                  op = AttribOp.DASHMATCH;
               if (op != null) {
                  skipWhitespace();
                  attrValue = nextAttribValue();
                  if (attrValue == null)
                     throw new SAXException("Invalid attribute selector in <style> element");
                  skipWhitespace();
               }
               if (!consume(']'))
                  throw new SAXException("Invalid attribute selector in <style> element");
               selectorPart.addAttrib(attrName, (op == null) ? AttribOp.EXISTS : op, attrValue);
               selector.addedAttributeOrPseudo();
               continue;
            }

            if (consume(':'))
            {
               // skip pseudo
               int  pseudoStart = position;
               if (nextIdentifier() != null) {
                  if (consume('(')) {
                     skipWhitespace();
                     if (nextIdentifier() != null) {
                        skipWhitespace();
                        if (!consume(')')) {
                           position = pseudoStart - 1;
                           break;
                        }
                     }
                  }
                  selectorPart.addPseudo(input.substring(pseudoStart, position));
                  selector.addedAttributeOrPseudo();
               }
            }

            break;
         }

         if (selectorPart != null)
         {
            selector.add(selectorPart);
            return true;
         }

         // Otherwise 'fail'
         position = start;
         return false;
      }

      /*
       * The value (bar) part of "[foo="bar"]".
       */
      private String  nextAttribValue()
      {
         if (empty())
            return null;

         String  result = nextQuotedString();
         if (result != null)
            return result;
         return nextIdentifier();
      }

      /*
       * Scans for a CSS property value.
       */
      public String  nextPropertyValue()
      {
         if (empty())
            return null;
         int  start = position;
         int  lastValidPos = position;

         int  ch = input.charAt(position);
         while (ch != -1 && ch != ';' && ch != '}' && ch != '!' && !isEOL(ch)) {
            if (!isWhitespace(ch))  // don't include an spaces at the end
               lastValidPos = position + 1;
            ch = advanceChar();
         }
         if (position > start)
            return input.substring(start, lastValidPos);
         position = start;
         return null;
      }

   }


   //==============================================================================


   // Returns true if 'rendererMediaType' matches one of the media types in 'mediaList'
   private static boolean mediaMatches(List<MediaType> mediaList, MediaType rendererMediaType)
   {
      for (MediaType type: mediaList) {
         if (type == MediaType.all || type == rendererMediaType)
            return true;
      }
      return false;
   }


   private static List<MediaType> parseMediaList(CSSTextScanner scan) throws SAXException
   {
      ArrayList<MediaType>  typeList = new ArrayList<MediaType>();
      while (!scan.empty()) {
         String  type = scan.nextToken(',');
         try {
            typeList.add(MediaType.valueOf(type));
         } catch (IllegalArgumentException e) {
            throw new SAXException("Invalid @media type list");
         }
         // If there is a comma, keep looping, otherwise break
         if (!scan.skipCommaWhitespace())
            break;
      }
      return typeList;
   }


   private void  parseAtRule(Ruleset ruleset, CSSTextScanner scan) throws SAXException
   {
      String  atKeyword = scan.nextIdentifier();
      scan.skipWhitespace();
      if (atKeyword == null)
         throw new SAXException("Invalid '@' rule in <style> element");
      if (!inMediaRule && atKeyword.equals("media"))
      {
         List<MediaType>  mediaList = parseMediaList(scan);
         if (!scan.consume('{'))
            throw new SAXException("Invalid @media rule: missing rule set");
            
         scan.skipWhitespace();
         if (mediaMatches(mediaList, rendererMediaType)) {
            inMediaRule = true;
            ruleset.addAll( parseRuleset(scan) );
            inMediaRule = false;
         } else {
            parseRuleset(scan);  // parse and ignore accompanying ruleset
         }

         if (!scan.consume('}'))
            throw new SAXException("Invalid @media rule: expected '}' at end of rule set");

      //} else if (atKeyword.equals("charset")) {
      //} else if (atKeyword.equals("import")) {
      }
      else
      {
         // Unknown/unsupported at-rule
         warn("Ignoring @%s rule", atKeyword);
         skipAtRule(scan);
      }
      scan.skipWhitespace();
   }


   // Skip an unsupported at-rule: "ignore everything
   // up to and including the next semicolon or block".
   private void  skipAtRule(CSSTextScanner scan)
   {
      int depth = 0;
      while (!scan.empty())
      {
         int ch = scan.nextChar();
         if (ch == ';' && depth == 0)
            return;
         if (ch == '{')
            depth++;
         else if (ch == '}' && depth > 0) {
            if (--depth == 0)
               return;
         }
      }
   }


   private Ruleset  parseRuleset(CSSTextScanner scan) throws SAXException
   {
      Ruleset  ruleset = new Ruleset(); 
      while (!scan.empty())
      {
         if (scan.consume("<!--"))
            continue;
         if (scan.consume("-->"))
            continue;

         if (scan.consume('@')) {
            parseAtRule(ruleset, scan);
            continue;
         }
         if (parseRule(ruleset, scan))
            continue;

         // Nothing recognisable found. Could be end of rule set. Return.
         break;
      }
      return ruleset;
   }


   private boolean  parseRule(Ruleset ruleset, CSSTextScanner scan) throws SAXException
   {
      List<Selector>  selectors = parseSelectorGroup(scan);
      if (selectors != null && !selectors.isEmpty())
      {
         if (!scan.consume('{'))
            throw new SAXException("Malformed rule block in <style> element: missing '{'");
         scan.skipWhitespace();
         SVG.Style  ruleStyle = parseDeclarations(scan);
         scan.skipWhitespace();
         for (Selector selector: selectors) {
            ruleset.add( new Rule(selector, ruleStyle) );
         }
         return true;
      }
      else
      {
         return false;
      }
   }


   /*
    * Parse a selector group (eg. E, F, G). In many/most cases there will be only one entry.
    */
   private List<Selector>  parseSelectorGroup(CSSTextScanner scan) throws SAXException
   {
      if (scan.empty())
         return null;

      ArrayList<Selector>  selectorGroup = new ArrayList<Selector>(1);
      Selector             selector = new Selector();

      while (!scan.empty())
      {
         if (scan.nextSimpleSelector(selector))
         {
            // If there is a comma, keep looping, otherwise break
            if (!scan.skipCommaWhitespace())
               continue;  // if not a comma, go back and check for next part of selector
            selectorGroup.add(selector);
            selector = new Selector();
         }
         else
            break;
      }
      if (!selector.isEmpty())
         selectorGroup.add(selector);
      return selectorGroup;
   }


   // Parse a list of
   private SVG.Style  parseDeclarations(CSSTextScanner scan) throws SAXException
   {
      SVG.Style  ruleStyle = new SVG.Style();
      while (true)
      {
         String  propertyName = scan.nextIdentifier();
         scan.skipWhitespace();
         if (!scan.consume(':'))
            break;  // Syntax error. Stop processing CSS rules.
         scan.skipWhitespace();
         String  propertyValue = scan.nextPropertyValue();
         if (propertyValue == null)
            break;  // Syntax error
         // Check for !important flag.
         scan.skipWhitespace();
         if (scan.consume('!')) {
            scan.skipWhitespace();
            if (!scan.consume("important")) {
               throw new SAXException("Malformed rule set in <style> element: " +
                       "found unexpected '!'");
            }
            // We don't do anything with these. We just ignore them.
            scan.skipWhitespace();
         }
         scan.consume(';');
         SVGParser.processStyleProperty(ruleStyle, propertyName, propertyValue);
         scan.skipWhitespace();
         if (scan.consume('}'))
            return ruleStyle;
         if (scan.empty())
            break;
      }
      throw new SAXException("Malformed rule set in <style> element");
   }


   /*
    * Used by SVGParser to parse the "class" attribute.
    */
   protected static List<String>  parseClassAttribute(String val) throws SAXException
   {
      CSSTextScanner  scan = new CSSTextScanner(val);
      List<String>    classNameList = null;

      while (!scan.empty())
      {
         String  className = scan.nextIdentifier();
         if (className == null)
            throw new SAXException("Invalid value for \"class\" attribute: "+val);
         if (classNameList == null)
            classNameList = new ArrayList<String>();
         classNameList.add(className);
         scan.skipWhitespace();
      }
      return classNameList;
   }


   /*
    * Used by renderer to check if a CSS rule matches the current element.
    */
   protected static boolean  ruleMatch(Selector selector, SvgElementBase obj)
   {
      // Build the list of ancestor objects
      List<SvgContainer> ancestors = new ArrayList<SvgContainer>();
      SvgContainer  parent = obj.parent;
      while (parent != null) {
         ancestors.add(0, parent);
         parent = ((SvgObject) parent).parent;
      }
      
      int  ancestorsPos = ancestors.size() - 1;

      // Check the most common case first as a shortcut.
      if (selector.size() == 1)
         return selectorMatch(selector.get(0), ancestors, ancestorsPos, obj);
      
      // We start at the last part of the selector and loop back through the parts
      // Get the next selector part
      return ruleMatch(selector, selector.size() - 1, ancestors, ancestorsPos, obj);
   }


   private static boolean  ruleMatch(Selector selector, int selPartPos,
                                     List<SvgContainer> ancestors,
                                     int ancestorsPos,
                                     SvgElementBase obj)
   {
      // We start at the last part of the selector and loop back through the parts
      // Get the next selector part
      SimpleSelector  sel = selector.get(selPartPos);
      if (!selectorMatch(sel, ancestors, ancestorsPos, obj))
         return false;

      // Selector part matched, check its combinator
      if (sel.combinator == Combinator.DESCENDANT)
      {
         if (selPartPos == 0)
            return true;
         // Search up the ancestors list for a node that matches the next selector
         while (ancestorsPos >= 0) {
            if (ruleMatchOnAncestors(selector, selPartPos - 1, ancestors, ancestorsPos))
               return true;
            ancestorsPos--;
         }
         return false;
      }
      else if (sel.combinator == Combinator.CHILD)
      {
         return ruleMatchOnAncestors(selector, selPartPos - 1, ancestors, ancestorsPos);
      }
      else //if (sel.combinator == Combinator.FOLLOWS)
      {
         int  childPos = getChildPosition(ancestors, ancestorsPos, obj);
         if (childPos <= 0)
            return false;
         SvgElementBase  prevSibling = (SvgElementBase) obj.parent.getChildren().get(childPos - 1);
         return ruleMatch(selector, selPartPos - 1, ancestors, ancestorsPos, prevSibling);
      }
   }


   private static boolean  ruleMatchOnAncestors(Selector selector, int selPartPos,
                                                List<SvgContainer> ancestors, int ancestorsPos)
   {
      SimpleSelector  sel = selector.get(selPartPos);
      SvgElementBase  obj = (SvgElementBase) ancestors.get(ancestorsPos);

      if (!selectorMatch(sel, ancestors, ancestorsPos, obj))
         return false;

      // Selector part matched, check its combinator
      if (sel.combinator == Combinator.DESCENDANT)
      {
         if (selPartPos == 0)
            return true;
         // Search up the ancestors list for a node that matches the next selector
         while (ancestorsPos > 0) {
            if (ruleMatchOnAncestors(selector, selPartPos - 1, ancestors, --ancestorsPos))
               return true;
         }
         return false;
      }
      else if (sel.combinator == Combinator.CHILD)
      {
         return ruleMatchOnAncestors(selector, selPartPos - 1, ancestors, ancestorsPos - 1);
      }
      else //if (sel.combinator == Combinator.FOLLOWS)
      {
         int  childPos = getChildPosition(ancestors, ancestorsPos, obj);
         if (childPos <= 0)
            return false;
         SvgElementBase  prevSibling = (SvgElementBase) obj.parent.getChildren().get(childPos - 1);
         return ruleMatch(selector, selPartPos - 1, ancestors, ancestorsPos, prevSibling);
      }
   }


   private static int getChildPosition(List<SvgContainer> ancestors, int ancestorsPos,
                                       SvgElementBase obj)
   {
      if (ancestorsPos < 0)  // Has no parent, so can't have a sibling
         return -1;
      if (ancestors.get(ancestorsPos) != obj.parent)  // parent doesn't match,
         // so obj must be an indirect reference (eg. from a <use>)
         return -1;
      int  childPos = 0;
      for (SvgObject child: obj.parent.getChildren())
      {
         if (child == obj)
            return childPos;
         childPos++;
      }
      return -1;
   }


   private static boolean selectorMatch(SimpleSelector sel, List<SvgContainer> ancestors,
                                        int ancestorsPos, SvgElementBase obj)
   {
      // Check tag name. tag==null means tag is "*" which matches everything.
      if (sel.tag != null) {
         // The Group object does not match its tag ("<g>"),
         // so we have to handle it as a special case.
         if (sel.tag.equalsIgnoreCase("G"))
         {
            if (!(obj instanceof SVG.Group))
               return false;
         }
         // all other element classes should match their tag names
         else if (!sel.tag.equals(obj.getClass().getSimpleName().toLowerCase(Locale.US)))
         {
            return false;
         }
      }
      // If here, then tag part matched

      // Check the attributes
      if (sel.attribs != null)
      {
         for (Attrib attr: sel.attribs)
         {
            if (attr.name == ID)
            {
               if (!attr.value.equals(obj.id))
                  return false;
            }
            else if (attr.name == CLASS)
            {
               if (obj.classNames == null)
                  return false;
               if (!obj.classNames.contains(attr.value))
                  return false;
            }
            else
            {
               // Other attribute selector not yet supported
               return false;
            }
         }
      }

      // Check the pseudo classes
      if (sel.pseudos != null) {
         for (String pseudo: sel.pseudos) {
            if (pseudo.equals("first-child")) {
               if (getChildPosition(ancestors, ancestorsPos, obj) != 0)
                  return false;
            } else {
               return false;
            }
         }
      }

      // If w reached this point, the selector matched
      return true;
   }


}
