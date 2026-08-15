package cn.yvmou.ylib;

import cn.yvmou.ylib.command.CommandManager;
import cn.yvmou.ylib.config.ConfigurationManager;
import cn.yvmou.ylib.logger.Logger;
import org.bukkit.plugin.Plugin;

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
}
