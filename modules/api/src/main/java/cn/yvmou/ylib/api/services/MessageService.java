package cn.yvmou.ylib.api.services;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * 消息服务接口
 * 
 * @author yvmou
 * @since 1.0.0-beta5
 */
public interface MessageService {
    
    /**
     * 格式化消息
     * 
     * @param message 消息模板
     * @param args 参数
     * @return 格式化后的消息
     */
    @NotNull
    String format(@NotNull String message, Object... args);
    
    /**
     * 发送消息给命令发送者
     * 
     * @param sender 命令发送者
     * @param message 消息
     */
    void sendMessage(@NotNull CommandSender sender, @NotNull String message);
    
    /**
     * 发送格式化消息给命令发送者
     * 
     * @param sender 命令发送者
     * @param message 消息模板
     * @param args 参数
     */
    void sendMessage(@NotNull CommandSender sender, @NotNull String message, Object... args);
    
    /**
     * 发送消息给玩家
     * 
     * @param player 玩家
     * @param message 消息
     */
    void sendMessage(@NotNull Player player, @NotNull String message);
    
    /**
     * 发送格式化消息给玩家
     * 
     * @param player 玩家
     * @param message 消息模板
     * @param args 参数
     */
    void sendMessage(@NotNull Player player, @NotNull String message, Object... args);
    
    /**
     * 广播消息给所有在线玩家
     * 
     * @param message 消息
     */
    void broadcast(@NotNull String message);
    
    /**
     * 广播格式化消息给所有在线玩家
     * 
     * @param message 消息模板
     * @param args 参数
     */
    void broadcast(@NotNull String message, Object... args);
    
    /**
     * 发送消息给多个玩家
     * 
     * @param players 玩家集合
     * @param message 消息
     */
    void sendMessage(@NotNull Collection<? extends Player> players, @NotNull String message);
    
    /**
     * 发送格式化消息给多个玩家
     * 
     * @param players 玩家集合
     * @param message 消息模板
     * @param args 参数
     */
    void sendMessage(@NotNull Collection<? extends Player> players, @NotNull String message, Object... args);
    
    /**
     * 发送标题给玩家
     * 
     * @param player 玩家
     * @param title 主标题
     * @param subtitle 副标题
     */
    void sendTitle(@NotNull Player player, @NotNull String title, @NotNull String subtitle);
    
    /**
     * 发送标题给玩家
     * 
     * @param player 玩家
     * @param title 主标题
     * @param subtitle 副标题
     * @param fadeIn 淡入时间（tick）
     * @param stay 停留时间（tick）
     * @param fadeOut 淡出时间（tick）
     */
    void sendTitle(@NotNull Player player, @NotNull String title, @NotNull String subtitle, 
                   int fadeIn, int stay, int fadeOut);
    
    /**
     * 发送ActionBar消息给玩家
     * 
     * @param player 玩家
     * @param message 消息
     */
    void sendActionBar(@NotNull Player player, @NotNull String message);
    
    /**
     * 发送消息给控制台
     * 
     * @param message 消息
     */
    void sendConsole(@NotNull String message);
    
    /**
     * 发送格式化消息给控制台
     * 
     * @param message 消息模板
     * @param args 参数
     */
    void sendConsole(@NotNull String message, Object... args);
}