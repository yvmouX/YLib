package cn.yvmou.yplugin.example;

import cn.yvmou.ylib.command.annotation.Arg;
import cn.yvmou.ylib.command.annotation.Command;
import cn.yvmou.ylib.command.annotation.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "example", aliases = "ec", description = "example command", permission = "ec.command.all")
public class ExampleCommand {

    @SubCommand("help")
    public void help(CommandSender sender) {
        sender.sendMessage("Example Command:");
    }

    @SubCommand("hello")
    public void hello(CommandSender sender, @Arg("target") Player target) {
        if (target != null && target == sender) {
            sender.sendMessage("我自己好！");
        } else if (target != null) {
            sender.sendMessage("你好，" + target.getName());
        }
    }
}
