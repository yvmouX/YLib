package cn.yvmou.ylib.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandOptions {
    String name();
    String permission();
    boolean onlyPlayer();
    String[] alias() default {};
    boolean register() default true;
    String usage() default "";
}
