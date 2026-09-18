package cn.yvmou.ylib.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 界面布局表：把界面画成一张文本图，一个字符就是一格，`` `名字` `` 让多字符名字也只占一格，空格是空位。
 * <p>
 * 一个名字可以占多格（{@code "#########"} 是 9 格同名），因此按「组」取用：静态槽位
 * {@link Menu#set(String, MenuItem)} 给整组放同一个物品，动态槽位 {@link Menu#fill(String, List)}
 * 按顺序填（任务列表这种）。这是代码里的布局，写错就是代码 bug，
 * 因此超行 / 超格 / 反引号没闭合 / 名字不存在一律当场抛。
 */
public final class SlotLayout {

    /** 一行的格数：与箱子界面一致。 */
    private static final int COLUMNS = 9;

    /** 名字 → 它占的格子下标（阅读顺序：先上后下、先左后右）。 */
    private final Map<String, List<Integer>> slots;

    private SlotLayout(Map<String, List<Integer>> slots) {
        this.slots = Collections.unmodifiableMap(slots);
    }

    /**
     * 解析布局表：每行最多 9 格，行数不能超过容器容量。
     *
     * @param size 容器大小（9 的倍数）
     * @param rows 每行一串字符，允许用空格撑出位置；空串也是一行（全空）
     */
    public static SlotLayout parse(int size, String... rows) {
        if (rows == null || rows.length == 0) {
            throw new IllegalArgumentException("布局表是空的：一个槽位都没声明");
        }
        int rowLimit = size / COLUMNS;
        if (rows.length > rowLimit) {
            throw new IllegalArgumentException(
                    "布局表写了 " + rows.length + " 行，容器大小 " + size + " 只放得下 " + rowLimit + " 行");
        }
        Map<String, List<Integer>> slots = new LinkedHashMap<String, List<Integer>>();
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row] == null ? "" : rows[row];
            List<String> names = split(line, row);
            if (names.size() > COLUMNS) {
                throw new IllegalArgumentException("布局表第 " + (row + 1) + " 行有 " + names.size()
                        + " 格，一行最多 " + COLUMNS + " 格（超出的那一格没有位置）");
            }
            for (int column = 0; column < names.size(); column++) {
                String name = names.get(column);
                if (name.trim().isEmpty()) {
                    continue;   // 空格：留空（占一格，但不登记名字）
                }
                List<Integer> found = slots.get(name);
                if (found == null) {
                    found = new ArrayList<Integer>();
                    slots.put(name, found);
                }
                found.add(row * COLUMNS + column);
            }
        }
        return new SlotLayout(slots);
    }

    /**
     * 把一行切成若干格：反引号包住的算一格（名字可以很长），其余每个字符算一格（空格也算一格）。
     * 按码点切，中文与 emoji 都只算一格。
     */
    private static List<String> split(String line, int row) {
        List<String> names = new ArrayList<String>();
        int index = 0;
        while (index < line.length()) {
            int codePoint = line.codePointAt(index);
            if (codePoint == '`') {
                int end = line.indexOf('`', index + 1);
                if (end < 0) {
                    throw new IllegalArgumentException(
                            "布局表第 " + (row + 1) + " 行的反引号没有闭合：` 后面的整段会被当成一个名字，位置全错");
                }
                names.add(line.substring(index + 1, end));
                index = end + 1;
                continue;
            }
            names.add(new String(Character.toChars(codePoint)));
            index += Character.charCount(codePoint);
        }
        return names;
    }

    /** 这个名字占的格子下标，按阅读顺序（一个名字可以占多格）；名字不存在就抛，并列出全部可选名字。 */
    public List<Integer> slots(String name) {
        List<Integer> found = slots.get(name);
        if (found == null) {
            throw new IllegalArgumentException("布局表里没有槽位「" + name + "」，只有 " + slots.keySet());
        }
        return Collections.unmodifiableList(new ArrayList<Integer>(found));
    }

    /** 动态槽位按顺序取前 {@code count} 格：物品比格子多时多的没有位置（不显示），少时剩下的格子留空。 */
    public List<Integer> take(String name, int count) {
        List<Integer> found = slots(name);
        int end = Math.max(0, Math.min(count, found.size()));
        return Collections.unmodifiableList(new ArrayList<Integer>(found.subList(0, end)));
    }

    /** 全部槽位名（错误信息与测试用）。 */
    public Set<String> names() {
        return Collections.unmodifiableSet(slots.keySet());
    }
}
