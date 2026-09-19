package cn.yvmou.ylib.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 布局表测试：这张图是「界面长什么样」的唯一声明，写错的表现是格子错位、物品跑到别的格子里。
 * 它是代码里的布局，因此每条错误都当场抛；一个名字占多格（动态槽位）是最容易写错的地方，重点钉住。
 */
class SlotLayoutTest {

    @Test
    @DisplayName("一个字符就是一格；反引号把长名字包成一格")
    void oneCellPerCharacter() {
        SlotLayout layout = SlotLayout.parse(54, "`diamond`123`门`");

        assertEquals(Arrays.asList(0), layout.slots("diamond"));
        assertEquals(Arrays.asList(1), layout.slots("1"));
        assertEquals(Arrays.asList(4), layout.slots("门"), "反引号里的中文只占一格");
        assertEquals(5, layout.names().size());
    }

    @Test
    @DisplayName("一个名字可以占多格：这就是动态槽位（######### 是 9 格同名）")
    void oneNameManyCells() {
        SlotLayout layout = SlotLayout.parse(54, "#########", "#########");

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
                layout.slots("#"));
    }

    @Test
    @DisplayName("格子按下标升序（先上后下、先左后右），空格占一格但不算名字")
    void cellsInReadingOrder() {
        SlotLayout layout = SlotLayout.parse(54, "# #", "", "  #");

        assertEquals(Arrays.asList(0, 2, 20), layout.slots("#"));
        assertEquals(1, layout.names().size(), "空格不登记名字");
    }

    @Test
    @DisplayName("两个视角：slots(名字) 给全部格子，take(名字, n) 给动态填充要用的前 n 格")
    void takeForDynamicSlots() {
        SlotLayout layout = SlotLayout.parse(54, "###");

        assertEquals(Arrays.asList(0, 1), layout.take("#", 2), "有几个物品就取几格");
        assertEquals(Arrays.asList(0, 1, 2), layout.take("#", 99), "物品比格子多就有多少格给多少格");
        assertTrue(layout.take("#", 0).isEmpty());
        assertTrue(layout.take("#", -1).isEmpty(), "负数当 0，不能炸");
    }

    @Test
    @DisplayName("emoji（代理对）与中文都只算一格")
    void codePointsCountAsOne() {
        SlotLayout layout = SlotLayout.parse(54, "`门`\uD83D\uDD25#");

        assertEquals(Arrays.asList(1), layout.slots("\uD83D\uDD25"));
        assertEquals(Arrays.asList(2), layout.slots("#"), "emoji 只占一格，后面的下标不该被撑开");
    }

    @Test
    @DisplayName("写错就抛：一行超过 9 格、行数超过容器、反引号没闭合、空表")
    void malformedLayoutThrows() {
        assertThrows(IllegalArgumentException.class, () -> SlotLayout.parse(54, "##########"));
        assertThrows(IllegalArgumentException.class, () -> SlotLayout.parse(27, "#", "#", "#", "#"));
        assertThrows(IllegalArgumentException.class, () -> SlotLayout.parse(54, "`prev"));
        assertThrows(IllegalArgumentException.class, () -> SlotLayout.parse(54));
    }

    @Test
    @DisplayName("名字不存在时抛错并列出可选名字（写错名字不该静默摆到 0 号格）")
    void unknownNameThrows() {
        SlotLayout layout = SlotLayout.parse(54, "`save`");

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> layout.slots("sav"));
        assertTrue(failure.getMessage().contains("sav"), failure.getMessage());
        assertTrue(failure.getMessage().contains("save"), "错误信息要给出可选的名字: " + failure.getMessage());
    }

    @Test
    @DisplayName("返回的集合不可改：拿到就用，改不动（库不该把自己内部状态漏出去）")
    void viewsAreImmutable() {
        SlotLayout layout = SlotLayout.parse(54, "##");
        List<Integer> slots = layout.slots("#");

        assertThrows(UnsupportedOperationException.class, () -> slots.add(9));
        assertThrows(UnsupportedOperationException.class, () -> layout.names().clear());
    }

    @Test
    @DisplayName("同一个名字用 `#` 与 # 两种写法是同一个槽位（反引号只是语法）")
    void backticksAreJustSyntax() {
        SlotLayout layout = SlotLayout.parse(54, "`#`#");

        assertEquals(Arrays.asList(0, 1), layout.slots("#"));
    }
}
