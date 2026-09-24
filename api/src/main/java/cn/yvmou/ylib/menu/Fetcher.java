package cn.yvmou.ylib.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 分页取数的**便捷写法**：{@code (viewer, page) -> 这一页的条目}。
 * <p>
 * 与 {@link PageSource} 是同一件事，只是不必写一个匿名类——只要不需要 {@code total()}
 * （页码指示器显示 {@code ?}、「下一页」按「铺满即还有」判断）就用它。
 * <p>
 * 需要让 {@code pageCount()} 有确定值时，请实现 {@link PageSource} 并覆盖 {@code total()}。
 */
@FunctionalInterface
public interface Fetcher {

    List<ItemStack> fetch(Player viewer, int page);
}
