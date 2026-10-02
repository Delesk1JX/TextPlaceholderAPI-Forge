package eu.pb4.placeholders.impl;

import java.util.Optional;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * Thin adapter over the Forge mod list.
 *
 * <p>Upstream Placeholder API is a Fabric library and reaches for {@code FabricLoader} in a few
 * places, mostly to answer "which mods are installed" style placeholders. Forge exposes the same
 * information through {@link ModList}, so every call site is routed through here instead of
 * scattering loader specifics across the API.
 */
public final class ForgePlatform {
    private ForgePlatform() {
    }

    /** Mirrors {@code FabricLoader#getInstance().isDevelopmentEnvironment()}. */
    public static boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.isProduction();
    }

    /**
     * Looks up a loaded mod by its id. Returns empty when the id is unknown, and also when the
     * mod list is not available yet - placeholders can in principle be evaluated very early.
     */
    public static Optional<IModInfo> getModInfo(String modId) {
        try {
            return ModList.get().getModContainerById(modId).map(ModContainer::getModInfo);
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    public static int getModCount() {
        try {
            return ModList.get().getMods().size();
        } catch (IllegalStateException e) {
            return 0;
        }
    }
}
