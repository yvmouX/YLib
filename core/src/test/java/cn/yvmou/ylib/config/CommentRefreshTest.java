package cn.yvmou.ylib.config;

import cn.yvmou.ylib.logger.Logger;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 注释按 {@code refreshComment} 刷新。
 * <p>
 * 默认行为（不写这个成员）是「注释只写一次」：键已存在时不动它，服主改了注释也不会被覆盖回去。
 * 写 {@code refreshComment = true} 的字段反过来——每次加载都按 {@code description} 重写，
 * 让说明能跟着插件版本更新。三种例外/边界都在这里钉住：
 * <ul>
 *   <li><b>没写 refreshComment 的字段不能被刷新</b>——否则「不想被覆盖」这个语义就没了；</li>
 *   <li><b>注释里写 {@code @keep} 的整块跳过</b>——服主写「本服特有约定」的唯一去处；</li>
 *   <li><b>注释已经一致时不写盘</b>——不然每次启动 mtime 都变，用户会以为配置被动了。</li>
 * </ul>
 */
class CommentRefreshTest {

    /** refresh 标了 true，plain 没标（对照组），kept 用来验证 @keep。 */
    @AutoConfiguration(configFile = "comment-refresh.yml", version = "1.0.0")
    static class CommentedConfig {

        @ConfigValue(value = "refresh", description = "代码里的说明", refreshComment = true)
        private String refresh = "r";

        @ConfigValue(value = "plain", description = "代码里的说明")
        private String plain = "p";
    }

    @Test
    @DisplayName("refreshComment = true：文件里被改过的注释会被写回 description")
    void refreshOverwritesEditedComment(@TempDir Path dataFolder) throws Exception {
        writeFile(dataFolder, "refresh: r", "# 我随便改的说明");

        merge(dataFolder);

        assertEquals(List.of("# 代码里的说明"), commentsBefore(lines(dataFolder), "refresh"));
    }

    @Test
    @DisplayName("没写 refreshComment：文件里的注释保持原样")
    void withoutFlagCommentIsLeftAlone(@TempDir Path dataFolder) throws Exception {
        writeFile(dataFolder, "plain: p", "# 服主自己的说明");

        merge(dataFolder);

        assertEquals(List.of("# 服主自己的说明"), commentsBefore(lines(dataFolder), "plain"));
    }

    @Test
    @DisplayName("@keep：整块注释跳过刷新（标记行本身也留着）")
    void keptCommentIsNotRefreshed(@TempDir Path dataFolder) throws Exception {
        writeFile(dataFolder, "refresh: r", "# 本服约定，别动  @keep");

        merge(dataFolder);

        assertEquals(List.of("# 本服约定，别动  @keep"), commentsBefore(lines(dataFolder), "refresh"));
    }

    @Test
    @DisplayName("注释已经一致时不写盘（mtime 不变）")
    void noWriteWhenCommentAlreadyMatches(@TempDir Path dataFolder) throws Exception {
        // 两个键都得在文件里：缺的键会被补齐（连带写注释），那样测的就不是「一致时不写」了
        Files.writeString(new File(dataFolder.toFile(), "comment-refresh.yml").toPath(),
                String.join("\n",
                        "config-version: 1.0.0",
                        "# 代码里的说明",
                        "refresh: r",
                        "# 代码里的说明",
                        "plain: p",
                        ""), StandardCharsets.UTF_8);
        File file = new File(dataFolder.toFile(), "comment-refresh.yml");
        long before = file.lastModified();

        merge(dataFolder);

        assertEquals(before, file.lastModified(), "注释没变却重写了文件（每次启动 mtime 都会变）");
    }

    @Test
    @DisplayName("多行 description 刷新后仍是多行（\n 会拆成多条注释）")
    void multiLineDescriptionStaysMultiLine(@TempDir Path dataFolder) throws Exception {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        ConfigurationLoader loader = new ConfigurationLoader(plugin, mock(Logger.class));
        ConfigurationMetadata metadata = new ConfigurationParser().parse(MultiLine.class);

        File file = new File(dataFolder.toFile(), "multi-line-refresh.yml");
        Files.writeString(file.toPath(), String.join("\n",
                "config-version: 1.0.0",
                "# 旧的",
                "v: x",
                ""), StandardCharsets.UTF_8);

        loader.mergeMissingKeys(new MultiLine(), metadata);

        assertEquals(List.of("# 第一行", "# 第二行"), commentsBefore(lines(dataFolder, "multi-line-refresh.yml"), "v"));
    }

    @AutoConfiguration(configFile = "multi-line-refresh.yml", version = "1.0.0")
    static class MultiLine {
        @ConfigValue(value = "v", description = "第一行\n第二行", refreshComment = true)
        private String v = "x";
    }

    // ---------------------------------------------------------------- 辅助

    /** 写一份「注释在键上方」的文件：{@code config-version} + comment + keyLine。 */
    private static void writeFile(Path dataFolder, String keyLine, String comment) throws Exception {
        Files.writeString(new File(dataFolder.toFile(), "comment-refresh.yml").toPath(),
                String.join("\n", "config-version: 1.0.0", comment, keyLine, ""), StandardCharsets.UTF_8);
    }

    private static void merge(Path dataFolder) throws Exception {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        new ConfigurationLoader(plugin, mock(Logger.class))
                .mergeMissingKeys(new CommentedConfig(), new ConfigurationParser().parse(CommentedConfig.class));
    }

    private static List<String> lines(Path dataFolder) throws Exception {
        return lines(dataFolder, "comment-refresh.yml");
    }

    private static List<String> lines(Path dataFolder, String name) throws Exception {
        return Files.readAllLines(new File(dataFolder.toFile(), name).toPath(), StandardCharsets.UTF_8);
    }

    /** 取某个键上方紧邻的连续注释行。 */
    private static List<String> commentsBefore(List<String> lines, String key) {
        int index = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(key + ":")) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new AssertionError("文件里找不到键: " + key + " → " + lines);
        }
        java.util.List<String> comments = new java.util.ArrayList<>();
        for (int i = index - 1; i >= 0 && lines.get(i).startsWith("#"); i--) {
            comments.add(0, lines.get(i));
        }
        return comments;
    }
}
