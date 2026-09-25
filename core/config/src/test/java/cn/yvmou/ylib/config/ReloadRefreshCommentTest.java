package cn.yvmou.ylib.config;

import cn.yvmou.ylib.ServiceLocator;
import cn.yvmou.ylib.YLibServices;
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
 * 重载（{@code reloadConfiguration}）也要刷新注释、补齐缺失的键。
 * <p>
 * 这条曾经不成立：{@code reloadConfiguration} 只跑 {@code load} + {@code validate}，不碰
 * {@code mergeMissingKeys}，于是「每次加载都刷新注释」在热重载这条路上是假的——
 * 启动时刷了，之后 {@code /reload} 多少次都不刷。这里把两条路径对齐的行为钉住。
 */
class ReloadRefreshCommentTest {

    @AutoConfiguration(configFile = "reload-comment.yml", version = "1.0.0")
    static class ReloadConfig {

        @ConfigValue(value = "annotated", description = "代码里的说明", refreshComment = true)
        private String annotated = "a";
    }

    @Test
    @DisplayName("重载后注释被刷新")
    void reloadRefreshesComment(@TempDir Path dataFolder) throws Exception {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        ConfigurationManager manager = ServiceLocator.locate(YLibServices.class, "CoreServices")
                .createConfigurationManager(plugin, mock(Logger.class));
        manager.registerConfiguration(ReloadConfig.class);

        // 手改注释，然后重载
        File file = new File(dataFolder.toFile(), "reload-comment.yml");
        Files.writeString(file.toPath(), Files.readString(file.toPath(), StandardCharsets.UTF_8)
                .replace("# 代码里的说明", "# 我改的"), StandardCharsets.UTF_8);
        assertEquals(List.of("# 我改的"), commentsBefore(Files.readAllLines(file.toPath()), "annotated"));

        manager.reloadConfiguration(ReloadConfig.class);

        assertEquals(List.of("# 代码里的说明"),
                commentsBefore(Files.readAllLines(file.toPath()), "annotated"),
                "重载没刷新注释（reloadConfiguration 漏掉了 mergeMissingKeys？）");
    }

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
        List<String> comments = new java.util.ArrayList<>();
        for (int i = index - 1; i >= 0 && lines.get(i).startsWith("#"); i--) {
            comments.add(0, lines.get(i));
        }
        return comments;
    }
}
