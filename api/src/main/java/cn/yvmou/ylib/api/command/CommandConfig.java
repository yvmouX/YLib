package cn.yvmou.ylib.api.command;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CommandConfig {
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
    boolean isCommandEnabled(@NotNull String commandName, @NotNull String subCommandName);
}
