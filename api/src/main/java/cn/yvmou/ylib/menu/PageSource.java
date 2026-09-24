package cn.yvmou.ylib.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 分页数据源：菜单不再自己切数据，而是**按页问业务侧要**。
 * <p>
 * 为什么不是「给一份完整列表、框架切」：真实数据几乎总是分页查询（SQL 的 {@code LIMIT/OFFSET}、
 * 缓存里的分段），给完整列表等于逼调用方先把全量读进内存，再让框架丢掉绝大部分。
 * <p>
 * 契约（{@code MenuDefinition} 渲染时依赖它，写错会静默显示错数据，因此写清楚）：
 * <ol>
 *   <li>{@link #fetch} 返回的**长度可以小于** {@code perPage}：末页就是不满的；</li>
 *   <li>返回的**长度不应大于** {@code perPage}：超出部分会被截断并记一条 warning
 *       （它意味着业务侧的 perPage 与布局里的槽位数没对齐）；</li>
 *   <li>页内顺序就是槽位顺序：{@code fetch(...).get(0)} 落进该符号的第 1 个槽位；</li>
 *   <li>条目不足时框架把多出来的槽位**显式写成空气**，不会沿用上一次渲染的残留。</li>
 * </ol>
 */
public interface PageSource {

    /** 取某一页的条目（0 起）。 */
    List<ItemStack> fetch(Player viewer, int page);

    /**
     * 总条目数；{@code -1} 表示**未知**（游标式 / 无限流数据）。
     * <p>
     * 未知时 {@link MenuContext#pageCount()} 返回 -1，页码指示器应当显示 {@code ?}，
     * 而「下一页」只能靠「本页返回的条数 &lt; perPage 即到底」判断。
     */
    default int total(Player viewer) {
        return -1;
    }
}
