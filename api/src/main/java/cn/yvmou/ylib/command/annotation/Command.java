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
    /**
     * 父权限聚合（可选）：声明本节点需要聚合的父子权限名及其默认 true。
     * YLib 会把本节点对应权限（或 permissionParent 指定的名字）注册为父节点，
     * children 为 true —— 授予父权限即自动拥有全部子权限（等价 plugin.yml 的 children）。
     */
    String[] permissionChildren() default {};
    /**
     * 当本节点自身不打算设置 permission（避免命令被门禁），
     * 但你又想注册一个"父节点聚合"时，用它指定要注册的父权限名。
     * 优先级高于 node.permission（仅用于该节点的权限注册，不影响命令门禁）。
     */
    String permissionParent() default "";
}
