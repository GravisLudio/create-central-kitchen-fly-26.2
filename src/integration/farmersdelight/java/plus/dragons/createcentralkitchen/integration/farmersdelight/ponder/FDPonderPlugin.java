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

package plus.dragons.createcentralkitchen.integration.farmersdelight.ponder;

import com.zurrtum.create.client.infrastructure.ponder.AllCreatePonderTags;
import java.util.List;
import com.zurrtum.create.client.ponder.api.registration.PonderSceneRegistrationHelper;
import com.zurrtum.create.client.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import plus.dragons.createcentralkitchen.client.ponder.CCKPonderPlugin;
import plus.dragons.createcentralkitchen.integration.ModIntegration;
import vectorwing.farmersdelight.common.block.AbstractStoveBlock;
import vectorwing.farmersdelight.common.block.FeastBlock;
import vectorwing.farmersdelight.common.block.PieBlock;

public class FDPonderPlugin {
    private static final Identifier CUTTING_BOARD = ModIntegration.FARMERSDELIGHT.asResource("cutting_board");
    private static final Identifier COOKING_POT = ModIntegration.FARMERSDELIGHT.asResource("cooking_pot");
    private static final Identifier FIERY_COOKING_POT = ModIntegration.TWILIGHTDELIGHT.asResource("fiery_cooking_pot");
    private static final Identifier POTTERY_COOKING_POT = ModIntegration.TRAILANDTALESDELIGHT.asResource("pottery_cooking_pot");
    private static final Identifier COPPER_POT = ModIntegration.MINERSDELIGHT.asResource("copper_pot");
    private static final Identifier SKILLET = ModIntegration.FARMERSDELIGHT.asResource("skillet");
    private static final Identifier MONSTER_POT = ModIntegration.DUNGEONSDELIGHT.asResource("monster_pot");

    public static void register() {
        CCKPonderPlugin.SCENES.add(FDPonderPlugin::registerScenes);
        CCKPonderPlugin.TAGS.add(FDPonderPlugin::registerTags);
    }

    private static void registerScenes(PonderSceneRegistrationHelper<Identifier> helper) {
        helper.forComponents(CUTTING_BOARD)
                .addStoryBoard("farmersdelight/cutting_board", FDPonderScenes::cuttingBoard,
                        AllCreatePonderTags.ARM_TARGETS);
        helper.forComponents(COOKING_POT, FIERY_COOKING_POT, POTTERY_COOKING_POT, COPPER_POT, MONSTER_POT)
                .addStoryBoard("farmersdelight/cooking_pot", FDPonderScenes::cookingPot,
                        AllCreatePonderTags.ARM_TARGETS,
                        AllCreatePonderTags.HIGH_LOGISTICS)
                .addStoryBoard("farmersdelight/heat_source", FDPonderScenes::heatSource);
        helper.forComponents(SKILLET)
                .addStoryBoard("farmersdelight/stove_and_skillet", FDPonderScenes::stoveAndSkillet,
                        AllCreatePonderTags.ARM_TARGETS)
                .addStoryBoard("farmersdelight/heat_source", FDPonderScenes::heatSource);
        var stoves = abstractStoves();
        if (!stoves.isEmpty()) {
            helper.forComponents(stoves)
                    .addStoryBoard("farmersdelight/stove_and_skillet", FDPonderScenes::stoveAndSkillet,
                            AllCreatePonderTags.ARM_TARGETS);
        }
        var portionableFoods = portionableFoods();
        if (!portionableFoods.isEmpty()) {
            helper.forComponents(portionableFoods)
                    .addStoryBoard("farmersdelight/portionable_foods", FDPonderScenes::portionableFoods,
                            AllCreatePonderTags.ARM_TARGETS);
        }
    }

    private static void registerTags(PonderTagRegistrationHelper<Identifier> helper) {
        var armTargets = helper.addToTag(AllCreatePonderTags.ARM_TARGETS)
                .add(CUTTING_BOARD)
                .add(COOKING_POT)
                .add(FIERY_COOKING_POT)
                .add(POTTERY_COOKING_POT)
                .add(COPPER_POT)
                .add(SKILLET)
                .add(MONSTER_POT);
        abstractStoves().forEach(armTargets::add);
        // Dragons Plus folds these into one carousel card; without its screen each feast and pie is listed on its own.
        portionableFoods().forEach(armTargets::add);
    }

    private static List<Identifier> abstractStoves() {
        return BuiltInRegistries.BLOCK.entrySet()
                .stream()
                .filter(entry -> entry.getValue() instanceof AbstractStoveBlock)
                .map(entry -> entry.getKey().identifier())
                .toList();
    }

    private static List<Identifier> portionableFoods() {
        return BuiltInRegistries.BLOCK.entrySet()
                .stream()
                .filter(entry -> entry.getValue() instanceof FeastBlock || entry.getValue() instanceof PieBlock)
                .map(entry -> entry.getKey().identifier())
                .toList();
    }

}
