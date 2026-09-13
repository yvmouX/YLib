package cn.yvmou.ylib;

import cn.yvmou.ylib.command.CommandManager;
import cn.yvmou.ylib.command.CommandManagerImpl;
import cn.yvmou.ylib.config.ConfigurationManager;
import cn.yvmou.ylib.config.ConfigurationManagerImpl;
import cn.yvmou.ylib.logger.Logger;
import cn.yvmou.ylib.logger.LoggerImpl;
import cn.yvmou.ylib.message.MessageService;
import cn.yvmou.ylib.message.MessageServiceImpl;
import cn.yvmou.ylib.message.MessageSettings;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * core 模块对 {@link YLibServices} 的默认实现，通过 ServiceLoader 注册。
 */
public class CoreServices implements YLibServices {

    @Override
    public Logger createLogger() {
        return new LoggerImpl();
    }

    @Override
    public ConfigurationManager createConfigurationManager(Plugin plugin, Logger logger) {
        return new ConfigurationManagerImpl(plugin, logger);
    }

    @Override
    public CommandManager createCommandManager(Plugin plugin, Logger logger) {
        return new CommandManagerImpl(plugin, logger);
    }

    @Override
    public MessageService createMessageService(@NotNull Plugin plugin, @NotNull Logger logger, @NotNull MessageSettings settings) {
        return new MessageServiceImpl(plugin, logger, settings);
    }
}
