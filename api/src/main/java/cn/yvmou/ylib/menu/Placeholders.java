package cn.yvmou.ylib.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 命名占位符 {@code {名字}} 的替换。
 *
 * <h2>三条行为约定</h2>
 * <ol>
 *   <li><b>认不出来的名字原样留着</b>：界面上的 {@code {foo}} 就是「这里写错了」的提示，
 *       比悄悄删掉好排查得多；</li>
 *   <li><b>值为 {@code null} 当空串</b>：占位符位置留空，比在界面上显示 {@code null} 好；</li>
 *   <li><b>纯数字的名字不算命名占位符</b>：{@code {0}}、{@code {1}} 归语言服务那套位置占位符，
 *       菜单这套只认有名字的。</li>
 * </ol>
 *
 * <h2>为什么不用 {@code java.text.MessageFormat}</h2>
 * 它会把文本里的单引号当转义符吃掉。中文文案与玩家昵称里出现单引号是常事，
 * 而症状是「某些玩家名字一进菜单就少一个字」，极难查。这里用手写的逐字符替换。
 */
public final class Placeholders {

    private Placeholders() {
    }

    /**
     * 替换一行文案里的全部 {@code {名字}}。
     *
     * @param template 模板；{@code null} / 空串原样返回
     * @param values   取值表；{@code null} / 空表时原样返回模板
     */
    public static String apply(String template, Map<String, String> values) {
        if (template == null || template.isEmpty() || values == null || values.isEmpty()) {
            return template;
        }
        StringBuilder result = new StringBuilder(template.length() + 16);
        int index = 0;
        while (index < template.length()) {
            int open = template.indexOf('{', index);
            int close = open < 0 ? -1 : template.indexOf('}', open + 1);
            if (open < 0 || close < 0) {
                result.append(template, index, template.length());
                break;
            }
            result.append(template, index, open);
            String name = template.substring(open + 1, close);
            String value = name.isEmpty() ? null : values.get(name);
            if (value == null) {
                result.append(template, open, close + 1);
            } else {
                result.append(value);
            }
            index = close + 1;
        }
        return result.toString();
    }

    /** 逐行替换（lore 用）；{@code templates} 为 {@code null} 时返回空表。 */
    public static List<String> apply(List<String> templates, Map<String, String> values) {
        if (templates == null) {
            return new ArrayList<String>();
        }
        List<String> result = new ArrayList<String>(templates.size());
        for (String each : templates) {
            result.add(apply(each, values));
        }
        return result;
    }

    /** 这个模板里用到了哪些命名占位符（按出现顺序、去重）；契约校验与文档用。 */
    public static List<String> names(String template) {
        if (template == null || template.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<String>();
        int index = 0;
        while (index < template.length()) {
            int open = template.indexOf('{', index);
            int close = open < 0 ? -1 : template.indexOf('}', open + 1);
            if (open < 0 || close < 0) {
                break;
            }
            String name = template.substring(open + 1, close);
            if (!name.isEmpty() && !isNumeric(name) && !names.contains(name)) {
                names.add(name);
            }
            index = close + 1;
        }
        return names;
    }

    /** 纯数字的名字是位置占位符（{@code {0}}），归语言服务，不是菜单的命名占位符。 */
    private static boolean isNumeric(String name) {
        for (int index = 0; index < name.length(); index++) {
            if (!Character.isDigit(name.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
