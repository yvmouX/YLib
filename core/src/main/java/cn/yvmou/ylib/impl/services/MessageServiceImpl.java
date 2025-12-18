package cn.yvmou.ylib.impl.services;

import cn.yvmou.ylib.api.services.LoggerService;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息服务类 - 提供消息发送功能
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class MessageServiceImpl implements cn.yvmou.ylib.api.services.MessageService {
    
    private final Plugin plugin;
    private final LoggerService logger;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    /**
     * 构造函数
     * @param plugin 插件实例
     * @param logger 日志服务
     */
    public MessageServiceImpl(@NotNull Plugin plugin, @NotNull LoggerService logger) {
        this.plugin = plugin;
        this.logger = logger;
    }
    
    /**
     * 发送消息给指定发送者
     * @param sender 发送者
     * @param message 消息内容
     */
    public void sendMessage(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(colorize(message));
    }
    
    /**
     * 发送消息给指定玩家
     * @param player 玩家
     * @param message 消息内容
     */
    public void sendMessage(@NotNull Player player, @NotNull String message) {
        player.sendMessage(colorize(message));
    }
    
    /**
     * 发送消息给所有玩家
     * @param message 消息内容
     */
    public void broadcast(@NotNull String message) {
        Bukkit.broadcastMessage(colorize(message));
    }
    
    /**
     * 发送消息给控制台
     * @param message 消息内容
     */
    public void sendToConsole(@NotNull String message) {
        Bukkit.getConsoleSender().sendMessage(colorize(message));
    }
    
    /**
     * 发送消息给所有在线玩家
     * @param message 消息内容
     */
    public void sendToAllPlayers(@NotNull String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessage(player, message);
        }
    }
    
    /**
     * 发送消息给有权限的玩家
     * @param message 消息内容
     * @param permission 权限
     */
    public void sendToPlayersWithPermission(@NotNull String message, @NotNull String permission) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                sendMessage(player, message);
            }
        }
    }
    
    /**
     * 发送标题给玩家
     * @param player 玩家
     * @param title 标题
     * @param subtitle 副标题
     */
    public void sendTitle(@NotNull Player player, @NotNull String title, String subtitle) {
        player.sendTitle(colorize(title), colorize(subtitle));
    }
    
    /**
     * 发送标题给玩家
     * @param player 玩家
     * @param title 标题
     * @param subtitle 副标题
     * @param fadeIn 淡入时间（tick）
     * @param stay 停留时间（tick）
     * @param fadeOut 淡出时间（tick）
     */
    public void sendTitle(@NotNull Player player, @NotNull String title, String subtitle, 
                         int fadeIn, int stay, int fadeOut) {
        player.sendTitle(colorize(title), colorize(subtitle), fadeIn, stay, fadeOut);
    }
    
    /**
     * 发送ActionBar给玩家
     * @param player 玩家
     * @param message 消息内容
     */
    public void sendActionBar(@NotNull Player player, @NotNull String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colorize(message)));
    }
    
    /**
     * 发送ActionBar给所有玩家
     * @param message 消息内容
     */
    public void sendActionBarToAll(@NotNull String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendActionBar(player, message);
        }
    }
    
    /**
     * 发送消息列表给玩家
     * @param player 玩家
     * @param messages 消息列表
     */
    public void sendMessages(@NotNull Player player, @NotNull List<String> messages) {
        for (String message : messages) {
            sendMessage(player, message);
        }
    }
    
    /**
     * 发送消息列表给发送者
     * @param sender 发送者
     * @param messages 消息列表
     */
    public void sendMessages(@NotNull CommandSender sender, @NotNull List<String> messages) {
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }
    
    /**
     * 颜色化消息
     * @param message 原始消息
     * @return String 颜色化后的消息
     */
    public String colorize(@NotNull String message) {
        if (message == null) return "";
        
        // 处理十六进制颜色代码
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String replacement = ChatColor.of("#" + hexCode).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);
        
        // 处理传统颜色代码
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    /**
     * 去除颜色代码
     * @param message 原始消息
     * @return String 去除颜色代码后的消息
     */
    public String stripColor(@NotNull String message) {
        return ChatColor.stripColor(message);
    }
    
    /**
     * 获取插件前缀
     * @return String 插件前缀
     */
    public String getPrefix() {
        return "[" + plugin.getName() + "] ";
    }
    
    /**
     * 发送带前缀的消息
     * @param sender 发送者
     * @param message 消息内容
     */
    public void sendPrefixedMessage(@NotNull CommandSender sender, @NotNull String message) {
        sendMessage(sender, getPrefix() + message);
    }
    
    /**
     * 发送带前缀的消息给玩家
     * @param player 玩家
     * @param message 消息内容
     */
    public void sendPrefixedMessage(@NotNull Player player, @NotNull String message) {
        sendMessage(player, getPrefix() + message);
    }
    
    /**
     * 发送成功消息
     * @param sender 发送者
     * @param message 消息内容
     */
    public void sendSuccess(@NotNull CommandSender sender, @NotNull String message) {
        sendMessage(sender, "§a" + message);
    }
    
    /**
     * 发送错误消息
     * @param sender 发送者
     * @param message 消息内容
     */
    public void sendError(@NotNull CommandSender sender, @NotNull String message) {
        sendMessage(sender, "§c" + message);
    }
    
    /**
     * 发送警告消息
     * @param sender 发送者
     * @param message 消息内容
     */
    public void sendWarning(@NotNull CommandSender sender, @NotNull String message) {
        sendMessage(sender, "§e" + message);
    }
    
    /**
     * 发送信息消息
     * @param sender 发送者
     * @param message 消息内容
     */
    public void sendInfo(@NotNull CommandSender sender, @NotNull String message) {
        sendMessage(sender, "§b" + message);
    }
    
    // ========== API接口要求的方法 ==========
    
    /**
     * 格式化消息
     * @param message 消息模板
     * @param args 参数
     * @return 格式化后的消息
     */
    @Override
    @NotNull
    public String format(@NotNull String message, Object... args) {
        return String.format(message, args);
    }
    
    /**
     * 发送格式化消息给命令发送者
     * @param sender 命令发送者
     * @param message 消息模板
     * @param args 参数
     */
    @Override
    public void sendMessage(@NotNull CommandSender sender, @NotNull String message, Object... args) {
        sendMessage(sender, format(message, args));
    }
    
    /**
     * 发送格式化消息给玩家
     * @param player 玩家
     * @param message 消息模板
     * @param args 参数
     */
    @Override
    public void sendMessage(@NotNull Player player, @NotNull String message, Object... args) {
        sendMessage(player, format(message, args));
    }
    
    /**
     * 广播格式化消息给所有在线玩家
     * @param message 消息模板
     * @param args 参数
     */
    @Override
    public void broadcast(@NotNull String message, Object... args) {
        broadcast(format(message, args));
    }
    
    /**
     * 发送消息给多个玩家
     * @param players 玩家集合
     * @param message 消息
     */
    @Override
    public void sendMessage(@NotNull java.util.Collection<? extends Player> players, @NotNull String message) {
        for (Player player : players) {
            sendMessage(player, message);
        }
    }
    
    /**
     * 发送格式化消息给多个玩家
     * @param players 玩家集合
     * @param message 消息模板
     * @param args 参数
     */
    @Override
    public void sendMessage(@NotNull java.util.Collection<? extends Player> players, @NotNull String message, Object... args) {
        sendMessage(players, format(message, args));
    }
    
    /**
     * 发送消息给控制台
     * @param message 消息
     */
    @Override
    public void sendConsole(@NotNull String message) {
        sendToConsole(message);
    }
    
    /**
     * 发送格式化消息给控制台
     * @param message 消息模板
     * @param args 参数
     */
    @Override
    public void sendConsole(@NotNull String message, Object... args) {
        sendConsole(format(message, args));
    }
} 