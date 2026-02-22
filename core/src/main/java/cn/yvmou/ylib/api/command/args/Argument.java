package cn.yvmou.ylib.api.command.args;

import cn.yvmou.ylib.api.command.context.CommandContext;
import cn.yvmou.ylib.api.command.exception.CommandParseException;
import cn.yvmou.ylib.api.command.exception.CommandValidationException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 命令参数定义
 * @param <T> 参数类型
 */
public class Argument<T> {
    private final String name;
    private final ArgumentParser<T> parser;
    private final List<ArgumentValidator<T>> validators = new ArrayList<>();
    private SuggestionProvider suggestionProvider;
    private boolean isOptional = false;
    private T defaultValue;

    private Argument(String name, ArgumentParser<T> parser) {
        this.name = name;
        this.parser = parser;
    }

    // ========== 静态工厂方法 ==========

    /**
     * 创建一个自定义类型的参数
     * @param name 参数名称，用于在上下文中获取值
     * @param parser 自定义解析器，将字符串输入转换为目标类型
     * @param <T> 参数的目标类型
     * @return 一个新的 Argument 实例
     */
    public static <T> Argument<T> custom(String name, ArgumentParser<T> parser) {
        return new Argument<>(name, parser);
    }

    /**
     * 创建一个字符串参数
     * @param name 参数名称
     * @return 一个新的字符串 Argument 实例
     */
    public static Argument<String> string(String name) {
        return new Argument<>(name, (sender, input) -> input);
    }

    /**
     * 创建一个整数参数
     * 内置解析逻辑，如果输入不是有效的整数，会抛出 CommandParseException
     * @param name 参数名称
     * @return 一个新的整数 Argument 实例
     */
    public static Argument<Integer> integer(String name) {
        return new Argument<>(name, (sender, input) -> {
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                throw new CommandParseException("无效的数字: " + input);
            }
        });
    }

    /**
     * 创建一个浮点数参数
     * 内置解析逻辑，如果输入不是有效的数字，会抛出 CommandParseException
     * @param name 参数名称
     * @return 一个新的浮点数 Argument 实例
     */
    public static Argument<Double> number(String name) {
        return new Argument<>(name, (sender, input) -> {
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                throw new CommandParseException("无效的数字: " + input);
            }
        });
    }

    /**
     * 创建一个布尔值参数
     * 支持 "true", "yes", "on" (true) 和 "false", "no", "off" (false)
     * 内置 "true" 和 "false" 的 Tab 补全
     * @param name 参数名称
     * @return 一个新的布尔值 Argument 实例
     */
    public static Argument<Boolean> bool(String name) {
        return new Argument<>(name, (sender, input) -> {
            if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("on")) {
                return true;
            }
            if (input.equalsIgnoreCase("false") || input.equalsIgnoreCase("no") || input.equalsIgnoreCase("off")) {
                return false;
            }
            throw new CommandParseException("无效的布尔值 (true/false): " + input);
        }).suggests((sender, context, current) -> {
            List<String> list = new ArrayList<>();
            list.add("true");
            list.add("false");
            return list;
        });
    }

    /**
     * 创建一个在线玩家参数
     * 如果玩家不在线或不存在，会抛出 CommandParseException
     * 内置在线玩家名称的 Tab 补全
     * @param name 参数名称
     * @return 一个新的玩家 Argument 实例
     */
    public static Argument<Player> player(String name) {
        return new Argument<>(name, (sender, input) -> {
            Player player = Bukkit.getPlayerExact(input);
            if (player == null) {
                throw new CommandParseException("玩家未在线或不存在: " + input);
            }
            return player;
        }).suggests((sender, context, current) -> 
            Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(p -> p.toLowerCase().startsWith(current.toLowerCase()))
                .collect(Collectors.toList())
        );
    }

    /**
     * 创建一个世界参数
     * 如果世界不存在，会抛出 CommandParseException
     * 内置已加载世界的 Tab 补全
     * @param name 参数名称
     * @return 一个新的世界 Argument 实例
     */
    public static Argument<World> world(String name) {
        return new Argument<>(name, (sender, input) -> {
            World world = Bukkit.getWorld(input);
            if (world == null) {
                throw new CommandParseException("世界不存在: " + input);
            }
            return world;
        }).suggests((sender, context, current) -> 
            Bukkit.getWorlds().stream()
                .map(World::getName)
                .filter(w -> w.toLowerCase().startsWith(current.toLowerCase()))
                .collect(Collectors.toList())
        );
    }

    /**
     * 创建一个枚举类型参数
     * 如果输入不是有效的枚举常量（忽略大小写），会抛出 CommandParseException
     * 内置枚举常量的 Tab 补全
     * @param name 参数名称
     * @param enumClass 枚举的 Class 对象
     * @param <E> 枚举类型
     * @return 一个新的枚举 Argument 实例
     */
    public static <E extends Enum<E>> Argument<E> enumValue(String name, Class<E> enumClass) {
        return new Argument<>(name, (sender, input) -> {
            try {
                return Enum.valueOf(enumClass, input.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CommandParseException("无效的选项: " + input);
            }
        }).suggests((sender, context, current) -> {
            List<String> suggestions = new ArrayList<>();
            for (E e : enumClass.getEnumConstants()) {
                if (e.name().toLowerCase().startsWith(current.toLowerCase())) {
                    suggestions.add(e.name());
                }
            }
            return suggestions;
        });
    }

    // ========== 链式配置方法 ==========

    /**
     * 添加一个验证器，用于在参数解析后进行额外的检查
     * 例如：检查数字是否在某个范围内
     * @param validator 验证逻辑
     * @return 当前 Argument 实例，支持链式调用
     */
    public Argument<T> validate(@NotNull ArgumentValidator<T> validator) {
        this.validators.add(validator);
        return this;
    }

    /**
     * 设置一个自定义的 Tab 补全提供器
     * @param provider Tab 补全逻辑
     * @return 当前 Argument 实例，支持链式调用
     */
    public Argument<T> suggests(@Nullable SuggestionProvider provider) {
        this.suggestionProvider = provider;
        return this;
    }

    /**
     * 将参数标记为可选，并提供一个默认值
     * 如果命令中未提供此参数，将使用默认值
     * @param defaultValue 默认值
     * @return 当前 Argument 实例，支持链式调用
     */
    public Argument<T> optional(@Nullable T defaultValue) {
        this.isOptional = true;
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * 将参数标记为可选，默认值为 null
     * @return 当前 Argument 实例，支持链式调用
     */
    public Argument<T> optional() {
        return optional(null);
    }

    // ========== 内部处理方法 ==========

    /**
     * （内部使用）解析字符串输入为目标类型
     * @param sender 命令发送者
     * @param input 字符串输入
     * @return 解析后的值
     * @throws CommandParseException 如果解析失败
     */
    public T parse(CommandSender sender, String input) throws CommandParseException {
        return parser.parse(sender, input);
    }

    /**
     * （内部使用）对解析后的值运行所有验证器
     * @param context 命令上下文
     * @param value 解析后的值
     * @throws CommandValidationException 如果验证失败
     */
    public void validate(CommandContext context, T value) throws CommandValidationException {
        for (ArgumentValidator<T> validator : validators) {
            validator.validate(context, value);
        }
    }

    /**
     * （内部使用）获取 Tab 补全建议
     * @param sender 命令发送者
     * @param context 命令上下文
     * @param currentInput 当前输入
     * @return 建议列表
     */
    public List<String> suggest(CommandSender sender, CommandContext context, String currentInput) {
        if (suggestionProvider != null) {
            return suggestionProvider.suggest(sender, context, currentInput);
        }
        return Collections.emptyList();
    }

    // ========== Getters ==========

    /**
     * 获取参数的名称
     * @return 参数名称
     */
    public String getName() {
        return name;
    }

    /**
     * 检查参数是否为可选
     * @return 如果是可选的，返回 true
     */
    public boolean isOptional() {
        return isOptional;
    }

    /**
     * 获取参数的默认值
     * @return 默认值，如果未设置则为 null
     */
    public T getDefaultValue() {
        return defaultValue;
    }
}
