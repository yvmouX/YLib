package cn.yvmou.ylib.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandComplete {

    Tab[] value() default {};

    @interface Tab {
        CompleteType type();

        String[] customOptions() default {};

        PresetType preset() default PresetType.NONE;
    }
}
