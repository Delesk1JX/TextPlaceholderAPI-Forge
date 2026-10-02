package eu.pb4.placeholders.api.node;

import eu.pb4.placeholders.api.ParserContext;
import net.minecraft.network.chat.Component;

public record NonTransformableNode(TextNode node) implements TextNode {
   public NonTransformableNode {
   }

   @Override
   public Component toText(ParserContext context, boolean removeBackslashes) {
      return this.node.toText(context, removeBackslashes);
   }

   @Override
   public boolean isDynamic() {
      return this.node.isDynamic();
   }
}
