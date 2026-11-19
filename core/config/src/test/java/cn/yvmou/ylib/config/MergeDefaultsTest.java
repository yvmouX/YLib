package cn.yvmou.ylib.config;

import cn.yvmou.ylib.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 「默认值」只能是代码里的字段初始值，不能是内存里上一次加载进来的值。
 * <p>
 *     这两条曾经都是反的：{@code mergeMissingKeys} 直接读正在使用的实例，于是 {@code /reload}
 *     时用户删掉的键被旧值填回去；Map 还按内存里的旧 section 逐 key 补，删掉的条目永远回不来。
 *     顺带钉住 {@code registerConfiguration(instance)}：没有无参构造器的配置类也要能写全默认文件
 *     （实例化不出「默认实例」时退回注册的那个实例）。
 * </p>
 */
class MergeDefaultsTest {

    @AutoConfiguration(configFile = "merge-scalar.yml", version = "1.0.0")
    static class ScalarConfig {

        @ConfigValue("max-slots")
        private int maxSlots = 10;
    }

    @AutoConfiguration(configFile = "merge-map.yml", version = "1.0.0")
    static class MapConfig {

        @ConfigValue("slot-cost")
        private Map<String, Integer> slotCost = new LinkedHashMap<>();

        {
            slotCost.put("default", 1);
            slotCost.put("sharpness", 5);
        }
    }

    /** 没有无参构造器：只能走 {@code registerConfiguration(instance)}，默认值只能从实例本身读。 */
    @AutoConfiguration(configFile = "merge-no-noarg.yml", version = "1.0.0")
    static class NoNoArgConfig {

        @ConfigValue("a")
        private int a = 7;

        @ConfigValue("b")
        private String b = "bee";

        private NoNoArgConfig(String mode) {
            if (mode == null) {
                throw new IllegalArgumentException("mode");
            }
        }

        static NoNoArgConfig create() {
            return new NoNoArgConfig("manual");
        }
    }

    @Test
    @DisplayName("/reload 补回的标量是代码默认值，不是内存里上一次加载的旧值")
    void deletedScalarFallsBackToCodeDefault(@TempDir Path dataFolder) throws Exception {
        write(dataFolder, "merge-scalar.yml", "config-version: 1.0.0", "max-slots: 5");
        ConfigurationManager manager = manager(dataFolder);
        ScalarConfig config = manager.registerConfiguration(ScalarConfig.class);
        assertEquals(5, config.maxSlots, "首次加载应读文件里的 5");

        write(dataFolder, "merge-scalar.yml", "config-version: 1.0.0");
        manager.reloadConfiguration(ScalarConfig.class);

        assertEquals(10, config.maxSlots, "删掉键后应回到代码默认值 10，而不是内存里的 5");
        assertEquals(10, load(dataFolder, "merge-scalar.yml").getInt("max-slots"),
                "写回文件的也必须是代码默认值");
    }

    @Test
    @DisplayName("/reload 不会把用户删掉的 Map 条目补回来")
    void deletedMapEntryStaysDeleted(@TempDir Path dataFolder) throws Exception {
        write(dataFolder, "merge-map.yml", "config-version: 1.0.0",
                "slot-cost:", "  default: 1", "  sharpness: 5");
        ConfigurationManager manager = manager(dataFolder);
        MapConfig config = manager.registerConfiguration(MapConfig.class);
        assertEquals(Arrays.asList("default", "sharpness"), new ArrayList<>(config.slotCost.keySet()));

        write(dataFolder, "merge-map.yml", "config-version: 1.0.0", "slot-cost:", "  default: 1");
        manager.reloadConfiguration(MapConfig.class);

        assertEquals(Arrays.asList("default"), new ArrayList<>(config.slotCost.keySet()),
                "删掉的 sharpness 不该回到内存");
        assertEquals(Arrays.asList("default"), keysOf(load(dataFolder, "merge-map.yml"), "slot-cost"),
                "删掉的 sharpness 不该写回文件");
    }

    @Test
    @DisplayName("Map 节已存在时，首次注册也不逐 key 补齐（Map 归用户所有）")
    void existingMapSectionIsNotDeepFilled(@TempDir Path dataFolder) throws Exception {
        write(dataFolder, "merge-map.yml", "config-version: 1.0.0", "slot-cost:", "  default: 9");
        MapConfig config = manager(dataFolder).registerConfiguration(MapConfig.class);

        assertEquals(Arrays.asList("default"), new ArrayList<>(config.slotCost.keySet()),
                "代码默认里的 sharpness 不该被塞进已有的 section");
        assertEquals(9, config.slotCost.get("default"), "用户写的值要保持");
    }

    @Test
    @DisplayName("没有无参构造器的配置类：registerConfiguration(instance) 仍写出全部默认值")
    void registeredInstanceSuppliesDefaultsWhenNoNoArgConstructor(@TempDir Path dataFolder) {
        NoNoArgConfig config = manager(dataFolder).registerConfiguration(NoNoArgConfig.create());

        assertEquals(7, config.a);
        assertEquals("bee", config.b);
        FileConfiguration file = load(dataFolder, "merge-no-noarg.yml");
        assertEquals(7, file.getInt("a"), "默认文件里应有字段初始值，而不是被写成空文件");
        assertEquals("bee", file.getString("b"));
    }

    // ---------------------------------------------------------------- 辅助

    private static ConfigurationManager manager(Path dataFolder) {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        return new ConfigurationManagerImpl(plugin, mock(Logger.class));
    }

    private static void write(Path dataFolder, String name, String... lines) throws Exception {
        Files.write(new File(dataFolder.toFile(), name).toPath(),
                (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static FileConfiguration load(Path dataFolder, String name) {
        return YamlConfiguration.loadConfiguration(new File(dataFolder.toFile(), name));
    }

    private static List<String> keysOf(FileConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        return section == null ? new ArrayList<String>() : new ArrayList<>(section.getKeys(false));
    }
}
