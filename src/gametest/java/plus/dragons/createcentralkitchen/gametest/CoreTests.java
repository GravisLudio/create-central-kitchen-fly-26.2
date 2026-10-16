package plus.dragons.createcentralkitchen.gametest;

import com.zurrtum.create.AllItemTags;
import com.zurrtum.create.foundation.fluid.FluidHelper;
import com.zurrtum.create.content.kinetics.belt.BeltHelper;
import com.zurrtum.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.zurrtum.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import plus.dragons.createcentralkitchen.common.mechanicalarm.StatefulBlockArmInteractionPoint;
import plus.dragons.createcentralkitchen.common.packager.ShapelessUnpackingHandler;
import plus.dragons.createcentralkitchen.config.CCKConfig;

/**
 * Server-side checks of the ported core: the mod and its mixin load, the belt mixin applies, the shapeless
 * unpacking plan fills a real {@link Container} through the same path a packager uses, the stateful arm point
 * transitions block states, and the JSON config comes up with its defaults.
 */
public class CoreTests {
    @GameTest
    public void modAndMixinLoad(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("create_central_kitchen"))
            throw helper.assertionException("create_central_kitchen is not loaded");
        helper.succeed();
    }

    /** BeltHelperMixin: an item is upright on a belt when its remainder is. Dragon's breath leaves a glass bottle. */
    @GameTest
    public void beltCarriesItemsWhoseRemainderIsUpright(GameTestHelper helper) {
        // Premise: on its own, Create does not consider dragon's breath upright (not in the tag, no fluid inventory),
        // so only the mixin's remainder rule can make it so.
        var breath = new ItemStack(Items.DRAGON_BREATH);
        if (breath.is(AllItemTags.UPRIGHT_ON_BELT) || FluidHelper.hasFluidInventory(breath))
            throw helper.assertionException("test premise broken: Create already treats dragon's breath as upright");
        BeltHelper.uprightCache.clear();
        expectUpright(helper, breath, true);
        // Already upright through Create's own tag: unchanged.
        expectUpright(helper, new ItemStack(Items.GLASS_BOTTLE), true);
        // The result is cached per item, so every case below starts from an empty cache.
        // Use remainder of an item that is upright: coal that turns into a bottle when used.
        BeltHelper.uprightCache.clear();
        var coal = new ItemStack(Items.COAL);
        coal.set(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStackTemplate(Items.GLASS_BOTTLE)));
        expectUpright(helper, coal, true);
        // Controls: a remainder that is not upright, and no remainder at all.
        BeltHelper.uprightCache.clear();
        var ingot = new ItemStack(Items.IRON_INGOT);
        ingot.set(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStackTemplate(Items.DIAMOND_SWORD)));
        expectUpright(helper, ingot, false);
        BeltHelper.uprightCache.clear();
        expectUpright(helper, new ItemStack(Items.STONE), false);
        BeltHelper.uprightCache.clear();
        helper.succeed();
    }

    private static void expectUpright(GameTestHelper helper, ItemStack stack, boolean expected) {
        boolean actual = BeltHelper.isItemUpright(stack);
        if (actual != expected)
            throw helper.assertionException("isItemUpright(%s) = %s, expected %s (crafting remainder %s, use remainder %s)".formatted(
                    stack, actual, expected, stack.getItem().getCraftingRemainder(), stack.get(net.minecraft.core.component.DataComponents.USE_REMAINDER)));
    }

    /** Mushroom stew is shapeless: brown mushroom + red mushroom + bowl. */
    @GameTest
    public void unpackingFillsAShapelessRecipeIntoAContainer(GameTestHelper helper) {
        var container = new SimpleContainer(9);
        var handler = new CraftingHandler(container);
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        BlockState state = Blocks.AIR.defaultBlockState();

        // Simulation must not touch the container.
        if (!handler.unpack(level, pos, state, Direction.UP, stew(), null, true))
            throw helper.assertionException("simulated unpack refused a valid stew");
        if (!container.isEmpty())
            throw helper.assertionException("simulated unpack changed the container: " + describe(container));

        if (!handler.unpack(level, pos, state, Direction.UP, stew(), null, false))
            throw helper.assertionException("unpack refused a valid stew");
        expectContents(helper, container, 1, "after the first serving");

        // A second package of the same recipe adds one more serving to the same layout.
        if (!handler.unpack(level, pos, state, Direction.UP, stew(), null, false))
            throw helper.assertionException("unpack refused a second serving");
        expectContents(helper, container, 2, "after the second serving");

        // Items that complete no recipe are refused and leave everything as it was.
        String before = describe(container);
        if (handler.unpack(level, pos, state, Direction.UP, new ArrayList<>(List.of(new ItemStack(Items.STONE))), null, false))
            throw helper.assertionException("unpack accepted a lone stone");
        if (!before.equals(describe(container)))
            throw helper.assertionException("a refused unpack changed the container: " + before + " -> " + describe(container));
        helper.succeed();
    }

    private static ArrayList<ItemStack> stew() {
        return new ArrayList<>(List.of(new ItemStack(Items.BROWN_MUSHROOM), new ItemStack(Items.RED_MUSHROOM), new ItemStack(Items.BOWL)));
    }

    private static void expectContents(GameTestHelper helper, Container container, int servings, String when) {
        int filled = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);
            if (stack.isEmpty())
                continue;
            filled++;
            if (stack.getCount() != servings || !(stack.is(Items.BROWN_MUSHROOM) || stack.is(Items.RED_MUSHROOM) || stack.is(Items.BOWL)))
                throw helper.assertionException("unexpected stack %s %s: %s".formatted(stack, when, describe(container)));
        }
        if (filled != 3)
            throw helper.assertionException("expected 3 filled slots %s, found %s: %s".formatted(when, filled, describe(container)));
    }

    private static String describe(Container container) {
        var text = new StringBuilder();
        for (int slot = 0; slot < container.getContainerSize(); slot++)
            if (!container.getItem(slot).isEmpty())
                text.append(slot).append('=').append(container.getItem(slot)).append(' ');
        return text.toString().trim();
    }

    /** Sea pickles: inserting one adds a pickle (up to 4), extracting takes one, the last one removes the block. */
    @GameTest
    public void statefulArmPointTransitionsBlockStates(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 1, 1);
        // Not waterlogged: removing a waterlogged block leaves its water behind.
        helper.setBlock(local, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 1).setValue(SeaPickleBlock.WATERLOGGED, false));
        BlockPos pos = helper.absolutePos(local);
        var level = helper.getLevel();
        var point = new PicklePoint(level, pos, level.getBlockState(pos));

        var offered = new ItemStack(Items.SEA_PICKLE, 3);
        var remainder = point.insert(null, offered, true);
        if (remainder.getCount() != 2 || pickles(helper, local) != 1)
            throw helper.assertionException("simulated insert: remainder %s, pickles %s".formatted(remainder.getCount(), pickles(helper, local)));
        remainder = point.insert(null, offered, false);
        if (remainder.getCount() != 2 || pickles(helper, local) != 2)
            throw helper.assertionException("insert: remainder %s, pickles %s".formatted(remainder.getCount(), pickles(helper, local)));
        if (!point.insert(null, new ItemStack(Items.STONE), false).is(Items.STONE))
            throw helper.assertionException("insert accepted a stone");

        if (!point.extract(null, 0, 0, false).isEmpty())
            throw helper.assertionException("extract with amount 0 returned items");
        if (!point.extract(null, 0, 1, true).is(Items.SEA_PICKLE) || pickles(helper, local) != 2)
            throw helper.assertionException("simulated extract changed the block or returned nothing");
        if (!point.extract(null, 0, 1, false).is(Items.SEA_PICKLE) || pickles(helper, local) != 1)
            throw helper.assertionException("extract: pickles " + pickles(helper, local));
        if (!point.extract(null, 0, 1, false).is(Items.SEA_PICKLE) || !helper.getBlockState(local).isAir())
            throw helper.assertionException("the last extraction should remove the block, found " + helper.getBlockState(local));
        helper.succeed();
    }

    private static int pickles(GameTestHelper helper, BlockPos local) {
        return helper.getBlockState(local).getValue(SeaPickleBlock.PICKLES);
    }

    @GameTest
    public void configComesUpWithDefaults(GameTestHelper helper) {
        if (CCKConfig.common() == null || !CCKConfig.recipes().convertCuttingBoardRecipesToSawingRecipes.get())
            throw helper.assertionException("common config missing or default changed");
        CCKConfig.registerClient();
        if (!CCKConfig.client().renderDeployerUsingItemWithCustomTransform.get())
            throw helper.assertionException("client config default changed");
        helper.succeed();
    }

    /** A packager handler over a plain container and the crafting recipe type. */
    private static final class CraftingHandler extends ShapelessUnpackingHandler {
        private final Container container;

        CraftingHandler(Container container) {
            super(0, 9);
            this.container = container;
        }

        @Override
        protected @Nullable Container getInventory(Level level, BlockPos pos, Direction side) {
            return container;
        }

        @Override
        protected RecipeType<?> getRecipeType() {
            return RecipeType.CRAFTING;
        }
    }

    /** An arm target that turns sea pickles into more or fewer pickles. */
    private static final class PicklePoint extends StatefulBlockArmInteractionPoint {
        private static final ArmInteractionPointType TYPE = new ArmInteractionPointType() {
            @Override
            public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
                return state.is(Blocks.SEA_PICKLE);
            }

            @Override
            public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
                return new PicklePoint(level, pos, state);
            }
        };

        PicklePoint(Level level, BlockPos pos, BlockState state) {
            super(TYPE, level, pos, state);
        }

        @Override
        protected @Nullable Insertion planInsertion(BlockState state, ItemStack stack) {
            int pickles = state.getValue(SeaPickleBlock.PICKLES);
            if (!stack.is(Items.SEA_PICKLE) || pickles >= 4)
                return null;
            return insertion(stack.copyWithCount(stack.getCount() - 1), state.setValue(SeaPickleBlock.PICKLES, pickles + 1), SoundEvents.SLIME_BLOCK_PLACE);
        }

        @Override
        protected @Nullable Extraction planExtraction(BlockState state) {
            int pickles = state.getValue(SeaPickleBlock.PICKLES);
            return extraction(new ItemStack(Items.SEA_PICKLE), pickles > 1 ? state.setValue(SeaPickleBlock.PICKLES, pickles - 1) : null, SoundEvents.SLIME_BLOCK_BREAK);
        }
    }
}
