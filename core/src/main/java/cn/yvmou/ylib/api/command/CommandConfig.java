package cn.yvmou.ylib.api.command;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * 命令配置管理器
 * <p>
 * 负责管理命令的配置文件，包括自动生成配置、读取配置等功能。
 * 支持命令别名、权限、使用限制等配置。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0
 */
public interface CommandConfig {
    /**
     * 加载命令配置
     *
     * <p>
     *     加载命令配置到内存
     *     如果命令配置不存在 则创建一个
     *     否则则加载配置文件到内存
     * </p>
     *
     */
    void loadCommandConfiguration();

    /**
     * 初始化命令配置
     * <p>
     * 根据子命令的注解信息，在配置文件中创建相应的配置项。
     * 如果配置已存在，则不会覆盖。
     * </p>
     *
     * @param mainCommandName 主命令名称
     * @param subCommandMap 子命令映射，键为子命令名称，值为子命令实例
     */
    void initCommandConfigFromAnnotations(String mainCommandName, Map<String, SubCommand> subCommandMap);

    /**
     * 获取配置文件
     * @return 配置文件实例
     */
    FileConfiguration getConfig();

    /**
     * 获取子命令列表
     * @param mainCommand 主命令
     * @return 子命令列表
     */
    List<String> getSubCommandList(String mainCommand);
    /**
     * 获取命令别名列表
     *
     * @param commandName 主命令名称
     * @param subCommandName 子命令名称
     * @return 别名列表
     */
    @NotNull
    List<String> getCommandAliases(@NotNull String commandName, @NotNull String subCommandName);

    /**
     * 获取命令使用方法
     *
     * @param commandName 主命令名称
     * @param subCommandName 子命令名称
     * @return 使用方法字符串
     */
    @NotNull
    String getCommandUsage(@NotNull String commandName, @NotNull String subCommandName);

    /**
     * 检查命令是否只允许玩家执行
     *
     * @param commandName 主命令名称
     * @param subCommandName 子命令名称
     * @return 如果只允许玩家执行返回true，否则返回false
     */
    boolean isPlayerOnly(@NotNull String commandName, @NotNull String subCommandName);

    /**
     * 获取命令权限
     *
     * @param commandName 主命令名称
     * @param subCommandName 子命令名称
     * @return 权限字符串，可能为空
     */
    @NotNull
    String getCommandPermission(@NotNull String commandName, @NotNull String subCommandName);

    /**
     * 检查命令是否启用
     *
     * @param commandName 主命令名称
     * @param subCommandName 子命令名称
     * @return 如果启用返回true，否则返回false
     */
    boolean isCommandRegister(@NotNull String commandName, @NotNull String subCommandName);
}
