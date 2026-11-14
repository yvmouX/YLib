package cn.yvmou.ylib.menu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在 {@link MenuTemplate} 实现类上：声明**这个菜单的配置文件怎么生成与读取**。
 * <p>
 * 不标这个注解的模板也能用，只是「外观全部写死在代码里」，没有可覆盖的配置文件。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MenuConfig {

    /**
     * 文件不存在时是否写入一份默认骨架。
     * <p>
     * {@code true}（默认）：文件不存在 → 生成；<b>已存在则绝不覆盖</b>（服主改过的配置不能被升级冲掉）。
     * {@code false}：不生成，但**仍会尝试读取**同名文件做覆盖——给「配置由运维预置 / 版本控制」的场景用。
     */
    boolean generateDefault() default true;

    /**
     * 相对 {@code plugin.getDataFolder()} 的路径，如 {@code "gui/adminmenu.yml"}；扩展名可省略。
     * <p>
     * 以 {@code res://} 开头表示**打进 jar 的只读内置配置**：只作为默认值来源，不落地到磁盘、也不回写。
     */
    String path();

    /** 配置版本号，用于迁移；语义见 {@link #migrate()}。 */
    int version() default 1;

    /**
     * 玩家离线后 session（页码、筛选条件这类跨「打开」的状态）保留的秒数。
     * <p>
     * 默认 30 分钟。{@code close()} 不再销毁 session，因此必须有一个兜底的回收时间点，
     * 否则一个玩家 × 每个菜单一份状态会一直留在内存里。
     */
    int sessionTtl() default 1800;

    /**
     * yml 的 {@code version} **小于**代码 {@code version()} 时的迁移函数名。
     * <p>
     * 迁移前会先把原文件备份成 {@code <名字>.v<旧版本>.bak}，再写回升版后的内容——
     * 因为迁移一旦写错，服主至少还能拿回原文件。
     * <p>
     * yml 的 {@code version} **大于**代码 {@code version()} 时**拒绝加载该文件**并报错
     * （提示「插件需要升级」）——而不是猜着读。
     */
    String migrate() default "";
}
