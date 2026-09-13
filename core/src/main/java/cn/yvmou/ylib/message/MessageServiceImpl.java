package cn.yvmou.ylib.message;

import cn.yvmou.ylib.logger.Logger;
import cn.yvmou.ylib.text.TextRenderer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多语言消息服务实现。
 * <p>
 * 每个语言维护两份配置：jar 内默认文件（回退与补键来源）与用户文件（优先，默认生成在
 * 插件数据文件夹的 {@code lang/} 子目录，可通过 {@link MessageSettings.Builder#languageFolder(String)} 自定义）。
 * 用户文件缺失键会从 jar 默认补齐（含注释），仅在需要时保存。
 * </p>
 */
public class MessageServiceImpl implements MessageService {

    private final Plugin plugin;
    private final Logger logger;
    private final MessageSettings settings;

    /** 当前全局语言（已规范为 settings 中声明的写法） */
    private volatile String currentLanguage;

    /** 用户文件（数据文件夹语言目录），按语言代码缓存 */
    private final Map<String, FileConfiguration> userConfigs = new ConcurrentHashMap<>();
    /** jar 内默认文件，按语言代码缓存 */
    private final Map<String, FileConfiguration> defaultConfigs = new ConcurrentHashMap<>();

    public MessageServiceImpl(@NotNull Plugin plugin, @NotNull Logger logger, @NotNull MessageSettings settings) {
        this.plugin = plugin;
        this.logger = logger;
        this.settings = settings;
        this.currentLanguage = settings.getDefaultLanguage();

        for (String code : settings.getAvailableLanguages()) {
            loadLanguage(code);
        }
    }

    @Override
    @NotNull
    public String getLanguage() {
        return currentLanguage;
    }

    @Override
    public boolean setLanguage(@NotNull String languageCode) {
        String canonical = canonicalize(languageCode);
        if (canonical == null) {
            logger.warn("不支持的语言: {}，可用语言: {}", languageCode, settings.getAvailableLanguages());
            return false;
        }
        loadLanguage(canonical);
        this.currentLanguage = canonical;
        return true;
    }

    @Override
    @NotNull
    public List<String> getAvailableLanguages() {
        return settings.getAvailableLanguages();
    }

    @Override
    public boolean has(@NotNull String key) {
        return lookup(currentLanguage, key) != null || lookup(settings.getDefaultLanguage(), key) != null;
    }

    @Override
    @NotNull
    public String raw(@NotNull String key, @NotNull Object... args) {
        return resolveMessage(currentLanguage, key, args);
    }

    @Override
    @NotNull
    public String raw(@NotNull CommandSender sender, @NotNull String key, @NotNull Object... args) {
        return resolveMessage(resolveLanguage(sender), key, args);
    }

    @Override
    public void send(@NotNull CommandSender sender, @NotNull String key, @NotNull Object... args) {
        String lang = resolveLanguage(sender);
        String message = resolveMessage(lang, key, args);
        String prefix = settings.getPrefixKey() == null ? "" : lookupOrEmpty(lang, settings.getPrefixKey());
        sender.sendMessage(prefix + message);
    }

    @Override
    public void sendRaw(@NotNull CommandSender sender, @NotNull String message) {
        String prefix = settings.getPrefixKey() == null
            ? ""
            : lookupOrEmpty(resolveLanguage(sender), settings.getPrefixKey());
        sender.sendMessage(prefix + message);
    }

    @Override
    @NotNull
    public String prefix() {
        return settings.getPrefixKey() == null ? "" : lookupOrEmpty(currentLanguage, settings.getPrefixKey());
    }

    @Override
    @NotNull
    public String getMinecraftLanguageCode(@NotNull String languageCode) {
        String mapped = settings.getMinecraftLanguageCodes().get(languageCode);
        if (mapped != null) {
            return mapped;
        }
        return languageCode.toLowerCase().replace('-', '_');
    }

    @Override
    public void reload() {
        userConfigs.clear();
        defaultConfigs.clear();
        for (String code : settings.getAvailableLanguages()) {
            loadLanguage(code);
        }
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Methods
       └─────────────────────────────────────────────────────────────────┘
     */

    /**
     * 加载一个语言：用户文件缺失时从 jar 默认生成；
     * jar 默认中存在而用户文件缺失的键自动补齐。
     */
    private void loadLanguage(@NotNull String code) {
        if (userConfigs.containsKey(code)) {
            return;
        }
        String fileName = String.format(settings.getFilePattern(), code);
        File directory = languageDirectory();
        File file = new File(directory, fileName);

        // jar 内默认文件（作为补键与回退来源）
        FileConfiguration defaultConfig = loadJarDefault(fileName);
        defaultConfigs.put(code, defaultConfig);

        // 用户文件不存在时从 jar 默认生成
        if (!file.exists()) {
            try (InputStream resource = openJarDefault(fileName)) {
                if (resource == null) {
                    logger.warn("语言文件 {} 在 jar 内不存在，跳过加载语言 {}", fileName, code);
                    return;
                }
                writeTo(resource, file);
                logger.info("已生成语言文件: {}", file.getPath());
            } catch (IOException e) {
                logger.warn("生成语言文件失败 {}: {}", file.getPath(), e.getMessage());
            }
        }

        // 用户文件 + 缺失键合并
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
        boolean needsSave = false;
        for (String key : defaultConfig.getKeys(true)) {
            if (defaultConfig.isConfigurationSection(key)) {
                continue; // 中间节点由子键自然创建
            }
            if (!userConfig.contains(key)) {
                userConfig.set(key, defaultConfig.get(key));
                copyComment(defaultConfig, userConfig, key);
                needsSave = true;
            }
        }
        if (needsSave) {
            try {
                userConfig.save(file);
                logger.info("已补齐语言文件缺失键: {}", file.getPath());
            } catch (IOException e) {
                logger.warn("保存语言文件失败 {}: {}", file.getPath(), e.getMessage());
            }
        }
        userConfigs.put(code, userConfig);
    }

    /**
     * 用户语言文件目录：null/空串 → 数据文件夹根目录（旧版行为）；
     * 绝对路径 → 原样使用；其余按相对插件数据文件夹解析（默认 {@code lang}）。
     */
    @NotNull
    private File languageDirectory() {
        String folder = settings.getLanguageFolder();
        if (folder == null || folder.isEmpty()) {
            return plugin.getDataFolder();
        }
        File dir = new File(folder);
        return dir.isAbsolute() ? dir : new File(plugin.getDataFolder(), folder);
    }

    /**
     * 读取 jar 内默认文件：优先镜像语言目录（相对时），未命中回退到资源根目录。
     * 两处都不存在返回空配置（该语言无 jar 默认，仅由用户文件提供）。
     */
    @NotNull
    private FileConfiguration loadJarDefault(@NotNull String fileName) {
        try (InputStream resource = openJarDefault(fileName)) {
            if (resource == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.warn("读取 jar 默认语言文件失败 {}: {}", fileName, e.getMessage());
            return new YamlConfiguration();
        }
    }

    /**
     * 打开 jar 内默认语言文件：语言目录为相对路径时先尝试 {@code <folder>/<fileName>}，
     * 未命中再尝试资源根目录的 {@code <fileName>}（兼容旧版打包）；绝对目录只查根目录。
     */
    @Nullable
    private InputStream openJarDefault(@NotNull String fileName) {
        String folder = settings.getLanguageFolder();
        if (folder != null && !folder.isEmpty()) {
            File dir = new File(folder);
            if (!dir.isAbsolute()) {
                String mirror = folder.replace('\\', '/') + "/" + fileName;
                InputStream resource = plugin.getResource(mirror);
                if (resource != null) {
                    return resource;
                }
            }
        }
        return plugin.getResource(fileName);
    }

    /**
     * 将 jar 资源内容写入目标文件（自动创建父目录）。
     */
    private void writeTo(@NotNull InputStream in, @NotNull File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent);
        }
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    /**
     * 语言代码规范化：在可用语言中忽略大小写精确匹配，未命中返回 null。
     */
    @Nullable
    private String canonicalize(@NotNull String languageCode) {
        for (String code : settings.getAvailableLanguages()) {
            if (code.equalsIgnoreCase(languageCode)) {
                return code;
            }
        }
        return null;
    }

    /**
     * 解析 sender 应使用的语言：开启 useClientLocale 且为玩家时按客户端语言匹配
     * （精确匹配 → 语言段匹配，en_us → en），否则回退全局语言。
     */
    @NotNull
    private String resolveLanguage(@NotNull CommandSender sender) {
        if (!settings.isUseClientLocale() || !(sender instanceof Player)) {
            return currentLanguage;
        }
        String locale = ((Player) sender).getLocale();
        if (locale == null || locale.isEmpty()) {
            return currentLanguage;
        }
        String normalized = locale.replace('-', '_');

        String exact = canonicalize(normalized);
        if (exact != null) {
            return exact;
        }
        int underscore = normalized.indexOf('_');
        if (underscore > 0) {
            String languagePart = canonicalize(normalized.substring(0, underscore));
            if (languagePart != null) {
                return languagePart;
            }
        }
        return currentLanguage;
    }

    /**
     * 键查找链：当前语言用户文件 → 当前语言 jar 默认 → null。
     */
    @Nullable
    private String lookup(@NotNull String lang, @NotNull String key) {
        FileConfiguration user = userConfigs.get(lang);
        if (user != null && user.contains(key)) {
            return user.getString(key);
        }
        FileConfiguration def = defaultConfigs.get(lang);
        if (def != null && def.contains(key)) {
            return def.getString(key);
        }
        return null;
    }

    @NotNull
    private String resolveMessage(@NotNull String lang, @NotNull String key, @NotNull Object... args) {
        String message = lookup(lang, key);
        if (message == null && !lang.equals(settings.getDefaultLanguage())) {
            message = lookup(settings.getDefaultLanguage(), key);
        }
        if (message == null) {
            logger.warn("缺少语言键: {}", key);
            return "Missing message: " + key;
        }
        return colorize(format(message, args));
    }

    /**
     * 宽松查找：缺失返回空串（前缀等非正文场景使用，不告警）。
     */
    @NotNull
    private String lookupOrEmpty(@NotNull String lang, @NotNull String key) {
        String message = lookup(lang, key);
        if (message == null && !lang.equals(settings.getDefaultLanguage())) {
            message = lookup(settings.getDefaultLanguage(), key);
        }
        return message != null ? colorize(message) : "";
    }

    /**
     * 替换 {0}、{1}… 数字占位符；未提供的占位符静默移除。
     */
    @NotNull
    private String format(@NotNull String message, @NotNull Object... args) {
        if (args.length == 0) {
            return message;
        }
        String result = message;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return result.replaceAll("\\{\\d+}", "");
    }

    @NotNull
    private String colorize(@NotNull String message) {
        // 交给 TextRenderer：MiniMessage 标签、& 码、§ 码都能处理，输出统一的 § 色码。
        // 以前这里只有 ChatColor.translateAlternateColorCodes，语言文件里写 <gray> 会被原样
        // 发给玩家显示成标签文本——那是所有消费方都要各自包一层的原因。
        return TextRenderer.render(message);
    }

    /**
     * 复制默认文件中某键的注释到用户文件（1.18.1+ 生效，旧版本静默降级）。
     */
    private void copyComment(@NotNull FileConfiguration from, @NotNull FileConfiguration to, @NotNull String key) {
        try {
            Object comments = from.getClass().getMethod("getComments", String.class).invoke(from, key);
            if (comments instanceof List && !((List<?>) comments).isEmpty()) {
                to.getClass().getMethod("setComments", String.class, List.class).invoke(to, key, comments);
            }
        } catch (Exception ignored) {
            // 注释 API 不可用时静默降级
        }
    }
}
