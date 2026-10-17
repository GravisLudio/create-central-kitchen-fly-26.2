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

package plus.dragons.createcentralkitchen.integration.farmersdelight.registry;

import com.zurrtum.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import plus.dragons.createcentralkitchen.common.registry.CCKArmInteractionPointTypes;
import plus.dragons.createcentralkitchen.integration.farmersdelight.mechanicalArm.CookingPotArmInteractionPoint;
import plus.dragons.createcentralkitchen.integration.farmersdelight.mechanicalArm.CuttingBoardArmInteractionPoint;
import plus.dragons.createcentralkitchen.integration.farmersdelight.mechanicalArm.PortionableFoodArmInteractionPoint;
import plus.dragons.createcentralkitchen.integration.farmersdelight.mechanicalArm.SkilletArmInteractionPoint;
import plus.dragons.createcentralkitchen.integration.farmersdelight.mechanicalArm.StoveArmInteractionPoint;

public class FDArmInteractionPointTypes {
    //spotless:off
    public static ArmInteractionPointType
            COOKING_POT,
            CUTTING_BOARD,
            PORTIONABLE_FOOD,
            SKILLET,
            STOVE;
    //spotless:on

    public static void register() {
        COOKING_POT = CCKArmInteractionPointTypes.register("cooking_pot", new CookingPotArmInteractionPoint.Type());
        CUTTING_BOARD = CCKArmInteractionPointTypes.register("cutting_board", new CuttingBoardArmInteractionPoint.Type());
        PORTIONABLE_FOOD = CCKArmInteractionPointTypes.register("portionable_food", new PortionableFoodArmInteractionPoint.Type());
        SKILLET = CCKArmInteractionPointTypes.register("skillet", new SkilletArmInteractionPoint.Type());
        STOVE = CCKArmInteractionPointTypes.register("stove", new StoveArmInteractionPoint.Type());
    }
}
