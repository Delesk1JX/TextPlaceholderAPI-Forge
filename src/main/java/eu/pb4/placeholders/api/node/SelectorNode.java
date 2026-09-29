package eu.pb4.placeholders.api.node;

import eu.pb4.placeholders.api.ParserContext;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public record SelectorNode(String pattern, Optional<TextNode> separator) implements TextNode {
   public SelectorNode {
   }

   @Override
   public Component toText(ParserContext context, boolean removeBackslashes) {
      return Component.selector(this.pattern, this.separator.map(x -> x.toText(context, removeBackslashes)));
   }

   @Override
   public boolean isDynamic() {
      return this.separator.isPresent() && this.separator.get().isDynamic();
   }
}
