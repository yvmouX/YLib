package cn.yvmou.ylib.config;

import cn.yvmou.ylib.logger.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 字段类型绑定的「集合」一档：文件里所有集合都只能写成 YAML 列表，装不装得进字段由目标类型决定。
 * 数组曾经是漏网的一档——声明成 {@code String[]} 时加载器把 ArrayList 直接塞进字段，抛的是
 * 「Can not set [Ljava.lang.String; field … to java.util.ArrayList」，而且只在启动读配置那一刻出现
 * （第一次启动只写默认文件、不赋值，所以看不出来）。这里把「列表 ↔ 数组」两个方向都钉住：
 * 读进来要变成数组，写出去要是列表而不是 {@code [Ljava.lang.String;@…}。
 */
class ConfigurationLoaderTest {

    /** 测试用配置：数组与 List 各一份，默认值用来验证写出去的是什么形状。 */
    @AutoConfiguration(configFile = "array.yml", version = "1.0.0")
    static class ArrayConfig {

        @ConfigValue("rows")
        private String[] rows = {"default-a", "default-b"};

        @ConfigValue("numbers")
        private int[] numbers = {1, 2};

        @ConfigValue("names")
        private List<String> names = Arrays.asList("x");
    }

    /** 用来验证 description 里的 {@code \n} 会真的写成多行注释。 */
    @AutoConfiguration(configFile = "multi-line-comment.yml", version = "1.0.0")
    static class CommentedConfig {

        @ConfigValue(value = "plain", description = "单行说明")
        private String plain = "p";

        @ConfigValue(value = "multi", description = "第一行\n第二行\n第三行")
        private String multi = "m";
    }

    @Test
    @DisplayName("description 里的 \\n 写成多行注释，每行都有 # 前缀")
    void descriptionNewlinesBecomeSeparateCommentLines(@TempDir Path dataFolder) throws Exception {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        CommentedConfig instance = new CommentedConfig();
        new ConfigurationLoader(plugin, mock(Logger.class))
                .generateDefault(instance, new ConfigurationParser().parse(CommentedConfig.class));

        List<String> lines = Files.readAllLines(
                new File(dataFolder.toFile(), "multi-line-comment.yml").toPath());

        // Bukkit 的 setComments 只给第一个元素加前缀，所以这里必须逐行核对——
        // 整段塞进去的话只有第一行是注释，其余会变成裸文本。
        assertEquals(List.of("# 第一行", "# 第二行", "# 第三行"), beforeKey(lines, "multi"));
        assertEquals(List.of("# 单行说明"), beforeKey(lines, "plain"));
    }

    /** 取某个键上方紧邻的连续注释行。 */
    private static List<String> beforeKey(List<String> lines, String key) {
        int index = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(key + ":")) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new AssertionError("生成的文件里找不到键: " + key + " → " + lines);
        }
        List<String> comments = new ArrayList<>();
        for (int i = index - 1; i >= 0 && lines.get(i).startsWith("#"); i--) {
            comments.add(0, lines.get(i));
        }
        return comments;
    }

    @Test
    @DisplayName("YAML 列表能装进 String[] 与 int[]（按组件类型转换），List 字段照旧")
    void listLoadsIntoArrays(@TempDir Path dataFolder) throws Exception {
        FileConfiguration file = new YamlConfiguration();
        file.set("rows", Arrays.asList("#########", "####`quests`####"));
        file.set("numbers", Arrays.asList(3, 4, 5));
        file.set("names", Arrays.asList("a", "b"));
        file.save(configFile(dataFolder));

        ArrayConfig config = load(dataFolder);

        assertArrayEquals(new String[]{"#########", "####`quests`####"}, config.rows);
        assertArrayEquals(new int[]{3, 4, 5}, config.numbers);
        assertEquals(Arrays.asList("a", "b"), config.names);
    }

    @Test
    @DisplayName("生成默认文件时数组写成 YAML 列表，而不是数组的 toString")
    void defaultsAreWrittenAsList(@TempDir Path dataFolder) {
        loader(dataFolder).generateDefault(new ArrayConfig(), metadata());

        FileConfiguration written = YamlConfiguration.loadConfiguration(configFile(dataFolder));
        assertEquals(Arrays.asList("default-a", "default-b"), written.get("rows"));
        assertEquals(Arrays.asList(1, 2), written.get("numbers"));
    }

    @Test
    @DisplayName("save() 同样把数组写成列表（配置版本变化重建文件时走这条路）")
    void saveWritesArraysAsList(@TempDir Path dataFolder) throws Exception {
        ArrayConfig config = new ArrayConfig();
        config.rows = new String[]{"changed"};
        loader(dataFolder).save(config, metadata());

        FileConfiguration written = YamlConfiguration.loadConfiguration(configFile(dataFolder));
        assertEquals(Arrays.asList("changed"), written.get("rows"));
    }

    // ---------------------------------------------------------------- 辅助

    /** 加载器只用到 {@code getDataFolder()}，其余靠 mock 兜住。 */
    private static ConfigurationLoader loader(Path dataFolder) {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        return new ConfigurationLoader(plugin, mock(Logger.class));
    }

    private static ArrayConfig load(Path dataFolder) throws Exception {
        ArrayConfig config = new ArrayConfig();
        loader(dataFolder).load(config, metadata());
        return config;
    }

    private static ConfigurationMetadata metadata() {
        try {
            return new ConfigurationParser().parse(ArrayConfig.class);
        } catch (ConfigurationException e) {
            throw new AssertionError("解析测试用配置失败: " + e.getMessage(), e);
        }
    }

    private static File configFile(Path dataFolder) {
        return new File(dataFolder.toFile(), "array.yml");
    }
}
