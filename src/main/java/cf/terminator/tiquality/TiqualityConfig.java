package cf.terminator.tiquality;

import cf.terminator.tiquality.interfaces.TiqualityBlock;
import cf.terminator.tiquality.monitor.TickMaster;
import cf.terminator.tiquality.tracking.DenyTracker;
import cf.terminator.tiquality.tracking.UpdateType;
import cf.terminator.tiquality.util.Constants;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

@Config(modid = Tiquality.MODID, name = Tiquality.NAME, type = Config.Type.INSTANCE, category = "Tiquality")
public class TiqualityConfig {

    @Config.Comment({
            "Tiquality pre-allocates the max tick time someone can use.",
            "This includes offline players (Loaded chunks with an offline player's base, for example)",
            "With this in mind, what multiplier should we use to assign tick time to them?",
            "",
            "Where 0 means offline player's base does not get any pre-allocated tick time and 1 means they will get the same tick time as an online player.",
            "Keep in mind that people might be living together..."
    })
    @Config.RangeDouble(min = 0, max = 1)
    public static double OFFLINE_PLAYER_TICK_TIME_MULTIPLIER = 0.5;


    @Config.Comment({
            "A block will tick if at least one of the following statements is true:",
            "- There's a Tracker assigned and the tracker has enough time to tick the block",
            "- The block is defined in the config NATURAL_BLOCKS and there's no tracker assigned to it",
            "- The block is defined in the config NATURAL_BLOCKS and the tracker has enough time to tick the block",
            "- The block is defined in the config ALWAYS_TICKED_BLOCKS It will tick even if a tracker has been assigned that ran out of time. Note that this will still consume the time on the tracker.",
            "",
            "The fastest way to solve this is simply by standing on the block and running `/tq set below NATURAL`. It will add the block to the config under NATURAL_BLOCKS.",
            "",
            "Protip: Use /tq info first, to see if you are actually positioned on the block correctly.",
            "",
            "For nicer formatting, see: https://github.com/TerminatorNL/Tiquality/blob/master/README.md#my-blocks-dont-tick-what-do-i-do"
})
    public static BLOCK_TICKING BLOCK_TICK_BEHAVIOR = new BLOCK_TICKING();

    public static class BLOCK_TICKING{
        @Config.Comment({
                "Some blocks are automatically generated in the world, but do require ticking in order to function properly.",
                "Define the blocks you wish to keep tick when the block has not been assigned an owner yet.",
                "Keep in mind, if there is an owner set on this block, the block can be throttled. See: ALWAYS_TICKED_BLOCKS",
        })
        public String[] NATURAL_BLOCKS = new String[]{
                "minecraft:mob_spawner",
                "minecraft:chest",
                "minecraft:ender_chest",
                "minecraft:trapped_chest",
                "REGEX=leaves",
                "REGEX=sapling",
                "REGEX=flowing",
                "minecraft:snow_layer",
                "minecraft:ice",
                "minecraft:water",
                "minecraft:lava",
                "minecraft:grass",
                "minecraft:sand",
                "minecraft:gravel",
                "minecraft:beetroots",
                "minecraft:wheat",
                "minecraft:carrots",
                "minecraft:potatoes",
                "minecraft:reeds",
                "minecraft:farmland",
                "minecraft:fire",
                "minecraft:cocoa",
                "minecraft:cactus",
                "minecraft:double_plant"
        };

        @Config.Comment({
                "Some blocks, you simply don't want to be throttled, ever. For example: piston extensions.",
                "Tiquality will still attempt to tick them per player, but if the player runs out of tick time, it will still tick these blocks.",
                "Items in this list are also appended to NATURAL_BLOCKS through code, there is no need to define blocks twice."
        })
        public String[] ALWAYS_TICKED_BLOCKS = new String[]{
                "minecraft:piston_extension"
        };
    }



    @Config.Comment({
            "Between ticks, the server must do some internal processing.",
            "Increase this value if you see \"can't keep up!\" errors.",
            "Try to keep this value as low as possible for performance."
    })
    @Config.RangeInt(min = 0)
    public static int TIME_BETWEEN_TICKS_IN_NS = 90000;

    @Config.Comment({
            "If someone has a large update queue, they can struggle to pick up their items.",
            "To fix this, we can make sure their items update first. However, this could lead to",
            "undesired/undefined behavior, since it's machines will tick slower than the items. Potential duping?"
    })
    public static boolean UPDATE_ITEMS_FIRST = false;

    @Config.Comment({
            "Define a maximum square range someone can claim using /tq claim [range].",
            "This will also be the default value for usage of /tq claim [range] without the range argument"
    })
    public static int MAX_CLAIM_RADIUS = 50;

    @Config.Comment({
            "When a tracker is being throttled, we can send a notification to the user.",
            "Throttling is measured by comparing the full tick cycles within ticking queues and comparing that to the server ticks.",
            "Every 100 server ticks the amount of completed full ticks of the tracker is compared.",
            "",
            "If the tracker is falling behind (actively throttling) the value for this tracker gets closer to zero. In",
            "comparison, a tracker that runs at full speed will have a value of 1 (100%)",
            "Whenever the value falls below the value specified here, a warning will be sent to the tracker",
            "",
            "There's currently a limitation making warning levels between 0.5 and 1.0 unreliable. (Between 50% and 100% speed)",
            "",
            "Set to 1 to disable",
            "",
            "Note: If the server is ticking at 18 TPS, the tracker can still have a value of 1. Server tick speed does not impact this value."
    })
    @Config.RangeDouble(min = 0, max = 1)
    public static double DEFAULT_THROTTLE_WARNING_LEVEL = 0.5;


    @Config.Comment({
            "When a tracker is being throttled, we can send a notification to the user.",
            "How often do you want the user to receive a message about his/her personal tick speed?",
            "",
            "Note: If you don't want to send a message at all, set DEFAULT_THROTTLE_WARNING_LEVEL to 1."
    })
    @Config.RangeInt(min = 0)
    public static int DEFAULT_THROTTLE_WARNING_INTERVAL_SECONDS = 600;

    public static class QuickConfig{

        private static HashSet<Block> AUTO_WORLD_ASSIGNED_OBJECTS_FAST = new HashSet<>();
        private static HashSet<Block> TICKFORCING_OBJECTS_FAST = new HashSet<>();
        private static HashSet<Block> MODIFIED_BLOCKS = new HashSet<>();

        public static void saveToFile(){
            ConfigManager.sync(Tiquality.MODID, Config.Type.INSTANCE);
        }

        public static void reloadFromFile() {
            /*
             * I know this is not a proper way to do this, if you know of a better way to reload the config
             * FROM DISK, I'd gladly move to your solution.
             */
            try {
                Field field = ConfigManager.class.getDeclaredField("CONFIGS");
                field.setAccessible(true);
                @SuppressWarnings("unchecked") Map<String, Configuration> STOLEN_VARIABLE = (Map<String, Configuration>) field.get(null);
                STOLEN_VARIABLE.remove(new File(Loader.instance().getConfigDir(), "Tiquality.cfg").getAbsolutePath());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            saveToFile();
            update();
        }

        public static void update(){
            TickMaster.TICK_DURATION = Constants.NS_IN_TICK_LONG - TIME_BETWEEN_TICKS_IN_NS;
            for(Block b : MODIFIED_BLOCKS){
                Tiquality.LOGGER.info("Unlinking: " + Block.REGISTRY.getNameForObject(b).toString());
                ((TiqualityBlock) b).setUpdateType(UpdateType.DEFAULT);
            }
            MODIFIED_BLOCKS.clear();

            Tiquality.LOGGER.info("SCANNING BLOCKS...");
            Tiquality.LOGGER.info("NATURAL blocks:");
            AUTO_WORLD_ASSIGNED_OBJECTS_FAST.clear();

            for (String input : BLOCK_TICK_BEHAVIOR.NATURAL_BLOCKS) {
                if(input.startsWith("REGEX=")){
                    AUTO_WORLD_ASSIGNED_OBJECTS_FAST.addAll(findBlocks(input.substring(6)));
                }else {
                    String[] split = input.split(":");
                    ResourceLocation location = new ResourceLocation(split[0], split[1]);

                    Block block = Block.REGISTRY.getObject(location);

                    if (block == Blocks.AIR) {
                        Tiquality.LOGGER.warn("!!!!#######################!!!!");
                        Tiquality.LOGGER.warn("INVALID CONFIG ENTRY");
                        Tiquality.LOGGER.warn("NATURAL_BLOCKS: " + block);
                        Tiquality.LOGGER.warn("This block has been skipped!");
                        Tiquality.LOGGER.warn("!!!!#######################!!!!");
                        continue;
                    }
                    AUTO_WORLD_ASSIGNED_OBJECTS_FAST.add(block);
                }
            }
            for(Block b : AUTO_WORLD_ASSIGNED_OBJECTS_FAST){
                Tiquality.LOGGER.info("+ " + Block.REGISTRY.getNameForObject(b).toString());
                ((TiqualityBlock) b).setUpdateType(UpdateType.NATURAL);
            }

            Tiquality.LOGGER.info("ALWAYS_TICKED blocks:");
            TICKFORCING_OBJECTS_FAST.clear();

            for (String input : BLOCK_TICK_BEHAVIOR.ALWAYS_TICKED_BLOCKS) {
                if(input.startsWith("REGEX=")){
                    TICKFORCING_OBJECTS_FAST.addAll(findBlocks(input.substring(6)));
                }else {
                    String[] split = input.split(":");
                    ResourceLocation location = new ResourceLocation(split[0], split[1]);

                    Block block = Block.REGISTRY.getObject(location);

                    if (block == Blocks.AIR) {
                        Tiquality.LOGGER.warn("!!!!#######################!!!!");
                        Tiquality.LOGGER.warn("INVALID CONFIG ENTRY");
                        Tiquality.LOGGER.warn("ALWAYS_TICKED_BLOCKS: " + block);
                        Tiquality.LOGGER.warn("This block has been skipped!");
                        Tiquality.LOGGER.warn("!!!!#######################!!!!");
                        continue;
                    }
                    TICKFORCING_OBJECTS_FAST.add(block);
                }
            }
            for(Block b : TICKFORCING_OBJECTS_FAST){
                Tiquality.LOGGER.info("+ " + Block.REGISTRY.getNameForObject(b).toString());
                ((TiqualityBlock) b).setUpdateType(UpdateType.ALWAYS_TICK);
            }
            AUTO_WORLD_ASSIGNED_OBJECTS_FAST.addAll(TICKFORCING_OBJECTS_FAST);
            MODIFIED_BLOCKS.addAll(AUTO_WORLD_ASSIGNED_OBJECTS_FAST);
            DenyTracker.unlinkAll();
        }

        private static ArrayList<Block> findBlocks(String regex){
            ArrayList<Block> list = new ArrayList<>();
            for(ResourceLocation resource : Block.REGISTRY.getKeys()){
                if(Pattern.compile(regex).matcher(resource.toString()).find()){
                    list.add(Block.REGISTRY.getObject(resource));
                    Tiquality.LOGGER.info("regex '" + regex + "' applied for: " + resource.toString());
                }
            }
            if(list.size() == 0){
                Tiquality.LOGGER.warn("regex '" + regex + "' had no matches!");
            }
            return list;
        }
    }
}
