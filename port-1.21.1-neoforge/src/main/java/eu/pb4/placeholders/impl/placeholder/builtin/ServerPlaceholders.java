package eu.pb4.placeholders.impl.placeholder.builtin;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.impl.ForgePlatform;
import eu.pb4.placeholders.impl.GeneralUtils;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class ServerPlaceholders {
   public ServerPlaceholders() {
   }

   /**
    * Average server tick time in milliseconds.
    *
    * <p>1.21 renamed {@code getAverageTickTime()} to {@code getAverageTickTimeNanos()} and changed
    * the unit to nanoseconds; every %server:tps% / %server:mspt% placeholder divided by 1000, so the
    * conversion has to happen before that arithmetic rather than at each call site.
    */
   private static float avgTickTimeMillis(PlaceholderContext ctx) {
      return ctx.server().getAverageTickTimeNanos() / 1_000_000.0F;
   }

   public static void register() {
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "tps"), (ctx, arg) -> {
         double tps = 1000.0F / Math.max(avgTickTimeMillis(ctx), 50.0F);
         String format = "%.1f";
         if (arg != null) {
            try {
               int x = Integer.parseInt(arg);
               format = "%." + x + "f";
            } catch (Exception e) {
               format = "%.1f";
            }
         }

         return PlaceholderResult.value(String.format(format, tps));
      });
      Placeholders.register(
         ResourceLocation.fromNamespaceAndPath("server", "tps_colored"),
         (ctx, arg) -> {
            double tps = 1000.0F / Math.max(avgTickTimeMillis(ctx), 50.0F);
            String format = "%.1f";
            if (arg != null) {
               try {
                  int x = Integer.parseInt(arg);
                  format = "%." + x + "f";
               } catch (Exception e) {
                  format = "%.1f";
               }
            }

            return PlaceholderResult.value(
               Component.literal(String.format(format, tps))
                  .withStyle(tps > 19.0 ? ChatFormatting.GREEN : (tps > 16.0 ? ChatFormatting.GOLD : ChatFormatting.RED))
            );
         }
      );
      Placeholders.register(
         ResourceLocation.fromNamespaceAndPath("server", "mspt"), (ctx, arg) -> PlaceholderResult.value(String.format("%.0f", avgTickTimeMillis(ctx)))
      );
      Placeholders.register(
         ResourceLocation.fromNamespaceAndPath("server", "mspt_colored"),
         (ctx, arg) -> {
            float x = avgTickTimeMillis(ctx);
            return PlaceholderResult.value(
               Component.literal(String.format("%.0f", x)).withStyle(x < 45.0F ? ChatFormatting.GREEN : (x < 51.0F ? ChatFormatting.GOLD : ChatFormatting.RED))
            );
         }
      );
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "time"), (ctx, arg) -> {
         SimpleDateFormat format = new SimpleDateFormat(arg != null ? arg : "HH:mm:ss");
         return PlaceholderResult.value(format.format(new Date(System.currentTimeMillis())));
      });
      var ref = new Object() {
         WeakReference<MinecraftServer> server;
         long ms;
      };
      Placeholders.register(
         ResourceLocation.fromNamespaceAndPath("server", "uptime"),
         (ctx, arg) -> {
            if (ref.server == null || !ref.server.refersTo(ctx.server())) {
               ref.server = new WeakReference<>(ctx.server());
               ref.ms = System.currentTimeMillis() - ctx.server().getTickCount() * 50L;
            }

            return PlaceholderResult.value(
               arg != null
                  ? DurationFormatUtils.formatDuration(System.currentTimeMillis() - ref.ms, arg, true)
                  : GeneralUtils.durationToString((System.currentTimeMillis() - ref.ms) / 1000L)
            );
         }
      );
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "version"), (ctx, arg) -> PlaceholderResult.value(ctx.server().getServerVersion()));
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "motd"), (ctx, arg) -> {
         ServerStatus metadata = ctx.server().getStatus();
         return metadata == null ? PlaceholderResult.invalid("Server metadata missing!") : PlaceholderResult.value(metadata.description());
      });
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "mod_version"), (ctx, arg) -> {
         if (arg != null) {
            Optional<String> version = ForgePlatform.getModInfo(arg).map(info -> info.getVersion().toString());

            if (version.isPresent()) {
               return PlaceholderResult.value(Component.literal(version.get()));
            }
         }

         return PlaceholderResult.invalid("Invalid argument");
      });
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "mod_name"), (ctx, arg) -> {
         if (arg != null) {
            Optional<String> name = ForgePlatform.getModInfo(arg).map(info -> info.getDisplayName());

            if (name.isPresent()) {
               return PlaceholderResult.value(Component.literal(name.get()));
            }
         }

         return PlaceholderResult.invalid("Invalid argument");
      });
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "brand"), (ctx, arg) -> PlaceholderResult.value(Component.literal(ctx.server().getServerModName())));
      Placeholders.register(
         ResourceLocation.fromNamespaceAndPath("server", "mod_count"),
         (ctx, arg) -> PlaceholderResult.value(Component.literal(ForgePlatform.getModCount() + ""))
      );
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "mod_description"), (ctx, arg) -> {
         if (arg != null) {
            Optional<String> description = ForgePlatform.getModInfo(arg).map(info -> info.getDescription());

            if (description.isPresent()) {
               return PlaceholderResult.value(Component.literal(description.get()));
            }
         }

         return PlaceholderResult.invalid("Invalid argument");
      });
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "name"), (ctx, arg) -> PlaceholderResult.value(ctx.server().name()));
      Placeholders.register(
         ResourceLocation.fromNamespaceAndPath("server", "used_ram"),
         (ctx, arg) -> {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            return PlaceholderResult.value(
               Objects.equals(arg, "gb")
                  ? String.format("%.1f", (float)heapUsage.getUsed() / 1.0737418E9F)
                  : String.format("%d", heapUsage.getUsed() / 1048576L)
            );
         }
      );
      Placeholders.register(
         ResourceLocation.fromNamespaceAndPath("server", "max_ram"),
         (ctx, arg) -> {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            return PlaceholderResult.value(
               Objects.equals(arg, "gb") ? String.format("%.1f", (float)heapUsage.getMax() / 1.0737418E9F) : String.format("%d", heapUsage.getMax() / 1048576L)
            );
         }
      );
      Placeholders.register(
         ResourceLocation.fromNamespaceAndPath("server", "online"), (ctx, arg) -> PlaceholderResult.value(String.valueOf(ctx.server().getPlayerList().getPlayerCount()))
      );
      Placeholders.register(
         ResourceLocation.fromNamespaceAndPath("server", "max_players"), (ctx, arg) -> PlaceholderResult.value(String.valueOf(ctx.server().getPlayerList().getMaxPlayers()))
      );
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "objective_name_top"), (ctx, arg) -> {
         String[] args = arg.split(" ");
         if (args.length >= 2) {
            ServerScoreboard scoreboard = ctx.server().getScoreboard();
            Objective scoreboardObjective = scoreboard.getObjective(args[0]);
            if (scoreboardObjective == null) {
               return PlaceholderResult.invalid("Invalid objective!");
            }

            try {
               int position = Integer.parseInt(args[1]);
               Collection<PlayerScoreEntry> playerScores = scoreboard.listPlayerScores(scoreboardObjective);
               PlayerScoreEntry entry = playerScores.toArray(new PlayerScoreEntry[0])[playerScores.size() - position];
               return PlaceholderResult.value(entry.ownerName());
            } catch (Exception e) {
               return PlaceholderResult.invalid("Invalid position!");
            }
         } else {
            return PlaceholderResult.invalid("Not enough arguments!");
         }
      });
      Placeholders.register(ResourceLocation.fromNamespaceAndPath("server", "objective_score_top"), (ctx, arg) -> {
         String[] args = arg.split(" ");
         if (args.length >= 2) {
            ServerScoreboard scoreboard = ctx.server().getScoreboard();
            Objective scoreboardObjective = scoreboard.getObjective(args[0]);
            if (scoreboardObjective == null) {
               return PlaceholderResult.invalid("Invalid objective!");
            }

            try {
               int position = Integer.parseInt(args[1]);
               Collection<PlayerScoreEntry> playerScores = scoreboard.listPlayerScores(scoreboardObjective);
               PlayerScoreEntry entry = playerScores.toArray(new PlayerScoreEntry[0])[playerScores.size() - position];
               return PlaceholderResult.value(String.valueOf(entry.value()));
            } catch (Exception e) {
               return PlaceholderResult.invalid("Invalid position!");
            }
         } else {
            return PlaceholderResult.invalid("Not enough arguments!");
         }
      });
   }
}
