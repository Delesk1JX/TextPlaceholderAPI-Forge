package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.Action;

public final class ClickActionNode extends ParentNode {
   private final Action action;
   private final TextNode value;

   public ClickActionNode(TextNode[] children, Action action, TextNode value) {
      super(children);
      this.action = action;
      this.value = value;
   }

   public Action action() {
      return this.action;
   }

   public TextNode value() {
      return this.value;
   }

   @Override
   protected Component applyFormatting(MutableComponent out, ParserContext context) {
      return out.setStyle(out.getStyle().withClickEvent(new ClickEvent(this.action, this.value.toText(context, true).getString())));
   }

   @Override
   public ParentTextNode copyWith(TextNode[] children) {
      return new ClickActionNode(children, this.action, this.value);
   }

   @Override
   public ParentTextNode copyWith(TextNode[] children, NodeParser parser) {
      return new ClickActionNode(children, this.action, TextNode.asSingle(parser.parseNodes(this.value)));
   }

   @Override
   public boolean isDynamicNoChildren() {
      return this.value.isDynamic();
   }

   @Override
   public String toString() {
      return "ClickActionNode{action=" + this.action + ", value=" + this.value + "}";
   }
}
