package plus.dragons.createcentralkitchen.gametest;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

/**
 * Test-only JEI plugin: it only keeps JEI's runtime so that {@link CentralKitchenClientTests} can ask which categories
 * and recipes JEI really loaded and open its recipe screen. Declared as {@code jei_mod_plugin} in the test mod's
 * fabric.mod.json; JEI is the only one that ever loads this class.
 */
public class ClientTestJeiPlugin implements IModPlugin {
    public static volatile IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("create_central_kitchen_gametest", "jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }
}
