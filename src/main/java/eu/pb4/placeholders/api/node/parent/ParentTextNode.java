package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.impl.textparser.TextParserImpl;
import java.util.Collection;

public interface ParentTextNode extends TextNode {
   TextNode[] getChildren();

   ParentTextNode copyWith(TextNode[] var1);

   default ParentTextNode copyWith(Collection<TextNode> children) {
      return this.copyWith(children.toArray(TextParserImpl.CASTER));
   }

   default boolean isDynamicNoChildren() {
      return false;
   }

   @Override
   default boolean isDynamic() {
      for (TextNode x : this.getChildren()) {
         if (x.isDynamic()) {
            return true;
         }
      }

      return this.isDynamicNoChildren();
   }

   default ParentTextNode copyWith(TextNode[] children, NodeParser parser) {
      return this.copyWith(children);
   }

   default ParentTextNode copyWith(Collection<TextNode> children, NodeParser parser) {
      return this.copyWith(children.toArray(TextParserImpl.CASTER), parser);
   }

   @Deprecated(forRemoval = true)
   @FunctionalInterface
   interface Constructor {
      ParentTextNode createNode(String var1, Collection<ParentTextNode> var2);
   }
}
