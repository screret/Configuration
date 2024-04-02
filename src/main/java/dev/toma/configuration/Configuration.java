package dev.toma.configuration;

import dev.toma.configuration.client.screen.ConfigGroupScreen;
import dev.toma.configuration.client.screen.ConfigScreen;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.format.ConfigFormats;
import dev.toma.configuration.config.format.IConfigFormatHandler;
import dev.toma.configuration.config.io.ConfigIO;
import dev.toma.configuration.config.value.ConfigValue;
import dev.toma.configuration.network.Networking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dedicated.DedicatedServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mod(Configuration.MODID)
public final class Configuration {

    public static final String MODID = "configuration";
    public static final Logger LOGGER = LogManager.getLogger("Configuration");
    public static final Marker MAIN_MARKER = MarkerManager.getMarker("main");

    public Configuration(IEventBus modEventBus) {
        modEventBus.addListener(this::init);
        modEventBus.addListener(Networking::registerPayloads);
        modEventBus.addListener(this::clientInit);
        NeoForge.EVENT_BUS.addListener(this::cleanUp);

        if (isDevelopmentEnvironment()) {
            registerConfig(TestingConfig.class, ConfigFormats.yaml());
        }
    }

    /**
     * Registers your config class. Config will be immediately loaded upon calling.
     *
     * @param cfgClass Your config class
     * @param formatFactory File format to be used by this config class. You can use values
     *                      from {@link ConfigFormats} for example.
     * @return Config holder containing your config instance. You obtain it by calling
     * {@link ConfigHolder#getConfigInstance()} method.
     * @param <CFG> Config type
     */
    public static <CFG> ConfigHolder<CFG> registerConfig(Class<CFG> cfgClass, IConfigFormatHandler formatFactory) {
        Config cfg = cfgClass.getAnnotation(Config.class);
        if (cfg == null) {
            throw new IllegalArgumentException("Config class must be annotated with '@Config' annotation");
        }
        String id = cfg.id();
        String filename = cfg.filename();
        if (filename.isEmpty()) {
            filename = id;
        }
        String group = cfg.group();
        if (group.isEmpty()) {
            group = id;
        }
        ConfigHolder<CFG> holder = new ConfigHolder<>(cfgClass, id, filename, group, formatFactory);
        ConfigHolder.registerConfig(holder);
        if (cfgClass.getAnnotation(Config.NoAutoSync.class) == null) {
            ConfigIO.FILE_WATCH_MANAGER.addTrackedConfig(holder);
        }
        return holder;
    }

    /**
     * You can obtain default config screen based on provided config class.
     *
     * @param configClass Your config class
     * @param previous Previously open screen
     * @return Either new config screen or {@code null} when no config exists for the provided class
     */
    @Nullable
    @OnlyIn(Dist.CLIENT)
    public static Screen getConfigScreen(Class<?> configClass, Screen previous) {
        Config cfg = configClass.getAnnotation(Config.class);
        if (cfg == null) {
            return null;
        }
        String id = cfg.id();
        return getConfigScreen(id, previous);
    }

    /**
     * You can obtain default config screen based on provided config ID.
     *
     * @param configId ID of your config
     * @param previous Previously open screen
     * @return Either new config screen or {@code null} when no config exists with the provided ID
     */
    @Nullable
    @OnlyIn(Dist.CLIENT)
    public static Screen getConfigScreen(String configId, Screen previous) {
        return ConfigHolder.getConfig(configId).map(holder -> {
            Map<String, ConfigValue<?>> valueMap = holder.getValueMap();
            return new ConfigScreen(configId, holder.getConfigId(), valueMap, previous);
        }).orElse(null);
    }

    /**
     * Obtain group of multiple configs based on group ID. This is useful when you have multiple config files
     * for your mod.
     *
     * @param group Group ID, usually mod ID
     * @param previous Previously open screen
     * @return Either new config group screen or null when no config exists under the provided group
     */
    @OnlyIn(Dist.CLIENT)
    public static Screen getConfigScreenByGroup(String group, Screen previous) {
        List<ConfigHolder<?>> list = ConfigHolder.getConfigsByGroup(group);
        if (list.isEmpty())
            return null;
        return getConfigScreenByGroup(list, group, previous);
    }

    @OnlyIn(Dist.CLIENT)
    private static Screen getConfigScreenByGroup(List<ConfigHolder<?>> group, String groupId, Screen previous) {
        return new ConfigGroupScreen(previous, groupId, group);
    }

    private void init(FMLCommonSetupEvent event) {
        ConfigIO.FILE_WATCH_MANAGER.startService();
    }

    private void cleanUp(ServerStoppingEvent event) {
        if (event.getServer() instanceof DedicatedServer) {
            ConfigIO.FILE_WATCH_MANAGER.stop();
        }
    }

    private void clientInit(FMLClientSetupEvent event) {
        Map<String, List<ConfigHolder<?>>> groups = ConfigHolder.getConfigGroupingByGroup();
        ModList modList = ModList.get();
        for (Map.Entry<String, List<ConfigHolder<?>>> entry : groups.entrySet()) {
            String modId = entry.getKey();
            Optional<? extends ModContainer> optional = modList.getModContainerById(modId);
            optional.ifPresent(modContainer -> {
                List<ConfigHolder<?>> list = entry.getValue();
                modContainer.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> {
                    if (list.size() == 1) {
                        return getConfigScreen(list.get(0).getConfigId(), screen);
                    }
                    return getConfigScreenByGroup(list, modId, screen);
                }));
            });
        }
    }

    private static boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }
}
