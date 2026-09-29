package eu.pb4.placeholders.api.parsers;

import com.mojang.brigadier.StringReader;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.TranslatedNode;
import eu.pb4.placeholders.api.node.parent.ClickActionNode;
import eu.pb4.placeholders.api.node.parent.FormattingNode;
import eu.pb4.placeholders.api.node.parent.HoverNode;
import eu.pb4.placeholders.api.node.parent.ParentTextNode;
import eu.pb4.placeholders.impl.textparser.TextParserImpl;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.ListIterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent.Action;
import org.jetbrains.annotations.Nullable;

public final class MarkdownLiteParserV1 implements NodeParser {
   public static NodeParser ALL = new MarkdownLiteParserV1(MarkdownLiteParserV1.MarkdownFormat.values());
   private final EnumSet<MarkdownLiteParserV1.MarkdownFormat> allowedFormatting = EnumSet.noneOf(MarkdownLiteParserV1.MarkdownFormat.class);
   private final Function<TextNode[], TextNode> spoilerFormatting;
   private final Function<TextNode[], TextNode> backtickFormatting;
   private final BiFunction<TextNode[], TextNode, TextNode> urlFormatting;

   public MarkdownLiteParserV1(MarkdownLiteParserV1.MarkdownFormat... formatting) {
      this(MarkdownLiteParserV1::defaultSpoilerFormatting, MarkdownLiteParserV1::defaultQuoteFormatting, formatting);
   }

   public MarkdownLiteParserV1(
      Function<TextNode[], TextNode> spoilerFormatting, Function<TextNode[], TextNode> quoteFormatting, MarkdownLiteParserV1.MarkdownFormat... formatting
   ) {
      this(spoilerFormatting, quoteFormatting, MarkdownLiteParserV1::defaultUrlFormatting, formatting);
   }

   public MarkdownLiteParserV1(
      Function<TextNode[], TextNode> spoilerFormatting,
      Function<TextNode[], TextNode> quoteFormatting,
      BiFunction<TextNode[], TextNode, TextNode> urlFormatting,
      MarkdownLiteParserV1.MarkdownFormat... formatting
   ) {
      for (MarkdownLiteParserV1.MarkdownFormat form : formatting) {
         this.allowedFormatting.add(form);
      }

      this.spoilerFormatting = spoilerFormatting;
      this.backtickFormatting = quoteFormatting;
      this.urlFormatting = urlFormatting;
   }

   public static TextNode defaultSpoilerFormatting(TextNode[] textNodes) {
      return new HoverNode<>(
         TextNode.array(
            new FormattingNode(
               TextNode.array(TextNode.of("["), TranslatedNode.of("options.hidden"), TextNode.of("]")), ChatFormatting.GRAY, ChatFormatting.ITALIC
            )
         ),
         HoverNode.Action.TEXT,
         TextNode.asSingle(textNodes)
      );
   }

   public static TextNode defaultQuoteFormatting(TextNode[] textNodes) {
      return new FormattingNode(textNodes, ChatFormatting.GRAY, ChatFormatting.ITALIC);
   }

   public static TextNode defaultUrlFormatting(TextNode[] textNodes, TextNode url) {
      return new ClickActionNode(TextNode.array(new FormattingNode(textNodes, ChatFormatting.BLUE, ChatFormatting.UNDERLINE)), Action.OPEN_URL, url);
   }

   @Override
   public TextNode[] parseNodes(TextNode input) {
      if (input instanceof LiteralNode literalNode) {
         ArrayList<MarkdownLiteParserV1.SubNode<?>> list = new ArrayList<>();
         this.parseLiteral(literalNode, list::add);
         return this.parseSubNodes(list.listIterator(), null, -1);
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
      } else if (input instanceof ParentTextNode parentTextNode) {
         ArrayList<MarkdownLiteParserV1.SubNode<?>> list = new ArrayList<>();

         for (TextNode children : parentTextNode.getChildren()) {
            if (children instanceof LiteralNode literalNode) {
               this.parseLiteral(literalNode, list::add);
            } else {
               list.add(new MarkdownLiteParserV1.SubNode<>(MarkdownLiteParserV1.SubNodeType.TEXT_NODE, TextNode.asSingle(this.parseNodes(children))));
            }
         }

         return new TextNode[]{parentTextNode.copyWith(this.parseSubNodes(list.listIterator(), null, -1), this)};
      } else {
         return new TextNode[]{input};
      }
   }

   private void parseLiteral(LiteralNode literalNode, Consumer<MarkdownLiteParserV1.SubNode<?>> consumer) {
      StringReader reader = new StringReader(literalNode.value());
      StringBuilder builder = new StringBuilder();

      while (reader.canRead()) {
         char i = reader.read();
         if (i == '\\' && reader.canRead()) {
            char next = reader.read();
            builder.append(i);
            builder.append(next);
         } else {
            MarkdownLiteParserV1.SubNodeType<String> type = null;
            if (reader.canRead()) {
               char i2 = reader.read();
               if (i2 == i) {
                  type = switch (i) {
                     case '|' -> MarkdownLiteParserV1.SubNodeType.SPOILER_LINE;
                     case '~' -> MarkdownLiteParserV1.SubNodeType.DOUBLE_WAVY_LINE;
                     default -> null;
                  };
               }

               if (type == null) {
                  reader.setCursor(reader.getCursor() - 1);
               }
            }

            if (type == null) {
               type = switch (i) {
                  case '(' -> MarkdownLiteParserV1.SubNodeType.BRACKET_OPEN;
                  case ')' -> MarkdownLiteParserV1.SubNodeType.BRACKET_CLOSE;
                  case '*' -> MarkdownLiteParserV1.SubNodeType.STAR;
                  case '[' -> MarkdownLiteParserV1.SubNodeType.SQR_BRACKET_OPEN;
                  case ']' -> MarkdownLiteParserV1.SubNodeType.SQR_BRACKET_CLOSE;
                  case '_' -> {
                     if (reader.getCursor() != 1 && reader.canRead() && !Character.isWhitespace(reader.peek(-2)) && !Character.isWhitespace(reader.peek())) {
                        yield null;
                     }

                     yield MarkdownLiteParserV1.SubNodeType.FLOOR;
                  }
                  case '`' -> MarkdownLiteParserV1.SubNodeType.BACK_TICK;
                  default -> null;
               };
            }

            if (type != null) {
               if (!builder.isEmpty()) {
                  consumer.accept(new MarkdownLiteParserV1.SubNode<>(MarkdownLiteParserV1.SubNodeType.STRING, builder.toString()));
                  builder = new StringBuilder();
               }

               consumer.accept(new MarkdownLiteParserV1.SubNode<>(type, type.selfValue));
            } else {
               builder.append(i);
            }
         }
      }

      if (!builder.isEmpty()) {
         consumer.accept(new MarkdownLiteParserV1.SubNode<>(MarkdownLiteParserV1.SubNodeType.STRING, builder.toString()));
      }
   }

   private TextNode[] parseSubNodes(ListIterator<MarkdownLiteParserV1.SubNode<?>> nodes, @Nullable MarkdownLiteParserV1.SubNodeType endAt, int count) {
      ArrayList<TextNode> out = new ArrayList<>();
      int startIndex = nodes.nextIndex();
      StringBuilder builder = new StringBuilder();

      label225:
      while (true) {
         if (!nodes.hasNext()) {
            if (endAt == null) {
               if (!builder.isEmpty()) {
                  out.add(new LiteralNode(builder.toString()));
               }

               return out.toArray(TextParserImpl.CASTER);
            }

            while (startIndex != nodes.nextIndex()) {
               nodes.previous();
            }

            return null;
         }

         MarkdownLiteParserV1.SubNode<?> next = nodes.next();
         if (next.type == endAt) {
            int foundCount = 1;
            if (foundCount == count) {
               if (!builder.isEmpty()) {
                  out.add(new LiteralNode(builder.toString()));
               }

               return out.toArray(TextParserImpl.CASTER);
            }

            int xStart = nodes.nextIndex();

            while (nodes.hasNext() && nodes.next().type == endAt) {
               if (++foundCount == count) {
                  if (!nodes.hasNext()) {
                     break label225;
                  }

                  MarkdownLiteParserV1.SubNode<?> prev = nodes.next();
                  nodes.previous();
                  if (prev.type != MarkdownLiteParserV1.SubNodeType.STRING || ((String)prev.value).startsWith(" ")) {
                     break label225;
                  }
                  break;
               }
            }

            while (xStart != nodes.nextIndex()) {
               nodes.previous();
            }
         }

         if (next.type == MarkdownLiteParserV1.SubNodeType.TEXT_NODE) {
            if (!builder.isEmpty()) {
               out.add(new LiteralNode(builder.toString()));
               builder = new StringBuilder();
            }

            out.add((TextNode)next.value);
         } else if (next.type == MarkdownLiteParserV1.SubNodeType.STRING) {
            builder.append((String)next.value);
         } else {
            if (next.type == MarkdownLiteParserV1.SubNodeType.BACK_TICK && this.allowedFormatting.contains(MarkdownLiteParserV1.MarkdownFormat.QUOTE)) {
               TextNode[] value = this.parseSubNodes(nodes, next.type, 1);
               if (value != null) {
                  if (!builder.isEmpty()) {
                     out.add(new LiteralNode(builder.toString()));
                     builder = new StringBuilder();
                  }

                  out.add(this.backtickFormatting.apply(value));
                  continue;
               }
            } else if (next.type == MarkdownLiteParserV1.SubNodeType.SPOILER_LINE
               && this.allowedFormatting.contains(MarkdownLiteParserV1.MarkdownFormat.SPOILER)) {
               TextNode[] value = this.parseSubNodes(nodes, next.type, 1);
               if (value != null) {
                  if (!builder.isEmpty()) {
                     out.add(new LiteralNode(builder.toString()));
                     builder = new StringBuilder();
                  }

                  out.add(this.spoilerFormatting.apply(value));
                  continue;
               }
            } else if (next.type == MarkdownLiteParserV1.SubNodeType.DOUBLE_WAVY_LINE
               && this.allowedFormatting.contains(MarkdownLiteParserV1.MarkdownFormat.STRIKETHROUGH)) {
               TextNode[] value = this.parseSubNodes(nodes, next.type, 1);
               if (value != null) {
                  if (!builder.isEmpty()) {
                     out.add(new LiteralNode(builder.toString()));
                     builder = new StringBuilder();
                  }

                  out.add(new FormattingNode(value, ChatFormatting.STRIKETHROUGH));
                  continue;
               }
            } else if (next.type == MarkdownLiteParserV1.SubNodeType.STAR || next.type == MarkdownLiteParserV1.SubNodeType.FLOOR) {
               boolean two = false;
               if (nodes.hasNext()
                  && (
                     next.type == MarkdownLiteParserV1.SubNodeType.STAR && this.allowedFormatting.contains(MarkdownLiteParserV1.MarkdownFormat.BOLD)
                        || next.type == MarkdownLiteParserV1.SubNodeType.FLOOR
                           && this.allowedFormatting.contains(MarkdownLiteParserV1.MarkdownFormat.UNDERLINE)
                  )) {
                  MarkdownLiteParserV1.SubNode<?> nexter = nodes.next();
                  if (nexter.type == next.type) {
                     two = true;
                     int i = nodes.nextIndex();
                     TextNode[] value = this.parseSubNodes(nodes, next.type, 2);
                     if (value != null) {
                        if (!builder.isEmpty()) {
                           out.add(new LiteralNode(builder.toString()));
                           builder = new StringBuilder();
                        }

                        out.add(new FormattingNode(value, next.type == MarkdownLiteParserV1.SubNodeType.STAR ? ChatFormatting.BOLD : ChatFormatting.UNDERLINE));
                        continue;
                     }
                  }

                  nodes.previous();
               }

               if (!two && this.allowedFormatting.contains(MarkdownLiteParserV1.MarkdownFormat.ITALIC)) {
                  boolean startingOrSpace;
                  if (!nodes.hasPrevious()) {
                     startingOrSpace = true;
                  } else {
                     MarkdownLiteParserV1.SubNode<?> prev = nodes.previous();
                     startingOrSpace = prev.type != MarkdownLiteParserV1.SubNodeType.STRING || ((String)prev.value).endsWith(" ");
                     nodes.next();
                  }

                  if (startingOrSpace) {
                     TextNode[] value = this.parseSubNodes(nodes, next.type, 1);
                     if (value != null) {
                        if (!builder.isEmpty()) {
                           out.add(new LiteralNode(builder.toString()));
                           builder = new StringBuilder();
                        }

                        out.add(new FormattingNode(value, ChatFormatting.ITALIC));
                        continue;
                     }
                  }
               }
            } else if (next.type == MarkdownLiteParserV1.SubNodeType.SQR_BRACKET_OPEN
               && this.allowedFormatting.contains(MarkdownLiteParserV1.MarkdownFormat.URL)
               && nodes.hasNext()) {
               int start = nodes.nextIndex();
               TextNode[] value = this.parseSubNodes(nodes, MarkdownLiteParserV1.SubNodeType.SQR_BRACKET_CLOSE, 1);
               if (value != null && nodes.hasNext()) {
                  boolean check = nodes.next().type == MarkdownLiteParserV1.SubNodeType.BRACKET_OPEN;
                  if (check) {
                     TextNode[] url = this.parseSubNodes(nodes, MarkdownLiteParserV1.SubNodeType.BRACKET_CLOSE, 1);
                     if (url != null) {
                        if (!builder.isEmpty()) {
                           out.add(new LiteralNode(builder.toString()));
                           builder = new StringBuilder();
                        }

                        out.add(this.urlFormatting.apply(value, TextNode.asSingle(url)));
                        continue;
                     }
                  }
               }

               while (start != nodes.nextIndex()) {
                  nodes.previous();
               }
            }

            builder.append((String)next.value);
         }
      }

      if (!builder.isEmpty()) {
         out.add(new LiteralNode(builder.toString()));
      }

      return out.toArray(TextParserImpl.CASTER);
   }

   public enum MarkdownFormat {
      BOLD,
      ITALIC,
      UNDERLINE,
      STRIKETHROUGH,
      QUOTE,
      SPOILER,
      URL;

      MarkdownFormat() {
      }
   }

   private record SubNode<T>(MarkdownLiteParserV1.SubNodeType<T> type, T value) {
      private SubNode {
      }
   }

   private record SubNodeType<T>(T selfValue) {
      public static final MarkdownLiteParserV1.SubNodeType<TextNode> TEXT_NODE = new MarkdownLiteParserV1.SubNodeType<>(null);
      public static final MarkdownLiteParserV1.SubNodeType<String> STRING = new MarkdownLiteParserV1.SubNodeType<>(null);
      public static final MarkdownLiteParserV1.SubNodeType<String> STAR = new MarkdownLiteParserV1.SubNodeType<>("*");
      public static final MarkdownLiteParserV1.SubNodeType<String> FLOOR = new MarkdownLiteParserV1.SubNodeType<>("_");
      public static final MarkdownLiteParserV1.SubNodeType<String> DOUBLE_WAVY_LINE = new MarkdownLiteParserV1.SubNodeType<>("~~");
      public static final MarkdownLiteParserV1.SubNodeType<String> BACK_TICK = new MarkdownLiteParserV1.SubNodeType<>("`");
      public static final MarkdownLiteParserV1.SubNodeType<String> SPOILER_LINE = new MarkdownLiteParserV1.SubNodeType<>("||");
      public static final MarkdownLiteParserV1.SubNodeType<String> BRACKET_OPEN = new MarkdownLiteParserV1.SubNodeType<>("(");
      public static final MarkdownLiteParserV1.SubNodeType<String> BRACKET_CLOSE = new MarkdownLiteParserV1.SubNodeType<>(")");
      public static final MarkdownLiteParserV1.SubNodeType<String> SQR_BRACKET_OPEN = new MarkdownLiteParserV1.SubNodeType<>("[");
      public static final MarkdownLiteParserV1.SubNodeType<String> SQR_BRACKET_CLOSE = new MarkdownLiteParserV1.SubNodeType<>("]");

      private SubNodeType {
      }
   }
}
