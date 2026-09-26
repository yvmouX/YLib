package cn.yvmou.ylib.menu;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link Items#resolveOr} 的回退契约：认得出来用解析值，认不出来（null / 空串 / 畸形名字）
 * 一律退 fallback——数据里的名字不该让界面开不出来。
 */
class ItemsResolveTest {

    @BeforeAll
    static void setUp() {
        TestServer.install();
    }

    @Test
    @DisplayName("认得出来的名字：用解析值")
    void knownNameResolves() {
        assertEquals(Material.STONE, Items.resolveOr("STONE", Material.PAPER));
        assertEquals(Material.STONE, Items.resolveOr("stone", Material.PAPER), "大小写不敏感");
    }

    @Test
    @DisplayName("认不出来的名字：退回 fallback，不抛")
    void unknownNameFallsBack() {
        assertEquals(Material.PAPER, Items.resolveOr("not_a_material", Material.PAPER));
        assertEquals(Material.PAPER, Items.resolveOr("", Material.PAPER));
        assertEquals(Material.PAPER, Items.resolveOr(null, Material.PAPER));
    }
}
