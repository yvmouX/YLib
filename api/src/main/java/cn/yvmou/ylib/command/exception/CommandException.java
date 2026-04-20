package cn.yvmou.ylib.command.exception;

/**
 * 命令执行异常基类
 */
public class CommandException extends RuntimeException {
    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
