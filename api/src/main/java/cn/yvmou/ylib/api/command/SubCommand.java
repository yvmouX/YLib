package cn.yvmou.ylib.api.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * 子命令接口
 * <p>
 * 用于定义子命令的执行逻辑。子命令可以通过 {@link CommandOptions} 注解
 * 进行配置，包括权限、使用限制、别名等。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public interface SubCommand {
    
    /**
     * 执行子命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     * @return 如果命令执行成功返回true，否则返回false
     */
    boolean execute(@NotNull CommandSender sender, @NotNull String[] args);
    
    /**
     * 获取命令名称
     * 
     * @return 命令名称
     */
    @NotNull
    default String getName() {
        CommandOptions options = getClass().getAnnotation(CommandOptions.class);
        if (options == null) {
            throw new IllegalStateException("子命令类必须使用 @CommandOptions 注解");
        }
        return options.name();
    }
    
    /**
     * 获取命令权限
     * 
     * @return 命令权限，如果不需要权限返回null
     */
    @Nullable
    default String getPermission() {
        CommandOptions options = getClass().getAnnotation(CommandOptions.class);
        if (options == null) {
            return null;
        }
        String permission = options.permission();
        return permission.isEmpty() ? null : permission;
    }
    
    /**
     * 获取命令别名
     * 
     * @return 命令别名数组
     */
    @NotNull
    default String[] getAliases() {
        CommandOptions options = getClass().getAnnotation(CommandOptions.class);
        if (options == null) {
            return new String[0];
        }
        return options.alias();
    }
    
    /**
     * 获取命令描述
     * 
     * @return 命令描述
     */
    @NotNull
    default String getDescription() {
        CommandOptions options = getClass().getAnnotation(CommandOptions.class);
        if (options == null) {
            return "";
        }
        return options.description();
    }
    
    /**
     * 获取命令用法
     * 
     * @return 命令用法
     */
    @NotNull
    default String getUsage() {
        CommandOptions options = getClass().getAnnotation(CommandOptions.class);
        if (options == null) {
            return "";
        }
        return options.usage();
    }
    
    /**
     * 检查是否只允许玩家执行
     * 
     * @return 如果只允许玩家执行返回true，否则返回false
     */
    default boolean isOnlyPlayer() {
        CommandOptions options = getClass().getAnnotation(CommandOptions.class);
        if (options == null) {
            return false;
        }
        return options.onlyPlayer();
    }
    
    /**
     * 检查命令是否启用
     * 
     * @return 如果命令启用返回true，否则返回false
     */
    default boolean isEnabled() {
        CommandOptions options = getClass().getAnnotation(CommandOptions.class);
        if (options == null) {
            return true;
        }
        return options.register();
    }
    
    /**
     * Tab补全
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     * @return 补全建议列表
     */
    @NotNull
    default List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }
}