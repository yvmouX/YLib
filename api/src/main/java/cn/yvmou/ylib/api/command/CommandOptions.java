package cn.yvmou.ylib.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 命令选项注解
 * <p>
 * 用于配置子命令的各种选项，包括名称、权限、使用限制、别名等。
 * 该注解应该用在实现了 {@link SubCommand} 接口的类上。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @CommandOptions(
 *     name = "reload",
 *     permission = "myplugin.admin.reload",
 *     onlyPlayer = false,
 *     alias = {"rl", "restart"},
 *     register = true,
 *     usage = "/myplugin reload",
 *     description = "重新加载插件配置"
 * )
 * public class ReloadCommand implements SubCommand {
 *     public boolean execute(CommandSender sender, String[] args) {
 *         // 命令逻辑
 *         return true;
 *     }
 * }
 * }</pre>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandOptions {
    
    /**
     * 命令名称
     * 
     * @return 命令名称
     */
    String name();
    
    /**
     * 执行命令所需的权限
     * 
     * @return 权限字符串，空字符串表示无需权限
     */
    String permission() default "";
    
    /**
     * 是否只允许玩家执行
     * 
     * @return 如果只允许玩家执行返回true，否则返回false
     */
    boolean onlyPlayer() default false;
    
    /**
     * 命令别名
     * 
     * @return 命令别名数组
     */
    String[] alias() default {};
    
    /**
     * 是否注册该命令
     * 
     * @return 如果注册该命令返回true，否则返回false
     */
    boolean register() default true;
    
    /**
     * 命令使用方法说明
     * 
     * @return 使用方法字符串
     */
    String usage() default "";
    
    /**
     * 命令描述
     * 
     * @return 命令描述
     */
    String description() default "";
}