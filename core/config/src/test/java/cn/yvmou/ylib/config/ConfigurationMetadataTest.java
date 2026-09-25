package cn.yvmou.ylib.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code configFile} 的路径语义：相对插件数据目录、可含子目录、不写 {@code .yml} 自动补。
 * 这层解析错了不会抛异常，只会把文件默默写到另一个位置（用户看到的现象是「配置没生成」），因此用测试钉住。
 */
class ConfigurationMetadataTest {

    @Test
    @DisplayName("不写扩展名时补 .yml；写了 .yml / .yaml 原样保留")
    void appendsMissingYamlExtension() {
        assertEquals("config.yml", ConfigurationMetadata.normalizeConfigFile("config"));
        assertEquals("config.yml", ConfigurationMetadata.normalizeConfigFile("config.yml"));
        assertEquals("settings.yaml", ConfigurationMetadata.normalizeConfigFile("settings.yaml"));
        assertEquals("config.YML", ConfigurationMetadata.normalizeConfigFile("config.YML"),
                "后缀判断忽略大小写，但路径本身保持原样");
    }

    @Test
    @DisplayName("子目录照写进路径：gui/gui → gui/gui.yml")
    void keepsSubDirectories() {
        assertEquals("gui/gui.yml", ConfigurationMetadata.normalizeConfigFile("gui/gui"));
        assertEquals("gui/gui.yml", ConfigurationMetadata.normalizeConfigFile("gui/gui.yml"));
        assertEquals("a/b/c.yaml", ConfigurationMetadata.normalizeConfigFile("a/b/c.yaml"));
    }

    @Test
    @DisplayName("反斜杠统一成斜杠，免得同一份配置在 Windows 与 Linux 上落到两个文件")
    void normalizesSeparators() {
        assertEquals("gui/gui.yml", ConfigurationMetadata.normalizeConfigFile("gui\\gui"));
        assertEquals("gui/gui.yml", ConfigurationMetadata.normalizeConfigFile("gui\\gui.yml"));
    }

    @Test
    @DisplayName("其它扩展名也照样补 .yml：注解里的路径就是最终文件名的底稿")
    void appendsAfterOtherExtensions() {
        assertEquals("settings.json.yml", ConfigurationMetadata.normalizeConfigFile("settings.json"));
    }

    @Test
    @DisplayName("元数据构造时就已归一化：加载 / 生成 / 备份 / 迁移都只看 configFile")
    void metadataHoldsNormalizedPath() {
        ConfigurationMetadata metadata = new ConfigurationMetadata(
                ConfigurationMetadataTest.class, "gui", "gui/gui", true, "NONE");

        assertEquals("gui/gui.yml", metadata.configFile);
    }
}
