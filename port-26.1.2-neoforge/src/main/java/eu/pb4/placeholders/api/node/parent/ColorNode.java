package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public final class ColorNode extends ParentNode {
   private final TextColor color;

   public ColorNode(TextNode[] children, TextColor color) {
      super(children);
      this.color = color;
   }

   @Override
   protected Component applyFormatting(MutableComponent out, ParserContext context) {
      return out.setStyle(out.getStyle().withColor(this.color));
   }

   @Override
   public ParentTextNode copyWith(TextNode[] children) {
      return new ColorNode(children, this.color);
   }

   @Override
   public String toString() {
      return "ColorNode{color=" + this.color + ", children=" + Arrays.toString(this.children) + "}";
   }
}
