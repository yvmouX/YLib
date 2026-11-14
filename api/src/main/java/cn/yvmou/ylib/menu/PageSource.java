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
 * <b>每页几条由布局决定</b>：它就是该符号在布局里出现的格数，作为 {@code perPage} 参数交给你。
 * 代码里不必（也不该）再写一个常量——「改了布局忘了改常量」是这类界面最典型的静默错误。
 * <p>
 * <b>这个接口不是函数式接口，是刻意的</b>：{@link #total} 没有默认值，因此它不会和
 * {@link Fetcher} 抢 lambda——{@code (viewer, page, perPage) -> ...} 只可能是 {@code Fetcher}。
 * 「总条数不知道」不是一种要传 {@code -1} 的状态，而是「你该用 {@link Fetcher}」。
 * <p>
 * 契约（{@code MenuDefinition} 渲染时依赖它，写错会静默显示错数据，因此写清楚）：
 * <ol>
 *   <li>{@link #fetch} 返回的**长度可以小于** {@code perPage}：末页就是不满的；</li>
 *   <li>返回的**长度不应大于** {@code perPage}：超出部分会被截断并记一条 warning
 *       （它意味着业务侧没按 {@code perPage} 切片）；</li>
 *   <li>页内顺序就是槽位顺序：{@code fetch(...).get(0)} 落进该符号的第 1 个槽位；</li>
 *   <li>条目不足时框架把多出来的槽位**显式写成空气**，不会沿用上一次渲染的残留。</li>
 * </ol>
 */
public interface PageSource {

    /**
     * 取某一页的条目（0 起）。
     *
     * @param perPage 该符号在当前布局下的每页条数（= 它在布局里出现的格数）
     */
    List<ItemStack> fetch(Player viewer, int page, int perPage);

    /**
     * 总条目数；它决定 {@link MenuContext#pageCount()}。
     * <p>
     * 边界情况由框架兜住：返回 0 而这一页却取到了条目时，{@code pageCount()} 至少给 1，
     * 免得页码指示器显示「第 1/0 页」。真不知道总数就别实现这个接口，用 {@link Fetcher}。
     */
    int total(Player viewer);
}
