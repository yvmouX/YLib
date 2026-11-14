package cn.yvmou.ylib.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 分页取数的**便捷写法**：{@code (viewer, page, perPage) -> 这一页的条目}。
 * <p>
 * 与 {@link PageSource} 是同一件事，只是不必写一个匿名类——只要不需要 {@code total()}
 * （页码指示器显示 {@code ?}、「下一页」按「这一页取满了没」判断）就用它。
 * <p>
 * <b>为什么容量是参数而不是常量</b>：每页几条由布局决定（= 该符号在布局里出现的格数）。
 * 代码里再写一个常量就得跟布局保持同步，而「改了布局忘了改常量」正是这类界面最典型的静默错误。
 * 需要让 {@code pageCount()} 有确定值时，请实现 {@link PageSource} 并覆盖 {@code total()}。
 */
@FunctionalInterface
public interface Fetcher {

    /**
     * 取某一页的条目（0 起）。
     *
     * @param perPage 该符号在当前布局下的每页条数；切片要用它，不要另写常量
     */
    List<ItemStack> fetch(Player viewer, int page, int perPage);
}
