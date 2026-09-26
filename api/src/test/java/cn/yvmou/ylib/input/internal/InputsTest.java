package cn.yvmou.ylib.input.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 状态机契约：不碰 Bukkit 就能钉住的那部分——
 * 取消词、超时判定、一问一答的顶替与摘除。
 */
class InputsTest {

    private static final long ALIVE = System.currentTimeMillis() + 60_000L;
    private static final long EXPIRED = System.currentTimeMillis() - 1L;

    @Test
    @DisplayName("取消词：整句「取消」或 cancel 才算，前后缀与大小写变化各有各的判法")
    void cancelWord() {
        assertTrue(Inputs.isCancelWord("取消"));
        assertTrue(Inputs.isCancelWord("cancel"));
        assertTrue(Inputs.isCancelWord("Cancel"));
        assertFalse(Inputs.isCancelWord("取消啦"), "带后缀的不是取消词");
        assertFalse(Inputs.isCancelWord("uncancel"), "带前缀的不是取消词");
        assertFalse(Inputs.isCancelWord(""));
        assertFalse(Inputs.isCancelWord("c a n c e l"));
    }

    @Test
    @DisplayName("判定：取消词与过期都走 CANCEL，其余（含空串、null）走 SUBMIT")
    void decision() {
        assertEquals(Inputs.Decision.CANCEL, Inputs.Decision.of("取消", ALIVE));
        assertEquals(Inputs.Decision.CANCEL, Inputs.Decision.of("  取消  ", ALIVE),
                "空白在判定前去除，前后带空格的取消词照样算");
        assertEquals(Inputs.Decision.CANCEL, Inputs.Decision.of("Cancel", ALIVE));
        assertEquals(Inputs.Decision.CANCEL, Inputs.Decision.of("随便一句", EXPIRED),
                "过期之后的下一句按取消处理");
        assertEquals(Inputs.Decision.SUBMIT, Inputs.Decision.of("  答案  ", ALIVE));
        assertEquals(Inputs.Decision.SUBMIT, Inputs.Decision.of("", ALIVE), "空串照常提交，语义交给调用方");
        assertEquals(Inputs.Decision.SUBMIT, Inputs.Decision.of(null, ALIVE));
    }

    @Test
    @DisplayName("顶替：重复 ask 返回旧状态且旧状态不再可取（旧回调不能因新输入被触发）")
    void putReplacesOld() {
        UUID id = UUID.randomUUID();
        Inputs.Pending old = pending(id, ALIVE);
        Inputs.Pending fresh = pending(id, EXPIRED);

        Inputs.put(old);
        assertSame(old, Inputs.put(fresh), "put 应返回被顶掉的旧状态");
        assertNotSame(old, Inputs.take(id));
        assertNull(Inputs.take(id), "新状态被摘走后表里不应再有余货");
    }

    @Test
    @DisplayName("摘除：null 与不存在的键都安全返回 null")
    void takeIsSafe() {
        assertNull(Inputs.take(null));
        assertNull(Inputs.take(UUID.randomUUID()));
    }

    @Test
    @DisplayName("clear：宿主禁用时一次清空")
    void clearEmptiesEverything() {
        Inputs.put(pending(UUID.randomUUID(), ALIVE));
        Inputs.put(pending(UUID.randomUUID(), ALIVE));
        Inputs.clear();
        assertNull(Inputs.take(UUID.randomUUID()));
    }

    private static Inputs.Pending pending(UUID id, long deadline) {
        AtomicInteger ignored = new AtomicInteger();
        Consumer<String> onAnswer = text -> ignored.incrementAndGet();
        return new Inputs.Pending(id, onAnswer, ignored::incrementAndGet, deadline);
    }
}
