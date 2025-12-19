package cn.yvmou.ylib.api.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class test implements TabCompleter {
    @CommandComplete({
            @CommandComplete.Tab(
                    type = CompleteType.AUTO
            ),
            @CommandComplete.Tab(
                    type = CompleteType.CUSTOM,
                    customOptions = {"1", "2", "3"}
            ),
            @CommandComplete.Tab(
                    type = CompleteType.PRESET,
                    preset = PresetType.ONLINE_PLAYER
            )
    })
    private void t() {

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
