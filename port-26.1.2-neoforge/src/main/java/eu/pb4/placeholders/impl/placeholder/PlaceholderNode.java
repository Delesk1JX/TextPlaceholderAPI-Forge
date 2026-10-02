package eu.pb4.placeholders.impl.placeholder;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.impl.GeneralUtils;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record PlaceholderNode(
   ParserContext.Key<PlaceholderContext> contextKey,
   String placeholder,
   Placeholders.PlaceholderGetter getter,
   boolean optionalContext,
   @Nullable String argument
) implements TextNode {
   public PlaceholderNode {
   }

   @Override
   public Component toText(ParserContext context, boolean removeBackslashes) {
      PlaceholderContext ctx = context.get(this.contextKey);
      PlaceholderHandler handler = this.getter.getPlaceholder(this.placeholder, context);
      if ((ctx != null || this.optionalContext) && handler != null) {
         try {
            return handler.onPlaceholderRequest(ctx, this.argument).text();
         } catch (Throwable e) {
            GeneralUtils.LOGGER.error("Error occurred while parsing placeholder " + this.placeholder + " / " + this.contextKey.key() + "!", e);
            return Component.empty();
         }
      } else {
         if (GeneralUtils.IS_DEV) {
            GeneralUtils.LOGGER
               .error("Missing context for placeholders requiring them (" + this.placeholder + " / " + this.contextKey.key() + ")!", new NullPointerException());
         }

         return Component.empty();
      }
   }

   @Override
   public boolean isDynamic() {
      return true;
   }
}
