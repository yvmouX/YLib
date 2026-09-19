package cn.yvmou.ylib.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link MessageService} 的构建配置。
 *
 * @author yvmou
 * @since 1.0.0-beta10
 */
public final class MessageSettings {

    private final String defaultLanguage;
    private final List<String> availableLanguages;
    private final String filePattern;
    private final String languageFolder;
    private final String prefixKey;
    private final boolean useClientLocale;
    private final Map<String, String> minecraftLanguageCodes;

    private MessageSettings(@NotNull Builder builder) {
        this.defaultLanguage = builder.defaultLanguage;
        this.availableLanguages = Collections.unmodifiableList(new ArrayList<>(builder.availableLanguages));
        this.filePattern = builder.filePattern;
        this.languageFolder = builder.languageFolder;
        this.prefixKey = builder.prefixKey;
        this.useClientLocale = builder.useClientLocale;
        this.minecraftLanguageCodes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.minecraftLanguageCodes));
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /** 默认语言代码（缺失键的最后回退目标） */
    @NotNull
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /** 可用语言代码列表 */
    @NotNull
    public List<String> getAvailableLanguages() {
        return availableLanguages;
    }

    /** 语言文件名模式，须含一个 %s 占位语言代码 */
    @NotNull
    public String getFilePattern() {
        return filePattern;
    }

    /**
     * 用户语言文件存放目录，相对插件数据文件夹；默认 {@code lang}（即
     * {@code plugins/<插件名>/lang/}）。null 或空串表示直接使用数据文件夹根目录（旧版行为），
     * 绝对路径表示使用该目录本身。
     * <p>
     * jar 内默认文件也会优先按 {@code <languageFolder>/<fileName>} 镜像查找，
     * 未命中再回退到资源根目录的 {@code <fileName>}。
     * </p>
     */
    @Nullable
    public String getLanguageFolder() {
        return languageFolder;
    }

    /** 前缀消息键（{@code send} 时拼接），null 表示不加前缀 */
    @Nullable
    public String getPrefixKey() {
        return prefixKey;
    }

    /** 是否按玩家客户端语言（Player#getLocale）解析消息 */
    public boolean isUseClientLocale() {
        return useClientLocale;
    }

    /** 插件语言代码 → Minecraft 客户端语言代码映射 */
    @NotNull
    public Map<String, String> getMinecraftLanguageCodes() {
        return minecraftLanguageCodes;
    }

    public static final class Builder {
        private String defaultLanguage = "en";
        private final List<String> availableLanguages = new ArrayList<>(Collections.singletonList("en"));
        private String filePattern = "lang_%s.yml";
        private String languageFolder = "lang";
        private String prefixKey = null;
        private boolean useClientLocale = false;
        private Map<String, String> minecraftLanguageCodes = defaultMinecraftLanguageCodes();

        private Builder() {
        }

        private static Map<String, String> defaultMinecraftLanguageCodes() {
            Map<String, String> codes = new LinkedHashMap<>();
            codes.put("en", "en_gb");
            codes.put("zh_CN", "zh_cn");
            return codes;
        }

        /** 默认语言（会自动加入可用语言列表） */
        @NotNull
        public Builder defaultLanguage(@NotNull String languageCode) {
            this.defaultLanguage = languageCode;
            return this;
        }

        /** 可用语言列表（覆盖默认） */
        @NotNull
        public Builder availableLanguages(@NotNull String... languageCodes) {
            this.availableLanguages.clear();
            Collections.addAll(this.availableLanguages, languageCodes);
            return this;
        }

        /** 语言文件名模式，默认 {@code lang_%s.yml}（仅文件名，目录由 {@link #languageFolder(String)} 指定） */
        @NotNull
        public Builder filePattern(@NotNull String pattern) {
            this.filePattern = pattern;
            return this;
        }

        /**
         * 用户语言文件存放目录，默认 {@code lang}（相对插件数据文件夹，
         * 生成到 {@code plugins/<插件名>/lang/}）。
         * 传 {@code null} 或空串则生成在数据文件夹根目录（旧版行为），
         * 传绝对路径则使用该目录（如跨插件共享语言目录）。
         * <p>
         * jar 内默认文件也会优先按此目录镜像查找，未命中回退资源根目录。
         * </p>
         */
        @NotNull
        public Builder languageFolder(@Nullable String folder) {
            this.languageFolder = folder;
            return this;
        }

        /** 前缀消息键（如 general.prefix）；不设置则 send 不加前缀 */
        @NotNull
        public Builder prefixKey(@Nullable String key) {
            this.prefixKey = key;
            return this;
        }

        /** 是否按玩家客户端语言解析消息，默认 false */
        @NotNull
        public Builder useClientLocale(boolean enabled) {
            this.useClientLocale = enabled;
            return this;
        }

        /** 整体替换语言代码映射 */
        @NotNull
        public Builder minecraftLanguageCodes(@NotNull Map<String, String> codes) {
            this.minecraftLanguageCodes = new LinkedHashMap<>(codes);
            return this;
        }

        /** 追加一条语言代码映射 */
        @NotNull
        public Builder addMinecraftLanguageCode(@NotNull String pluginLanguage, @NotNull String minecraftLanguage) {
            this.minecraftLanguageCodes.put(pluginLanguage, minecraftLanguage);
            return this;
        }

        @NotNull
        public MessageSettings build() {
            if (defaultLanguage == null || defaultLanguage.isEmpty()) {
                throw new IllegalArgumentException("defaultLanguage must not be empty");
            }
            if (filePattern == null || !filePattern.contains("%s")) {
                throw new IllegalArgumentException("filePattern must contain a %s placeholder for the language code");
            }
            if (!availableLanguages.contains(defaultLanguage)) {
                availableLanguages.add(0, defaultLanguage);
            }
            return new MessageSettings(this);
        }
    }
}
