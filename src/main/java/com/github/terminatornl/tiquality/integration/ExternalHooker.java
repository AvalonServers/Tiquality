package com.github.terminatornl.tiquality.integration;

import com.github.terminatornl.tiquality.Tiquality;
import com.github.terminatornl.tiquality.integration.ftbutilities.FTBUtilitiesHook;
import net.minecraftforge.fml.common.Loader;

import java.util.HashSet;

public class ExternalHooker {

    public static final HashSet<String> LOADED_HOOKS = new HashSet<>();

    public static void init() {
        if (Loader.isModLoaded("ftbutilities")) {
            Tiquality.LOGGER.info("FTB Utilities detected. Adding hooks...");
            LOADED_HOOKS.add("ftbutilities");
            FTBUtilitiesHook.init();
            Tiquality.LOGGER.info("Done.");
        }
    }
}
