package eu.pb4.placeholders.api.node;

import eu.pb4.placeholders.api.ParserContext;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.DataSource;

public record NbtNode(String rawPath, boolean interpret, Optional<TextNode> separator, DataSource dataSource) implements TextNode {
   public NbtNode {
   }

   @Override
   public Component toText(ParserContext context, boolean removeBackslashes) {
      return Component.nbt(this.rawPath, this.interpret, this.separator.map(x -> x.toText(context, removeBackslashes)), this.dataSource);
   }

   @Override
   public boolean isDynamic() {
      return this.separator.isPresent() && this.separator.get().isDynamic();
   }
}
