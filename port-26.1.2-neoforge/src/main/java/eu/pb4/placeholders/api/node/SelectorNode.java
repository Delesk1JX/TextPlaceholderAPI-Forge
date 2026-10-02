package eu.pb4.placeholders.api.node;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.impl.GeneralUtils;
import java.util.Optional;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;

public record SelectorNode(String pattern, Optional<TextNode> separator) implements TextNode {
   public SelectorNode {
   }

   @Override
   public Component toText(ParserContext context, boolean removeBackslashes) {
      // 26.1 wants a CompilableString here, so the raw pattern is compiled on the way out. An
      // unparseable selector yields no text instead of throwing.
      var selector = GeneralUtils.compile(EntitySelector.COMPILABLE_CODEC, this.pattern);

      if (selector == null) {
         return Component.empty();
      }

      return Component.selector(selector, this.separator.map(x -> x.toText(context, removeBackslashes)));
   }

   @Override
   public boolean isDynamic() {
      return this.separator.isPresent() && this.separator.get().isDynamic();
   }
}
