package eu.pb4.placeholders.api;

import com.google.common.collect.ImmutableMap;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.parent.ParentNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.PatternPlaceholderParser;
import eu.pb4.placeholders.impl.placeholder.builtin.PlayerPlaceholders;
import eu.pb4.placeholders.impl.placeholder.builtin.ServerPlaceholders;
import eu.pb4.placeholders.impl.placeholder.builtin.WorldPlaceholders;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class Placeholders {
   @Deprecated
   public static final Pattern PLACEHOLDER_PATTERN = PatternPlaceholderParser.PLACEHOLDER_PATTERN;
   @Deprecated
   public static final Pattern ALT_PLACEHOLDER_PATTERN = PatternPlaceholderParser.ALT_PLACEHOLDER_PATTERN;
   @Deprecated
   public static final Pattern PLACEHOLDER_PATTERN_CUSTOM = PatternPlaceholderParser.PLACEHOLDER_PATTERN_CUSTOM;
   @Deprecated
   public static final Pattern ALT_PLACEHOLDER_PATTERN_CUSTOM = PatternPlaceholderParser.ALT_PLACEHOLDER_PATTERN_CUSTOM;
   @Deprecated
   public static final Pattern PREDEFINED_PLACEHOLDER_PATTERN = PatternPlaceholderParser.PREDEFINED_PLACEHOLDER_PATTERN;
   private static final HashMap<ResourceLocation, PlaceholderHandler> PLACEHOLDERS = new HashMap<>();
   private static final List<Placeholders.PlaceholderListChangedCallback> CHANGED_CALLBACKS = new ArrayList<>();
   public static final Placeholders.PlaceholderGetter DEFAULT_PLACEHOLDER_GETTER = new Placeholders.PlaceholderGetter() {
      @Override
      public PlaceholderHandler getPlaceholder(String placeholder) {
         return Placeholders.PLACEHOLDERS.get(ResourceLocation.tryParse(placeholder));
      }

      @Override
      public boolean isContextOptional() {
         return false;
      }
   };
   public static final NodeParser DEFAULT_PLACEHOLDER_PARSER = PatternPlaceholderParser.of(
      PLACEHOLDER_PATTERN, PlaceholderContext.KEY, DEFAULT_PLACEHOLDER_GETTER
   );

   public Placeholders() {
   }

   public static PlaceholderResult parsePlaceholder(ResourceLocation identifier, String argument, PlaceholderContext context) {
      return PLACEHOLDERS.containsKey(identifier)
         ? PLACEHOLDERS.get(identifier).onPlaceholderRequest(context, argument)
         : PlaceholderResult.invalid("Placeholder doesn't exist!");
   }

   public static ParentNode parseNodes(TextNode node) {
      return asSingleParent(DEFAULT_PLACEHOLDER_PARSER.parseNodes(node));
   }

   public static ParentNode parseNodes(TextNode node, ParserContext.Key<PlaceholderContext> contextKey) {
      return asSingleParent(PatternPlaceholderParser.of(PLACEHOLDER_PATTERN, contextKey, DEFAULT_PLACEHOLDER_GETTER).parseNodes(node));
   }

   public static ParentNode parseNodes(TextNode node, Pattern pattern) {
      return parseNodes(node, pattern, PlaceholderContext.KEY);
   }

   public static ParentNode parseNodes(TextNode node, Pattern pattern, ParserContext.Key<PlaceholderContext> contextKey) {
      return asSingleParent(PatternPlaceholderParser.of(pattern, contextKey, DEFAULT_PLACEHOLDER_GETTER).parseNodes(node));
   }

   public static ParentNode parseNodes(TextNode node, Pattern pattern, Placeholders.PlaceholderGetter placeholderGetter) {
      return parseNodes(node, pattern, placeholderGetter, PlaceholderContext.KEY);
   }

   public static ParentNode parseNodes(
      TextNode node, Pattern pattern, Placeholders.PlaceholderGetter placeholderGetter, ParserContext.Key<PlaceholderContext> contextKey
   ) {
      return asSingleParent(PatternPlaceholderParser.of(pattern, contextKey, placeholderGetter).parseNodes(node));
   }

   public static ParentNode parseNodes(TextNode node, Pattern pattern, Map<String, Component> placeholders) {
      return asSingleParent(PatternPlaceholderParser.ofTextMap(pattern, placeholders).parseNodes(node));
   }

   public static ParentNode parseNodes(TextNode node, Pattern pattern, Set<String> placeholders, ParserContext.Key<Placeholders.PlaceholderGetter> key) {
      return parseNodes(node, pattern, placeholders, key, PlaceholderContext.KEY);
   }

   public static ParentNode parseNodes(
      TextNode node,
      Pattern pattern,
      Set<String> placeholders,
      ParserContext.Key<Placeholders.PlaceholderGetter> key,
      ParserContext.Key<PlaceholderContext> contextKey
   ) {
      return asSingleParent(PatternPlaceholderParser.of(pattern, contextKey, new Placeholders.PlaceholderGetter() {
         @Override
         public PlaceholderHandler getPlaceholder(String placeholder, ParserContext context) {
            Placeholders.PlaceholderGetter get = context.get(key);
            return get != null ? get.getPlaceholder(placeholder, context) : null;
         }

         @Override
         public PlaceholderHandler getPlaceholder(String placeholder) {
            return placeholders.contains(placeholder) ? PlaceholderHandler.EMPTY : null;
         }

         @Override
         public boolean isContextOptional() {
            return true;
         }
      }).parseNodes(node));
   }

   public static Component parseText(Component text, PlaceholderContext context) {
      return parseNodes(TextNode.convert(text)).toText(ParserContext.of(PlaceholderContext.KEY, context));
   }

   public static Component parseText(Component text, PlaceholderContext context, Pattern pattern) {
      return parseNodes(TextNode.convert(text), pattern).toText(ParserContext.of(PlaceholderContext.KEY, context));
   }

   public static Component parseText(Component text, PlaceholderContext context, Pattern pattern, Placeholders.PlaceholderGetter placeholderGetter) {
      return parseNodes(TextNode.convert(text), pattern, placeholderGetter).toText(ParserContext.of(PlaceholderContext.KEY, context));
   }

   public static Component parseText(Component text, Pattern pattern, Map<String, Component> placeholders) {
      return parseNodes(TextNode.convert(text), pattern, placeholders).toText(ParserContext.of());
   }

   public static Component parseText(Component text, Pattern pattern, Set<String> placeholders, ParserContext.Key<Placeholders.PlaceholderGetter> key) {
      return parseNodes(TextNode.convert(text), pattern, placeholders, key).toText(ParserContext.of());
   }

   public static Component parseText(TextNode textNode, PlaceholderContext context) {
      return parseNodes(textNode).toText(ParserContext.of(PlaceholderContext.KEY, context));
   }

   public static Component parseText(TextNode textNode, PlaceholderContext context, Pattern pattern) {
      return parseNodes(textNode, pattern).toText(ParserContext.of(PlaceholderContext.KEY, context));
   }

   public static Component parseText(TextNode textNode, PlaceholderContext context, Pattern pattern, Placeholders.PlaceholderGetter placeholderGetter) {
      return parseNodes(textNode, pattern, placeholderGetter).toText(ParserContext.of(PlaceholderContext.KEY, context));
   }

   public static Component parseText(TextNode textNode, PlaceholderContext context, Pattern pattern, Map<String, Component> placeholders) {
      return parseNodes(textNode, pattern, placeholders).toText(ParserContext.of(PlaceholderContext.KEY, context));
   }

   public static Component parseText(TextNode textNode, Pattern pattern, Map<String, Component> placeholders) {
      return parseNodes(textNode, pattern, placeholders).toText();
   }

   public static Component parseText(TextNode textNode, Pattern pattern, Set<String> placeholders, ParserContext.Key<Placeholders.PlaceholderGetter> key) {
      return parseNodes(textNode, pattern, placeholders, key).toText();
   }

   public static void register(ResourceLocation identifier, PlaceholderHandler handler) {
      PLACEHOLDERS.put(identifier, handler);

      for (Placeholders.PlaceholderListChangedCallback e : CHANGED_CALLBACKS) {
         e.onPlaceholderListChange(identifier, false);
      }
   }

   public static void remove(ResourceLocation identifier) {
      if (PLACEHOLDERS.remove(identifier) != null) {
         for (Placeholders.PlaceholderListChangedCallback e : CHANGED_CALLBACKS) {
            e.onPlaceholderListChange(identifier, true);
         }
      }
   }

   public static ImmutableMap<ResourceLocation, PlaceholderHandler> getPlaceholders() {
      return ImmutableMap.copyOf(PLACEHOLDERS);
   }

   public static void registerChangeEvent(Placeholders.PlaceholderListChangedCallback callback) {
      CHANGED_CALLBACKS.add(callback);
   }

   private static ParentNode asSingleParent(TextNode... textNodes) {
      return textNodes.length == 1 && textNodes[0] instanceof ParentNode ? (ParentNode)textNodes[0] : new ParentNode(textNodes);
   }

   static {
      PlayerPlaceholders.register();
      ServerPlaceholders.register();
      WorldPlaceholders.register();
   }

   public interface PlaceholderGetter {
      @Nullable
      PlaceholderHandler getPlaceholder(String var1);

      @Nullable
      default PlaceholderHandler getPlaceholder(String placeholder, ParserContext context) {
         return this.getPlaceholder(placeholder);
      }

      default boolean isContextOptional() {
         return false;
      }

      default boolean exists(String placeholder) {
         return this.getPlaceholder(placeholder) != null;
      }
   }

   public interface PlaceholderListChangedCallback {
      void onPlaceholderListChange(ResourceLocation var1, boolean var2);
   }
}
