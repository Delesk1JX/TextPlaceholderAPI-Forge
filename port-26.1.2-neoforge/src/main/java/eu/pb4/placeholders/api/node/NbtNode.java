package eu.pb4.placeholders.api.node;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.impl.GeneralUtils;
import java.util.Optional;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.data.DataSource;

public record NbtNode(String rawPath, boolean interpret, Optional<TextNode> separator, DataSource dataSource) implements TextNode {
   public NbtNode {
   }

   @Override
   public Component toText(ParserContext context, boolean removeBackslashes) {
      // 26.1 wants a CompilableString here, so the raw path is compiled on the way out. An
      // unparseable path yields no text instead of throwing.
      var path = GeneralUtils.compile(NbtContents.NBT_PATH_CODEC, this.rawPath);

      if (path == null) {
         return Component.empty();
      }

      return Component.nbt(
         path,
         this.interpret,
         false,
         this.separator.map(x -> x.toText(context, removeBackslashes)),
         this.dataSource
      );
   }

   @Override
   public boolean isDynamic() {
      return this.separator.isPresent() && this.separator.get().isDynamic();
   }
}
