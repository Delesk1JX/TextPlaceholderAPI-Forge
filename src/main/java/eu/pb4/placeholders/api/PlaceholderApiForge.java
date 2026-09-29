package eu.pb4.placeholders.api;

import eu.pb4.placeholders.impl.GeneralUtils;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint.
 *
 * <p>Placeholder API is a library: the whole registry is populated from a static initializer on
 * {@link Placeholders}, and every {@code PlayerPlaceholders#register()} and friends call is chained
 * off that class. So there is nothing to wire up here beyond forcing the class to load early, which
 * makes the built-in {@code %server%}, {@code %player%} and {@code %world%} placeholders available
 * to other mods regardless of the order Forge happens to construct mod instances in.
 */
@Mod(PlaceholderApiForge.MOD_ID)
public final class PlaceholderApiForge {
    public static final String MOD_ID = "placeholderapi";

    public PlaceholderApiForge() {
        // Touching the registry here guarantees registration happens during mod construction.
        int registered = Placeholders.getPlaceholders().size();

        GeneralUtils.LOGGER.info("Placeholder API ready, {} built-in placeholders registered.", registered);
    }
}
