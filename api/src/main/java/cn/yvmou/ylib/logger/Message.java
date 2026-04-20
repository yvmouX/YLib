package cn.yvmou.ylib.logger;

import org.jetbrains.annotations.NotNull;

public interface Message {
    void msg(@NotNull String format, @NotNull Object... args);
}
