package eu.pb4.placeholders.impl.textparser;

import eu.pb4.placeholders.api.node.DirectTextNode;
import eu.pb4.placeholders.api.node.KeybindNode;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.NbtNode;
import eu.pb4.placeholders.api.node.ScoreNode;
import eu.pb4.placeholders.api.node.SelectorNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.TranslatedNode;
import eu.pb4.placeholders.api.node.parent.BoldNode;
import eu.pb4.placeholders.api.node.parent.ClickActionNode;
import eu.pb4.placeholders.api.node.parent.ColorNode;
import eu.pb4.placeholders.api.node.parent.FontNode;
import eu.pb4.placeholders.api.node.parent.FormattingNode;
import eu.pb4.placeholders.api.node.parent.GradientNode;
import eu.pb4.placeholders.api.node.parent.HoverNode;
import eu.pb4.placeholders.api.node.parent.InsertNode;
import eu.pb4.placeholders.api.node.parent.ItalicNode;
import eu.pb4.placeholders.api.node.parent.ObfuscatedNode;
import eu.pb4.placeholders.api.node.parent.ParentNode;
import eu.pb4.placeholders.api.node.parent.StrikethroughNode;
import eu.pb4.placeholders.api.node.parent.TransformNode;
import eu.pb4.placeholders.api.node.parent.UnderlinedNode;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import eu.pb4.placeholders.impl.GeneralUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.network.chat.HoverEvent.ShowEntity;
import net.minecraft.network.chat.HoverEvent.ShowItem;
import net.minecraft.network.chat.contents.data.BlockDataSource;
import net.minecraft.network.chat.contents.data.DataSource;
import net.minecraft.network.chat.contents.data.EntityDataSource;
import net.minecraft.network.chat.contents.data.StorageDataSource;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class TextTags {
   public TextTags() {
   }

   /**
    * Resolves a click action by its serialised name.
    *
    * <p>1.21 dropped {@code ClickEvent.Action#getByName(String)}; the enum only exposes the
    * {@link net.minecraft.util.StringRepresentable} contract, so match on that instead.
    */
   private static net.minecraft.network.chat.ClickEvent.Action clickActionByName(String name) {
      for (net.minecraft.network.chat.ClickEvent.Action action : net.minecraft.network.chat.ClickEvent.Action.values()) {
         if (action.getSerializedName().equals(name)) {
            return action;
         }
      }

      return null;
   }

   /**
    * Resolves a hover action by its serialised name.
    *
    * <p>1.21 turned {@code HoverEvent.Action} from an enum into a final class with three static
    * instances and dropped its by-name lookup, so the three known actions are matched explicitly.
    */
   /**
    * Wraps the {@code raw_style} tag's JSON. Despite the tag name it holds a raw JSON component,
    * which is what the tag has always parsed. Invalid JSON degrades to an empty component.
    */
   private static Component rawComponent(String json) {
      Component parsed = GeneralUtils.parseComponentJson(json);

      return parsed != null ? parsed : Component.empty();
   }
   private static net.minecraft.network.chat.HoverEvent.Action hoverActionByName(String name) {
      for (net.minecraft.network.chat.HoverEvent.Action action : new net.minecraft.network.chat.HoverEvent.Action[]{
              net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
              net.minecraft.network.chat.HoverEvent.Action.SHOW_ITEM,
              net.minecraft.network.chat.HoverEvent.Action.SHOW_ENTITY}) {
         if (action.getSerializedName().equals(name)) {
            return action;
         }
      }

      return null;
   }

   public static void register() {
      Map<String, List<String>> aliases = new HashMap<>();
      aliases.put("gold", List.of("orange"));
      aliases.put("gray", List.of("grey"));
      aliases.put("light_purple", List.of("pink"));
      aliases.put("dark_gray", List.of("dark_grey"));

      for (ChatFormatting formatting : ChatFormatting.values()) {
         if (!formatting.isFormat()) {
            TextParserV1.registerDefault(
               TextParserV1.TextTag.of(
                  formatting.getName(),
                  aliases.containsKey(formatting.getName()) ? aliases.get(formatting.getName()) : List.of(),
                  "color",
                  true,
                  wrap((nodes, arg) -> new FormattingNode(nodes, formatting))
               )
            );
         }
      }

      TextParserV1.registerDefault(TextParserV1.TextTag.of("bold", List.of("b"), "formatting", true, bool(BoldNode::new)));
      TextParserV1.registerDefault(TextParserV1.TextTag.of("underline", List.of("underlined", "u"), "formatting", true, bool(UnderlinedNode::new)));
      TextParserV1.registerDefault(TextParserV1.TextTag.of("strikethrough", List.of("st"), "formatting", true, bool(StrikethroughNode::new)));
      TextParserV1.registerDefault(TextParserV1.TextTag.of("obfuscated", List.of("obf", "matrix"), "formatting", true, bool(ObfuscatedNode::new)));
      TextParserV1.registerDefault(TextParserV1.TextTag.of("italic", List.of("i", "em"), "formatting", true, bool(ItalicNode::new)));
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "color",
            List.of("colour", "c"),
            "color",
            true,
            wrap((nodes, data) -> new ColorNode(nodes, TextColor.parseColor(TextParserImpl.cleanArgument(data)).result().orElse(null)))
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "font", "other_formatting", false, wrap((nodes, data) -> new FontNode(nodes, Identifier.tryParse(TextParserImpl.cleanArgument(data))))
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "lang",
            List.of("translate"),
            "special",
            false,
            (tag, data, input, handlers, endAt) -> {
               String[] lines = data.split(":");
               if (lines.length > 0) {
                  List<TextNode> textList = new ArrayList<>();
                  boolean skipped = false;

                  for (String part : lines) {
                     if (!skipped) {
                        skipped = true;
                     } else {
                        textList.add(new ParentNode(TextParserImpl.parse(TextParserImpl.removeEscaping(TextParserImpl.cleanArgument(part)), handlers)));
                     }
                  }

                  TranslatedNode out = TranslatedNode.of(
                     TextParserImpl.removeEscaping(TextParserImpl.cleanArgument(lines[0])), textList.toArray(TextParserImpl.CASTER)
                  );
                  return new TextParserV1.TagNodeValue(out, 0);
               } else {
                  return TextParserV1.TagNodeValue.EMPTY;
               }
            }
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "lang_fallback",
            List.of("translatef", "langf", "translate_fallback"),
            "special",
            false,
            (tag, data, input, handlers, endAt) -> {
               String[] lines = data.split(":");
               if (lines.length > 1) {
                  List<TextNode> textList = new ArrayList<>();
                  int skipped = 0;

                  for (String part : lines) {
                     if (skipped < 2) {
                        skipped++;
                     } else {
                        textList.add(new ParentNode(TextParserImpl.parse(TextParserImpl.removeEscaping(TextParserImpl.cleanArgument(part)), handlers)));
                     }
                  }

                  TranslatedNode out = TranslatedNode.ofFallback(
                     TextParserImpl.removeEscaping(TextParserImpl.cleanArgument(lines[0])),
                     TextParserImpl.removeEscaping(TextParserImpl.cleanArgument(lines[1])),
                     textList.toArray(TextParserImpl.CASTER)
                  );
                  return new TextParserV1.TagNodeValue(out, 0);
               } else {
                  return TextParserV1.TagNodeValue.EMPTY;
               }
            }
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "keybind",
            List.of("key"),
            "special",
            false,
            (tag, data, input, handlers, endAt) -> !data.isEmpty()
               ? new TextParserV1.TagNodeValue(new KeybindNode(TextParserImpl.cleanArgument(data)), 0)
               : TextParserV1.TagNodeValue.EMPTY
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "click",
            "click_action",
            false,
            (tag, data, input, handlers, endAt) -> {
               String[] lines = data.split(":", 2);
               TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
               if (lines.length > 1) {
                  net.minecraft.network.chat.ClickEvent.Action action = clickActionByName(TextParserImpl.cleanArgument(lines[0]));
                  if (action != null) {
                     return out.value(
                        new ClickActionNode(
                           out.nodes(), action, new LiteralNode(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[1])))
                        )
                     );
                  }
               }

               return out.value(new ParentNode(out.nodes()));
            }
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "run_command",
            List.of("run_cmd"),
            "click_action",
            false,
            (tag, data, input, handlers, endAt) -> {
               TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
               return !data.isEmpty()
                  ? out.value(
                     new ClickActionNode(
                        out.nodes(),
                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                        new LiteralNode(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(data)))
                     )
                  )
                  : out.value(new ParentNode(out.nodes()));
            }
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "suggest_command",
            List.of("cmd"),
            "click_action",
            false,
            (tag, data, input, handlers, endAt) -> {
               TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
               return !data.isEmpty()
                  ? out.value(
                     new ClickActionNode(
                        out.nodes(),
                        net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND,
                        new LiteralNode(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(data)))
                     )
                  )
                  : out.value(new ParentNode(out.nodes()));
            }
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "open_url",
            List.of("url"),
            "click_action",
            false,
            (tag, data, input, handlers, endAt) -> {
               TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
               return !data.isEmpty()
                  ? out.value(
                     new ClickActionNode(
                        out.nodes(),
                        net.minecraft.network.chat.ClickEvent.Action.OPEN_URL,
                        new LiteralNode(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(data)))
                     )
                  )
                  : out.value(new ParentNode(out.nodes()));
            }
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "copy_to_clipboard",
            List.of("copy"),
            "click_action",
            false,
            (tag, data, input, handlers, endAt) -> {
               TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
               return !data.isEmpty()
                  ? out.value(
                     new ClickActionNode(
                        out.nodes(),
                        net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD,
                        new LiteralNode(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(data)))
                     )
                  )
                  : out.value(new ParentNode(out.nodes()));
            }
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "change_page",
            List.of("page"),
            "click_action",
            true,
            (tag, data, input, handlers, endAt) -> {
               TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
               return !data.isEmpty()
                  ? out.value(
                     new ClickActionNode(
                        out.nodes(),
                        net.minecraft.network.chat.ClickEvent.Action.CHANGE_PAGE,
                        new LiteralNode(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(data)))
                     )
                  )
                  : out.value(new ParentNode(out.nodes()));
            }
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "hover",
            "hover_event",
            true,
            (tag, data, input, handlers, endAt) -> {
               String[] lines = data.split(":", 2);
               TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);

               try {
                  if (lines.length <= 1) {
                     return out.value(
                        new HoverNode<>(
                           out.nodes(),
                           HoverNode.Action.TEXT,
                           new ParentNode(TextParserImpl.parse(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(data)), handlers))
                        )
                     );
                  }

                  Action action = hoverActionByName(TextParserImpl.cleanArgument(lines[0].toLowerCase(Locale.ROOT)));
                  if (action == Action.SHOW_TEXT) {
                     return out.value(
                        new HoverNode<>(
                           out.nodes(),
                           HoverNode.Action.TEXT,
                           new ParentNode(TextParserImpl.parse(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[1])), handlers))
                        )
                     );
                  }

                  if (action == Action.SHOW_ENTITY) {
                     lines = lines[1].split(":", 3);
                     if (lines.length == 3) {
                        return out.value(
                           new HoverNode<>(
                              out.nodes(),
                              HoverNode.Action.ENTITY,
                              new HoverNode.EntityNodeContent(
                                 EntityType.byString(
                                       TextParserImpl.restoreOriginalEscaping(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[0])))
                                    )
                                    .orElse(EntityType.PIG),
                                 UUID.fromString(TextParserImpl.cleanArgument(lines[1])),
                                 new ParentNode(
                                    TextParserImpl.parse(
                                       TextParserImpl.restoreOriginalEscaping(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[2]))),
                                       handlers
                                    )
                                 )
                              )
                           )
                        );
                     }
                  } else {
                     if (action != Action.SHOW_ITEM) {
                        return out.value(
                           new HoverNode<>(
                              out.nodes(),
                              HoverNode.Action.TEXT,
                              new ParentNode(TextParserImpl.parse(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(data)), handlers))
                           )
                        );
                     }

                     try {
                        return out.value(
                           new HoverNode<>(
                              out.nodes(),
                              HoverNode.Action.ITEM_STACK,
                              ItemStack.CODEC.parse(NbtOps.INSTANCE, GeneralUtils.parseTag(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[1])))).result().orElse(ItemStack.EMPTY)
                           )
                        );
                     } catch (Throwable e) {
                        lines = lines[1].split(":", 2);
                        // 26.1: Registry#get returns a Holder.Reference, so getValue is the one
                        // that yields the item itself; it is nullable for an unknown id.
                        Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(lines[0]));

                        if (item == null) {
                           return out.value(new ParentNode(out.nodes()));
                        }

                        ItemStack stack = item.getDefaultInstance();

                        if (lines.length > 1) {
                           stack.setCount(Integer.parseInt(lines[1]));
                        }

                        if (lines.length > 2) {
                           // 1.21 replaced ItemStack#setTag with data components, so the NBT
                           // argument is now parsed as a whole ItemStack instead of patched on.
                           CompoundTag parsedTag = GeneralUtils.parseTag(
                              TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[2]))
                           );

                           if (parsedTag != null) {
                              ItemStack parsed = ItemStack.CODEC.parse(NbtOps.INSTANCE, parsedTag).result().orElse(ItemStack.EMPTY);

                              if (!parsed.isEmpty()) {
                                 stack = parsed;
                              }
                           }
                        }

                        return out.value(new HoverNode<>(out.nodes(), HoverNode.Action.ITEM_STACK, stack));
                     }
                  }
               } catch (Exception var11) {
               }

               return out.value(new ParentNode(out.nodes()));
            }
         )
      );
      TextParserV1.registerDefault(TextParserV1.TextTag.of("insert", List.of("insertion"), "click_action", false, (tag, data, input, handlers, endAt) -> {
         TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
         return out.value(new InsertNode(out.nodes(), new LiteralNode(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(data)))));
      }));
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of("clear_color", List.of("uncolor", "colorless"), "special", false, (tag, data, input, handlers, endAt) -> {
            TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
            return out.value(GeneralUtils.removeColors(new ParentNode(out.nodes())));
         })
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "rainbow",
            List.of("rb"),
            "gradient",
            true,
            (tag, data, input, handlers, endAt) -> {
               String[] val = data.split(":");
               float freq = 1.0F;
               float saturation = 1.0F;
               float offset = 0.0F;
               int overriddenLength = -1;
               if (val.length >= 1) {
                  try {
                     freq = Float.parseFloat(val[0]);
                  } catch (Exception var14) {
                  }
               }

               if (val.length >= 2) {
                  try {
                     saturation = Float.parseFloat(val[1]);
                  } catch (Exception var13) {
                  }
               }

               if (val.length >= 3) {
                  try {
                     offset = Float.parseFloat(val[2]);
                  } catch (Exception var12) {
                  }
               }

               if (val.length >= 4) {
                  try {
                     overriddenLength = Integer.parseInt(val[3]);
                  } catch (Exception var11) {
                  }
               }

               TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
               return out.value(
                  overriddenLength < 0
                     ? GradientNode.rainbow(saturation, 1.0F, freq, offset, out.nodes())
                     : GradientNode.rainbow(saturation, 1.0F, freq, offset, overriddenLength, out.nodes())
               );
            }
         )
      );
      TextParserV1.registerDefault(TextParserV1.TextTag.of("gradient", List.of("gr"), "gradient", true, (tag, data, input, handlers, endAt) -> {
         String[] val = data.split(":");
         TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
         List<TextColor> textColors = new ArrayList<>();

         for (String string : val) {
            TextColor color = TextColor.parseColor(string).result().orElse(null);
            if (color != null) {
               textColors.add(color);
            }
         }

         return out.value(GradientNode.colors(textColors, out.nodes()));
      }));
      TextParserV1.registerDefault(TextParserV1.TextTag.of("hard_gradient", List.of("hgr"), "gradient", true, (tag, data, input, handlers, endAt) -> {
         String[] val = data.split(":");
         TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
         ArrayList<TextColor> textColors = new ArrayList<>();

         for (String string : val) {
            TextColor color = TextColor.parseColor(string).result().orElse(null);
            if (color != null) {
               textColors.add(color);
            }
         }

         return out.value(GradientNode.colorsHard(textColors, out.nodes()));
      }));
      TextParserV1.registerDefault(TextParserV1.TextTag.of("clear", "special", false, (tag, data, input, handlers, endAt) -> {
         String[] val = data.isEmpty() ? new String[0] : data.split(":");
         TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
         return out.value(new TransformNode(out.nodes(), getTransform(val)));
      }));
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "raw_style",
            "special",
            false,
            (tag, data, input, handlers, endAt) -> new TextParserV1.TagNodeValue(
               new DirectTextNode(rawComponent(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(data)))), 0
            )
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "score",
            "special",
            false,
            (tag, data, input, handlers, endAt) -> {
               String[] lines = data.split(":");
               return lines.length == 2
                  ? new TextParserV1.TagNodeValue(
                     new ScoreNode(
                        TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[0])),
                        TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[1]))
                     ),
                     0
                  )
                  : TextParserV1.TagNodeValue.EMPTY;
            }
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "selector",
            "special",
            false,
            (tag, data, input, handlers, endAt) -> {
               String[] lines = data.split(":");
               if (lines.length == 2) {
                  return new TextParserV1.TagNodeValue(
                     new SelectorNode(
                        TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[0])),
                        Optional.of(
                           TextNode.asSingle(
                              TextParserImpl.recursiveParsing(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[1])), handlers, null)
                                 .nodes()
                           )
                        )
                     ),
                     0
                  );
               } else {
                  return lines.length == 1
                     ? new TextParserV1.TagNodeValue(
                        new SelectorNode(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[0])), Optional.empty()), 0
                     )
                     : TextParserV1.TagNodeValue.EMPTY;
               }
            }
         )
      );
      TextParserV1.registerDefault(
         TextParserV1.TextTag.of(
            "nbt",
            "special",
            false,
            (tag, data, input, handlers, endAt) -> {
               String[] lines = data.split(":");
               if (lines.length < 3) {
                  return TextParserV1.TagNodeValue.EMPTY;
               }

               String cleanLine1 = TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[1]));

               // 26.1 wraps the block/entity coordinates and entity selector in a CompilableString,
               // so each data source compiles its own argument and a bad one is skipped.
               Record type = null;

               if (lines[0].equals("block")) {
                  var coordinates = GeneralUtils.compile(BlockDataSource.BLOCK_POS_CODEC, cleanLine1);
                  type = coordinates != null ? new BlockDataSource(coordinates) : null;
               } else if (lines[0].equals("entity")) {
                  var selector = GeneralUtils.compile(EntitySelector.COMPILABLE_CODEC, cleanLine1);
                  type = selector != null ? new EntityDataSource(selector) : null;
               } else if (lines[0].equals("storage")) {
                  type = new StorageDataSource(Identifier.tryParse(cleanLine1));
               }
               if (type == null) {
                  return TextParserV1.TagNodeValue.EMPTY;
               }

               Optional<TextNode> separator = lines.length > 3
                  ? Optional.of(
                     TextNode.asSingle(
                        TextParserImpl.recursiveParsing(TextParserImpl.restoreOriginalEscaping(TextParserImpl.cleanArgument(lines[3])), handlers, null).nodes()
                     )
                  )
                  : Optional.empty();
               boolean shouldInterpret = lines.length > 4 && Boolean.parseBoolean(lines[4]);
               return new TextParserV1.TagNodeValue(new NbtNode(lines[2], shouldInterpret, separator, (DataSource)type), 0);
            }
         )
      );
   }

   private static Function<MutableComponent, Component> getTransform(String[] val) {
      if (val.length == 0) {
         return GeneralUtils.MutableTransformer.CLEAR;
      }

      Function<Style, Style> func = x -> x;

      for (String arg : val) {
         func = func.andThen(switch (arg) {
            case "hover" -> x -> x.withHoverEvent(null);
            case "click" -> x -> x.withClickEvent(null);
            case "color" -> x -> x.withColor((TextColor)null);
            case "insertion" -> x -> x.withInsertion(null);
            case "font" -> x -> x.withFont(null);
            case "bold" -> x -> x.withBold(null);
            case "italic" -> x -> x.withItalic(null);
            case "underline" -> x -> x.withUnderlined(null);
            case "strikethrough" -> x -> x.withStrikethrough(null);
            case "all" -> x -> Style.EMPTY;
            default -> x -> x;
         });
      }

      return new GeneralUtils.MutableTransformer(func);
   }

   private static boolean isntFalse(String arg) {
      return arg.isEmpty() || !arg.equals("false");
   }

   private static TextParserV1.TagNodeBuilder wrap(TextTags.Wrapper wrapper) {
      return (tag, data, input, handlers, endAt) -> {
         TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
         return new TextParserV1.TagNodeValue(wrapper.wrap(out.nodes(), data), out.length());
      };
   }

   private static TextParserV1.TagNodeBuilder bool(TextTags.BooleanTag wrapper) {
      return (tag, data, input, handlers, endAt) -> {
         TextParserV1.NodeList out = TextParserImpl.recursiveParsing(input, handlers, endAt);
         return new TextParserV1.TagNodeValue(wrapper.wrap(out.nodes(), isntFalse(data)), out.length());
      };
   }

   interface BooleanTag {
      TextNode wrap(TextNode[] var1, boolean var2);
   }

   interface Wrapper {
      TextNode wrap(TextNode[] var1, String var2);
   }
}
