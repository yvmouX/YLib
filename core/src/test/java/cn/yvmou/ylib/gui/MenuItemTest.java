package cn.yvmou.ylib.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「这一格接不接收玩家物品」的契约：默认不接收，只有 {@link MenuItem#acceptItem} 造出来的接收。
 * <p>
 * 测试 JVM 里没有服务端，而 {@code ItemStack#getItemMeta()} 会去问 {@code Bukkit.getItemFactory()}，
 * 不装一个假服务端连 {@code of(...)} 都造不出来。这里只回答「有服务端」这一件事：
 * 假工厂返回的 meta 是 null，图标渲染因此在拿不到 meta 时提前返回，正好绕开渲染细节。
 * 真要服务端的部分（拖拽 / 点击事件怎么派发、回调拿到的是不是克隆）测不到，只能真机验。
 */
class MenuItemTest {

    @BeforeAll
    static void fakeServer() {
        ItemFactory factory = (ItemFactory) Proxy.newProxyInstance(MenuItemTest.class.getClassLoader(),
                new Class<?>[]{ItemFactory.class}, (proxy, method, args) -> null);
        Server server = (Server) Proxy.newProxyInstance(MenuItemTest.class.getClassLoader(),
                new Class<?>[]{Server.class}, (proxy, method, args) -> {
                    if ("getItemFactory".equals(method.getName())) {
                        return factory;
                    }
                    // setServer 会自己写一行启动日志，得给它一个真的 Logger
                    return "getLogger".equals(method.getName()) ? Logger.getLogger("MenuItemTest") : null;
                });
        Bukkit.setServer(server);
    }

    @Test
    @DisplayName("默认不接收：of / display / filler / 自己拼的 ItemStack 都只响应点击")
    void notAcceptingByDefault() {
        assertFalse(MenuItem.of(Material.DIAMOND, "&b钻石", lore -> { }, null).acceptsItems());
        assertFalse(MenuItem.display(Material.DIAMOND, "&b钻石", lore -> { }).acceptsItems());
        assertFalse(MenuItem.filler().acceptsItems());

        MenuItem raw = new MenuItem(new ItemStack(Material.PAPER), null);
        assertFalse(raw.acceptsItems());
        assertNull(raw.onItem(), "不接收物品就没有回调可调");
    }

    @Test
    @DisplayName("acceptItem 造出来的格子接收物品，图标照旧、回调原样拿着")
    void acceptItemReceives() {
        Consumer<ItemStack> onItem = item -> { };
        MenuItem item = MenuItem.acceptItem(Material.DIAMOND, "&b钻石", lore -> { }, onItem, null);

        assertTrue(item.acceptsItems());
        assertSame(onItem, item.onItem());
        assertEquals(Material.DIAMOND, item.icon().getType(), "图标还是那一格自己的图标");
    }

    @Test
    @DisplayName("withAmount / glow 只是换一份图标，接收物品这件事跟着走（别在复制时丢掉）")
    void copiesKeepAccepting() {
        MenuItem item = MenuItem.acceptItem(Material.DIAMOND, "&b钻石", lore -> { }, i -> { }, null);

        assertTrue(item.withAmount(3).acceptsItems());
        assertTrue(item.glow().acceptsItems());
        assertSame(item.onItem(), item.withAmount(3).onItem());
    }
}
