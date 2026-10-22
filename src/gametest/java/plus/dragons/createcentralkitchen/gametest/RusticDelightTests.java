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

package plus.dragons.createcentralkitchen.gametest;

import com.phantomwing.rusticdelight.block.ModBlocks;
import com.phantomwing.rusticdelight.block.custom.PancakeBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import plus.dragons.createcentralkitchen.integration.rusticdelight.mechanicalArm.PancakeArmInteractionPoint;
import plus.dragons.createcentralkitchen.integration.rusticdelight.registry.RusticDelightArmInteractionPointTypes;

/**
 * Checks of the Rustic Delight integration against the real Rustic Delight pancake blocks in a headless server.
 * Adapted from the NeoForge game test of the original mod (same cases, Fabric API instead of NeoForge's).
 */
public class RusticDelightTests {
    @GameTest
    public void integrationStartedAndRegisteredThePancakeArmPoint(GameTestHelper helper) {
        check(FabricLoader.getInstance().isModLoaded("rusticdelight"), "Rustic Delight is not loaded");
        check(RusticDelightArmInteractionPointTypes.PANCAKE != null, "the pancake arm point type was not registered");
        helper.succeed();
    }

    @GameTest
    public void pancakeTransactionsHonorPublicEncoding(GameTestHelper helper) {
        var local = new BlockPos(1, 1, 1);
        var pos = helper.absolutePos(local);
        var level = helper.getLevel();
        var flavors = pancakeBlocks();
        for (int index = 0; index < flavors.size(); index++) {
            var pancakes = flavors.get(index);
            var six = PancakeArmInteractionPoint.stateForCount(
                    pancakes.defaultBlockState().setValue(PancakeBlock.FACING, Direction.EAST), 6);
            expect(six.getValue(PancakeBlock.SERVINGS), 0, "Six pancakes must use the upstream encoded value");
            expect(PancakeArmInteractionPoint.stateForCount(six, 1).getValue(PancakeBlock.SERVINGS), 5, "One-pancake encoding changed");
            expect(PancakeArmInteractionPoint.stateForCount(six, 7).getValue(PancakeBlock.SERVINGS), 6, "Seven-pancake encoding changed");
            expect(PancakeArmInteractionPoint.stateForCount(six, 12).getValue(PancakeBlock.SERVINGS), 11, "Twelve-pancake encoding changed");

            helper.setBlock(local, six);
            var point = new PancakeArmInteractionPoint.Type().createPoint(level, pos, six);
            check(point != null, "Every PancakeBlock must create an arm interaction point");
            var serving = pancakes.getServingItem();

            var simulatedExtract = point.extract(null, 0, 1, true);
            check(ItemStack.isSameItemSameComponents(simulatedExtract, serving), "Simulation returned the wrong pancake flavor");
            assertCount(level.getBlockState(pos), 6, "Extraction simulation changed the stack");
            check(point.extract(null, 0, 0, false).isEmpty(), "Zero-sized extraction must be rejected");
            check(ItemStack.isSameItemSameComponents(point.extract(null, 0, 1, false), serving), "Extraction returned the wrong flavor");
            assertCount(level.getBlockState(pos), 5, "Extraction did not cross the lower encoding branch");
            expect(level.getBlockState(pos).getValue(PancakeBlock.FACING), Direction.EAST, "Extraction changed facing");

            var seven = PancakeArmInteractionPoint.stateForCount(six, 7);
            helper.setBlock(local, seven);
            var three = serving.copyWithCount(3);
            check(point.insert(null, three, true).isEmpty(), "A simulated legal insertion should accept all three pancakes");
            assertCount(level.getBlockState(pos), 7, "Insertion simulation changed the stack");
            check(point.insert(null, three, false).isEmpty(), "A legal insertion should accept all three pancakes");
            assertCount(level.getBlockState(pos), 10, "Insertion did not use the public non-linear encoding");

            var remainder = point.insert(null, serving.copyWithCount(7), false);
            expect(remainder.getCount(), 5, "Insertion did not stop at twelve pancakes");
            assertCount(level.getBlockState(pos), 12, "Insertion exceeded or missed the twelve-pancake cap");
            check(ItemStack.matches(point.insert(null, serving, false), serving), "A full stack must reject excess pancakes");

            var wrongFlavor = flavors.get((index + 1) % flavors.size()).getServingItem();
            helper.setBlock(local, seven);
            check(ItemStack.matches(point.insert(null, wrongFlavor, false), wrongFlavor), "A pancake stack accepted the wrong flavor");
            assertCount(level.getBlockState(pos), 7, "Wrong-flavor insertion changed the block");

            helper.setBlock(local, PancakeArmInteractionPoint.stateForCount(six, 1));
            check(ItemStack.isSameItemSameComponents(point.extract(null, 0, 1, false), serving), "The final pancake was not returned");
            check(level.getBlockState(pos).isAir(), "The final pancake must remove the block without loot");
        }
        helper.succeed();
    }

    private static void assertCount(BlockState state, int expected, String message) {
        expect(PancakeBlock.getPancakesPresent(state), expected, message);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static void expect(Object actual, Object expected, String message) {
        if (!expected.equals(actual))
            throw new AssertionError("%s (expected %s, got %s)".formatted(message, expected, actual));
    }

    private static List<PancakeBlock> pancakeBlocks() {
        return List.of(
                (PancakeBlock) ModBlocks.PANCAKES,
                (PancakeBlock) ModBlocks.HONEY_PANCAKES,
                (PancakeBlock) ModBlocks.CHOCOLATE_PANCAKES,
                (PancakeBlock) ModBlocks.CHERRY_BLOSSOM_PANCAKES,
                (PancakeBlock) ModBlocks.VEGETABLE_PANCAKES,
                (PancakeBlock) ModBlocks.PUMPKIN_PANCAKES,
                (PancakeBlock) ModBlocks.COFFEE_PANCAKES);
    }
}
