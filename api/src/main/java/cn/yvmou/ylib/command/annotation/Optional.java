package cn.yvmou.ylib.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法参数为可选。
 * 如果调用时未提供该参数，将使用默认值（数字为0，对象为null，布尔为false）。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Optional {
    // 可以在这里增加 defaultStringValue 等属性，暂时先做最简单的默认值填充
}
