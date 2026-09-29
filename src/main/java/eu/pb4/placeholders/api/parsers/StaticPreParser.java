package eu.pb4.placeholders.api.parsers;

import eu.pb4.placeholders.api.node.DirectTextNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.parent.ParentNode;
import java.util.ArrayList;

public record StaticPreParser() implements NodeParser {
   public static final NodeParser INSTANCE = new StaticPreParser();

   @Override
   public TextNode[] parseNodes(TextNode input) {
      return new TextNode[]{parse(input)};
   }

   public static TextNode parse(TextNode node) {
      if (!node.isDynamic()) {
         return new DirectTextNode(node.toText());
      } else if (!(node instanceof ParentNode parentNode)) {
         return node;
      } else {
         ArrayList<TextNode> c = new ArrayList<>();

         for (TextNode child : parentNode.getChildren()) {
            c.add(parse(child));
         }

         return parentNode.copyWith(c.toArray(new TextNode[0]));
      }
   }
}
