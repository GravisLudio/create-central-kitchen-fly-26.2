package plus.dragons.createcentralkitchen.gametest;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.client.content.kinetics.deployer.DeployerRenderer.DeployerItemRenderState;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import com.zurrtum.create.client.ponder.foundation.ui.PonderUI;
import com.zurrtum.create.content.kinetics.deployer.DeployerBlockEntity;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
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
import org.lwjgl.glfw.GLFW;
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
    private static final Identifier APPLE_PIE = Identifier.fromNamespaceAndPath("farmersdelight", "apple_pie");
    /** Items whose Ponder scenes Central Kitchen adds: they are compiled, and opened on screen. */
    private static final List<Identifier> PONDER_ITEMS = List.of(COOKING_POT, SKILLET, CUTTING_BOARD, APPLE_PIE);

    /**
     * Recording mode (environment variable {@code CCK_TEST_RECORD}, set by {@code probar-cliente.ps1 -Grabacion}): the HUD
     * stays on so the captions show, and each key shot is held for a few seconds with a caption saying what is checked.
     */
    private static final boolean SHORT = System.getenv("CCK_TEST_SHORT") != null;
    private static final boolean RECORD = SHORT || System.getenv("CCK_TEST_RECORD") != null;
    private static final int HOLD_TICKS = 80;
    /**
     * Short mode ({@code probar-cliente.ps1 -Corto}, environment variable {@code CCK_TEST_SHORT}, implies recording mode):
     * a vertical 9:16 window of a fixed size (default 608x1080, {@code CCK_TEST_SIZE=WxH}) and a ~35 s route made for a
     * short video instead of the full checks: a burner with and without a pot seen from the side, deployers, JEI, Ponder.
     */
    private static final int[] WINDOW = windowSize();

    private final List<String> failures = new ArrayList<>();

    /** What each blaze burner of the row has above it and what the mod should do about it. */
    private record BurnerCase(String name, int x, boolean hat, String caption) {}

    private static final List<BurnerCase> BURNERS = List.of(
            new BurnerCase("control-nothing-above", -4, false, "Sin nada encima: cabeza baja y sin sombrero"),
            new BurnerCase("cooking-pot", -2, true, "Bajo una olla de Farmer's Delight: cabeza levantada y sombrero de chef"),
            new BurnerCase("skillet", 0, true, "Bajo una sartén: también sombrero de chef"),
            new BurnerCase("chute-then-pot", 2, true, "Con una tolva en medio, el calor pasa y sale el sombrero"),
            new BurnerCase("stone-above", 4, false, "Bajo una piedra: no cambia nada"));

    @FunctionalInterface
    private interface Step {
        void run() throws Exception;
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            context.waitTicks(40);
            setUpWindow(context);
            server.runCommand("gamerule send_command_feedback false");   // no chat lines over the picture
            context.runOnClient(mc -> {
                if (!RECORD && !mc.gui.hud.isHidden())
                    mc.gui.hud.toggle();   // same as pressing F1
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
            });
            server.runCommand("gamemode spectator @p");
            server.runCommand("difficulty peaceful");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            buildPlatform(server);
            context.waitTicks(20);
            // Flywheel only picks its backend when a level is loaded: asking at the title screen says "simple" always.
            log("backend of Create's renderer inside the world: " + flywheelBackend());
            log("recording mode: " + RECORD);
            if (RECORD)
                step("wait for the go", () -> waitForGo(context, server));

            if (SHORT) {
                step("short route", () -> shortRoute(context, server));
            } else {
                step("blaze burners", () -> burners(context, server));
                step("deployers", () -> deployers(context, server));
                step("JEI", () -> jei(context, server));
                step("Ponder", () -> ponder(context, server));
            }
        }   // <- closes the world: the client goes back to the title screen with all the mod's client state alive

        context.waitTicks(40);
        step("leave the world", () -> expect(context.computeOnClient(mc -> mc.level == null && mc.player == null),
                "the client still has a world after closing it"));
        step("game log has no data errors", this::checkGameLog);

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

    private static void placeBurners(TestServerContext server) throws Exception {
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
    }

    /**
     * Recording mode: with the kitchen already built and the camera on the burner row, wait until {@code iniciar.txt}
     * shows up in the run folder ({@code probar-cliente.ps1 -Iniciar} creates it), so the capture software can be linked
     * to the Minecraft window and started first. Then a title card, and the test goes on. Gives up after 30 minutes.
     */
    private void waitForGo(ClientGameTestContext context, TestServerContext server) throws Exception {
        Path go = Path.of("iniciar.txt").toAbsolutePath();
        Files.deleteIfExists(go);
        if (SHORT) {
            placeLoneBurner(server);
            lookAtLoneBurner(server);
        } else {
            placeBurners(server);
            look(server, 0.5, 203.0, 7.0, 0.5, 202.0, BURNER_Z + 0.5);
        }
        log("READY: waiting for " + go + " (up to 30 minutes)");
        for (int second = 0; second < 30 * 60 && !Files.exists(go); second++) {
            if (second % 30 == 29)
                feedBurners(server);   // a coal lasts 80 s
            context.waitTicks(20);
        }
        Files.deleteIfExists(go);
        log("GO: recording starts now");
        feedBurners(server);
        if (SHORT) {
            // Narrow window: the big title is 24 px per letter at GUI scale 2, so it is split over title and subtitle.
            server.runCommand("title @a times 5 40 10");
            server.runCommand("title @a subtitle \"KITCHEN llega a 26.2\"");
            server.runCommand("title @a title \"CENTRAL\"");
            context.waitTicks(55);
            return;
        }
        server.runCommand("title @a times 5 60 15");
        server.runCommand("title @a subtitle \"Port a Fabric 26.2 con Create Fly\"");
        server.runCommand("title @a title \"CENTRAL KITCHEN\"");
        context.waitTicks(90);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Short mode: a ~35 s route in a vertical window, made for a short video
    // ------------------------------------------------------------------------------------------------------------

    private static int[] windowSize() {
        String size = System.getenv("CCK_TEST_SIZE");
        if (size != null && size.matches("\\d+x\\d+")) {
            String[] parts = size.split("x");
            return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
        }
        return SHORT ? new int[] { 608, 1080 } : new int[] { 1280, 760 };
    }

    /**
     * Fabric's resizeWindow only changes the size the game thinks it has, not the operating system's window (the first
     * recording showed a huge window that the capture software cut to its corner). In short mode the real window is
     * restored from maximized, resized, moved on screen, and the GUI scale is fixed so captions and JEI fit.
     */
    private static void setUpWindow(ClientGameTestContext context) {
        context.getInput().resizeWindow(WINDOW[0], WINDOW[1]);
        if (!SHORT)
            return;
        context.runOnClient(mc -> {
            long handle = mc.getWindow().handle();
            GLFW.glfwRestoreWindow(handle);
            GLFW.glfwSetWindowSize(handle, WINDOW[0], WINDOW[1]);
            GLFW.glfwSetWindowPos(handle, 80, 30);
            mc.options.guiScale().set(2);
            mc.resizeGui();
        });
        context.waitTicks(20);
        log("window now " + context.computeOnClient(mc -> mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight()
                + " (screen " + mc.getWindow().getScreenWidth() + "x" + mc.getWindow().getScreenHeight()
                + ", GUI scale " + mc.getWindow().getGuiScale() + ")"));
    }

    private static void placeLoneBurner(TestServerContext server) throws Exception {
        server.runOnServer(s -> s.overworld().setBlockAndUpdate(new BlockPos(0, 201, BURNER_Z),
                AllBlocks.BLAZE_BURNER.defaultBlockState()
                        .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED)
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH)));
        feedBurners(server);
    }

    /** Side-on at the height of the blaze's head, a little over two blocks away; the pot and its steam fit above. */
    private static void lookAtLoneBurner(TestServerContext server) {
        look(server, 0.5, 202.0, BURNER_Z + 2.8, 0.5, 202.0, BURNER_Z + 0.5);
    }

    private void shortRoute(ClientGameTestContext context, TestServerContext server) throws Exception {
        // 1) The same burner: alone, then with a pot, then with a skillet.
        lookAtLoneBurner(server);
        caption(server, "Un blaze burner de Create");
        context.waitTicks(50);
        snap(context, "short-1-burner-alone");
        setBlockAbove(server, COOKING_POT);
        caption(server, "Le pones una olla encima...");
        context.waitTicks(30);
        feedBurners(server);
        caption(server, "...y sube la cabeza con sombrero de chef");
        context.waitTicks(45);
        snap(context, "short-2-burner-pot");
        setBlockAbove(server, SKILLET);
        caption(server, "Con una sartén pasa igual");
        context.waitTicks(55);
        snap(context, "short-3-burner-skillet");

        // 2) Two deployers, one over the other, side-on: punch mode on top, use mode below (tilted tool).
        BlockPos use = new BlockPos(0, 201, DEPLOYER_Z), punch = new BlockPos(0, 203, DEPLOYER_Z);
        server.runOnServer(s -> {
            for (BlockPos pos : List.of(use, punch))
                s.overworld().setBlockAndUpdate(pos, AllBlocks.DEPLOYER.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        });
        context.waitTicks(20);
        server.runOnServer(s -> {
            giveDeployer(s.overworld(), use, DeployerBlockEntity.Mode.USE);
            giveDeployer(s.overworld(), punch, DeployerBlockEntity.Mode.PUNCH);
        });
        look(server, 1.2, 202.5, DEPLOYER_Z + 4.5, 1.2, 202.5, DEPLOYER_Z + 0.5);
        context.waitTicks(40);
        caption(server, "Deployer: arriba golpe, abajo uso");
        context.waitTicks(45);
        caption(server, "En uso, el pico va inclinado");
        context.waitTicks(45);
        snap(context, "short-4-deployers");

        // 3) JEI: the two new categories.
        context.waitFor(mc -> ClientTestJeiPlugin.runtime != null, 1200);
        IJeiRuntime runtime = ClientTestJeiPlugin.runtime;
        context.runOnClient(mc -> runtime.getRecipesGui().showTypes(List.<IRecipeType<?>>of(FDJeiExtension.SAWING)));
        context.waitTicks(15);
        caption(server, "JEI: la sierra corta lo de la tabla");
        context.waitTicks(45);
        snap(context, "short-5-jei-sawing");
        context.runOnClient(mc -> runtime.getRecipesGui().showTypes(List.<IRecipeType<?>>of(FDJeiExtension.DEPLOYING)));
        context.waitTicks(10);
        caption(server, "Y el deployer con cuchillo, también");
        context.waitTicks(45);
        snap(context, "short-6-jei-deploying");
        context.setScreen(() -> null);
        context.waitTicks(5);

        // 4) Ponder: the pot's scene, then the skillet's.
        ponderFor(context, server, COOKING_POT, 125, "Ponder: la olla con brazo y empaquetador", "short-7-ponder-pot");
        ponderFor(context, server, SKILLET, 100, "Y la sartén con el fogón", "short-8-ponder-skillet");
    }

    private static void setBlockAbove(TestServerContext server, Identifier blockId) throws Exception {
        server.runOnServer(s -> s.overworld().setBlockAndUpdate(new BlockPos(0, 202, BURNER_Z), block(blockId).defaultBlockState()));
    }

    private static void giveDeployer(ServerLevel level, BlockPos pos, DeployerBlockEntity.Mode mode) {
        if (!(level.getBlockEntity(pos) instanceof DeployerBlockEntity be))
            throw new IllegalStateException("no deployer at " + pos);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        be.mode = mode;
        be.heldItem = pickaxe.copy();
        if (be.getPlayer() != null)
            be.getPlayer().cast().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pickaxe.copy());
        be.setChanged();
        be.sendData();
    }

    private void ponderFor(ClientGameTestContext context, TestServerContext server, Identifier id, int ticks, String caption, String shotName) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
        context.setScreen(() -> PonderUI.of(stack));
        context.waitTicks(20);
        caption(server, caption);
        context.waitTicks(ticks);
        snap(context, shotName);
        context.setScreen(() -> null);
        context.waitTicks(5);
    }

    private static void snap(ClientGameTestContext context, String name) {
        context.takeScreenshot(name);
    }

    private void burners(ClientGameTestContext context, TestServerContext server) throws Exception {
        placeBurners(server);
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
        shot(context, server, "burners-row", "Blaze Burners con Central Kitchen: solo, olla, sartén, tolva + olla y piedra");
        for (BurnerCase c : BURNERS) {
            feedBurners(server);
            look(server, c.x() + 0.5 + 1.6, 202.4, BURNER_Z + 3.0, c.x() + 0.5, 201.7, BURNER_Z + 0.5);
            context.waitTicks(15);
            Path picture = shot(context, server, "burner-" + c.name(), c.caption());
            if (!RECORD) {
                // The chef hat is light neutral grey (222) and nothing else around a burner is: count those pixels at the
                // centre of the picture, where the head is. Measured in the first real run: 285-414 with a hat, 0 without.
                int hatPixels = hatPixels(picture);
                log("burner " + c.name() + ": " + hatPixels + " pixels of the chef hat's colour");
                expect(c.hat() ? hatPixels >= 100 : hatPixels <= 5,
                        "burner " + c.name() + ": " + hatPixels + " hat-coloured pixels, expected " + (c.hat() ? "100 or more" : "none"));
            }
        }
        // Same pot seen from the front and from above, as a player would normally see it.
        look(server, -1.5, 204.3, BURNER_Z + 2.2, -1.5, 202.2, BURNER_Z + 0.5);
        context.waitTicks(15);
        shot(context, server, "burner-cooking-pot-from-above", "Vista desde arriba, como la vería un jugador");
        // Side-on at the height of the blaze's head, 1.5 blocks away: the hat is the light grey slab on top of the head.
        for (BurnerCase c : BURNERS) {
            if (RECORD)
                break;
            feedBurners(server);
            look(server, c.x() + 0.5, 201.9, BURNER_Z + 2.0, c.x() + 0.5, 201.9, BURNER_Z + 0.5);
            context.waitTicks(15);
            context.takeScreenshot("burner-side-" + c.name());
        }
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
        record Case(String name, int x, DeployerBlockEntity.Mode mode, ItemStack item, String caption) {}
        List<Case> cases = List.of(
                new Case("use-pickaxe", -6, DeployerBlockEntity.Mode.USE, tool, "Deployer en modo uso con un pico: la herramienta se ve inclinada"),
                new Case("use-knife", -3, DeployerBlockEntity.Mode.USE, knife, "Lo mismo con un cuchillo de Farmer's Delight"),
                new Case("punch-pickaxe", 0, DeployerBlockEntity.Mode.PUNCH, tool, "En modo golpe se dibuja recto, como siempre"),
                new Case("use-apple", 3, DeployerBlockEntity.Mode.USE, apple, "Lo que no es una herramienta no se inclina"));

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
            shot(context, server, "deployer-" + c.name(), c.caption());
        }
        look(server, -1.5, 203.2, DEPLOYER_Z + 6.5, -1.5, 201.5, DEPLOYER_Z + 0.5);
        context.waitTicks(15);
        shot(context, server, "deployers-row", "Los cuatro deployers juntos");
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
            shot(context, server, "jei-" + type.getUid().getPath(), type == FDJeiExtension.SAWING
                    ? "JEI: la sierra mecánica ahora corta lo que corta la tabla de corte"
                    : "JEI: y el deployer con cuchillo, también");
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
        for (Identifier id : PONDER_ITEMS) {
            step("Ponder scenes of " + id, () -> {
                expect(context.computeOnClient(mc -> PonderIndex.getSceneAccess().doScenesExistForId(id)), "no Ponder scenes registered for " + id);
                int scenes = context.computeOnClient(mc -> PonderIndex.getSceneAccess().compile(id).size());
                log("Ponder " + id + ": " + scenes + " scenes compiled");
                expect(scenes > 0, "no Ponder scene of " + id + " compiled");
            });
        }
        for (Identifier id : PONDER_ITEMS) {
            step("Ponder screen of " + id, () -> {
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
                context.setScreen(() -> PonderUI.of(stack));
                context.waitTicks(20);
                expect(context.computeOnClient(mc -> mc.gui.screen() instanceof PonderUI), "the Ponder screen of " + id + " did not open");
                context.waitTicks(150);
                shot(context, server, "ponder-" + id.getPath() + "-1", null);
                context.getInput().pressKey(o -> o.keyRight);
                context.waitTicks(150);
                shot(context, server, "ponder-" + id.getPath() + "-2", null);
                context.setScreen(() -> null);
                context.waitTicks(5);
            });
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------

    /**
     * The game does not fail when a data file is in an old format: it logs a warning and uses defaults (an arm that loses
     * its interaction points, an empty vault). The checks above cannot see that, so read the game's own log. First real run
     * (2026-10-19) logged exactly this for the Ponder structures: "Unknown element name:DEPOSIT", "Serialization errors".
     */
    private void checkGameLog() throws IOException {
        Path gameLog = Path.of("logs", "latest.log");
        if (!Files.exists(gameLog)) {
            log("no game log to check at " + gameLog.toAbsolutePath());
            return;
        }
        int bad = 0;
        for (String line : new String(Files.readAllBytes(gameLog), StandardCharsets.UTF_8).split("\\R")) {
            if (line.contains("Serialization errors") || line.contains("Failed to decode value")
                    || line.contains("Unknown element name") || line.contains("Missing texture references in model create_central_kitchen")) {
                if (++bad <= 6)
                    log("game log: " + line.strip());
            }
        }
        expect(bad == 0, bad + " line(s) in the game log about data that did not load (see the [CCK-TEST] game log lines above)");
    }

    /** Takes a screenshot; in recording mode first shows the caption (kept alive) for a few seconds. */
    private static Path shot(ClientGameTestContext context, TestServerContext server, String name, String caption) {
        if (RECORD && caption != null) {
            for (int left = HOLD_TICKS; left > 0; left -= 40) {
                caption(server, caption);
                context.waitTicks(Math.min(40, left));
            }
        }
        return context.takeScreenshot(name);
    }

    private static void caption(TestServerContext server, String text) {
        try {
            server.runCommand("title @a actionbar \"" + text.replace("\"", "'") + "\"");
        } catch (RuntimeException e) {
            log("could not show a caption: " + e);
        }
    }

    /** Pixels of the chef hat's colour (light, neutral grey) in the middle of the picture, where a burner's head is. */
    private static int hatPixels(Path picture) throws IOException {
        BufferedImage image = ImageIO.read(picture.toFile());
        int width = image.getWidth(), height = image.getHeight(), count = 0;
        for (int x = width * 41 / 100; x < width * 59 / 100; x++) {
            for (int y = height * 39 / 100; y < height * 63 / 100; y++) {
                int rgb = image.getRGB(x, y);
                int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
                if (min > 165 && max - min < 14)
                    count++;
            }
        }
        return count;
    }

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
