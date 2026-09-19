package cn.yvmou.ylib.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页算术测试：翻页差一位在游戏里只表现为「某一页少一条」，是最难靠肉眼发现的一类 bug，
 * 因此总页数 / 页码夹紧 / 切片范围全部钉住。
 */
class PagingTest {

    @Test
    @DisplayName("总页数：够除就是整除，多一条就多一页，空列表也算 1 页")
    void totalPages() {
        assertEquals(1, Paging.totalPages(0, 45), "空列表给 1 页，空界面比 0 页好解释");
        assertEquals(1, Paging.totalPages(1, 45));
        assertEquals(1, Paging.totalPages(45, 45));
        assertEquals(2, Paging.totalPages(46, 45), "第 46 条要翻到第二页");
        assertEquals(3, Paging.totalPages(100, 45));
    }

    @Test
    @DisplayName("页码夹紧：越界夹回最后一页，负数当 0")
    void clampPage() {
        assertEquals(0, Paging.clampPage(0, 5, 45));
        assertEquals(0, Paging.clampPage(-3, 5, 45));
        assertEquals(2, Paging.clampPage(9, 100, 45), "数据变少后旧页码越界，夹回最后一页而不是空白页");
    }

    @Test
    @DisplayName("切片范围：包含起点、不含终点，最后一条不漏不越界")
    void slice() {
        assertEquals(0, Paging.fromIndex(0, 45));
        assertEquals(45, Paging.toIndex(0, 45, 100));
        assertEquals(45, Paging.fromIndex(1, 45));
        assertEquals(90, Paging.toIndex(1, 45, 100));
        assertEquals(90, Paging.fromIndex(2, 45));
        assertEquals(100, Paging.toIndex(2, 45, 100), "最后一页只到条目总数，不能越界");
        assertEquals(0, Paging.toIndex(0, 45, 0));
    }

    @Test
    @DisplayName("每页格数为 0 或负数时当 1，不除零")
    void pageSizeGuard() {
        assertEquals(45, Paging.totalPages(45, 0));
        assertEquals(0, Paging.fromIndex(0, 0));
        assertEquals(1, Paging.toIndex(0, -5, 45));
    }

    @Test
    @DisplayName("切片：最后一页只到条目总数，页码越界自动夹回，空列表给空表")
    void sliceOfPage() {
        List<Integer> items = new ArrayList<Integer>();
        for (int index = 0; index < 100; index++) {
            items.add(index);
        }

        assertEquals(Arrays.asList(0, 1, 2), Paging.slice(items, 0, 3));
        assertEquals(Arrays.asList(9, 10, 11), Paging.slice(items, 3, 3));
        List<Integer> last = Paging.slice(items, 99, 45);
        assertEquals(10, last.size(), "页码越界夹回最后一页：100 条按 45 一页，最后一页是 90~99");
        assertEquals(90, last.get(0).intValue());
        assertEquals(99, last.get(9).intValue());
        assertEquals(100, Paging.slice(items, 0, 1000).size());
        assertTrue(Paging.slice(Collections.<Integer>emptyList(), 0, 45).isEmpty());
        assertTrue(Paging.slice(null, 0, 45).isEmpty(), "null 当空列表，不能炸");
    }
}
