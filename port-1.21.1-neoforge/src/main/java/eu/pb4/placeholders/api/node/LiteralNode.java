package eu.pb4.placeholders.api.node;

import eu.pb4.placeholders.api.ParserContext;
import net.minecraft.network.chat.Component;

public record LiteralNode(String value) implements TextNode {
   public LiteralNode(StringBuilder builder) {
      this(builder.toString());
   }

   public LiteralNode {
   }

   @Override
   public Component toText(ParserContext context, boolean removeBackslashes) {
      if (this.value.isEmpty()) {
         return Component.empty();
      }

      if (!removeBackslashes) {
         return Component.literal(this.value());
      }

      StringBuilder builder = new StringBuilder();
      int length = this.value.length();

      for (int i = 0; i < length; i++) {
         char c = this.value.charAt(i);
         if (c == '\\' && i + 1 < length) {
            char n = this.value.charAt(i + 1);
            if (!Character.isWhitespace(n) && !Character.isLetterOrDigit(n)) {
               builder.append(n);
               i++;
            } else {
               builder.append(c);
            }
         } else {
            builder.append(c);
         }
      }

      return Component.literal(builder.toString());
   }
}
