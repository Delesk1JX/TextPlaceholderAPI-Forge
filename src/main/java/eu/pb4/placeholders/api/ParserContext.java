package eu.pb4.placeholders.api;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class ParserContext {
   private final Map<ParserContext.Key<?>, Object> map = new HashMap<>();

   private ParserContext() {
   }

   public static ParserContext of() {
      return new ParserContext();
   }

   public static <T> ParserContext of(ParserContext.Key<T> key, T object) {
      return new ParserContext().with(key, object);
   }

   public <T> ParserContext with(ParserContext.Key<T> key, T object) {
      this.map.put(key, object);
      return this;
   }

   @Nullable
   public <T> T get(ParserContext.Key<T> key) {
      return (T)this.map.get(key);
   }

   public record Key<T>(String key, @Nullable Class<T> type) {
      public static final ParserContext.Key<Boolean> COMPACT_TEXT = new ParserContext.Key<>("compact_text", Boolean.class);

      public Key {
      }

      public static <T> ParserContext.Key<T> of(String key, T type) {
         return new ParserContext.Key<>(key, (Class<T>)type.getClass());
      }

      public static <T> ParserContext.Key<T> of(String key) {
         return new ParserContext.Key<>(key, null);
      }
   }
}
