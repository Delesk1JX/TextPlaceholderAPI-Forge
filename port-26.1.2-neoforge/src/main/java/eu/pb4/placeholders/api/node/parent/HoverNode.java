package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent.EntityTooltipInfo;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public final class HoverNode<T, H> extends ParentNode {
   private final HoverNode.Action<T, H> action;
   private final T value;

   public HoverNode(TextNode[] children, HoverNode.Action<T, H> action, T value) {
      super(children);
      this.action = action;
      this.value = value;
   }

   @Override
   protected Component applyFormatting(MutableComponent out, ParserContext context) {
      // 26.1 made HoverEvent a sealed interface with one record per action, so the concrete
      // implementation is chosen by branch and the payload type is pinned down at the same time.
      if (this.action == HoverNode.Action.TEXT) {
         Component text = ((TextNode)this.value).toText(context, true);

         return out.setStyle(out.getStyle().withHoverEvent(new HoverEvent.ShowText(text)));
      } else if (this.action == HoverNode.Action.ENTITY) {
         EntityTooltipInfo info = ((HoverNode.EntityNodeContent)this.value).toVanilla(context);

         return out.setStyle(out.getStyle().withHoverEvent(new HoverEvent.ShowEntity(info)));
      }

      return out.setStyle(
         out.getStyle().withHoverEvent(new HoverEvent.ShowItem(ItemStackTemplate.fromNonEmptyStack((ItemStack)this.value)))
      );
   }

   @Override
   public ParentTextNode copyWith(TextNode[] children) {
      return new HoverNode<>(children, this.action, this.value);
   }

   @Override
   public ParentTextNode copyWith(TextNode[] children, NodeParser parser) {
      if (this.action == HoverNode.Action.TEXT) {
         return new HoverNode<>(children, HoverNode.Action.TEXT, parser.parseNode((TextNode)this.value));
      } else if (this.action == HoverNode.Action.ENTITY && ((HoverNode.EntityNodeContent)this.value).name != null) {
         HoverNode.EntityNodeContent val = (HoverNode.EntityNodeContent)this.value;
         return new HoverNode<>(children, HoverNode.Action.ENTITY, new HoverNode.EntityNodeContent(val.entityType, val.uuid, parser.parseNode(val.name)));
      } else {
         return this.copyWith(children);
      }
   }

   public HoverNode.Action<T, H> action() {
      return this.action;
   }

   public T value() {
      return this.value;
   }

   @Override
   public String toString() {
      return "HoverNode{value=" + this.value + ", children=" + Arrays.toString(this.children) + "}";
   }

   @Override
   public boolean isDynamicNoChildren() {
      return this.action == HoverNode.Action.TEXT && ((TextNode)this.value).isDynamic()
         || this.action == HoverNode.Action.ENTITY && ((HoverNode.EntityNodeContent)this.value).name.isDynamic();
   }

   public record Action<T, H>(net.minecraft.network.chat.HoverEvent.Action vanillaType) {
      public static final HoverNode.Action<HoverNode.EntityNodeContent, EntityTooltipInfo> ENTITY = new HoverNode.Action<>(
         net.minecraft.network.chat.HoverEvent.Action.SHOW_ENTITY
      );
      public static final HoverNode.Action<ItemStack, ItemStack> ITEM_STACK = new HoverNode.Action<>(
         net.minecraft.network.chat.HoverEvent.Action.SHOW_ITEM
      );
      public static final HoverNode.Action<TextNode, Component> TEXT = new HoverNode.Action<>(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT);

      public Action {
      }
   }

   public record EntityNodeContent(EntityType<?> entityType, UUID uuid, @Nullable TextNode name) {
      public EntityNodeContent {
      }

      public EntityTooltipInfo toVanilla(ParserContext context) {
         return new EntityTooltipInfo(this.entityType, this.uuid, this.name != null ? this.name.toText(context, true) : null);
      }
   }
}
