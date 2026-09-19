package cn.yvmou.ylib;

import cn.yvmou.ylib.command.CommandManager;
import cn.yvmou.ylib.config.ConfigurationManager;
import cn.yvmou.ylib.logger.Logger;
import cn.yvmou.ylib.message.MessageService;
import cn.yvmou.ylib.message.MessageSettings;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * YLib 核心服务的工厂接口。
 * <p>
 * core 模块通过 {@code META-INF/services/cn.yvmou.ylib.YLibServices} 注册实现，
 * YLib 使用 {@link java.util.ServiceLoader} 发现它。相比 Class.forName 字符串常量，
 * ServiceLoader 在 Shadow 重定位（relocate）后依然可用。
 */
public interface YLibServices {

    Logger createLogger();

    ConfigurationManager createConfigurationManager(Plugin plugin, Logger logger);

    CommandManager createCommandManager(Plugin plugin, Logger logger);

    /**
     * 创建多语言消息服务。
     * default 方法保证第三方 SPI 实现向前兼容（不实现也不会编译失败）。
     */
    default MessageService createMessageService(@NotNull Plugin plugin, @NotNull Logger logger, @NotNull MessageSettings settings) {
        throw new UnsupportedOperationException("This YLibServices provider does not implement createMessageService");
    }
}
