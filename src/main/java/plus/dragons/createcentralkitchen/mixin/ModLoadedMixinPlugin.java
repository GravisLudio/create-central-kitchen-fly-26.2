/*
 * Copyright (C) 2025  DragonsPlus
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package plus.dragons.createcentralkitchen.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import plus.dragons.createcentralkitchen.integration.ModIntegration.Mods;

/**
 * Mixin config plugin shared by the per-integration configs ({@code create_central_kitchen.<integration>.mixins.json}).
 * Stands in for the conditional-mixin library of the NeoForge build: the integration is the last segment of the mixin
 * package, and its mixins apply only when that mod is loaded.
 *
 * <p>This runs while Mixin is still preparing every config, so it must not load any Minecraft class: a class loaded
 * that early can no longer be the target of another mod's mixin. The first version asked {@code ModIntegration} (an
 * enum whose methods mention {@code Identifier} and Fabric's {@code ResourceCondition}), which loaded
 * {@code net.minecraft.resources.Identifier} and made Iris crash at startup ("mixins.iris.json:texture.MixinIdentifier
 * ... was loaded too early"). Only the string constants of {@code Mods} (inlined by the compiler) and
 * {@code FabricLoader} are used here.</p>
 */
public class ModLoadedMixinPlugin implements IMixinConfigPlugin {
    /** Last segment of the mixin package -> mod id of the integrated mod. */
    private static final Map<String, String> MOD_IDS = Map.ofEntries(
            Map.entry("farmersdelight", Mods.FARMERSDELIGHT),
            Map.entry("brewinandchewin", Mods.BREWINANDCHEWIN),
            Map.entry("mynethersdelight", Mods.MYNETHERSDELIGHT),
            Map.entry("endsdelight", Mods.ENDSDELIGHT),
            Map.entry("twilightdelight", Mods.TWILIGHTDELIGHT),
            Map.entry("trailandtalesdelight", Mods.TRAILANDTALESDELIGHT),
            Map.entry("minersdelight", Mods.MINERSDELIGHT),
            Map.entry("extradelight", Mods.EXTRADELIGHT),
            Map.entry("dungeonsdelight", Mods.DUNGEONSDELIGHT),
            Map.entry("culturaldelights", Mods.CULTURALDELIGHTS),
            Map.entry("rusticdelight", Mods.RUSTICDELIGHT),
            Map.entry("festivedelight", Mods.FESTIVEDELIGHT),
            Map.entry("hearthandharvest", Mods.HEARTHANDHARVEST));

    private boolean enabled;

    @Override
    public void onLoad(String mixinPackage) {
        String name = mixinPackage.substring(mixinPackage.lastIndexOf('.') + 1);
        String modId = MOD_IDS.get(name);
        if (modId == null)
            throw new IllegalStateException("Mixin package " + mixinPackage + " does not name a known integration");
        enabled = FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return enabled;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
