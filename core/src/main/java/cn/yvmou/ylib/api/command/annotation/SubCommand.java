package cn.yvmou.ylib.api.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 子命令注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommand {
    /**
     * 子命令路径，支持空格分隔，如 "give <player> <item>"
     */
    String value();
    String permission() default "";
    String description() default "";
}
