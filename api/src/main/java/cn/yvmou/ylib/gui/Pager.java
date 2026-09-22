package cn.yvmou.ylib.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 翻页：算术（总页数 / 页码夹紧 / 这一页的切片）与状态（当前页 + 总页数）都在这里。
 * <p>
 * 为什么合成一个类：这两件事本来就分不开——菜单手里那个「第几页、能翻吗」是算出来的结果，
 * 单独给算术开一个类，调用方就得在两个名字之间来回跳。{@code Menu.pagedList(...)} 返回一个 {@link Pager}，
 * 翻页按钮只看 {@link #hasPrevious()} / {@link #hasNext()}。
 * <p>
 * 页码一律 **0 起**（下标语义）；要给玩家看的那一页用 {@link #pageNumber()}。
 * 单独成类是因为界面本身要服务端才能跑，而「第几页到第几页」这类差一位的 bug 恰恰最难在游戏里发现
 * （只表现为某一页少一条或多一条），放这里能用单测钉死。
 */
public final class Pager {

    /** 每页至少 1 条：布局里那个区域一格都没有时不至于除零。 */
    private static final int MIN_PAGE_SIZE = 1;

    /** 箱子界面最多 6 行。 */
    private static final int MAX_ROWS = 6;

    private final int page;
    private final int totalPages;

    public Pager(int page, int totalPages) {
        this.totalPages = Math.max(1, totalPages);
        this.page = Math.max(0, Math.min(page, this.totalPages - 1));
    }

    /** 按条目总数与每页格数算出一个夹紧后的翻页状态（页码越界、每页 0 格都能安全处理）。 */
    public static Pager of(int page, int totalItems, int pageSize) {
        return new Pager(clampPage(page, totalItems, pageSize), totalPages(totalItems, pageSize));
    }

    /** 当前页，0 起。 */
    public int page() {
        return page;
    }

    /** 总页数；没有条目时也是 1（空界面比 0 页好解释）。 */
    public int totalPages() {
        return totalPages;
    }

    /** 当前页，1 起——写进 {@code {page}} 占位符、显示给玩家用的那个数。 */
    public int pageNumber() {
        return page + 1;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page < totalPages - 1;
    }

    // ---------- 算术 ----------

    /** 总页数；没有条目时也是 1 页。 */
    public static int totalPages(int totalItems, int pageSize) {
        int size = Math.max(MIN_PAGE_SIZE, pageSize);
        int total = totalItems <= 0 ? 0 : (totalItems + size - 1) / size;
        return Math.max(1, total);
    }

    /** 把页码夹进合法范围：数据变少（重载、筛选）后旧页码会越界，夹回来而不是给一页空白。 */
    public static int clampPage(int page, int totalItems, int pageSize) {
        int last = totalPages(totalItems, pageSize) - 1;
        if (page < 0) {
            return 0;
        }
        return Math.min(page, last);
    }

    /**
     * 取这一页的条目：页码与每页条数都会先夹到合法范围（传 99 页进来也不会越界），空列表给空表。
     * <p>
     * 只做切片不改数据：调用方对「一次渲染要拿几次数据」有完全的掌控（库里不缓存、也不替你重算）。
     */
    public static <T> List<T> slice(List<T> items, int page, int pageSize) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        int size = Math.max(MIN_PAGE_SIZE, pageSize);
        int safePage = clampPage(page, items.size(), size);
        int from = safePage * size;
        int to = Math.min(items.size(), from + size);
        return new ArrayList<T>(items.subList(from, to));
    }

    @Override
    public String toString() {
        return "Pager[" + pageNumber() + "/" + totalPages + "]";
    }
}
