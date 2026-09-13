package cn.yvmou.ylib.message;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 多语言消息服务。
 * <p>
 * 语言文件从插件 jar 内资源与数据文件夹双向管理：用户文件默认生成在数据文件夹的
 * {@code lang/} 子目录（可用 {@link MessageSettings.Builder#languageFolder(String)} 自定义位置），
 * 缺失时从 jar 默认生成/复制；jar 默认文件中存在而用户文件缺失的键会在加载时自动补齐
 * （保留用户改动）。
 * </p>
 * <p>
 * 消息占位符使用数字风格 {@code {0}、{1}…}，调用时按序传入；
 * 未提供的占位符会被静默移除。
 * </p>
 * <p>
 * 文本格式采用 <b>MiniMessage 为主、兼容传统颜色码</b>：语言文件里写
 * {@code <gray>} 标签、{@code &a} 或 {@code §a} 都能正确渲染，三种写法可以任意混排；
 * 所有出口（含 {@link #raw(String, Object...)} 与 {@link #prefix()}）返回的都是
 * 已渲染好的 {@code §} 色码字符串，调用方<b>不需要也不应该</b>再渲染一次。
 * </p>
 * <p>
 * 参数<b>不</b>参与渲染：{@code {0}} 位置传入的内容按字面插入（其中的 {@code §} 会保留），
 * 因此可以直接传玩家名、物品名这类来自游戏内的字符串。
 * 需要渲染任意文本请用 {@code cn.yvmou.ylib.text.TextRenderer}。
 * </p>
 * <p>
 * 键查找链：当前语言用户文件 → 当前语言 jar 默认 → 默认语言 → 缺失告警。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta10
 */
public interface MessageService {

    /**
     * 获取当前全局语言代码
     */
    @NotNull String getLanguage();

    /**
     * 切换全局语言
     *
     * @param languageCode 语言代码（须在 {@link MessageSettings} 声明的可用语言内，忽略大小写）
     * @return 是否切换成功
     */
    boolean setLanguage(@NotNull String languageCode);

    /**
     * 获取可用语言代码列表
     */
    @NotNull List<String> getAvailableLanguages();

    /**
     * 判断键是否存在（在当前语言或回退链上）
     */
    boolean has(@NotNull String key);

    /**
     * 获取消息（不带前缀，已替换占位符并渲染为 {@code §} 色码）。
     * 缺失时输出告警并返回 {@code "Missing message: <key>"}。
     */
    @NotNull String raw(@NotNull String key, @NotNull Object... args);

    /**
     * 同 {@link #raw(String, Object...)}，
     * 但 {@link MessageSettings.Builder#useClientLocale(boolean)} 开启时按 sender 的客户端语言解析。
     */
    @NotNull String raw(@NotNull CommandSender sender, @NotNull String key, @NotNull Object... args);

    /**
     * 向发送者发送带前缀的消息（未配置 prefixKey 时不加前缀）。
     * useClientLocale 开启时按玩家客户端语言解析。
     */
    void send(@NotNull CommandSender sender, @NotNull String key, @NotNull Object... args);

    /**
     * 向发送者发送 前缀 + 自定义文本。
     * <p>
     * 文本同样会走渲染（MiniMessage / {@code &} / {@code §} 均可），因此传入原始
     * 文本即可；已经渲染过的 {@code §} 色码再渲染一次也不会被破坏。
     */
    void sendRaw(@NotNull CommandSender sender, @NotNull String message);

    /**
     * 获取配置的前缀（未配置 prefixKey 时返回空字符串）
     */
    @NotNull String prefix();

    /**
     * 插件语言代码 → Minecraft 客户端语言代码（如 en → en_gb）。
     * 未映射时返回小写化处理的语言代码。
     */
    @NotNull String getMinecraftLanguageCode(@NotNull String languageCode);

    /**
     * 重载所有语言文件（重读 jar 默认与用户文件，并补齐缺失键）
     */
    void reload();
}
