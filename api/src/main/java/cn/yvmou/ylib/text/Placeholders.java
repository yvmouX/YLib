package cn.yvmou.ylib.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 命名占位符：取值表的构造、替换、以及「这个模板里用了哪些名字」的扫描。三件事放一处，
 * 因为它们说的是同一件事——{@code {名字}} 这套写法在库里的全部行为。
 * <p>
 * 与 {@link cn.yvmou.ylib.message.MessageService} 的数字占位符 {@code {0}}、{@code {1}} 是两套，各管一段：
 * 数字写法给「代码里组织的句子」（顺序、数量都在代码里），命名写法给「交给管理员改的文案」
 * （一条 lore 里可能同时出现任务 id、类型、页码，位置写法要求调用点凑齐参数且顺序固定，翻译时也没法重排语序）。
 * 因此 {@link #names} 不把 {@code {0}} 当名字——它归 {@code MessageService}。
 * <p>
 * 认不出来的名字**原样留着**：界面上的 {@code {foo}} 就是「这里写错了」的提示，比悄悄删掉好排查。
 */
public final class Placeholders {

    private Placeholders() {
    }

    // ---------- 取值表 ----------

    /** 没有占位符时的空表：与 {@code vars()} 不同，它是个常量，可随手传。 */
    public static Map<String, String> empty() {
        return Collections.emptyMap();
    }

    /**
     * 成对给出名字与值：{@code vars("count", 3, "quest", "挖矿")}；一个都不给等于 {@link #empty()}。
     * <p>
     * 值自动转字符串（{@code null} 转空串——占位符位置留空，比在界面上显示 {@code null} 好），
     * 键的个数必须是偶数（少写一个值的表现是整条文案错位，最难查，宁可当场抛并说明格式）。
     */
    public static Map<String, String> vars(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Collections.emptyMap();
        }
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("命名占位符要成对给（名字、值、名字、值……），收到 "
                    + keyValues.length + " 个参数");
        }
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (int index = 0; index < keyValues.length; index += 2) {
            Object key = keyValues[index];
            if (key == null) {
                throw new IllegalArgumentException("第 " + (index / 2 + 1) + " 个占位符的名字是 null");
            }
            values.put(String.valueOf(key), valueOf(keyValues[index + 1]));
        }
        return Collections.unmodifiableMap(values);
    }

    // ---------- 替换 ----------

    /** 替换一行文案里的全部 {@code {名字}}；{@code template} 或 {@code values} 为空时原样返回。 */
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

    /** 逐行替换（lore 用）；{@code templates} 为 {@code null} 时返回空列表。 */
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

    // ---------- 扫名字 ----------

    /**
     * 这个模板里用到了哪些命名占位符（按出现顺序，去重）。
     * <p>
     * 用它的地方是**菜单契约对账**：配置里的模板要哪些名字，和代码里给了哪些名字一比，
     * 就能在构建时把「名字写错 / 配置要了代码没给」当场报出来——不必等界面显示 {@code {count}} 才去猜。
     * <p>
     * 两条与 {@link #apply} 一致的规则：没闭合的 {@code &#123;} 按普通文本处理（不当名字），
     * 纯数字的 {@code {0}} 不算命名占位符（它归 {@code MessageService}）。
     */
    public static Set<String> names(String template) {
        Set<String> names = new LinkedHashSet<String>();
        if (template == null || template.isEmpty()) {
            return names;
        }
        int index = 0;
        while (index < template.length()) {
            int open = template.indexOf('{', index);
            int close = open < 0 ? -1 : template.indexOf('}', open + 1);
            if (open < 0 || close < 0) {
                break;
            }
            String name = template.substring(open + 1, close);
            if (!name.isEmpty() && !isNumeric(name)) {
                names.add(name);
            }
            index = close + 1;
        }
        return names;
    }

    /** 一组模板用到的全部名字（lore 用）。 */
    public static Set<String> names(List<String> templates) {
        Set<String> names = new LinkedHashSet<String>();
        if (templates != null) {
            for (String each : templates) {
                names.addAll(names(each));
            }
        }
        return names;
    }

    /** 纯数字的名字是位置占位符（{@code {0}}），归 MessageService，不是命名占位符。 */
    private static boolean isNumeric(String name) {
        for (int index = 0; index < name.length(); index++) {
            if (!Character.isDigit(name.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
