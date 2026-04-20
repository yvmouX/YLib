package cn.yvmou.ylib.command.exception;

/**
 * 命令验证异常
 */
public class CommandValidationException extends CommandException {
    public CommandValidationException(String message) {
        super(message);
    }
}
