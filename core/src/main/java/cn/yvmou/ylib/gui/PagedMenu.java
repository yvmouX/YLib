package cn.yvmou.ylib.gui;

import cn.yvmou.ylib.message.MessageService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 分页菜单基类：布局图里 {@code #} 那一片格子当「这一页」，{@code prev} / {@code next} / {@code pages}
 * 三个槽位放翻页按钮与页码。
 * <p>
 * 子类只回答三件事：界面长什么样（{@link #shape()}）、有哪些条目（{@link #items()}）、
 * 一条条目画成什么（{@link #render}）；列表之外还要摆什么（排序、新建、返回）写在 {@link #decorate()} 里。
 * 条目多了自动分页、少了自动留空、数据变少导致页码越界会被夹回来（{@link Paging} 算，单测钉住）。
 *
 * <pre>
 * private static final String[] SHAPE = {
 *         "#########",
 *         "#########",
 *         "`prev` `pages` `next`",
 * };
 * </pre>
 */
public abstract class PagedMenu<T> extends Menu {

    /** 这一页的条目区（布局图里重复写 {@code #} 就能铺出一片，行数决定每页多少条）。 */
    public static final String PAGE_SLOT = "#";
    /** 上一页 / 下一页 / 页码的约定槽位名。 */
    public static final String PREVIOUS_SLOT = "prev";
    public static final String NEXT_SLOT = "next";
    public static final String INFO_SLOT = "pages";

    /** 当前页码（0 基）。 */
    private int page;

    protected PagedMenu(Player viewer, MessageService messages, int size, String title, Object... titleArgs) {
        super(viewer, messages, size, title, titleArgs);
    }

    // ---------- 子类实现 ----------

    /** 界面布局图（见 {@link SlotLayout}）；至少要有一个 {@code #} 格、以及 {@code prev}/{@code next} 两个按钮位。 */
    protected abstract String[] shape();

    /** 这一页要展示的全部条目（按顺序）。 */
    protected abstract List<T> items();

    /**
     * 一条条目 → 菜单项。
     *
     * @param index 它在<b>整体列表</b>里的下标（不是这一页里的位置）：按位置做移动、删除时用它；
     *              不需要就忽略这个参数
     */
    protected abstract MenuItem render(T item, int index);

    /** 条目区与页码之外的物品（排序、新建、返回…）；默认什么都不摆。 */
    protected void decorate() {
    }

    /** 一条条目都没有时在条目区第一格显示什么；返回 {@code null} 表示不显示（默认）。 */
    protected MenuItem whenEmpty() {
        return null;
    }

    // ---------- 翻页控件（想换文案就重写这三个） ----------

    /** 上一页按钮；{@code enabled} 为 {@code false} 时应当摆成灰色不可点。 */
    protected MenuItem previousButton(boolean enabled) {
        return pagerButton(enabled, Material.ARROW, "gui.previous", "&e<");
    }

    /** 下一页按钮。 */
    protected MenuItem nextButton(boolean enabled) {
        return pagerButton(enabled, Material.ARROW, "gui.next", "&e>");
    }

    /** 页码（第几页 / 共几页）。 */
    protected MenuItem pageInfo(int current, int totalPages) {
        String label = hasKey("gui.page-info")
                ? text("gui.page-info", current + 1, totalPages)
                : literal("&7{0}/{1}", current + 1, totalPages);
        return MenuItem.display(Material.PAPER, label, Collections.<String>emptyList());
    }

    // ---------- 页码状态 ----------

    /** 当前页码（0 基）：重开界面时要把它带回来，否则看一眼别的再回来就回到第一页。 */
    protected final int page() {
        return page;
    }

    /** 直接跳到第几页（0 基，越界会被夹回）；构造里也可以调，构建发生在真正打开时。 */
    protected final void page(int page) {
        this.page = Math.max(0, page);
    }

    // ---------- 骨架 ----------

    @Override
    protected final void build() {
        layout(shape());

        List<T> all = items() == null ? Collections.<T>emptyList() : items();
        int pageSize = Math.max(1, slots(PAGE_SLOT).size());
        page = Paging.clampPage(page, all.size(), pageSize);
        int totalPages = Paging.totalPages(all.size(), pageSize);
        int from = Paging.fromIndex(page, pageSize);
        int to = Paging.toIndex(page, pageSize, all.size());

        List<MenuItem> icons = new ArrayList<MenuItem>();
        for (int index = from; index < to; index++) {
            icons.add(render(all.get(index), index));
        }
        if (icons.isEmpty()) {
            MenuItem empty = whenEmpty();
            if (empty != null) {
                set(slots(PAGE_SLOT).get(0), empty);
            }
        } else {
            fill(PAGE_SLOT, icons);
        }

        set(PREVIOUS_SLOT, previousButton(page > 0));
        set(NEXT_SLOT, nextButton(page < totalPages - 1));
        set(INFO_SLOT, pageInfo(page, totalPages));
        decorate();
        fill(MenuItem.filler());
    }

    /** 翻到指定页并重建；页码越界由 {@link Paging#clampPage} 兜住。 */
    protected final void goToPage(int page) {
        this.page = Math.max(0, page);
        refresh();
    }

    // ---------- 内部 ----------

    private MenuItem pagerButton(boolean enabled, Material material, String key, String fallback) {
        String label = hasKey(key) ? text(key) : literal(fallback);
        if (!enabled) {
            return MenuItem.display(Material.GRAY_DYE, label, Collections.<String>emptyList());
        }
        return MenuItem.of(material, label, Collections.<String>emptyList(), new Consumer<MenuItem.ClickContext>() {
            @Override
            public void accept(MenuItem.ClickContext context) {
                // 左键往后翻、右键往前翻：一个按钮就能来回，省一个格子
                goToPage(context.isRight() ? page - 1 : page + 1);
            }
        });
    }
}
