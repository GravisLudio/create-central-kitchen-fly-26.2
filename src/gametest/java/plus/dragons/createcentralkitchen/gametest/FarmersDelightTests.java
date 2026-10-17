package plus.dragons.createcentralkitchen.gametest;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.AllRecipeTypes;
import com.zurrtum.create.api.packager.unpacking.UnpackingHandler;
import com.zurrtum.create.content.kinetics.deployer.DeployerBlockEntity;
import com.zurrtum.create.content.kinetics.saw.SawBlockEntity;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import plus.dragons.createcentralkitchen.integration.farmersdelight.mechanicalArm.CookingPotArmInteractionPoint;
import plus.dragons.createcentralkitchen.integration.farmersdelight.mechanicalArm.CuttingBoardArmInteractionPoint;
import plus.dragons.createcentralkitchen.integration.farmersdelight.mechanicalArm.PortionableFoodArmInteractionPoint;
import plus.dragons.createcentralkitchen.integration.farmersdelight.mechanicalArm.SkilletArmInteractionPoint;
import plus.dragons.createcentralkitchen.integration.farmersdelight.mechanicalArm.StoveArmInteractionPoint;
import plus.dragons.createcentralkitchen.integration.farmersdelight.packager.CookingPotUnpackingHandler;
import plus.dragons.createcentralkitchen.integration.farmersdelight.recipe.CuttingBoardDeployerRecipe;
import plus.dragons.createcentralkitchen.integration.farmersdelight.recipe.CuttingBoardRecipeConverters;
import plus.dragons.createcentralkitchen.integration.farmersdelight.registry.FDArmInteractionPointTypes;
import vectorwing.farmersdelight.common.block.FeastBlock;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;
import vectorwing.farmersdelight.common.block.entity.StoveBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.registry.ModBlocks;
import vectorwing.farmersdelight.common.registry.ModItems;
import vectorwing.farmersdelight.common.tag.ModTags;

/**
 * Checks of the Farmer's Delight integration against the real Farmer's Delight blocks and recipes running in a
 * headless server: the packager fills a cooking pot, Cutting Board recipes show up for the saw and the deployer, the
 * arm interaction points move food, and the tags Create and Farmer's Delight share.
 */
public class FarmersDelightTests {
    @GameTest
    public void packagerFillsACookingPotThroughTheRegistry(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 1, 1);
        helper.setBlock(local, ModBlocks.COOKING_POT.get());
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(local);
        BlockState state = level.getBlockState(pos);

        // Create finds the handler by block, lazily: this is the registration path a packager uses.
        if (!(UnpackingHandler.REGISTRY.get(state) instanceof CookingPotUnpackingHandler handler))
            throw helper.assertionException("no cooking pot unpacking handler registered for " + state);
        if (UnpackingHandler.REGISTRY.get(Blocks.STONE.defaultBlockState()) instanceof CookingPotUnpackingHandler)
            throw helper.assertionException("the cooking pot handler answers for stone");

        // A real recipe from the game's data: 2 to 4 inputs, each a single item.
        CookingPotRecipe recipe = level.recipeAccess().getRecipes().stream()
                .map(RecipeHolder::value)
                .filter(CookingPotRecipe.class::isInstance).map(CookingPotRecipe.class::cast)
                .filter(r -> r.input().size() >= 2 && r.input().size() <= 4
                        && r.input().stream().allMatch(ingredient -> ingredient.items().count() == 1))
                .findFirst().orElseThrow(() -> helper.assertionException("no cooking pot recipe with plain inputs"));
        var items = new ArrayList<ItemStack>();
        for (var ingredient : recipe.input())
            items.add(new ItemStack(ingredient.items().findFirst().orElseThrow().value()));
        String wanted = describe(items);

        var pot = helper.getBlockEntity(local, CookingPotBlockEntity.class);
        if (!handler.unpack(level, pos, state, Direction.UP, copy(items), null, true))
            throw helper.assertionException("simulated unpack refused " + wanted);
        if (!potInputs(pot).isEmpty())
            throw helper.assertionException("simulated unpack changed the pot: " + describe(potInputs(pot)));

        if (!handler.unpack(level, pos, state, Direction.UP, copy(items), null, false))
            throw helper.assertionException("unpack refused " + wanted);
        if (!sameItems(items, potInputs(pot), 1))
            throw helper.assertionException("pot holds [%s], expected [%s]".formatted(describe(potInputs(pot)), wanted));

        // A second package of the same recipe stacks onto the same layout.
        if (!handler.unpack(level, pos, state, Direction.UP, copy(items), null, false))
            throw helper.assertionException("unpack refused a second serving of " + wanted);
        if (!sameItems(items, potInputs(pot), 2))
            throw helper.assertionException("after two servings the pot holds [%s]".formatted(describe(potInputs(pot))));
        helper.succeed();
    }

    private static List<ItemStack> potInputs(CookingPotBlockEntity pot) {
        var result = new ArrayList<ItemStack>();
        for (int slot = 0; slot < 6; slot++)
            if (!pot.getInventory().getStackInSlot(slot).isEmpty())
                result.add(pot.getInventory().getStackInSlot(slot));
        return result;
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        return new ArrayList<>(stacks.stream().map(ItemStack::copy).toList());
    }

    /** Same multiset of items, every stack of the given size. */
    private static boolean sameItems(List<ItemStack> expected, List<ItemStack> actual, int count) {
        var remaining = new ArrayList<>(expected.stream().map(ItemStack::getItem).toList());
        for (var stack : actual) {
            if (stack.getCount() != count || !remaining.remove(stack.getItem()))
                return false;
        }
        return remaining.isEmpty();
    }

    private static String describe(List<ItemStack> stacks) {
        return String.join(", ", stacks.stream().map(ItemStack::toString).toList());
    }

    /** Create's saw looks recipes up through RecipeFinder; the mixin must add the Cutting Board ones. */
    @GameTest
    public void sawCutsWhatTheCuttingBoardCuts(GameTestHelper helper) throws ReflectiveOperationException {
        BlockPos local = new BlockPos(1, 1, 1);
        helper.setBlock(local, AllBlocks.MECHANICAL_SAW);
        var saw = helper.getBlockEntity(local, SawBlockEntity.class);
        var index = CuttingBoardRecipeConverters.index(helper.getLevel().recipeAccess());
        if (index.sawing().isEmpty())
            throw helper.assertionException("no Cutting Board recipe can be sawn (cutting " + index.cutting().size() + ")");

        var holder = index.sawing().stream().filter(h -> h.value().ingredient().items().count() == 1).findFirst()
                .orElseThrow(() -> helper.assertionException("no sawing recipe with a plain input"));
        ItemStack input = new ItemStack(holder.value().ingredient().items().findFirst().orElseThrow().value());

        Method updateRecipe = SawBlockEntity.class.getDeclaredMethod("updateRecipe", SingleRecipeInput.class, boolean.class);
        updateRecipe.setAccessible(true);
        Object found = updateRecipe.invoke(saw, new SingleRecipeInput(input), false);
        if (found == null)
            throw helper.assertionException("the saw found no recipe for " + input + " although " + holder.id().identifier() + " exists");
        Recipe<?> recipe = (Recipe<?>) found.getClass().getMethod("getFirst").invoke(found);
        if (recipe.getType() != AllRecipeTypes.CUTTING)
            throw helper.assertionException("the saw picked a " + recipe.getType() + " recipe for " + input);

        // Nothing that no recipe cuts.
        Object none = updateRecipe.invoke(saw, new SingleRecipeInput(new ItemStack(Items.BEDROCK)), false);
        if (none != null)
            throw helper.assertionException("the saw found a recipe for bedrock");
        helper.succeed();
    }

    /** Create-Fly has no deployer recipe event: the mixin offers the Cutting Board recipes when Create finds none. */
    @GameTest(maxTicks = 100)
    public void deployerCutsWithAKnife(GameTestHelper helper) {
        BlockPos local = new BlockPos(1, 1, 1);
        helper.setBlock(local, AllBlocks.DEPLOYER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));
        var deployer = helper.getBlockEntity(local, DeployerBlockEntity.class);
        var knife = new ItemStack(ModItems.IRON_KNIFE.get());
        var index = CuttingBoardRecipeConverters.index(helper.getLevel().recipeAccess());
        var board = index.cutting().stream()
                .filter(h -> h.value().getInput().items().count() == 1 && h.value().getTool().test(knife)
                        && AllRecipeTypes.CAN_BE_AUTOMATED.test(h))
                .findFirst().orElseThrow(() -> helper.assertionException("no Cutting Board recipe a knife can do"));
        ItemStack input = new ItemStack(board.value().getInput().items().findFirst().orElseThrow().value());

        // The deployer creates its fake player on its first tick.
        helper.succeedWhen(() -> {
            if (deployer.getPlayer() == null)
                throw helper.assertionException("the deployer has no player yet");
            deployer.getPlayer().cast().setItemInHand(InteractionHand.MAIN_HAND, knife.copy());
            var recipe = deployer.getRecipe(input);
            if (!(recipe instanceof CuttingBoardDeployerRecipe cutting))
                throw helper.assertionException("deployer with a knife found %s for %s".formatted(recipe, input));
            if (cutting.shouldKeepHeldTool(knife))
                throw helper.assertionException("a damageable knife must not be kept for free");
            if (deployer.getRecipe(new ItemStack(Items.BEDROCK)) != null)
                throw helper.assertionException("the deployer found a recipe for bedrock");
        });
    }

    @GameTest
    public void armPointsMoveFood(GameTestHelper helper) {
        var level = helper.getLevel();

        // Portionable food: take servings one by one, refill the last one with a whole feast.
        BlockPos feastLocal = new BlockPos(1, 1, 1);
        helper.setBlock(feastLocal.below(), Blocks.STONE);
        var feast = (FeastBlock) ModBlocks.ROAST_CHICKEN_BLOCK.get();
        var servings = feast.getServingsProperty();
        helper.setBlock(feastLocal, feast.defaultBlockState().setValue(servings, 2));
        BlockPos feastPos = helper.absolutePos(feastLocal);
        var food = new PortionableFoodArmInteractionPoint(FDArmInteractionPointTypes.PORTIONABLE_FOOD, level, feastPos, level.getBlockState(feastPos));
        if (food.extract(null, 0, 1, true).isEmpty() || helper.getBlockState(feastLocal).getValue(servings) != 2)
            throw helper.assertionException("simulated extraction from the feast failed or changed it");
        if (food.extract(null, 0, 1, false).isEmpty() || helper.getBlockState(feastLocal).getValue(servings) != 1)
            throw helper.assertionException("extraction should leave one serving, found " + helper.getBlockState(feastLocal));
        if (!food.extract(null, 0, 1, false).isEmpty())
            throw helper.assertionException("the last serving must stay");
        food.insert(null, new ItemStack(feast.asItem()), false);
        if (helper.getBlockState(feastLocal).getValue(servings) != servings.getPossibleValues().stream().max(Integer::compare).orElseThrow())
            throw helper.assertionException("inserting a whole feast should refill it, found " + helper.getBlockState(feastLocal));

        // Cooking pot: container in, finished meal out.
        BlockPos potLocal = new BlockPos(3, 1, 1);
        helper.setBlock(potLocal, ModBlocks.COOKING_POT.get());
        BlockPos potPos = helper.absolutePos(potLocal);
        var pot = helper.getBlockEntity(potLocal, CookingPotBlockEntity.class);
        var potPoint = new CookingPotArmInteractionPoint(FDArmInteractionPointTypes.COOKING_POT, level, potPos, level.getBlockState(potPos));
        var bowl = potPoint.insert(null, new ItemStack(Items.BOWL, 3), false);
        if (!pot.getInventory().getStackInSlot(7).is(Items.BOWL))
            throw helper.assertionException("the bowl did not reach the container slot (remainder %s)".formatted(bowl));
        pot.getInventory().setStackInSlot(8, new ItemStack(Items.MUSHROOM_STEW, 2));
        var taken = potPoint.extract(null, 0, 8, false);
        if (!taken.is(Items.MUSHROOM_STEW) || taken.getCount() != 2 || !pot.getInventory().getStackInSlot(8).isEmpty())
            throw helper.assertionException("the finished meal was not taken: " + taken);

        // Cutting board: only items with a cutting recipe go on.
        BlockPos boardLocal = new BlockPos(5, 1, 1);
        helper.setBlock(boardLocal, ModBlocks.CUTTING_BOARD.get());
        BlockPos boardPos = helper.absolutePos(boardLocal);
        var board = helper.getBlockEntity(boardLocal, CuttingBoardBlockEntity.class);
        var boardPoint = new CuttingBoardArmInteractionPoint(FDArmInteractionPointTypes.CUTTING_BOARD, level, boardPos, level.getBlockState(boardPos));
        var cuttable = CuttingBoardRecipeConverters.index(level.recipeAccess()).cutting().stream()
                .filter(h -> h.value().getInput().items().count() == 1).findFirst()
                .orElseThrow(() -> helper.assertionException("no Cutting Board recipe in the game"));
        var onBoard = new ItemStack(cuttable.value().getInput().items().findFirst().orElseThrow().value());
        if (!boardPoint.insert(null, new ItemStack(Items.BEDROCK), false).is(Items.BEDROCK) || !board.isEmpty())
            throw helper.assertionException("the board accepted bedrock");
        boardPoint.insert(null, onBoard.copy(), false);
        if (!board.getStoredItem().is(onBoard.getItem()))
            throw helper.assertionException("the board holds %s, expected %s".formatted(board.getStoredItem(), onBoard));

        // Stove and skillet: raw food in.
        BlockPos stoveLocal = new BlockPos(7, 1, 1);
        helper.setBlock(stoveLocal, ModBlocks.STOVE.get());
        BlockPos stovePos = helper.absolutePos(stoveLocal);
        var stove = helper.getBlockEntity(stoveLocal, StoveBlockEntity.class);
        var stovePoint = new StoveArmInteractionPoint(FDArmInteractionPointTypes.STOVE, level, stovePos, level.getBlockState(stovePos));
        var left = stovePoint.insert(null, new ItemStack(Items.BEEF, 2), true);
        if (left.getCount() != 1 || stove.getItems().getStackInSlot(0).getCount() != 0)
            throw helper.assertionException("simulated stove insertion: remainder %s".formatted(left));
        stovePoint.insert(null, new ItemStack(Items.BEEF, 2), false);
        boolean cooking = false;
        for (int slot = 0; slot < stove.getItems().getSlotCount(); slot++)
            cooking |= stove.getItems().getStackInSlot(slot).is(Items.BEEF);
        if (!cooking)
            throw helper.assertionException("the stove did not take the beef");

        BlockPos skilletLocal = new BlockPos(9, 1, 1);
        helper.setBlock(skilletLocal, ModBlocks.SKILLET.get());
        BlockPos skilletPos = helper.absolutePos(skilletLocal);
        var skillet = helper.getBlockEntity(skilletLocal, SkilletBlockEntity.class);
        var skilletPoint = new SkilletArmInteractionPoint(FDArmInteractionPointTypes.SKILLET, level, skilletPos, level.getBlockState(skilletPos));
        skilletPoint.insert(null, new ItemStack(Items.BEEF, 2), false);
        if (!skillet.getStoredStack().is(Items.BEEF))
            throw helper.assertionException("the skillet did not take the beef, holds " + skillet.getStoredStack());
        helper.succeed();
    }

    @GameTest
    public void createBlocksAreHeatConductorsAndTraySources(GameTestHelper helper) {
        for (Block block : List.<Block>of(AllBlocks.CHUTE, AllBlocks.SMART_CHUTE, AllBlocks.PACKAGER, AllBlocks.REPACKAGER))
            if (!block.defaultBlockState().is(ModTags.Blocks.HEAT_CONDUCTORS))
                throw helper.assertionException(block + " is not a heat conductor");
        for (Block block : List.<Block>of(AllBlocks.BLAZE_BURNER, AllBlocks.LIT_BLAZE_BURNER))
            if (!block.defaultBlockState().is(ModTags.Blocks.TRAY_HEAT_SOURCES))
                throw helper.assertionException(block + " is not a tray heat source");
        helper.succeed();
    }
}
