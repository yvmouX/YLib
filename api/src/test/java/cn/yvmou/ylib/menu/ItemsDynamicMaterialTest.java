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
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code Items.of(函数)} 的契约：材质在**每次渲染**时求值，求值失败不打断界面。
 * <p>
 * 这条能力是给「这一格显示的正是数据里的那件东西」用的（创建任务表单的图标格）：静态材质在注册期
 * 就定稿了，所以它必须有单独的入口；而「框架不该被一个写坏的取值函数弄崩」与占位符求值失败
 * 是同一个要求，因此也要钉住。
 */
class ItemsDynamicMaterialTest {

    @Test
    @DisplayName("动态材质：渲染时按上下文求值，材质随数据变")
    void dynamicMaterialResolvesPerRender() {
        Material[] next = {Material.DIAMOND};
        Items items = Items.of(ctx -> next[0]).name("&e图标").build();
        MenuContext ctx = mock(MenuContext.class);

        assertEquals(Material.DIAMOND, items.materialFor(ctx));
        next[0] = Material.GOLD_INGOT;
        assertEquals(Material.GOLD_INGOT, items.materialFor(ctx), "同一个定义应该每次现算");
        assertTrue(items.dynamicMaterial());
        assertEquals(null, items.material(), "动态材质的格子没有静态材质");
    }

    @Test
    @DisplayName("静态材质：materialFor 原样返回，不算动态")
    void staticMaterialIsUntouched() {
        Items items = Items.of(Material.BOOK).build();
        assertFalse(items.dynamicMaterial());
        assertEquals(Material.BOOK, items.materialFor(mock(MenuContext.class)));
    }

    @Test
    @DisplayName("取值函数返回 null 或抛异常：回落纸，不让界面开不出来")
    void brokenSourceFallsBackToPaper() {
        MenuContext ctx = mock(MenuContext.class);
        assertEquals(Material.PAPER, Items.of(x -> null).build().materialFor(ctx));
        assertEquals(Material.PAPER, Items.of(x -> {
            throw new IllegalStateException("坏掉了");
        }).build().materialFor(ctx));
    }

    @Test
    @DisplayName("渲染出来的物品用的是这一次算出的材质")
    void renderUsesResolvedMaterial() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<MenuItems> menuItems = mockStatic(MenuItems.class)) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                    .thenReturn(mock(Inventory.class));
            menuItems.when(() -> MenuItems.render(any(Material.class), anyString(), anyList()))
                    .thenReturn(new ItemStack(Material.STONE));

            Player viewer = mock(Player.class);
            when(viewer.getUniqueId()).thenReturn(UUID.randomUUID());

            MenuTemplate template = new MenuTemplate() {
                @Override
                public String[] layout() {
                    return new String[]{"i########"};
                }

                @Override
                public String title() {
                    return "t";
                }

                @Override
                public void declare(MenuBuilder b) {
                    // 带 lore 的写法：lore 为 null 时走的是另一个重载，mock 匹配不到
                    b.symbol('i').of(Items.of(ctx -> Material.DIAMOND).name("&e图标").lore(" "));
                    b.symbol('#').of(Items.filler());
                }
            };
            MenuBuilder builder = new MenuBuilder();
            template.declare(builder);
            MenuDefinition definition = builder.build(template, null);

            MenuView.create(definition, viewer, MenuSession.of("k", viewer, 1800L), Map.of());

            menuItems.verify(() -> MenuItems.render(eq(Material.DIAMOND), anyString(),
                    org.mockito.ArgumentMatchers.anyList()));
        }
    }

    @Test
    @DisplayName("merge：文件只改了名字 / lore 时动态材质原样留着（配置层靠这条）")
    void dynamicMaterialSurvivesMerge() {
        Items base = Items.of(ctx -> Material.DIAMOND).name("代码名").lore("代码说明").build();
        Items fromFile = Items.basedOn(base).name("文件里的名").resolve(base);

        assertTrue(fromFile.dynamicMaterial(), "文件没写 material 时不该把动态材质弄丢");
        assertEquals(Material.DIAMOND, fromFile.materialFor(null));
        assertEquals("文件里的名", fromFile.name(), "name 以文件为准");
        assertEquals(List.of("代码说明"), fromFile.lore(), "文件没写 lore 就回落代码默认值");
    }

    @Test
    @DisplayName("merge：材质只有一个来源——显式给了静态材质就顶掉动态的（不会两边都留着）")
    void staticMaterialReplacesDynamicOnMerge() {
        Items base = Items.of(ctx -> Material.DIAMOND).build();
        Items merged = Items.of(Material.STONE).resolve(base);

        assertFalse(merged.dynamicMaterial());
        assertEquals(Material.STONE, merged.material());
        assertEquals(Material.STONE, merged.materialFor(null), "两个 getter 必须说同一件事");
    }
}
