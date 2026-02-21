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

    public static <T> Argument<T> custom(String name, ArgumentParser<T> parser) {
        return new Argument<>(name, parser);
    }

    public static Argument<String> string(String name) {
        return new Argument<>(name, (sender, input) -> input);
    }

    public static Argument<Integer> integer(String name) {
        return new Argument<>(name, (sender, input) -> {
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                throw new CommandParseException("无效的数字: " + input);
            }
        });
    }

    public static Argument<Double> number(String name) {
        return new Argument<>(name, (sender, input) -> {
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                throw new CommandParseException("无效的数字: " + input);
            }
        });
    }

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
     * 添加验证器
     */
    public Argument<T> validate(@NotNull ArgumentValidator<T> validator) {
        this.validators.add(validator);
        return this;
    }

    /**
     * 设置自定义补全提供器
     */
    public Argument<T> suggests(@Nullable SuggestionProvider provider) {
        this.suggestionProvider = provider;
        return this;
    }

    /**
     * 标记为可选参数，并设置默认值
     */
    public Argument<T> optional(@Nullable T defaultValue) {
        this.isOptional = true;
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * 标记为可选参数，默认值为 null
     */
    public Argument<T> optional() {
        return optional(null);
    }

    // ========== 内部处理方法 ==========

    public T parse(CommandSender sender, String input) throws CommandParseException {
        return parser.parse(sender, input);
    }

    public void validate(CommandContext context, T value) throws CommandValidationException {
        for (ArgumentValidator<T> validator : validators) {
            validator.validate(context, value);
        }
    }

    public List<String> suggest(CommandSender sender, CommandContext context, String currentInput) {
        if (suggestionProvider != null) {
            return suggestionProvider.suggest(sender, context, currentInput);
        }
        return Collections.emptyList();
    }

    // ========== Getters ==========

    public String getName() {
        return name;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public T getDefaultValue() {
        return defaultValue;
    }
}
