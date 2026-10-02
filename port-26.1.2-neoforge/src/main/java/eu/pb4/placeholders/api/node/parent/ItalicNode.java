package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ItalicNode extends ParentNode {
   private final boolean value;

   public ItalicNode(TextNode[] nodes, boolean value) {
      super(nodes);
      this.value = value;
   }

   @Override
   protected Component applyFormatting(MutableComponent out, ParserContext context) {
      return out.setStyle(out.getStyle().withItalic(this.value));
   }

   @Override
   public ParentTextNode copyWith(TextNode[] children) {
      return new ItalicNode(children, this.value);
   }

   @Override
   public String toString() {
      return "ItalicNode{value=" + this.value + ", children=" + Arrays.toString(this.children) + "}";
   }
}
