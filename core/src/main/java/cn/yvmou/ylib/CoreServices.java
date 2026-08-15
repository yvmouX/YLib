package cn.yvmou.ylib;

import cn.yvmou.ylib.command.CommandManager;
import cn.yvmou.ylib.command.CommandManagerImpl;
import cn.yvmou.ylib.config.ConfigurationManager;
import cn.yvmou.ylib.config.ConfigurationManagerImpl;
import cn.yvmou.ylib.logger.Logger;
import cn.yvmou.ylib.logger.LoggerImpl;
import org.bukkit.plugin.Plugin;

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
}
