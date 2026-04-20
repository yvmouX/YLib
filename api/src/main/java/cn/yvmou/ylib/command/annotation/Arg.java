package cn.yvmou.ylib.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数注解，用于指定参数名称和动态补全提供器
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Arg {
    /**
     * 参数名称
     */
    String value();

    /**
     * 动态补全提供器名称
     * <p>
     * 对应于注册在命令实例中的方法名，该方法必须返回 List<String>
     * 且接受 (CommandSender, CommandContext, String) 参数
     * </p>
     */
    String suggestion() default "";
}
