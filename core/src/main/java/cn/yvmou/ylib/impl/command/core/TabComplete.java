package cn.yvmou.ylib.impl.command.core;

import cn.yvmou.ylib.api.command.CommandComplete;
import cn.yvmou.ylib.api.command.PresetType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class TabComplete implements TabCompleter {
    private final String mainCommandName;
    private final List<String> subCommandNameList;
    private final Map<String, CommandComplete.Tab[]> subCommandTabs;

    public TabComplete(String mainCommandName, List<String> subCommandNameList, Map<String, CommandComplete.Tab[]> subCommandTabs) {
        this.mainCommandName = mainCommandName;
        this.subCommandNameList = subCommandNameList;
        this.subCommandTabs = subCommandTabs;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 匹配不到正确命令的情况下返回一个空列表
        if (!command.getName().equalsIgnoreCase(mainCommandName)) {
            return Collections.emptyList();
        }

        // 补全子命令
        if (args.length == 1) {
            String current = args[0].toLowerCase();
            return subCommandNameList.stream()
                    .filter(name -> name.toLowerCase().startsWith(current))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // 补全子命令参数
        if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            if (subCommandTabs.containsKey(subCommandName)) {
                CommandComplete.Tab[] tabs = subCommandTabs.get(subCommandName);
                
                // args[0] 是子命令，args[1] 是第一个参数 (对应 tabs[0])
                // 所以 tabs 的索引是 args.length - 2
                int tabIndex = args.length - 2;

                if (tabIndex >= 0 && tabIndex < tabs.length) {
                    return getCompletions(tabs[tabIndex], args[args.length - 1]);
                }
            }
        }
        
        return Collections.emptyList();
    }

    private List<String> getCompletions(CommandComplete.Tab tab, String currentArg) {
        List<String> completions = new ArrayList<>();
        
        switch (tab.type()) {
            case CUSTOM:
                String[] customOptions = tab.customOptions();
                if (customOptions != null) {
                    completions.addAll(Arrays.asList(customOptions));
                }
                break;
            case PRESET:
                if (tab.preset() == PresetType.ONLINE_PLAYER) {
                    completions = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                }
                break;
            case AUTO:
                break;
        }
        
        String prefix = currentArg.toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }
}
