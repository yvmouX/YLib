package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.MenuItems;
import cn.yvmou.ylib.menu.internal.MenuRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 「这一格接收物品」的**派发契约**：谁先拿到、事件取消没有、什么手势才算。
 * <p>
 * 这几条全在事件处理里，真机上只能靠人反复试（「我拿着方块点了一下按钮，按钮就没了」）；
 * 单测把容器与渲染换成替身，直接对着 {@link MenuRegistry} 的事件方法断言。
 * <p>
 * 监听器只由 {@code Menus.init} 登记、构造器私有，因此这里反射取一个实例——要测的两个事件方法
 * 是 public 的，逻辑本身不依赖注册。
 */
class ItemDropDispatchTest {

    /** 第 0 格接收物品、也点得动；第 1 格只点得动（对照组）。 */
    private static final int ACCEPTING_SLOT = 0;
    private static final int PLAIN_SLOT = 1;

    /** 菜单只有一行（9 格），54 号是玩家背包里的格。 */
    private static final int OUTSIDE_MENU_SLOT = 54;

    private final List<String> drops = new ArrayList<String>();
    private final List<String> clicks = new ArrayList<String>();

    private MenuView view;
    private InventoryView inventoryView;

    @Test
    @DisplayName("光标带物品点接收格：回调拿到那件物品，事件被取消，点击回调不跑")
    void cursorItemGoesToDropHandler() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            render(bukkit, menuItems);

            click(ACCEPTING_SLOT, new ItemStack(Material.DIAMOND));

            assertEquals(List.of("0:DIAMOND"), drops);
            assertEquals(List.of(), clicks, "手上有物品时点击回调不该再触发");
        }
    }

    @Test
    @DisplayName("光标空着点接收格：走普通点击回调，不碰接收物品的回调")
    void emptyCursorGoesToClickHandler() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            render(bukkit, menuItems);

            click(ACCEPTING_SLOT, new ItemStack(Material.AIR));

            assertEquals(List.of(), drops);
            assertEquals(List.of("i"), clicks);
        }
    }

    @Test
    @DisplayName("光标带物品点不接收的格子：物品不交给它，点击回调照常（物品也不会真放进去）")
    void cursorItemOnPlainSlotStaysAClick() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            render(bukkit, menuItems);

            click(PLAIN_SLOT, new ItemStack(Material.DIAMOND));

            assertEquals(List.of(), drops);
            assertEquals(List.of("c"), clicks);
        }
    }

    @Test
    @DisplayName("拖拽只落在接收格：走进来并拦下原版分配")
    void dragOntoAcceptingSlotDrops() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            render(bukkit, menuItems);

            InventoryDragEvent event = drag(Set.of(ACCEPTING_SLOT), new ItemStack(Material.EMERALD));

            assertEquals(List.of("0:EMERALD"), drops);
            verify(event).setCancelled(true);
        }
    }

    @Test
    @DisplayName("拖拽落在不接收的格子：不交给任何人，但 READ_ONLY 下照样拦下")
    void dragOntoPlainSlotStaysCancelled() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            render(bukkit, menuItems);

            InventoryDragEvent event = drag(Set.of(PLAIN_SLOT), new ItemStack(Material.EMERALD));

            assertEquals(List.of(), drops);
            verify(event).setCancelled(true);
        }
    }

    @Test
    @DisplayName("拖过好几格不认：「放进某一格」才是这一格，分配式拖拽交给交互策略处理")
    void dragAcrossManySlotsIsNotADrop() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            render(bukkit, menuItems);

            InventoryDragEvent event = drag(Set.of(ACCEPTING_SLOT, PLAIN_SLOT), new ItemStack(Material.EMERALD));

            assertEquals(List.of(), drops, "拖过多格不是「放进这一格」");
            verify(event).setCancelled(true);
        }
    }

    @Test
    @DisplayName("整段落在玩家背包里的拖拽不归本库管：连取消都不该动（原版要先把物品拖起来）")
    void dragOutsideMenuIsUntouched() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            render(bukkit, menuItems);

            InventoryDragEvent event = drag(Set.of(OUTSIDE_MENU_SLOT), new ItemStack(Material.EMERALD));

            verify(event, never()).setCancelled(true);
            assertTrue(drops.isEmpty() && clicks.isEmpty());
        }
    }

    // ---------- 环境 ----------

    /** 装出一个最小环境：一行布局（接收格 + 普通格 + 填充），并渲染出视图。 */
    private void render(MockedStatic<Bukkit> bukkit, MockedStatic<MenuItems> menuItems) {
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
                return new String[]{"ic#######"};
            }

            @Override
            public String title() {
                return "t";
            }

            @Override
            public void declare(MenuBuilder b) {
                b.symbol('i').of(Items.of(Material.ITEM_FRAME).name("i").lore(" "))
                        .onItemDrop((slot, ctx, dropped) ->
                                drops.add(slot.index() + ":" + dropped.getType().name()))
                        .onClick((slot, ctx) -> clicks.add("i"));
                b.symbol('c').of(Items.of(Material.BOOK).name("c").lore(" "))
                        .onClick((slot, ctx) -> clicks.add("c"));
                b.symbol('#').of(Items.filler());
            }
        };
        MenuBuilder builder = new MenuBuilder();
        template.declare(builder);
        MenuDefinition definition = builder.build(template, null);

        view = MenuView.create(definition, viewer, MenuSession.of("k", viewer, 1800L), Map.of());
        inventoryView = mock(InventoryView.class);
        when(inventoryView.getTopInventory()).thenReturn(view.inventory());
    }

    /**
     * 造一次点击：光标带什么、点在哪一格。
     * <p>
     * 只断言「拦下了」而不是「拦了几次」：{@code READ_ONLY} 与接收物品这两条路各会取消一次，
     * 重复取消是无害的，测试不该把它钉成契约。
     */
    private void click(int slot, ItemStack cursor) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(inventoryView);
        when(event.getClickedInventory()).thenReturn(view.inventory());
        when(event.getSlot()).thenReturn(slot);
        when(event.getClick()).thenReturn(org.bukkit.event.inventory.ClickType.LEFT);
        when(event.getCursor()).thenReturn(cursor);
        registry().onClick(event);
        verify(event, atLeastOnce()).setCancelled(true);
    }

    /** 造一次拖拽：拖过哪些（原始）格子、手上拖的是哪一件。 */
    private InventoryDragEvent drag(Set<Integer> rawSlots, ItemStack cursor) {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getView()).thenReturn(inventoryView);
        when(event.getRawSlots()).thenReturn(rawSlots);
        when(event.getOldCursor()).thenReturn(cursor);
        registry().onDrag(event);
        return event;
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
