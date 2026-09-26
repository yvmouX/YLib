package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.MenuItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * {@link MenuView} 渲染契约的运行时验证：静态符号在**每次绘制时**求值 ph 占位符，
 * 名字与 lore 都要被替换——这是「当前: （未填） → 当前: 玩家填的值」这条体验的底层保证。
 * <p>
 * Bukkit 的容器与 {@link MenuItems} 的成品渲染全部 mock 掉，只验证「传给最终渲染的名字 / lore
 * 是占位符已替换后的文案」；Bukkit 的 ItemMeta 行为不在此测试范围。
 */
class MenuViewPlaceholderTest {

    @Test
    @DisplayName("静态符号绘制时：名字与 lore 的 {value} 都被 ph 求值替换")
    void staticSymbolRenderGetsEvaluatedPlaceholder() {
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class);
        try {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                    .thenReturn(mock(Inventory.class));
            menuItems.when(() -> MenuItems.render(any(Material.class), anyString(), anyList()))
                    .thenReturn(new ItemStack(Material.STONE));

            Player viewer = mock(Player.class);
            when(viewer.getUniqueId()).thenReturn(UUID.randomUUID());

            MenuTemplate template = new MenuTemplate() {
                @Override
                public String[] layout() {
                    return new String[]{"c########"};
                }

                @Override
                public String title() {
                    return "t";
                }

                @Override
                public void declare(MenuBuilder b) {
                    b.symbol('c').of(Items.of(Material.BOOK)
                                    .name("&e{value}")
                                    .lore("&7当前: &f{value}"))
                            .ph("value", ctx -> ctx.session().<String>get("v"));
                    b.symbol('#').of(Items.filler());
                }
            };
            MenuBuilder builder = new MenuBuilder();
            template.declare(builder);
            MenuDefinition definition = builder.build(template, null);

            MenuSession session = MenuSession.of("k", viewer, 1800L);
            session.put("v", "草稿里的值");

            MenuView.create(definition, viewer, session, Map.of());

            ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<List<String>> lore = ArgumentCaptor.forClass(List.class);
            menuItems.verify(() -> MenuItems.render(eq(Material.BOOK), name.capture(), lore.capture()));
            assertEquals("&e草稿里的值", name.getValue(), "名字的占位符应被求值替换");
            assertEquals(List.of("&7当前: &f草稿里的值"), lore.getValue(), "lore 的占位符应被求值替换");
        } finally {
            bukkit.close();
            menuItems.close();
        }
    }
}
