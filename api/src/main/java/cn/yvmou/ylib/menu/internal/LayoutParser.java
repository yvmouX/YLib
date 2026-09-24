package cn.yvmou.ylib.menu.internal;

import cn.yvmou.ylib.menu.MenuConfigException;
import cn.yvmou.ylib.menu.Slot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 布局表解析：ASCII 图 → 符号 → 槽位。
 *
 * <h2>语法</h2>
 * <table>
 *   <caption>规则</caption>
 *   <tr><th>写法</th><th>含义</th></tr>
 *   <tr><td>一行字符串 = 一行槽位</td><td>第 n 行、第 m 列 → 槽位号 {@code n * columns + m}</td></tr>
 *   <tr><td>单字符符号裸写（{@code #} {@code c} {@code _}）</td><td>占 1 格</td></tr>
 *   <tr><td>多字符符号用反引号包裹（{@code `quests`}）</td><td>占 1 格，反引号本身不占位</td></tr>
 *   <tr><td>空白</td><td>不占位，只用于源码对齐</td></tr>
 * </table>
 *
 * <h2>为什么反引号是词法边界而不是装饰</h2>
 * {@code ##`quests`#`create`##} 是 <b>9 格</b>；去掉反引号的 {@code ##quests#create##} 是 <b>7 格</b>
 * ——后者会被当成 7 个单字符符号，在第 3 列就因为「符号 q 未声明」失败。反引号让「多字符符号」有了
 * 无歧义的边界，因此这段解析**不需要预先知道符号表**（不用「最长匹配」那一套）：
 * 引号内的一切是多字符符号，引号外的一切必然是单字符符号。
 * <p>
 * 由此得到一个有用的性质：解析与 {@code declare()} 的执行顺序完全无关，{@code layout()} 可以独立单测。
 */
public final class LayoutParser {

    /** 反引号。 */
    private static final char TICK = '`';

    /** 一行最多（也是标准）的格数：箱子界面一行 9 格。 */
    public static final int COLUMNS = 9;

    /** 箱子界面最多 6 行（54 格）。 */
    public static final int MAX_ROWS = 6;

    private LayoutParser() {
    }

    /** 解析结果：符号 → 槽位、槽位 → 符号，都是不可变视图。 */
    public static final class Result {

        private final Map<String, List<Slot>> slotsBySymbol;
        private final List<Slot> slotsInOrder;
        private final String[] slotSymbols;
        private final int columns;

        Result(Map<String, List<Slot>> slotsBySymbol, String[] slotSymbols, int columns) {
            this.slotsBySymbol = Collections.unmodifiableMap(slotsBySymbol);
            this.slotSymbols = slotSymbols;
            this.columns = columns;
            List<Slot> ordered = new ArrayList<Slot>();
            for (List<Slot> slots : slotsBySymbol.values()) {
                ordered.addAll(slots);
            }
            this.slotsInOrder = Collections.unmodifiableList(ordered);
        }

        /** 符号 → 它占据的所有槽位（按阅读顺序）。 */
        public Map<String, List<Slot>> slotsBySymbol() {
            return slotsBySymbol;
        }

        /** 全部被占用的槽位（按符号首次出现的顺序分组，组内按阅读顺序）。 */
        public List<Slot> slotsInOrder() {
            return slotsInOrder;
        }

        /** 某个槽位上的符号；空位返回 {@code null}。 */
        public String symbolAt(int index) {
            return index < 0 || index >= slotSymbols.length ? null : slotSymbols[index];
        }

        public int columns() {
            return columns;
        }

        /** 容器大小（槽位数）。 */
        public int size() {
            return slotSymbols.length;
        }
    }

    /**
     * 解析布局表。
     *
     * @param layout  布局行；行数 1..{@value #MAX_ROWS}
     * @param columns 每行期望的格数（默认 9）
     */
    public static Result parse(String[] layout, int columns) {
        if (columns < 1) {
            throw new MenuConfigException("columns 必须 ≥ 1，收到 " + columns);
        }
        if (layout == null || layout.length == 0) {
            throw new MenuConfigException("布局表是空的：至少要有一行");
        }
        if (layout.length > MAX_ROWS) {
            throw new MenuConfigException("菜单行数 " + layout.length + " 超出 1.." + MAX_ROWS
                    + "（箱子界面最多 " + (MAX_ROWS * columns) + " 格）");
        }

        Map<String, List<Slot>> bySymbol = new LinkedHashMap<String, List<Slot>>();
        String[] slotSymbols = new String[layout.length * columns];

        for (int row = 0; row < layout.length; row++) {
            String line = layout[row] == null ? "" : layout[row];
            int col = 0;       // 槽位列号（不含反引号、不含空白）
            int cursor = 0;    // 字符串游标（含反引号）
            int symbolStartCol = -1;
            StringBuilder pending = null;
            int pendingRow = row;

            while (cursor < line.length()) {
                char ch = line.charAt(cursor);

                if (ch == TICK) {
                    if (pending == null) {
                        pending = new StringBuilder();
                        symbolStartCol = col;
                        pendingRow = row;
                    } else {
                        if (pending.length() == 0) {
                            throw new MenuConfigException("第" + (row + 1) + "行第" + (symbolStartCol + 1)
                                    + "列：空的反引号对");
                        }
                        place(bySymbol, slotSymbols, pending.toString(), row, col, columns);
                        col++;
                        pending = null;
                    }
                    cursor++;
                    continue;
                }

                if (pending != null) {
                    pending.append(ch);
                    cursor++;
                    continue;
                }

                if (Character.isWhitespace(ch)) {
                    cursor++;      // 空白只用于源码对齐，不占槽位
                    continue;
                }

                if (col >= columns) {
                    throw new MenuConfigException("第" + (row + 1) + "行共 " + (col + 1)
                            + " 列，期望 " + columns + " 列（超出的那一格没有位置）");
                }
                place(bySymbol, slotSymbols, String.valueOf(ch), row, col, columns);
                col++;
                cursor++;
            }

            if (pending != null) {
                throw new MenuConfigException("第" + (pendingRow + 1) + "行第" + (symbolStartCol + 1)
                        + "列：反引号未闭合");
            }
            if (col != columns) {
                throw new MenuConfigException("第" + (row + 1) + "行共 " + col + " 列，期望 " + columns + " 列");
            }
        }

        return new Result(bySymbol, slotSymbols, columns);
    }

    /** 把一个符号登记到某个槽位上。 */
    private static void place(Map<String, List<Slot>> bySymbol, String[] slotSymbols,
                             String symbol, int row, int col, int columns) {
        int index = row * columns + col;
        List<Slot> slots = bySymbol.get(symbol);
        if (slots == null) {
            slots = new ArrayList<Slot>();
            bySymbol.put(symbol, slots);
        }
        slots.add(new Slot(index, row, col));
        slotSymbols[index] = symbol;
    }

    /**
     * 单字符符号与多字符符号互为前缀时报错。
     * <p>
     * 这一条在**布局解析之后、{@code declare()} 之前**跑：一旦同时存在 {@code symbol('q')} 与
     * {@code symbol("quests")}，源码里裸写 {@code q} 到底指谁就没有答案了；而这个错误如果拖到
     * 运行期，表现是「某个按钮偶尔指向另一个物品」，极难查。
     */
    public static void checkPrefixConflict(Iterable<String> symbols) {
        List<String> singles = new ArrayList<String>();
        List<String> multi = new ArrayList<String>();
        for (String symbol : symbols) {
            if (symbol.length() == 1) {
                singles.add(symbol);
            } else {
                multi.add(symbol);
            }
        }
        for (String single : singles) {
            for (String longer : multi) {
                if (longer.startsWith(single)) {
                    throw new MenuConfigException("符号 '" + single + "' 与 '" + longer
                            + "' 前缀冲突（裸写时无法区分）");
                }
            }
        }
    }

    /**
     * 给「未知符号」找一个像样的建议：把单字符当成多字符符号的首字母时提示它。
     * <p>
     * 最常见的笔误是把多字符符号忘了加反引号，报错信息里带上「是否想写 {@code `quests`}?」
     * 能省掉一半的调试时间。
     */
    public static String suggest(String unknown, Iterable<String> candidates) {
        List<String> hits = new ArrayList<String>();
        for (String candidate : candidates) {
            if (candidate.length() > 1 && candidate.startsWith(unknown)) {
                hits.add(candidate);
            }
        }
        if (hits.isEmpty()) {
            return "";
        }
        return "（是否想写 `" + hits.get(0) + "`?）";
    }
}
