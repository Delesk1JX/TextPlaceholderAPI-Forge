package eu.pb4.placeholders.api.parsers;

import com.google.common.collect.ImmutableList;
import eu.pb4.placeholders.api.node.EmptyNode;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.parent.ParentNode;
import eu.pb4.placeholders.api.node.parent.ParentTextNode;
import eu.pb4.placeholders.impl.textparser.TextParserImpl;
import eu.pb4.placeholders.impl.textparser.TextTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public class TextParserV1 implements NodeParser {
   public static final TextParserV1 DEFAULT = new TextParserV1();
   public static final TextParserV1 SAFE = new TextParserV1();
   private boolean allowOverrides = false;
   private final List<TextParserV1.TextTag> tags = new ArrayList<>();
   private final Map<String, TextParserV1.TextTag> byName = new HashMap<>();
   private final Map<String, TextParserV1.TextTag> byNameAlias = new HashMap<>();

   public TextParserV1() {
   }

   public static TextParserV1 createDefault() {
      return DEFAULT.copy();
   }

   public static TextParserV1 createSafe() {
      return SAFE.copy();
   }

   public static void registerDefault(TextParserV1.TextTag tag) {
      DEFAULT.register(tag);
      if (tag.userSafe()) {
         SAFE.register(tag);
      }
   }

   public void register(TextParserV1.TextTag tag) {
      if (this.byName.containsKey(tag.name())) {
         if (!this.allowOverrides) {
            throw new RuntimeException("Duplicate tag identifier!");
         }

         this.tags.removeIf(t -> t.name().equals(tag.name()));
      }

      this.byName.put(tag.name(), tag);
      this.tags.add(tag);
      this.byNameAlias.put(tag.name(), tag);
      if (tag.aliases() != null) {
         for (int i = 0; i < tag.aliases().length; i++) {
            String alias = tag.aliases()[i];
            TextParserV1.TextTag old = this.byNameAlias.get(alias);
            if (old == null || !old.name().equals(alias)) {
               this.byNameAlias.put(alias, tag);
            }
         }
      }
   }

   public List<TextParserV1.TextTag> getTags() {
      return ImmutableList.copyOf(this.tags);
   }

   @Override
   public TextNode[] parseNodes(TextNode input) {
      return parseNodesWith(input, this::getTagParser);
   }

   public TextParserV1 copy() {
      TextParserV1 parser = new TextParserV1();

      for (TextParserV1.TextTag tag : this.tags) {
         parser.register(tag);
      }

      return parser;
   }

   public static TextNode[] parseNodesWith(TextNode input, TextParserV1.TagParserGetter getter) {
      if (input instanceof LiteralNode literalNode) {
         return TextParserImpl.parse(literalNode.value(), getter);
      } else if (!(input instanceof ParentTextNode parentTextNode)) {
         return new TextNode[]{input};
      } else {
         ArrayList<TextNode> list = new ArrayList<>();

         for (TextNode child : parentTextNode.getChildren()) {
            list.add(new ParentNode(parseNodesWith(child, getter)));
         }

         return list.toArray(new TextNode[0]);
      }
   }

   public static TextParserV1.NodeList parseNodesWith(String input, TextParserV1.TagParserGetter handlers, @Nullable String endingTag) {
      return TextParserImpl.recursiveParsing(input, handlers, endingTag);
   }

   @Nullable
   public TextParserV1.TagNodeBuilder getTagParser(String name) {
      TextParserV1.TextTag o = this.byNameAlias.get(name);
      return o != null ? o.parser() : null;
   }

   static {
      TextTags.register();
   }

   public record NodeList(TextNode[] nodes, int length) {
      public static final TextParserV1.NodeList EMPTY = new TextParserV1.NodeList(new TextNode[0], 0);

      public NodeList {
      }

      public TextParserV1.TagNodeValue value(TextNode node) {
         return new TextParserV1.TagNodeValue(node, this.length);
      }

      public TextParserV1.TagNodeValue value(Function<TextNode[], TextNode> function) {
         return new TextParserV1.TagNodeValue(function.apply(this.nodes), this.length);
      }
   }

   @FunctionalInterface
   public interface TagNodeBuilder {
      TextParserV1.TagNodeValue parseString(String var1, String var2, String var3, TextParserV1.TagParserGetter var4, String var5);

      static TextParserV1.TagNodeBuilder selfClosing(TextParserV1.TagNodeBuilder.SelfTagCreator selfTagCreator) {
         return (tag, data, input, handlers, endAt) -> new TextParserV1.TagNodeValue(selfTagCreator.createTextNode(data), 0);
      }

      static TextParserV1.TagNodeBuilder wrapping(TextParserV1.TagNodeBuilder.FormattingTagCreator formattingTagCreator) {
         return (tag, data, input, handlers, endAt) -> {
            TextParserV1.NodeList out = TextParserV1.parseNodesWith(input, handlers, endAt);
            return new TextParserV1.TagNodeValue(formattingTagCreator.createTextNode(out.nodes(), data), out.length());
         };
      }

      static TextParserV1.TagNodeBuilder wrappingBoolean(TextParserV1.TagNodeBuilder.BooleanFormattingTagCreator formattingTagCreator) {
         return (tag, data, input, handlers, endAt) -> {
            TextParserV1.NodeList out = TextParserV1.parseNodesWith(input, handlers, endAt);
            return new TextParserV1.TagNodeValue(
               formattingTagCreator.createTextNode(out.nodes(), data == null || data.isEmpty() || !data.equals("false")), out.length()
            );
         };
      }

      interface BooleanFormattingTagCreator {
         TextNode createTextNode(TextNode[] var1, boolean var2);
      }

      interface FormattingTagCreator {
         TextNode createTextNode(TextNode[] var1, String var2);
      }

      interface SelfTagCreator {
         TextNode createTextNode(String var1);
      }
   }

   public record TagNodeValue(TextNode node, int length) {
      public static final TextParserV1.TagNodeValue EMPTY = new TextParserV1.TagNodeValue(EmptyNode.INSTANCE, 0);

      public TagNodeValue {
      }
   }

   @FunctionalInterface
   public interface TagParserGetter {
      @Nullable
      TextParserV1.TagNodeBuilder getTagParser(String var1);
   }

   public record TextTag(String name, String[] aliases, String type, boolean userSafe, TextParserV1.TagNodeBuilder parser) {
      public TextTag {
      }

      public static TextParserV1.TextTag of(String name, String type, TextParserV1.TagNodeBuilder parser) {
         return of(name, type, true, parser);
      }

      public static TextParserV1.TextTag of(String name, String type, boolean userSafe, TextParserV1.TagNodeBuilder parser) {
         return of(name, List.of(), type, userSafe, parser);
      }

      public static TextParserV1.TextTag of(String name, List<String> aliases, String type, boolean userSafe, TextParserV1.TagNodeBuilder parser) {
         return new TextParserV1.TextTag(name, aliases.toArray(new String[0]), type, userSafe, parser);
      }
   }
}
