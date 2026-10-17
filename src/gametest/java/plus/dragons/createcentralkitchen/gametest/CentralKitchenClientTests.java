package plus.dragons.createcentralkitchen.gametest;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.client.content.kinetics.deployer.DeployerRenderer.DeployerItemRenderState;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import com.zurrtum.create.client.ponder.foundation.ui.PonderUI;
import com.zurrtum.create.content.kinetics.deployer.DeployerBlockEntity;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import plus.dragons.createcentralkitchen.client.burner.BlazeBurnerClientExtension;
import plus.dragons.createcentralkitchen.client.burner.BlazeBurnerRenderOverride;
import plus.dragons.createcentralkitchen.client.deployer.HandheldDeployerItemRenderState;
import plus.dragons.createcentralkitchen.client.registry.CCKPartialModels;
import plus.dragons.createcentralkitchen.data.CCKItemTags;
import plus.dragons.createcentralkitchen.integration.farmersdelight.jei.FDJeiExtension;

/**
 * Real-client test of Central Kitchen on Minecraft 26.2 with the real Create Fly, Dragons Plus, Farmer's Delight and JEI:
 * opens the game, creates a world, builds a small kitchen in the sky and checks and photographs the parts of the mod
 * that only exist on the client: the chef hat and raised head of the blaze burner, the tilted tool in the deployer, the
 * two JEI categories and the Ponder scenes.
 *
 * Run it with {@code herramientas\probar-cliente.ps1} (it refuses to start while Minecraft is open or RAM is low), or
 * {@code gradlew runClientGameTest --offline}. Screenshots: {@code build/run/clientGameTest/screenshots}.
 * Look for the {@code [CCK-TEST]} markers in the output. The checks are collected and the test fails at the end, so one
 * run reports everything that is wrong; the screenshots are taken whatever the checks say.
 */
public class CentralKitchenClientTests implements FabricClientGameTest {
    private static final double EYE = 1.62;
    private static final int BURNER_Z = 0;
    private static final int DEPLOYER_Z = -8;
    private static final Identifier COOKING_POT = Identifier.fromNamespaceAndPath("farmersdelight", "cooking_pot");
    private static final Identifier SKILLET = Identifier.fromNamespaceAndPath("farmersdelight", "skillet");
    private static final Identifier CUTTING_BOARD = Identifier.fromNamespaceAndPath("farmersdelight", "cutting_board");
    private static final Identifier IRON_KNIFE = Identifier.fromNamespaceAndPath("farmersdelight", "iron_knife");

    private final List<String> failures = new ArrayList<>();

    /** What each blaze burner of the row has above it and what the mod should do about it. */
    private record BurnerCase(String name, int x, boolean hat) {}

    private static final List<BurnerCase> BURNERS = List.of(
            new BurnerCase("control-nothing-above", -4, false),
            new BurnerCase("cooking-pot", -2, true),
            new BurnerCase("skillet", 0, true),
            new BurnerCase("chute-then-pot", 2, true),
            new BurnerCase("stone-above", 4, false));

    @FunctionalInterface
    private interface Step {
        void run() throws Exception;
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        log("backend of Create's renderer: " + flywheelBackend());
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            context.waitTicks(40);
            context.getInput().resizeWindow(1280, 760);
            context.runOnClient(mc -> {
                if (!mc.gui.hud.isHidden())
                    mc.gui.hud.toggle();   // same as pressing F1
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
            });
            server.runCommand("gamemode spectator @p");
            server.runCommand("difficulty peaceful");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            buildPlatform(server);
            context.waitTicks(20);

            step("blaze burners", () -> burners(context, server));
            step("deployers", () -> deployers(context, server));
            step("JEI", () -> jei(context, server));
            step("Ponder", () -> ponder(context, server));
        }   // <- closes the world: the client goes back to the title screen with all the mod's client state alive

        context.waitTicks(40);
        step("leave the world", () -> expect(context.computeOnClient(mc -> mc.level == null && mc.player == null),
                "the client still has a world after closing it"));

        if (failures.isEmpty()) {
            log("ALL OK");
            return;
        }
        log("FAILED CHECKS (" + failures.size() + "):");
        failures.forEach(f -> log("  - " + f));
        throw new AssertionError(failures.size() + " check(s) failed, first: " + failures.getFirst());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Blaze burners: chef hat and raised head
    // ------------------------------------------------------------------------------------------------------------

    private void burners(ClientGameTestContext context, TestServerContext server) throws Exception {
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            BlockState burner = AllBlocks.BLAZE_BURNER.defaultBlockState()
                    .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED)
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
            for (BurnerCase c : BURNERS)
                level.setBlockAndUpdate(new BlockPos(c.x(), 201, BURNER_Z), burner);
            set(level, 202, "cooking-pot", COOKING_POT);
            set(level, 202, "skillet", SKILLET);
            level.setBlockAndUpdate(new BlockPos(2, 202, BURNER_Z), AllBlocks.CHUTE.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(2, 203, BURNER_Z), block(COOKING_POT).defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(4, 202, BURNER_Z), Blocks.STONE.defaultBlockState());
        });
        feedBurners(server);
        // Give the client time to receive the block entities, tick them once and (with Flywheel) build the visuals.
        context.waitTicks(60);

        for (BurnerCase c : BURNERS) {
            BlockPos pos = new BlockPos(c.x(), 201, BURNER_Z);
            String what = "burner " + c.name();
            step(what, () -> {
                // The client picks the override while it animates the burner: wait for the answer we expect.
                if (c.hat())
                    context.waitFor(mc -> burnerOverride(mc, pos) != null, 200);
                String described = context.computeOnClient(mc -> {
                    BlazeBurnerBlockEntity be = burner(mc, pos);
                    return "heat=" + be.getHeatLevelForRender() + " validAbove=" + be.isValidBlockAbove()
                            + " override=" + (((BlazeBurnerClientExtension) be).getRenderOverride() != null);
                });
                log(what + ": " + described);
                boolean hasOverride = context.computeOnClient(mc -> burnerOverride(mc, pos) != null);
                expect(hasOverride == c.hat(), what + ": render override " + (hasOverride ? "present" : "missing")
                        + ", expected " + (c.hat() ? "present" : "absent"));
                boolean validAbove = context.computeOnClient(mc -> burner(mc, pos).isValidBlockAbove());
                expect(validAbove == c.hat(), what + ": isValidBlockAbove()=" + validAbove + " (raised head), expected " + c.hat());
                if (c.hat()) {
                    boolean hatLoaded = context.computeOnClient(mc ->
                            BlazeBurnerRenderOverride.isModelLoaded(burnerOverride(mc, pos).getHatModel(false)));
                    expect(hatLoaded, what + ": the chef hat model is not loaded (missing or not baked)");
                }
            });
        }
        step("chef hat models baked", () -> {
            expect(context.computeOnClient(mc -> CCKPartialModels.CHEF_HAT.get() != null), "chef_hat model is not baked");
            expect(context.computeOnClient(mc -> CCKPartialModels.CHEF_HAT_SMALL.get() != null), "chef_hat_small model is not baked");
        });

        // Photos: the whole row, then each case close up from the same angle so they can be compared one by one.
        look(server, 0.5, 203.0, 7.0, 0.5, 202.0, BURNER_Z + 0.5);
        context.waitTicks(20);
        context.takeScreenshot("burners-row");
        for (BurnerCase c : BURNERS) {
            feedBurners(server);
            look(server, c.x() + 0.5 + 1.6, 202.4, BURNER_Z + 3.0, c.x() + 0.5, 201.7, BURNER_Z + 0.5);
            context.waitTicks(15);
            context.takeScreenshot("burner-" + c.name());
        }
        // Same pot seen from the front and from above, as a player would normally see it.
        look(server, -1.5, 204.3, BURNER_Z + 2.2, -1.5, 202.2, BURNER_Z + 0.5);
        context.waitTicks(15);
        context.takeScreenshot("burner-cooking-pot-from-above");
    }

    private static void set(ServerLevel level, int y, String name, Identifier blockId) {
        BurnerCase c = BURNERS.stream().filter(b -> b.name().equals(name)).findFirst().orElseThrow();
        level.setBlockAndUpdate(new BlockPos(c.x(), y, BURNER_Z), block(blockId).defaultBlockState());
    }

    private static Block block(Identifier id) {
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == Blocks.AIR)
            throw new IllegalStateException("block " + id + " is not registered");
        return block;
    }

    /** Burners lose their fuel on the first tick; give them coal so the flame and the awake blaze are on screen. */
    private static void feedBurners(TestServerContext server) {
        server.runOnServer(s -> {
            for (BurnerCase c : BURNERS) {
                if (!(s.overworld().getBlockEntity(new BlockPos(c.x(), 201, BURNER_Z)) instanceof BlazeBurnerBlockEntity be))
                    continue;
                try {
                    Method tryUpdateFuel = BlazeBurnerBlockEntity.class.getDeclaredMethod(
                            "tryUpdateFuel", ItemStack.class, boolean.class, boolean.class);
                    tryUpdateFuel.setAccessible(true);
                    tryUpdateFuel.invoke(be, new ItemStack(Items.COAL), false, false);
                    be.updateBlockState();
                } catch (ReflectiveOperationException e) {
                    log("could not feed the burner at " + be.getBlockPos() + ": " + e);
                }
            }
        });
    }

    private static BlazeBurnerBlockEntity burner(Minecraft mc, BlockPos pos) {
        if (!(mc.level.getBlockEntity(pos) instanceof BlazeBurnerBlockEntity be))
            throw new IllegalStateException("the client has no blaze burner at " + pos + ", it has " + mc.level.getBlockState(pos));
        return be;
    }

    private static BlazeBurnerRenderOverride burnerOverride(Minecraft mc, BlockPos pos) {
        return ((BlazeBurnerClientExtension) burner(mc, pos)).getRenderOverride();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Deployers: the tool held in use mode is drawn tilted
    // ------------------------------------------------------------------------------------------------------------

    private void deployers(ClientGameTestContext context, TestServerContext server) throws Exception {
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        ItemStack knife = new ItemStack(BuiltInRegistries.ITEM.getValue(IRON_KNIFE));
        ItemStack apple = new ItemStack(Items.APPLE);
        // x -> (mode, item)
        record Case(String name, int x, DeployerBlockEntity.Mode mode, ItemStack item) {}
        List<Case> cases = List.of(
                new Case("use-pickaxe", -6, DeployerBlockEntity.Mode.USE, tool),
                new Case("use-knife", -3, DeployerBlockEntity.Mode.USE, knife),
                new Case("punch-pickaxe", 0, DeployerBlockEntity.Mode.PUNCH, tool),
                new Case("use-apple", 3, DeployerBlockEntity.Mode.USE, apple));

        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            for (Case c : cases)
                level.setBlockAndUpdate(new BlockPos(c.x(), 201, DEPLOYER_Z), AllBlocks.DEPLOYER.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        });
        context.waitTicks(20);
        server.runOnServer(s -> {
            for (Case c : cases) {
                if (!(s.overworld().getBlockEntity(new BlockPos(c.x(), 201, DEPLOYER_Z)) instanceof DeployerBlockEntity be))
                    throw new IllegalStateException("no deployer at x=" + c.x());
                be.mode = c.mode();
                be.heldItem = c.item().copy();
                if (be.getPlayer() != null)
                    be.getPlayer().cast().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, c.item().copy());
                be.setChanged();
                be.sendData();
            }
        });
        context.waitTicks(40);

        step("deployer tag", () -> {
            expect(context.computeOnClient(mc -> tool.is(CCKItemTags.HANDHELD_IN_DEPLOYER_USE)),
                    "the iron pickaxe is not in #create:handheld_in_deployer_use on the client");
            expect(context.computeOnClient(mc -> !apple.is(CCKItemTags.HANDHELD_IN_DEPLOYER_USE)),
                    "the apple is in #create:handheld_in_deployer_use");
            boolean knifeInTag = context.computeOnClient(mc -> knife.is(CCKItemTags.HANDHELD_IN_DEPLOYER_USE));
            log("farmersdelight:iron_knife in #create:handheld_in_deployer_use: " + knifeInTag);
        });
        for (Case c : cases) {
            BlockPos pos = new BlockPos(c.x(), 201, DEPLOYER_Z);
            step("deployer " + c.name(), () -> {
                context.waitFor(mc -> mc.level.getBlockEntity(pos) instanceof DeployerBlockEntity be
                        && !be.heldItem.isEmpty() && be.mode == c.mode(), 200);
                String drawn = context.computeOnClient(mc -> {
                    DeployerBlockEntity be = (DeployerBlockEntity) mc.level.getBlockEntity(pos);
                    return DeployerItemRenderState.create(be, mc.getItemModelResolver(), mc.level, be.heldItem,
                            Vec3.ZERO, Direction.EAST).getClass().getSimpleName();
                });
                boolean handheld = HandheldDeployerItemRenderState.class.getSimpleName().equals(drawn);
                boolean tagged = context.computeOnClient(mc -> c.item().is(CCKItemTags.HANDHELD_IN_DEPLOYER_USE));
                boolean shouldBe = c.mode() == DeployerBlockEntity.Mode.USE && tagged;
                log("deployer " + c.name() + " draws its item with " + drawn);
                expect(handheld == shouldBe, "deployer " + c.name() + ": drawn with " + drawn
                        + (shouldBe ? ", expected the tilted handheld state" : ", expected the normal state"));
            });
        }
        for (Case c : cases) {
            look(server, c.x() + 2.3, 202.4, DEPLOYER_Z + 3.2, c.x() + 1.0, 201.5, DEPLOYER_Z + 0.5);
            context.waitTicks(15);
            context.takeScreenshot("deployer-" + c.name());
        }
        look(server, -1.5, 203.2, DEPLOYER_Z + 6.5, -1.5, 201.5, DEPLOYER_Z + 0.5);
        context.waitTicks(15);
        context.takeScreenshot("deployers-row");
    }

    // ------------------------------------------------------------------------------------------------------------
    // JEI: the two Farmer's Delight categories
    // ------------------------------------------------------------------------------------------------------------

    private void jei(ClientGameTestContext context, TestServerContext server) throws Exception {
        context.waitFor(mc -> ClientTestJeiPlugin.runtime != null, 1200);
        IJeiRuntime runtime = ClientTestJeiPlugin.runtime;
        List<IRecipeType<?>> types = List.of(FDJeiExtension.SAWING, FDJeiExtension.DEPLOYING);
        for (IRecipeType<?> type : types) {
            step("JEI category " + type.getUid(), () -> {
                long categories = context.computeOnClient(mc -> runtime.getRecipeManager().createRecipeCategoryLookup()
                        .limitTypes(List.<IRecipeType<?>>of(type)).get().count());
                expect(categories == 1, "JEI has " + categories + " categories of type " + type.getUid() + ", expected 1");
                long recipes = context.computeOnClient(mc -> countRecipes(runtime, type));
                log("JEI " + type.getUid() + ": " + recipes + " recipes");
                expect(recipes > 0, "JEI has no recipes in " + type.getUid());
            });
        }
        for (IRecipeType<?> type : types) {
            context.runOnClient(mc -> runtime.getRecipesGui().showTypes(List.<IRecipeType<?>>of(type)));
            context.waitTicks(30);
            context.takeScreenshot("jei-" + type.getUid().getPath());
        }
        context.setScreen(() -> null);
        context.waitTicks(5);
    }

    private static long countRecipes(IJeiRuntime runtime, IRecipeType<?> type) {
        return runtime.getRecipeManager().createRecipeLookup(type).get().count();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ponder: scenes compile and play
    // ------------------------------------------------------------------------------------------------------------

    private void ponder(ClientGameTestContext context, TestServerContext server) throws Exception {
        for (Identifier id : List.of(COOKING_POT, SKILLET, CUTTING_BOARD)) {
            step("Ponder scenes of " + id, () -> {
                expect(context.computeOnClient(mc -> PonderIndex.getSceneAccess().doScenesExistForId(id)), "no Ponder scenes registered for " + id);
                int scenes = context.computeOnClient(mc -> PonderIndex.getSceneAccess().compile(id).size());
                log("Ponder " + id + ": " + scenes + " scenes compiled");
                expect(scenes > 0, "no Ponder scene of " + id + " compiled");
            });
        }
        for (Identifier id : List.of(COOKING_POT, SKILLET, CUTTING_BOARD)) {
            step("Ponder screen of " + id, () -> {
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
                context.setScreen(() -> PonderUI.of(stack));
                context.waitTicks(20);
                expect(context.computeOnClient(mc -> mc.gui.screen() instanceof PonderUI), "the Ponder screen of " + id + " did not open");
                context.waitTicks(150);
                context.takeScreenshot("ponder-" + id.getPath() + "-1");
                context.getInput().pressKey(o -> o.keyRight);
                context.waitTicks(150);
                context.takeScreenshot("ponder-" + id.getPath() + "-2");
                context.setScreen(() -> null);
                context.waitTicks(5);
            });
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------

    private static void buildPlatform(TestServerContext server) {
        server.runCommand("fill -12 201 -12 12 215 14 air");
        server.runCommand("fill -12 200 -12 12 200 14 stone");
    }

    /** Puts the spectator's eyes at the given point looking at the target. */
    private static void look(TestServerContext server, double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.4f %.4f", ex, ey - EYE, ez, yaw, pitch));
    }

    /** Runs one part of the test; a failure is recorded and the next part still runs. */
    private void step(String name, Step step) {
        try {
            step.run();
            log(name + ": done");
        } catch (Throwable t) {
            String message = name + ": " + t;
            log("FAIL: " + message);
            t.printStackTrace(System.out);
            failures.add(message);
        }
    }

    private void expect(boolean ok, String message) {
        if (!ok) {
            log("FAIL: " + message);
            failures.add(message);
        }
    }

    private static String flywheelBackend() {
        try {
            Class<?> manager = Class.forName("com.zurrtum.create.client.flywheel.api.backend.BackendManager");
            return String.valueOf(manager.getMethod("currentBackend").invoke(null));
        } catch (ReflectiveOperationException | LinkageError e) {
            return "unknown (" + e + ")";
        }
    }

    private static void log(String message) {
        System.out.println("[CCK-TEST] " + message);
    }
}
