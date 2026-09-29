package eu.pb4.placeholders.impl.placeholder.builtin;

import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.impl.GeneralUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Team;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class PlayerPlaceholders {
   public PlayerPlaceholders() {
   }

   public static void register() {
      Placeholders.register(new ResourceLocation("player", "name"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            return PlaceholderResult.value(ctx.player().getName());
         } else {
            return ctx.hasGameProfile() ? PlaceholderResult.value(Component.nullToEmpty(ctx.gameProfile().getName())) : PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "name_visual"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            return PlaceholderResult.value(GeneralUtils.removeHoverAndClick(ctx.player().getName()));
         } else {
            return ctx.hasGameProfile() ? PlaceholderResult.value(Component.nullToEmpty(ctx.gameProfile().getName())) : PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "name_unformatted"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            return PlaceholderResult.value(ctx.player().getName().getString());
         } else {
            return ctx.hasGameProfile() ? PlaceholderResult.value(Component.nullToEmpty(ctx.gameProfile().getName())) : PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(
         new ResourceLocation("player", "ping"),
         (ctx, arg) -> ctx.hasPlayer() ? PlaceholderResult.value(String.valueOf(ctx.player().latency)) : PlaceholderResult.invalid("No player!")
      );
      Placeholders.register(
         new ResourceLocation("player", "ping_colored"),
         (ctx, arg) -> {
            if (ctx.hasPlayer()) {
               int x = ctx.player().latency;
               return PlaceholderResult.value(
                  Component.literal(String.valueOf(x)).withStyle(x < 100 ? ChatFormatting.GREEN : (x < 200 ? ChatFormatting.GOLD : ChatFormatting.RED))
               );
            } else {
               return PlaceholderResult.invalid("No player!");
            }
         }
      );
      Placeholders.register(new ResourceLocation("player", "displayname"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            return PlaceholderResult.value(ctx.player().getDisplayName());
         } else {
            return ctx.hasGameProfile() ? PlaceholderResult.value(Component.nullToEmpty(ctx.gameProfile().getName())) : PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(
         new ResourceLocation("player", "display_name"), (PlaceholderHandler)Placeholders.getPlaceholders().get(new ResourceLocation("player", "displayname"))
      );
      Placeholders.register(new ResourceLocation("player", "displayname_visual"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            return PlaceholderResult.value(GeneralUtils.removeHoverAndClick(ctx.player().getDisplayName()));
         } else {
            return ctx.hasGameProfile() ? PlaceholderResult.value(Component.nullToEmpty(ctx.gameProfile().getName())) : PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(
         new ResourceLocation("player", "display_name_visual"),
         (PlaceholderHandler)Placeholders.getPlaceholders().get(new ResourceLocation("player", "displayname_visual"))
      );
      Placeholders.register(new ResourceLocation("player", "displayname_unformatted"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            return PlaceholderResult.value(Component.literal(ctx.player().getDisplayName().getString()));
         } else {
            return ctx.hasGameProfile() ? PlaceholderResult.value(Component.nullToEmpty(ctx.gameProfile().getName())) : PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(
         new ResourceLocation("player", "display_name_unformatted"),
         (PlaceholderHandler)Placeholders.getPlaceholders().get(new ResourceLocation("player", "displayname_unformatted"))
      );
      Placeholders.register(new ResourceLocation("player", "inventory_slot"), (ctx, arg) -> {
         if (ctx.hasPlayer() && arg != null) {
            try {
               int slot = Integer.parseInt(arg);
               Inventory inventory = ctx.player().getInventory();
               if (slot >= 0 && slot < inventory.getContainerSize()) {
                  ItemStack stack = inventory.getItem(slot);
                  return PlaceholderResult.value(GeneralUtils.getItemText(stack, true));
               }
            } catch (Exception var5) {
            }

            return PlaceholderResult.invalid("Invalid argument");
         } else {
            return PlaceholderResult.invalid("No player or invalid argument!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "inventory_slot_no_rarity"), (ctx, arg) -> {
         if (ctx.hasPlayer() && arg != null) {
            try {
               int slot = Integer.parseInt(arg);
               Inventory inventory = ctx.player().getInventory();
               if (slot >= 0 && slot < inventory.getContainerSize()) {
                  ItemStack stack = inventory.getItem(slot);
                  return PlaceholderResult.value(GeneralUtils.getItemText(stack, false));
               }
            } catch (Exception var5) {
            }

            return PlaceholderResult.invalid("Invalid argument");
         } else {
            return PlaceholderResult.invalid("No player or invalid argument!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "equipment_slot"), (ctx, arg) -> {
         if (ctx.hasPlayer() && arg != null) {
            try {
               EquipmentSlot slot = EquipmentSlot.byName(arg);
               ItemStack stack = ctx.player().getItemBySlot(slot);
               return PlaceholderResult.value(GeneralUtils.getItemText(stack, true));
            } catch (Exception var4) {
               return PlaceholderResult.invalid("Invalid argument");
            }
         } else {
            return PlaceholderResult.invalid("No player or invalid argument!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "equipment_slot_no_rarity"), (ctx, arg) -> {
         if (ctx.hasPlayer() && arg != null) {
            try {
               EquipmentSlot slot = EquipmentSlot.byName(arg);
               ItemStack stack = ctx.player().getItemBySlot(slot);
               return PlaceholderResult.value(GeneralUtils.getItemText(stack, false));
            } catch (Exception var4) {
               return PlaceholderResult.invalid("Invalid argument");
            }
         } else {
            return PlaceholderResult.invalid("No player or invalid argument!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "playtime"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            int x = ctx.player().getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            return PlaceholderResult.value(arg != null ? DurationFormatUtils.formatDuration(x * 50L, arg, true) : GeneralUtils.durationToString(x / 20L));
         } else {
            return PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "statistic"), (ctx, arg) -> {
         if (ctx.hasPlayer() && arg != null) {
            try {
               String[] args = arg.split(" ");
               if (args.length == 1) {
                  ResourceLocation identifier = ResourceLocation.tryParse(args[0]);
                  if (identifier != null) {
                     Stat<ResourceLocation> stat = Stats.CUSTOM.get((ResourceLocation)BuiltInRegistries.CUSTOM_STAT.get(identifier));
                     int x = ctx.player().getStats().getValue(stat);
                     return PlaceholderResult.value(stat.format(x));
                  }
               } else if (args.length >= 2) {
                  ResourceLocation type = ResourceLocation.tryParse(args[0]);
                  ResourceLocation id = ResourceLocation.tryParse(args[1]);
                  if (type != null) {
                     StatType<Object> statType = (StatType<Object>)BuiltInRegistries.STAT_TYPE.get(type);
                     if (statType != null) {
                        Object key = statType.getRegistry().get(id);
                        if (key != null) {
                           Stat<Object> stat = statType.get(key);
                           int x = ctx.player().getStats().getValue(stat);
                           return PlaceholderResult.value(stat.format(x));
                        }
                     }
                  }
               }
            } catch (Exception var9) {
            }

            return PlaceholderResult.invalid("Invalid statistic!");
         } else {
            return PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "statistic_raw"), (ctx, arg) -> {
         if (ctx.hasPlayer() && arg != null) {
            try {
               String[] args = arg.split(" ");
               if (args.length == 1) {
                  ResourceLocation identifier = ResourceLocation.tryParse(args[0]);
                  if (identifier != null) {
                     Stat<ResourceLocation> stat = Stats.CUSTOM.get((ResourceLocation)BuiltInRegistries.CUSTOM_STAT.get(identifier));
                     int x = ctx.player().getStats().getValue(stat);
                     return PlaceholderResult.value(String.valueOf(x));
                  }
               } else if (args.length >= 2) {
                  ResourceLocation type = ResourceLocation.tryParse(args[0]);
                  ResourceLocation id = ResourceLocation.tryParse(args[1]);
                  if (type != null) {
                     StatType<Object> statType = (StatType<Object>)BuiltInRegistries.STAT_TYPE.get(type);
                     if (statType != null) {
                        Object key = statType.getRegistry().get(id);
                        if (key != null) {
                           Stat<Object> stat = statType.get(key);
                           int x = ctx.player().getStats().getValue(stat);
                           return PlaceholderResult.value(String.valueOf(x));
                        }
                     }
                  }
               }
            } catch (Exception var9) {
            }

            return PlaceholderResult.invalid("Invalid statistic!");
         } else {
            return PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "objective"), (ctx, arg) -> {
         if (ctx.hasPlayer() && arg != null) {
            try {
               ServerScoreboard scoreboard = ctx.server().getScoreboard();
               Objective scoreboardObjective = scoreboard.getOrCreateObjective(arg);
               if (scoreboardObjective == null) {
                  return PlaceholderResult.invalid("Invalid objective!");
               }

               Score score = scoreboard.getOrCreatePlayerScore(ctx.player().getScoreboardName(), scoreboardObjective);
               return PlaceholderResult.value(String.valueOf(score.getScore()));
            } catch (Exception var5) {
               return PlaceholderResult.invalid("Invalid objective!");
            }
         } else {
            return PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "pos_x"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            double value = ctx.player().getX();
            String format = "%.2f";
            if (arg != null) {
               try {
                  int x = Integer.parseInt(arg);
                  format = "%." + x + "f";
               } catch (Exception e) {
                  format = "%.2f";
               }
            }

            return PlaceholderResult.value(String.format(format, value));
         } else {
            return PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "pos_y"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            double value = ctx.player().getY();
            String format = "%.2f";
            if (arg != null) {
               try {
                  int x = Integer.parseInt(arg);
                  format = "%." + x + "f";
               } catch (Exception e) {
                  format = "%.2f";
               }
            }

            return PlaceholderResult.value(String.format(format, value));
         } else {
            return PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "pos_z"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            double value = ctx.player().getZ();
            String format = "%.2f";
            if (arg != null) {
               try {
                  int x = Integer.parseInt(arg);
                  format = "%." + x + "f";
               } catch (Exception e) {
                  format = "%.2f";
               }
            }

            return PlaceholderResult.value(String.format(format, value));
         } else {
            return PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(
         new ResourceLocation("player", "uuid"),
         (ctx, arg) -> {
            if (ctx.hasPlayer()) {
               return PlaceholderResult.value(ctx.player().getStringUUID());
            } else {
               return ctx.hasGameProfile()
                  ? PlaceholderResult.value(Component.nullToEmpty(ctx.gameProfile().getId() + ""))
                  : PlaceholderResult.invalid("No player!");
            }
         }
      );
      Placeholders.register(
         new ResourceLocation("player", "health"),
         (ctx, arg) -> ctx.hasPlayer() ? PlaceholderResult.value(String.format("%.0f", ctx.player().getHealth())) : PlaceholderResult.invalid("No player!")
      );
      Placeholders.register(
         new ResourceLocation("player", "max_health"),
         (ctx, arg) -> ctx.hasPlayer() ? PlaceholderResult.value(String.format("%.0f", ctx.player().getMaxHealth())) : PlaceholderResult.invalid("No player!")
      );
      Placeholders.register(
         new ResourceLocation("player", "hunger"),
         (ctx, arg) -> ctx.hasPlayer()
            ? PlaceholderResult.value(String.format("%.0f", ctx.player().getFoodData().getFoodLevel()))
            : PlaceholderResult.invalid("No player!")
      );
      Placeholders.register(
         new ResourceLocation("player", "saturation"),
         (ctx, arg) -> ctx.hasPlayer()
            ? PlaceholderResult.value(String.format("%.0f", ctx.player().getFoodData().getSaturationLevel()))
            : PlaceholderResult.invalid("No player!")
      );
      Placeholders.register(new ResourceLocation("player", "team_name"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            Team team = ctx.player().getTeam();
            return PlaceholderResult.value((Component)(team == null ? Component.empty() : Component.nullToEmpty(team.getName())));
         } else {
            return PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "team_displayname"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            PlayerTeam team = (PlayerTeam)ctx.player().getTeam();
            return PlaceholderResult.value((Component)(team == null ? Component.empty() : team.getDisplayName()));
         } else {
            return PlaceholderResult.invalid("No player!");
         }
      });
      Placeholders.register(new ResourceLocation("player", "team_displayname_formatted"), (ctx, arg) -> {
         if (ctx.hasPlayer()) {
            PlayerTeam team = (PlayerTeam)ctx.player().getTeam();
            return PlaceholderResult.value(team == null ? Component.empty() : team.getFormattedDisplayName());
         } else {
            return PlaceholderResult.invalid("No player!");
         }
      });
   }
}
