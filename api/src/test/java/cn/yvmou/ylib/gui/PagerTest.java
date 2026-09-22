package cn.yvmou.ylib.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 翻页：算术（总页数 / 页码夹紧 / 切片）与状态（当前页 + 总页数）都在 {@link Pager} 里。
 * <p>
 * 翻页差一位在游戏里只表现为「某一页少一条」，是最难靠肉眼发现的一类 bug，因此逐项钉住。
 */
class PagerTest {

    @Test
    @DisplayName("按条目数与每页格数算：页数、页码、能不能翻")
    void fromItems() {
        Pager pager = Pager.of(0, 20, 9);

        assertEquals(0, pager.page());
        assertEquals(1, pager.pageNumber(), "给玩家看的页码从 1 起");
        assertEquals(3, pager.totalPages(), "20 条按 9 格一页是 3 页");
        assertFalse(pager.hasPrevious());
        assertTrue(pager.hasNext());
    }

    @Test
    @DisplayName("页码越界夹回合法范围：数据变少后旧页码不该给出一页空白")
    void outOfRangePageIsClamped() {
        assertEquals(2, Pager.of(99, 20, 9).page());
        assertEquals(0, Pager.of(-5, 20, 9).page(), "负数当第一页");
        assertFalse(Pager.of(99, 20, 9).hasNext());
        assertEquals(2, new Pager(7, 3).page(), "直接构造也夹紧");
        assertEquals("Pager[3/3]", new Pager(7, 3).toString());
    }

    @Test
    @DisplayName("没有条目也是 1 页：空界面比 0 页好解释")
    void emptyIsStillOnePage() {
        Pager pager = Pager.of(0, 0, 9);

        assertEquals(1, pager.totalPages());
        assertEquals(0, pager.page());
        assertFalse(pager.hasPrevious());
        assertFalse(pager.hasNext());
    }

    @Test
    @DisplayName("总页数：够除就是整除，多一条就多一页")
    void totalPages() {
        assertEquals(1, Pager.totalPages(0, 45), "空列表给 1 页");
        assertEquals(1, Pager.totalPages(1, 45));
        assertEquals(1, Pager.totalPages(45, 45));
        assertEquals(2, Pager.totalPages(46, 45), "第 46 条要翻到第二页");
        assertEquals(3, Pager.totalPages(100, 45));
    }

    @Test
    @DisplayName("页码夹紧：越界夹回最后一页，负数当 0")
    void clampPage() {
        assertEquals(0, Pager.clampPage(0, 5, 45));
        assertEquals(0, Pager.clampPage(-3, 5, 45));
        assertEquals(2, Pager.clampPage(9, 100, 45), "数据变少后旧页码越界，夹回最后一页而不是空白页");
    }

    @Test
    @DisplayName("每页格数为 0 或负数时当 1，不除零")
    void pageSizeGuard() {
        assertEquals(45, Pager.totalPages(45, 0));
        Pager pager = Pager.of(0, 5, 0);
        assertEquals(5, pager.totalPages());
        assertEquals(1, Pager.slice(range(45), 0, 0).size(), "每页按 1 条算，第一页就只有一条");
    }

    @Test
    @DisplayName("切片：最后一页只到条目总数，页码越界自动夹回，空列表与 null 都给空表")
    void slice() {
        List<Integer> items = range(100);

        assertEquals(Arrays.asList(0, 1, 2), Pager.slice(items, 0, 3));
        assertEquals(Arrays.asList(9, 10, 11), Pager.slice(items, 3, 3));
        List<Integer> last = Pager.slice(items, 99, 45);
        assertEquals(10, last.size(), "页码越界夹回最后一页：100 条按 45 一页，最后一页是 90~99");
        assertEquals(90, last.get(0).intValue());
        assertEquals(99, last.get(9).intValue());
        assertEquals(100, Pager.slice(items, 0, 1000).size(), "一页装得下就全给");
        assertTrue(Pager.slice(Collections.<Integer>emptyList(), 0, 45).isEmpty());
        assertTrue(Pager.slice(null, 0, 45).isEmpty(), "null 当空列表，不能炸");
    }

    private static List<Integer> range(int count) {
        List<Integer> items = new ArrayList<Integer>();
        for (int index = 0; index < count; index++) {
            items.add(index);
        }
        return items;
    }
}
