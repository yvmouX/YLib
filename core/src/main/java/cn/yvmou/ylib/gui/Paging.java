package cn.yvmou.ylib.gui;

/**
 * 分页算术：总页数、页码夹紧、这一页的切片范围。
 * <p>
 * 单独抽出来是因为界面本身要服务端才能跑，而「第几页到第几页」这类差一位的 bug 恰恰最难在游戏里发现
 * （只表现为某一页少一条或多一条）。{@link PagedMenu} 用的是同一份计算。
 */
public final class Paging {

    private Paging() {
    }

    /** 每页至少 1 条：布局里那个区域一格都没有时不至于除零。 */
    private static int safePageSize(int pageSize) {
        return pageSize < 1 ? 1 : pageSize;
    }

    /** 总页数；没有条目时也是 1 页（空界面比 0 页好解释）。 */
    public static int totalPages(int totalItems, int pageSize) {
        int size = safePageSize(pageSize);
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

    /** 这一页第一条在整体列表里的下标。 */
    public static int fromIndex(int page, int pageSize) {
        return Math.max(0, page) * safePageSize(pageSize);
    }

    /** 这一页的结束下标（不含），已按条目总数夹住。 */
    public static int toIndex(int page, int pageSize, int totalItems) {
        return Math.min(totalItems, fromIndex(page, pageSize) + safePageSize(pageSize));
    }
}
