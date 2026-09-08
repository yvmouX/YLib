package cn.yvmou.ylib.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 主命令注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String name();
    String[] aliases() default {};
    String description() default "";
    String permission() default "";
    /**
     * 权限默认值（可选，等价于 plugin.yml 中 permissions 段的 default），
     * 取值 "true"/"false"/"op"/"notop"；非空时 YLib 注册命令时自动向 Bukkit 注册该权限节点。
     * 为空表示不自动注册，保持旧行为。注意：若 plugin.yml 已声明同名节点则以其为准。
     */
    String permissionDefault() default "";
}
