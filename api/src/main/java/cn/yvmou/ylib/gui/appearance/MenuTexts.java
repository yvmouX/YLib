package cn.yvmou.ylib.gui.appearance;

import cn.yvmou.ylib.message.MessageService;
import cn.yvmou.ylib.text.Placeholders;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 菜单文案的取值规则：**语言键存在就用语言文件，没有才用配置里的字面量**。
 * <p>
 * 这条规则不是新发明的：{@code Menu.textOr(键, 兜底文案)} 早就是这个语义。菜单物品的名称与 lore
 * 只是把同一套语义搬到配置里，因此两处来源都做一遍命名占位符替换即可。换句话说，
 * 配置里的文案是「默认值」，语言文件是「可选的覆盖层」：
 * <ul>
 *   <li>只想改这台服务器的观感 → 改配置，不用碰语言文件；</li>
 *   <li>想按玩家客户端语言显示不同文案 → 在语言文件里加上同名键，配置那份自动让位。</li>
 * </ul>
 * <p>
 * lore 的覆盖写法与物品名不同：语言文件里一个键只能是一个字符串，因此多行 lore 要用 YAML 块标量
 * （{@code |-}）写成带换行的一整串——{@code MenuItem} 会把一条字符串里的 {@code \n} 拆成多行，
 * 否则语言文件就没法覆盖多行 lore。
 * <p>
 * 这层单独成类是为了**能脱离服务端测试**：语言键与配置谁赢这条规则要是只能靠开菜单来验，
 * 那它多半会被改错（{@code Menu} 的构造要走 {@code Bukkit.createInventory}，单测里起不来）。
 * <p>
 * 返回值**不渲染**：{@code &}/{@code §}/MiniMessage 由 {@code MenuItem} 在摆物品时统一渲染一次。
 * 两处来源只有一处渲染，才不会出现「先渲染再拼接、二次渲染把 {@code §} 码当普通字符」这类错。
 */
public final class MenuTexts {

    private MenuTexts() {
    }

    /** 取一条文案：语言键优先，否则用配置里的模板；最后替换命名占位符。 */
    public static String text(MessageService messages, CommandSender viewer, String key,
                             String template, Map<String, String> vars) {
        return Placeholders.apply(source(messages, viewer, key, template), vars);
    }

    /** 取一组文案（lore）：语言文件里那个键写多行就按换行拆开，否则用配置里的列表。 */
    public static List<String> lines(MessageService messages, CommandSender viewer, String key,
                                    List<String> templates, Map<String, String> vars) {
        if (messages == null || !messages.has(key)) {
            return Placeholders.apply(templates, vars);
        }
        String raw = raw(messages, viewer, key);
        if (raw.isEmpty()) {
            // 语言文件里把键写成空串 = 这台服务器不要这几行；不这么处理的话会渲染出一行空白
            return Collections.emptyList();
        }
        return Placeholders.apply(Arrays.asList(raw.split("\n")), vars);
    }

    private static String source(MessageService messages, CommandSender viewer, String key, String template) {
        return messages == null || !messages.has(key) ? template : raw(messages, viewer, key);
    }

    /**
     * 语言文件里这个键的原串；没有这个键（或没有语言服务）时返回 {@code null}。
     * <p>
     * 给**契约对账**用：某一处的模板可能被语言文件整段覆盖，扫「这个模板要哪些占位符」时必须把它一起算进去，
     * 否则「配置里没写、语言文件里写了」的那种写法会被误报成「代码给的值没人用」。
     */
    public static String languageValue(MessageService messages, CommandSender viewer, String key) {
        if (messages == null || key == null || !messages.has(key)) {
            return null;
        }
        return raw(messages, viewer, key);
    }

    /**
     * 取语言文件里的原串。**不传占位符参数**：{@code MessageService} 只认 {@code {0}} 那种数字占位符，
     * 而这里的 {@code {名字}} 由 {@link Placeholders} 处理，两边各管一段。
     */
    private static String raw(MessageService messages, CommandSender viewer, String key) {
        return viewer == null ? messages.raw(key) : messages.raw(viewer, key);
    }
}
