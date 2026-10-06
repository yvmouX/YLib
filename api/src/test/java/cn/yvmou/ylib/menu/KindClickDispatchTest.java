package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.MenuItems;
import cn.yvmou.ylib.menu.internal.MenuRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * **键位回调的派发契约**：{@code onLeftClick} / {@code onRightClick} 注册进的是「按键位分表」的回调，
 * 派发层必须拿着**真实的 ClickType** 在点击那一刻去解析——渲染期不知道玩家会按哪个键，
 * 在那里提前解析只能拿到兜底回调，键位回调会全部失联（点了没反应、也没有任何日志）。
 * <p>
 * 这正是出过的事：派发层曾用渲染期缓存的那一个回调（等价于「只看兜底」），
 * 于是所有只注册了键位回调的格子都静默失灵。这条测试就是钉住「不许再退回去」。
 */
class KindClickDispatchTest {

    private final List<String> clicks = new ArrayList<String>();

    @Test
    @DisplayName("左键只落到 onLeftClick 那一格；右键只落到 onRightClick 那一格")
    void kindClicksReachTheirOwnHandler() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            MenuView view = render(bukkit, menuItems);

            click(view, 0, org.bukkit.event.inventory.ClickType.LEFT);
            assertEquals(List.of("L"), clicks, "左键该落到 onLeftClick 的格子");

            clicks.clear();
            click(view, 1, org.bukkit.event.inventory.ClickType.RIGHT);
            assertEquals(List.of("R"), clicks, "右键该落到 onRightClick 的格子");
        }
    }

    @Test
    @DisplayName("键位不匹配时落到别的格子头上：右键点 onLeftClick 的格子什么都不发生")
    void wrongKindDoesNotFire() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            MenuView view = render(bukkit, menuItems);

            click(view, 0, org.bukkit.event.inventory.ClickType.RIGHT);
            assertEquals(List.of(), clicks, "onLeftClick 的格子不该响应右键");

            click(view, 1, org.bukkit.event.inventory.ClickType.LEFT);
            assertEquals(List.of(), clicks, "onRightClick 的格子不该响应左键");
        }
    }

    @Test
    @DisplayName("普通 onClick（兜底回调）左右键都照常：键位化没有把老写法带坏")
    void fallbackStillFiresForBothKinds() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            MenuView view = render(bukkit, menuItems);

            click(view, 2, org.bukkit.event.inventory.ClickType.LEFT);
            click(view, 2, org.bukkit.event.inventory.ClickType.RIGHT);

            assertEquals(List.of("O", "O"), clicks);
        }
    }

    @Test
    @DisplayName("Shift 组合没单独注册时退化到不带修饰键的那一个（文档承诺的回退）")
    void shiftFallsBackToPlainKind() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            MenuView view = render(bukkit, menuItems);

            click(view, 0, org.bukkit.event.inventory.ClickType.SHIFT_LEFT);

            assertEquals(List.of("L"), clicks, "Shift+左键没单独注册时该当普通左键");
        }
    }

    // ---------- 环境 ----------

    /** 一行布局：`l` 只注册左键、`r` 只注册右键、`o` 是普通兜底回调，`#` 是填充。 */
    private MenuView render(MockedStatic<Bukkit> bukkit, MockedStatic<MenuItems> menuItems) {
        bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                .thenAnswer(invocation -> {
                    InventoryHolder holder = invocation.getArgument(0);
                    Inventory inventory = mock(Inventory.class);
                    when(inventory.getHolder()).thenReturn(holder);
                    return inventory;
                });
        menuItems.when(() -> MenuItems.render(any(Material.class), anyString(), anyList()))
                .thenReturn(new ItemStack(Material.STONE));

        Player viewer = mock(Player.class);
        when(viewer.getUniqueId()).thenReturn(UUID.randomUUID());

        MenuTemplate template = new MenuTemplate() {
            @Override
            public String[] layout() {
                return new String[]{"lro######"};
            }

            @Override
            public String title() {
                return "t";
            }

            @Override
            public void declare(MenuBuilder b) {
                b.symbol('l').of(Items.of(Material.LIME_WOOL).name("l").lore(" "))
                        .onLeftClick((slot, ctx) -> clicks.add("L"));
                b.symbol('r').of(Items.of(Material.RED_WOOL).name("r").lore(" "))
                        .onRightClick((slot, ctx) -> clicks.add("R"));
                b.symbol('o').of(Items.of(Material.OAK_PLANKS).name("o").lore(" "))
                        .onClick((slot, ctx) -> clicks.add("O"));
                b.symbol('#').of(Items.filler());
            }
        };
        MenuBuilder builder = new MenuBuilder();
        template.declare(builder);
        MenuDefinition definition = builder.build(template, null);

        return MenuView.create(definition, viewer, MenuSession.of("k", viewer, 1800L), Map.of());
    }

    private void click(MenuView view, int slot, org.bukkit.event.inventory.ClickType type) {
        InventoryView inventoryView = mock(InventoryView.class);
        when(inventoryView.getTopInventory()).thenReturn(view.inventory());

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(inventoryView);
        when(event.getClickedInventory()).thenReturn(view.inventory());
        when(event.getSlot()).thenReturn(slot);
        when(event.getClick()).thenReturn(type);
        when(event.getCursor()).thenReturn(new ItemStack(Material.AIR));
        when(event.getInventory()).thenReturn(view.inventory());
        when(event.getInventoryType()).thenReturn(InventoryType.CHEST);

        registry().onClick(event);
    }

    private static MenuRegistry registry() {
        try {
            Constructor<MenuRegistry> constructor = MenuRegistry.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException impossible) {
            throw new IllegalStateException("MenuRegistry 反射实例化失败", impossible);
        }
    }
}
