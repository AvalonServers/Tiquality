package com.github.terminatornl.tiquality.mixinhelper;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.github.terminatornl.tiquality.Tiquality.LOGGER;
import static com.github.terminatornl.tiquality.mixinhelper.MixinConfigPlugin.MIXINS_TO_LOAD;

public class MixinValidator {

    public static void validate() {
        HashMap<String, String> FAILED_OR_UNLOADED_MIXINS = new HashMap<>(MIXINS_TO_LOAD);
        for (String target : new HashSet<>(FAILED_OR_UNLOADED_MIXINS.values())) {
            try {
                LOGGER.info("Loading mixin target class: " + target);
                Class.forName(target);
            } catch (Exception e) {
                LOGGER.warn("Failed to load class: " + target + ". This is required to apply mixins!");
                e.printStackTrace();
            }
        }
        if (MIXINS_TO_LOAD.size() > 0) {
            LOGGER.fatal("Not all required mixins have been applied!");
            LOGGER.fatal("To prevent you from wasting your time, the process has ended.");
            LOGGER.fatal("");
            LOGGER.fatal("Required mixins that have not been applied:");
            for (Map.Entry<String, String> entry : MIXINS_TO_LOAD.entrySet()) {
                LOGGER.fatal("- " + entry.getKey() + " targeting: " + entry.getValue());
            }
            LOGGER.fatal("");
            LOGGER.fatal("This means that Tiquality will not function properly.");
            LOGGER.fatal("Make sure your versions are correct for Forge as well as SpongeForge.");
            LOGGER.fatal("");
            FMLCommonHandler.instance().exitJava(1, false);
        } else {
            LOGGER.info("All mixins have been applied. If they were not overridden by another mod, everything should work.");
        }
    }
}
