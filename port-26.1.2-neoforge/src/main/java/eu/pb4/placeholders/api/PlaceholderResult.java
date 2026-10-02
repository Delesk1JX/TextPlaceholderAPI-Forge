package eu.pb4.placeholders.api;

import eu.pb4.placeholders.api.parsers.TextParserV1;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class PlaceholderResult {
   private final Component text;
   private String string;
   private final boolean valid;

   private PlaceholderResult(Component text, String reason) {
      if (text != null) {
         this.text = text;
         this.valid = true;
      } else {
         this.text = Component.literal("[" + (reason != null ? reason : "Invalid placeholder!") + "]")
            .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true));
         this.valid = false;
      }
   }

   public Component text() {
      return this.text;
   }

   @Deprecated
   public String string() {
      if (this.string == null) {
         this.string = this.text.getString();
      }

      return this.string;
   }

   public boolean isValid() {
      return this.valid;
   }

   public static PlaceholderResult invalid(String reason) {
      return new PlaceholderResult(null, reason);
   }

   public static PlaceholderResult invalid() {
      return new PlaceholderResult(null, null);
   }

   public static PlaceholderResult value(Component text) {
      return new PlaceholderResult(text, null);
   }

   public static PlaceholderResult value(String text) {
      return new PlaceholderResult(TextParserV1.DEFAULT.parseText(text, null), null);
   }
}
