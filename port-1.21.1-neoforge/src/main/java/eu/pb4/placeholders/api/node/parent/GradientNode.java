package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.impl.GeneralUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;

public final class GradientNode extends ParentNode {
   private final GradientNode.GradientProvider gradientProvider;

   public GradientNode(TextNode[] children, GradientNode.GradientProvider gradientBuilder) {
      super(children);
      this.gradientProvider = gradientBuilder;
   }

   public static Component apply(Component text, GradientNode.GradientProvider gradientProvider) {
      return GeneralUtils.toGradient(text, gradientProvider);
   }

   public static GradientNode rainbow(float saturation, float value, float frequency, float offset, int gradientLength, TextNode... nodes) {
      return new GradientNode(nodes, GradientNode.GradientProvider.rainbow(saturation, value, frequency, offset, gradientLength));
   }

   public static GradientNode rainbow(float saturation, float value, float frequency, float offset, TextNode... nodes) {
      return new GradientNode(nodes, GradientNode.GradientProvider.rainbow(saturation, value, frequency, offset));
   }

   public static GradientNode rainbow(float saturation, float value, float frequency, TextNode... nodes) {
      return rainbow(saturation, value, frequency, 0.0F, nodes);
   }

   public static GradientNode rainbow(float saturation, float value, TextNode... nodes) {
      return rainbow(saturation, value, 1.0F, 0.0F, nodes);
   }

   public static GradientNode rainbow(float saturation, TextNode... nodes) {
      return rainbow(saturation, 1.0F, 1.0F, 0.0F, nodes);
   }

   public static GradientNode rainbow(TextNode... nodes) {
      return rainbow(1.0F, 1.0F, 1.0F, 0.0F, nodes);
   }

   public static GradientNode colors(TextColor from, TextColor to, TextNode... nodes) {
      return colors(List.of(from, to), nodes);
   }

   public static GradientNode colors(List<TextColor> colors, TextNode... nodes) {
      return new GradientNode(nodes, GradientNode.GradientProvider.colors(colors));
   }

   public static GradientNode colorsHard(TextColor from, TextColor to, TextNode... nodes) {
      return colorsHard(List.of(from, to), nodes);
   }

   public static GradientNode colorsHard(List<TextColor> colors, TextNode... nodes) {
      return new GradientNode(nodes, GradientNode.GradientProvider.colorsHard(colors));
   }

   @Override
   protected Component applyFormatting(MutableComponent out, ParserContext context) {
      return GeneralUtils.toGradient(out, this.gradientProvider);
   }

   @Override
   public ParentTextNode copyWith(TextNode[] children) {
      return new GradientNode(children, this.gradientProvider);
   }

   @Override
   public String toString() {
      return "GradientNode{gradientProvider=" + this.gradientProvider + ", children=" + Arrays.toString(this.children) + "}";
   }

   @FunctionalInterface
   public interface GradientProvider {
      TextColor getColorAt(int var1, int var2);

      static GradientNode.GradientProvider colors(List<TextColor> colors) {
         ArrayList<GeneralUtils.HSV> hvs = new ArrayList<>(colors.size());

         for (TextColor color : colors) {
            hvs.add(GeneralUtils.rgbToHsv(color.getValue()));
         }

         if (hvs.size() == 0) {
            hvs.add(new GeneralUtils.HSV(1.0F, 1.0F, 1.0F));
         } else if (hvs.size() == 1) {
            hvs.add(hvs.get(0));
         }

         int colorSize = hvs.size();
         return (pos, length) -> {
            double step = (colorSize - 1.0) / length;
            float sectionSize = (float)length / (colorSize - 1);
            float progress = pos % sectionSize / sectionSize;
            GeneralUtils.HSV colorA = hvs.get(Math.min((int)(pos / sectionSize), colorSize - 1));
            GeneralUtils.HSV colorB = hvs.get(Math.min((int)(pos / sectionSize) + 1, colorSize - 1));
            float h = colorB.h() - colorA.h();
            float delta = h + (Math.abs(h) > 0.50001 ? (h < 0.0F ? 1 : -1) : 0);
            float futureHue = (float)(colorA.h() + delta * step * pos);
            if (futureHue < 0.0F) {
               futureHue++;
            } else if (futureHue > 1.0F) {
               futureHue--;
            }

            float hue = futureHue;
            h = Mth.clamp(colorB.s() * progress + colorA.s() * (1.0F - progress), 0.0F, 1.0F);
            delta = Mth.clamp(colorB.v() * progress + colorA.v() * (1.0F - progress), 0.0F, 1.0F);
            return TextColor.fromRgb(GeneralUtils.hvsToRgb(Mth.clamp(hue, 0.0F, 1.0F), h, delta));
         };
      }

      static GradientNode.GradientProvider colorsHard(List<TextColor> colors) {
         int colorSize = colors.size();
         return (pos, length) -> {
            if (length == 0) {
               return colors.get(0);
            }

            float sectionSize = (float)length / colorSize;
            return colors.get(Math.min((int)(pos / sectionSize), colorSize - 1));
         };
      }

      static GradientNode.GradientProvider rainbow(float saturation, float value, float frequency, float offset, int gradientLength) {
         float finalFreqLength = frequency < 0.0F ? -frequency : 0.0F;
         return (pos, length) -> TextColor.fromRgb(
            GeneralUtils.hvsToRgb(((pos * frequency + finalFreqLength * length) / (gradientLength + 1) + offset) % 1.0F, saturation, value)
         );
      }

      static GradientNode.GradientProvider rainbow(float saturation, float value, float frequency, float offset) {
         float finalFreqLength = frequency < 0.0F ? -frequency : 0.0F;
         return (pos, length) -> TextColor.fromRgb(
            GeneralUtils.hvsToRgb(((pos * frequency + finalFreqLength * length) / (length + 1) + offset) % 1.0F, saturation, 1.0F)
         );
      }
   }
}
