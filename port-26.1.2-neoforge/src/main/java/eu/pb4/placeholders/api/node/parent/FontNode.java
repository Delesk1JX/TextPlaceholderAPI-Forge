package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

public final class FontNode extends ParentNode {
   private final Identifier font;

   public FontNode(TextNode[] children, Identifier font) {
      super(children);
      this.font = font;
   }

   @Override
   protected Component applyFormatting(MutableComponent out, ParserContext context) {
      return out.setStyle(out.getStyle().withFont(new FontDescription.Resource(this.font)));
   }

   @Override
   public ParentTextNode copyWith(TextNode[] children) {
      return new FontNode(children, this.font);
   }

   @Override
   public String toString() {
      return "FontNode{font=" + this.font + ", children=" + Arrays.toString(this.children) + "}";
   }
}
