package cn.yvmou.ylib.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 子命令注解
 * <p>
 * value 为子命令路径，空格分隔，支持多级字面量与参数占位符混写：
 * </p>
 * <ul>
 *   <li>{@code @SubCommand("reload")} —— 纯字面量子命令，方法上带 {@code @Arg} 的参数按声明顺序追加为参数节点</li>
 *   <li>{@code @SubCommand("give <player> <item>")} —— 占位符段按名称绑定到方法上对应的 {@code @Arg}
 *       参数（类型、补全、可选性随参数）；路径中未出现的 {@code @Arg} 参数仍追加在路径末端</li>
 *   <li>{@code @SubCommand("warp set <name>")} —— 多级字面量与占位符混写</li>
 * </ul>
 * 标注在嵌套类上时作为分组节点，路径中的占位符降级为字符串参数节点。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommand {
    /**
     * 子命令路径，空格分隔，支持多级字面量（如 "reload config"）与参数占位符（如 "give <player> <item>"）
     */
    String value();
    String permission() default "";
    String description() default "";
    /**
     * 权限默认值（可选，等价于 plugin.yml 中 permissions 段的 default），
     * 取值 "true"/"false"/"op"/"notop"；非空时 YLib 注册命令时自动向 Bukkit 注册该权限节点。
     * 为空表示不自动注册。注意：若 plugin.yml 已声明同名节点则以其为准。
     */
    String permissionDefault() default "";
    /**
     * 父权限聚合（可选）：同 {@link Command#permissionChildren()}，
     * 注册一个 children 均为 true 的父权限节点。
     */
    String[] permissionChildren() default {};
    /**
     * 指定要注册的父权限名（等价 plugin.yml 的聚合），优先级高于本节点 permission，
     * 不参与命令门禁。见 {@link Command#permissionParent()}。
     */
    String permissionParent() default "";
}
