package eu.pb4.placeholders.api.parsers;

import com.mojang.brigadier.StringReader;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.TranslatedNode;
import eu.pb4.placeholders.api.node.parent.ColorNode;
import eu.pb4.placeholders.api.node.parent.FormattingNode;
import eu.pb4.placeholders.api.node.parent.ParentTextNode;
import eu.pb4.placeholders.impl.textparser.TextParserImpl;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;

public class LegacyFormattingParser implements NodeParser {
   public static NodeParser COLORS = new LegacyFormattingParser(
      true, Arrays.stream(ChatFormatting.values()).filter(x -> !x.isColor()).toArray(ChatFormatting[]::new)
   );
   public static NodeParser BASE_COLORS = new LegacyFormattingParser(
      false, Arrays.stream(ChatFormatting.values()).filter(x -> !x.isColor()).toArray(ChatFormatting[]::new)
   );
   public static NodeParser ALL = new LegacyFormattingParser(true, ChatFormatting.values());
   private final Char2ObjectOpenHashMap<ChatFormatting> map = new Char2ObjectOpenHashMap();
   private final boolean allowRgb;

   public LegacyFormattingParser(boolean allowRgb, ChatFormatting... allowedFormatting) {
      this.allowRgb = allowRgb;

      for (ChatFormatting formatting : allowedFormatting) {
         this.map.put(formatting.getChar(), formatting);
      }
   }

   @Override
   public TextNode[] parseNodes(TextNode input) {
      return this.parseNodes(input, new ArrayList<>());
   }

   public TextNode[] parseNodes(TextNode input, List<TextNode> nextNodes) {
      if (input instanceof LiteralNode literalNode) {
         return this.parseLiteral(literalNode, nextNodes);
      } else if (input instanceof TranslatedNode translatedNode) {
         ArrayList<Object> list = new ArrayList<>();

         for (Object arg : translatedNode.args()) {
            if (arg instanceof TextNode textNode) {
               list.add(TextNode.asSingle(this.parseNodes(textNode)));
            } else {
               list.add(arg);
            }
         }

         return new TextNode[]{TranslatedNode.ofFallback(translatedNode.key(), translatedNode.fallback(), list.toArray())};
      } else {
         return input instanceof ParentTextNode parentTextNode ? this.parseParents(parentTextNode) : new TextNode[]{input};
      }
   }

   private TextNode[] parseParents(ParentTextNode parentTextNode) {
      ArrayList<TextNode> list = new ArrayList<>();
      if (parentTextNode.getChildren().length > 0) {
         ArrayList<TextNode> nodes = new ArrayList<>(List.of(parentTextNode.getChildren()));

         while (!nodes.isEmpty()) {
            list.add(TextNode.asSingle(this.parseNodes(nodes.remove(0), nodes)));
         }
      }

      return new TextNode[]{parentTextNode.copyWith(list.toArray(TextParserImpl.CASTER), this)};
   }

   private TextNode[] parseLiteral(LiteralNode literalNode, List<TextNode> nexts) {
      StringBuilder builder = new StringBuilder();
      StringReader reader = new StringReader(literalNode.value());

      while (reader.canRead(2)) {
         char i = reader.read();
         if (i == '\\') {
            i = reader.read();
            builder.append('\\');
            builder.append(i);
         } else if (i == '&') {
            i = reader.read();
            if (this.allowRgb && i == '#' && reader.canRead(6)) {
               int start = reader.getCursor();

               try {
                  StringBuilder builder1 = new StringBuilder();

                  for (int z = 0; z < 6; z++) {
                     builder1.append(reader.read());
                  }

                  int rgb = Integer.parseInt(builder1.toString(), 16);
                  ArrayList<TextNode> list = new ArrayList<>();
                  list.addAll(nexts);
                  nexts.clear();
                  TextNode base = TextNode.asSingle(this.parseLiteral(new LiteralNode(reader.getRemaining()), list));
                  list.add(0, base);
                  return new TextNode[]{new LiteralNode(builder.toString()), new ColorNode(list.toArray(TextParserImpl.CASTER), TextColor.fromRgb(rgb))};
               } catch (Throwable var11) {
                  reader.setCursor(start);
               }
            }

            ChatFormatting x = (ChatFormatting)this.map.get(i);
            if (x != null) {
               ArrayList<TextNode> list = new ArrayList<>();
               list.addAll(nexts);
               nexts.clear();
               TextNode base = TextNode.asSingle(this.parseLiteral(new LiteralNode(reader.getRemaining()), list));
               list.add(0, base);
               return new TextNode[]{new LiteralNode(builder.toString()), new FormattingNode(list.toArray(TextParserImpl.CASTER), x)};
            }

            builder.append('&');
         }

         builder.append(i);
      }

      return new TextNode[]{literalNode};
   }
}
