package eu.pb4.placeholders.impl;

import eu.pb4.placeholders.api.node.KeybindNode;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.NbtNode;
import eu.pb4.placeholders.api.node.ScoreNode;
import eu.pb4.placeholders.api.node.SelectorNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.TranslatedNode;
import eu.pb4.placeholders.api.node.parent.ColorNode;
import eu.pb4.placeholders.api.node.parent.FormattingNode;
import eu.pb4.placeholders.api.node.parent.GradientNode;
import eu.pb4.placeholders.api.node.parent.ParentNode;
import eu.pb4.placeholders.api.node.parent.ParentTextNode;
import eu.pb4.placeholders.api.node.parent.StyledNode;
import java.util.ArrayList;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.network.chat.HoverEvent.ItemStackInfo;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Internal
public class GeneralUtils {
   public static final Logger LOGGER = LoggerFactory.getLogger("Text Placeholder API");
   public static final boolean IS_DEV = ForgePlatform.isDevelopmentEnvironment();
   public static final TextNode[] CASTER = new TextNode[0];
   public static final boolean IS_LEGACY_TRANSLATION;

   public GeneralUtils() {
   }

   public static String durationToString(long x) {
      long seconds = x % 60L;
      long minutes = x / 60L % 60L;
      long hours = x / 3600L % 24L;
      long days = x / 86400L;
      if (days > 0L) {
         return String.format("%dd%dh%dm%ds", days, hours, minutes, seconds);
      } else if (hours > 0L) {
         return String.format("%dh%dm%ds", hours, minutes, seconds);
      } else if (minutes > 0L) {
         return String.format("%dm%ds", minutes, seconds);
      } else {
         return seconds > 0L ? String.format("%ds", seconds) : "---";
      }
   }

   public static boolean isEmpty(Component text) {
      return (text.getContents() == ComponentContents.EMPTY || text.getContents() instanceof LiteralContents l && l.text().isEmpty())
         && text.getSiblings().isEmpty();
   }

   public static MutableComponent toGradient(Component base, GradientNode.GradientProvider posToColor) {
      return recursiveGradient(base, posToColor, 0, getGradientLength(base)).text();
   }

   private static int getGradientLength(Component base) {
      int length = base.getContents() instanceof LiteralContents l ? l.text().length() : (base.getContents() == ComponentContents.EMPTY ? 0 : 1);

      for (Component text : base.getSiblings()) {
         length += getGradientLength(text);
      }

      return length;
   }

   private static GeneralUtils.TextLengthPair recursiveGradient(Component base, GradientNode.GradientProvider posToColor, int pos, int totalLength) {
      if (base.getStyle().getColor() != null) {
         return new GeneralUtils.TextLengthPair(base.copy(), pos + base.getString().length());
      }

      MutableComponent out = Component.empty().setStyle(base.getStyle());
      if (base.getContents() instanceof LiteralContents literalTextContent) {
         int l = literalTextContent.text().length();

         for (int i = 0; i < l; i++) {
            char character = literalTextContent.text().charAt(i);
            int value;
            if (Character.isHighSurrogate(character) && i + 1 < l) {
               char next = literalTextContent.text().charAt(++i);
               if (Character.isLowSurrogate(next)) {
                  value = Character.toCodePoint(character, next);
               } else {
                  value = character;
               }
            } else {
               value = character;
            }

            out.append(Component.literal(Character.toString(value)).setStyle(Style.EMPTY.withColor(posToColor.getColorAt(pos++, totalLength))));
         }
      } else {
         out.append(base.plainCopy().setStyle(Style.EMPTY.withColor(posToColor.getColorAt(pos++, totalLength))));
      }

      for (Component sibling : base.getSiblings()) {
         GeneralUtils.TextLengthPair pair = recursiveGradient(sibling, posToColor, pos, totalLength);
         pos = pair.length;
         out.append(pair.text);
      }

      return new GeneralUtils.TextLengthPair(out, pos);
   }

   public static int hvsToRgb(float hue, float saturation, float value) {
      int h = (int)(hue * 6.0F) % 6;
      float f = hue * 6.0F - h;
      float p = value * (1.0F - saturation);
      float q = value * (1.0F - f * saturation);
      float t = value * (1.0F - (1.0F - f) * saturation);

      return switch (h) {
         case 0 -> rgbToInt(value, t, p);
         case 1 -> rgbToInt(q, value, p);
         case 2 -> rgbToInt(p, value, t);
         case 3 -> rgbToInt(p, q, value);
         case 4 -> rgbToInt(t, p, value);
         case 5 -> rgbToInt(value, p, q);
         default -> 0;
      };
   }

   public static int rgbToInt(float r, float g, float b) {
      return ((int)(r * 255.0F) & 0xFF) << 16 | ((int)(g * 255.0F) & 0xFF) << 8 | (int)(b * 255.0F) & 0xFF;
   }

   public static GeneralUtils.HSV rgbToHsv(int rgb) {
      float b = rgb % 256 / 255.0F;
      rgb >>= 8;
      float g = rgb % 256 / 255.0F;
      rgb >>= 8;
      float r = rgb % 256 / 255.0F;
      float cmax = Math.max(r, Math.max(g, b));
      float cmin = Math.min(r, Math.min(g, b));
      float diff = cmax - cmin;
      float h = -1.0F;
      float s = -1.0F;
      if (cmax == cmin) {
         h = 0.0F;
      } else if (cmax == r) {
         h = (0.1666F * ((g - b) / diff) + 1.0F) % 1.0F;
      } else if (cmax == g) {
         h = (0.1666F * ((b - r) / diff) + 0.333F) % 1.0F;
      } else if (cmax == b) {
         h = (0.1666F * ((r - g) / diff) + 0.666F) % 1.0F;
      }

      if (cmax == 0.0F) {
         s = 0.0F;
      } else {
         s = diff / cmax;
      }

      return new GeneralUtils.HSV(h, s, cmax);
   }

   public static Component deepTransform(Component input) {
      MutableComponent output = cloneText(input);
      removeHoverAndClick(output);
      return output;
   }

   public static Component removeHoverAndClick(Component input) {
      MutableComponent output = cloneText(input);
      removeHoverAndClick(output);
      return output;
   }

   private static void removeHoverAndClick(MutableComponent input) {
      if (input.getStyle() != null) {
         input.setStyle(input.getStyle().withHoverEvent(null).withClickEvent(null));
      }

      if (input.getContents() instanceof TranslatableContents text) {
         for (int i = 0; i < text.getArgs().length; i++) {
            if (text.getArgs()[i] instanceof MutableComponent argText) {
               removeHoverAndClick(argText);
            }
         }
      }

      for (Component sibling : input.getSiblings()) {
         removeHoverAndClick((MutableComponent)sibling);
      }
   }

   public static MutableComponent cloneText(Component input) {
      MutableComponent baseText;
      if (input.getContents() instanceof TranslatableContents translatable) {
         ArrayList<Object> obj = new ArrayList<>();

         for (Object arg : translatable.getArgs()) {
            if (arg instanceof Component argText) {
               obj.add(cloneText(argText));
            } else {
               obj.add(arg);
            }
         }

         baseText = Component.translatable(translatable.getKey(), obj.toArray());
      } else {
         baseText = input.plainCopy();
      }

      for (Component sibling : input.getSiblings()) {
         baseText.append(cloneText(sibling));
      }

      baseText.setStyle(input.getStyle());
      return baseText;
   }

   public static MutableComponent cloneTransformText(Component input, Function<MutableComponent, MutableComponent> transform) {
      MutableComponent baseText;
      if (input.getContents() instanceof TranslatableContents translatable) {
         ArrayList<Object> obj = new ArrayList<>();

         for (Object arg : translatable.getArgs()) {
            if (arg instanceof Component argText) {
               obj.add(cloneTransformText(argText, transform));
            } else {
               obj.add(arg);
            }
         }

         baseText = Component.translatable(translatable.getKey(), obj.toArray());
      } else {
         baseText = input.plainCopy();
      }

      for (Component sibling : input.getSiblings()) {
         baseText.append(cloneTransformText(sibling, transform));
      }

      baseText.setStyle(input.getStyle());
      return transform.apply(baseText);
   }

   public static Component getItemText(ItemStack stack, boolean rarity) {
      if (!stack.isEmpty()) {
         MutableComponent mutableText = Component.empty().append(stack.getHoverName());
         if (stack.hasCustomHoverName()) {
            mutableText.withStyle(ChatFormatting.ITALIC);
         }

         if (rarity) {
            mutableText.withStyle(stack.getRarity().color);
         }

         mutableText.withStyle(style -> style.withHoverEvent(new HoverEvent(Action.SHOW_ITEM, new ItemStackInfo(stack))));
         return mutableText;
      } else {
         return Component.empty().append(ItemStack.EMPTY.getHoverName());
      }
   }

   public static ParentNode convertToNodes(Component input) {
      ArrayList<TextNode> list = new ArrayList<>();
      if (input.getContents() instanceof LiteralContents content) {
         list.add(new LiteralNode(content.text()));
      } else if (input.getContents() instanceof TranslatableContents content) {
         ArrayList<Object> args = new ArrayList<>();

         for (Object arg : content.getArgs()) {
            if (arg instanceof Component text) {
               args.add(convertToNodes(text));
            } else if (arg instanceof String s) {
               args.add(new LiteralNode(s));
            } else {
               args.add(arg);
            }
         }

         if (IS_LEGACY_TRANSLATION) {
            list.add(TranslatedNode.of(content.getKey(), args.toArray()));
         } else {
            list.add(TranslatedNode.ofFallback(content.getKey(), content.getFallback(), args.toArray()));
         }
      } else if (input.getContents() instanceof ScoreContents content) {
         list.add(new ScoreNode(content.getName(), content.getObjective()));
      } else if (input.getContents() instanceof KeybindContents content) {
         list.add(new KeybindNode(content.getName()));
      } else if (input.getContents() instanceof SelectorContents content) {
         list.add(new SelectorNode(content.getPattern(), content.getSeparator().map(GeneralUtils::convertToNodes)));
      } else if (input.getContents() instanceof NbtContents content) {
         list.add(
            new NbtNode(content.getNbtPath(), content.isInterpreting(), content.getSeparator().map(GeneralUtils::convertToNodes), content.getDataSource())
         );
      }

      for (Component child : input.getSiblings()) {
         list.add(convertToNodes(child));
      }

      if (input.getStyle() == Style.EMPTY) {
         return new ParentNode(list);
      }

      Style style = input.getStyle();
      ParentNode hoverValue = style.getHoverEvent() != null && style.getHoverEvent().getAction() == Action.SHOW_TEXT
         ? convertToNodes((Component)style.getHoverEvent().getValue(Action.SHOW_TEXT))
         : null;
      LiteralNode clickValue = style.getClickEvent() != null ? new LiteralNode(style.getClickEvent().getValue()) : null;
      LiteralNode insertion = style.getInsertion() != null ? new LiteralNode(style.getInsertion()) : null;
      return new StyledNode(list.toArray(new TextNode[0]), style, hoverValue, clickValue, insertion);
   }

   public static TextNode removeColors(TextNode node) {
      if (!(node instanceof ParentTextNode parentNode)) {
         return node;
      } else {
         ArrayList<TextNode> list = new ArrayList<>();

         for (TextNode child : parentNode.getChildren()) {
            list.add(removeColors(child));
         }

         if (node instanceof ColorNode || node instanceof FormattingNode) {
            return new ParentNode(list.toArray(new TextNode[0]));
         } else {
            return node instanceof StyledNode styledNode
               ? new StyledNode(
                  list.toArray(new TextNode[0]),
                  styledNode.rawStyle().withColor((TextColor)null),
                  styledNode.hoverValue(),
                  styledNode.clickValue(),
                  styledNode.insertion()
               )
               : parentNode.copyWith(list.toArray(new TextNode[0]));
         }
      }
   }

   static {
      IS_LEGACY_TRANSLATION = compareVersions(SharedConstants.getCurrentVersion().getId(), "1.19.4") < 0;
   }

   /**
    * Compares two dotted Minecraft version strings such as {@code 1.20.1}, ignoring any suffix.
    */
   private static int compareVersions(String a, String b) {
      String[] left = a.split("\\.");
      String[] right = b.split("\\.");
      int length = Math.max(left.length, right.length);

      for (int i = 0; i < length; i++) {
         int l = i < left.length ? parseVersionPart(left[i]) : 0;
         int r = i < right.length ? parseVersionPart(right[i]) : 0;

         if (l != r) {
            return Integer.compare(l, r);
         }
      }

      return 0;
   }

   private static int parseVersionPart(String part) {
      int end = 0;

      while (end < part.length() && Character.isDigit(part.charAt(end))) {
         end++;
      }

      return end == 0 ? 0 : Integer.parseInt(part.substring(0, end));
   }

   public record HSV(float h, float s, float v) {
      public HSV {
      }
   }

   public record MutableTransformer(Function<Style, Style> textMutableTextFunction) implements Function<MutableComponent, Component> {
      public static final GeneralUtils.MutableTransformer CLEAR = new GeneralUtils.MutableTransformer(x -> Style.EMPTY);

      public MutableTransformer {
      }

      public Component apply(MutableComponent text) {
         return GeneralUtils.cloneTransformText(text, this::transformStyle);
      }

      private MutableComponent transformStyle(MutableComponent mutableText) {
         return mutableText.setStyle(this.textMutableTextFunction.apply(mutableText.getStyle()));
      }
   }

   public record Pair<L, R>(L left, R right) {
      public Pair {
      }
   }

   public record TextLengthPair(MutableComponent text, int length) {
      public static final GeneralUtils.TextLengthPair EMPTY = new GeneralUtils.TextLengthPair(null, 0);

      public TextLengthPair {
      }
   }
}
