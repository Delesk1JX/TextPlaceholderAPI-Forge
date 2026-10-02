package eu.pb4.placeholders.api;

import com.mojang.authlib.GameProfile;
import eu.pb4.placeholders.impl.placeholder.ViewObjectImpl;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record PlaceholderContext(
   MinecraftServer server,
   Supplier<CommandSourceStack> lazySource,
   @Nullable ServerLevel world,
   @Nullable ServerPlayer player,
   @Nullable Entity entity,
   @Nullable GameProfile gameProfile,
   PlaceholderContext.ViewObject view
) {
   public static ParserContext.Key<PlaceholderContext> KEY = new ParserContext.Key<>("placeholder_context", PlaceholderContext.class);

   public PlaceholderContext(
      MinecraftServer server,
      CommandSourceStack source,
      @Nullable ServerLevel world,
      @Nullable ServerPlayer player,
      @Nullable Entity entity,
      @Nullable GameProfile gameProfile,
      PlaceholderContext.ViewObject view
   ) {
      this(server, () -> source, world, player, entity, gameProfile, view);
   }

   public PlaceholderContext(
      MinecraftServer server,
      CommandSourceStack source,
      @Nullable ServerLevel world,
      @Nullable ServerPlayer player,
      @Nullable Entity entity,
      @Nullable GameProfile gameProfile
   ) {
      this(server, source, world, player, entity, gameProfile, PlaceholderContext.ViewObject.DEFAULT);
   }

   public PlaceholderContext {
   }

   public CommandSourceStack source() {
      return this.lazySource.get();
   }

   public boolean hasWorld() {
      return this.world != null;
   }

   public boolean hasPlayer() {
      return this.player != null;
   }

   public boolean hasGameProfile() {
      return this.gameProfile != null;
   }

   public boolean hasEntity() {
      return this.entity != null;
   }

   public ParserContext asParserContext() {
      return ParserContext.of(KEY, this);
   }

   public PlaceholderContext withView(PlaceholderContext.ViewObject view) {
      return new PlaceholderContext(this.server, this.lazySource, this.world, this.player, this.entity, this.gameProfile, view);
   }

   public void addToContext(ParserContext context) {
      context.with(KEY, this);
   }

   public static PlaceholderContext of(MinecraftServer server) {
      return of(server, PlaceholderContext.ViewObject.DEFAULT);
   }

   public static PlaceholderContext of(MinecraftServer server, PlaceholderContext.ViewObject view) {
      return new PlaceholderContext(server, server::createCommandSourceStack, null, null, null, null, view);
   }

   public static PlaceholderContext of(GameProfile profile, MinecraftServer server) {
      return of(profile, server, PlaceholderContext.ViewObject.DEFAULT);
   }

   public static PlaceholderContext of(GameProfile profile, MinecraftServer server, PlaceholderContext.ViewObject view) {
      String name = profile.getName() != null ? profile.getName() : profile.getId().toString();
      return new PlaceholderContext(
         server,
         () -> new CommandSourceStack(
            CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, server.overworld(), server.getProfilePermissions(profile), name, Component.literal(name), server, null
         ),
         null,
         null,
         null,
         profile,
         view
      );
   }

   public static PlaceholderContext of(ServerPlayer player) {
      return of(player, PlaceholderContext.ViewObject.DEFAULT);
   }

   public static PlaceholderContext of(ServerPlayer player, PlaceholderContext.ViewObject view) {
      return new PlaceholderContext(player.getServer(), player::createCommandSourceStack, player.serverLevel(), player, player, player.getGameProfile(), view);
   }

   public static PlaceholderContext of(CommandSourceStack source) {
      return of(source, PlaceholderContext.ViewObject.DEFAULT);
   }

   public static PlaceholderContext of(CommandSourceStack source, PlaceholderContext.ViewObject view) {
      return new PlaceholderContext(
         source.getServer(),
         source,
         source.getLevel(),
         source.getPlayer(),
         source.getEntity(),
         source.getPlayer() != null ? source.getPlayer().getGameProfile() : null,
         view
      );
   }

   public static PlaceholderContext of(Entity entity) {
      return of(entity, PlaceholderContext.ViewObject.DEFAULT);
   }

   public static PlaceholderContext of(Entity entity, PlaceholderContext.ViewObject view) {
      return entity instanceof ServerPlayer player
         ? of(player, view)
         : new PlaceholderContext(entity.getServer(), entity::createCommandSourceStack, (ServerLevel)entity.level(), null, entity, null, view);
   }

   public interface ViewObject {
      PlaceholderContext.ViewObject DEFAULT = of(ResourceLocation.fromNamespaceAndPath("placeholder_api", "default"));

      static PlaceholderContext.ViewObject of(ResourceLocation identifier) {
         return new ViewObjectImpl(identifier);
      }

      ResourceLocation identifier();
   }
}
