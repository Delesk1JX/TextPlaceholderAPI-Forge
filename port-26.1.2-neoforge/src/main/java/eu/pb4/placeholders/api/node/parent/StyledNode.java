package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import net.minecraft.network.chat.ClickEvent;
import eu.pb4.placeholders.impl.GeneralUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.Action;
import org.jetbrains.annotations.Nullable;

public final class StyledNode extends ParentNode {
   private final Style style;
   private final ParentNode hoverValue;
   private final TextNode clickValue;
   private final TextNode insertion;

   public StyledNode(TextNode[] children, Style style, @Nullable ParentNode hoverValue, @Nullable TextNode clickValue, @Nullable TextNode insertion) {
      super(children);
      this.style = style;
      this.hoverValue = hoverValue;
      this.clickValue = clickValue;
      this.insertion = insertion;
   }

   public Style style(ParserContext context) {
      Style style = this.style;
      if (this.hoverValue != null && style.getHoverEvent() instanceof HoverEvent.ShowText) {
         style = style.withHoverEvent(new HoverEvent.ShowText(this.hoverValue.toText(context, true)));
      }

      if (this.clickValue != null && style.getClickEvent() != null) {
         ClickEvent clickEvent = GeneralUtils.createClickEvent(style.getClickEvent().action(), this.clickValue.toText(context, true).getString());

         if (clickEvent != null) {
            style = style.withClickEvent(clickEvent);
         }
      }

      if (this.insertion != null) {
         style = style.withInsertion(this.insertion.toText(context, true).getString());
      }

      return style;
   }

   public Style rawStyle() {
      return this.style;
   }

   @Nullable
   public ParentNode hoverValue() {
      return this.hoverValue;
   }

   @Nullable
   public TextNode clickValue() {
      return this.clickValue;
   }

   @Nullable
   public TextNode insertion() {
      return this.insertion;
   }

   @Override
   protected Component applyFormatting(MutableComponent out, ParserContext context) {
      return (out.getStyle() == Style.EMPTY ? out : Component.empty().append(out)).setStyle(this.style(context));
   }

   @Override
   public ParentTextNode copyWith(TextNode[] children) {
      return new StyledNode(children, this.style, this.hoverValue, this.clickValue, this.insertion);
   }

   @Override
   public ParentTextNode copyWith(TextNode[] children, NodeParser parser) {
      return new StyledNode(
         children,
         this.style,
         this.hoverValue != null ? new ParentNode(parser.parseNodes(this.hoverValue)) : null,
         this.clickValue != null ? TextNode.asSingle(parser.parseNodes(this.clickValue)) : null,
         this.insertion != null ? TextNode.asSingle(parser.parseNodes(this.insertion)) : null
      );
   }

   @Override
   public boolean isDynamicNoChildren() {
      return this.clickValue != null && this.clickValue.isDynamic()
         || this.hoverValue != null && this.hoverValue.isDynamic()
         || this.insertion != null && this.insertion.isDynamic();
   }

   @Override
   public String toString() {
      return "StyledNode{style=" + this.style + ", hoverValue=" + this.hoverValue + ", clickValue=" + this.clickValue + ", insertion=" + this.insertion + "}";
   }
}
