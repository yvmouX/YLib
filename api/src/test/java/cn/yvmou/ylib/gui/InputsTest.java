package cn.yvmou.ylib.gui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 聊天输入状态机的纯逻辑测试：事件、调度、文本渲染都要一整个服务端，这里只钉住
 * 「哪句话算提交、哪句话算取消」与「同一玩家重复 ask 只保留最后一次」——
 * 这两类语义写错了在游戏里只表现为「填的值丢了」，靠肉眼很难发现。
 */
class InputsTest {

    @AfterEach
    void clearState() {
        Inputs.PENDING.clear();
    }

    @Test
    @DisplayName("取消：中文「取消」与任意大小写的 cancel 都算放弃，其余一律采纳")
    void decision() {
        assertEquals(Inputs.Decision.CANCEL, Inputs.Decision.of("取消"));
        assertEquals(Inputs.Decision.CANCEL, Inputs.Decision.of("  取消  "), "玩家常带空格");
        assertEquals(Inputs.Decision.CANCEL, Inputs.Decision.of("cancel"));
        assertEquals(Inputs.Decision.CANCEL, Inputs.Decision.of("CANCEL"));
        assertEquals(Inputs.Decision.CANCEL, Inputs.Decision.of("Cancel"));

        assertEquals(Inputs.Decision.SUBMIT, Inputs.Decision.of("取消一下"), "整句相等才算取消，不做包含匹配");
        assertEquals(Inputs.Decision.SUBMIT, Inputs.Decision.of("cancelled"));
        assertEquals(Inputs.Decision.SUBMIT, Inputs.Decision.of(""), "空串是「清空」而不是取消");
        assertEquals(Inputs.Decision.SUBMIT, Inputs.Decision.of(null), "null 按空串处理");
        assertEquals(Inputs.Decision.SUBMIT, Inputs.Decision.of("   "));
        assertEquals(Inputs.Decision.SUBMIT, Inputs.Decision.of("挖矿日常"));
    }

    @Test
    @DisplayName("同一玩家重复登记只保留最后一次：后者覆盖前者，旧状态摘不到也回不去")
    void singlePendingPerPlayer() {
        UUID id = UUID.randomUUID();
        Inputs.Pending first = Inputs.of(id, "第一个字段", new Recorder());
        Inputs.Pending second = Inputs.of(id, "第二个字段", new Recorder());

        Inputs.put(first);
        Inputs.put(second);

        assertEquals(1, Inputs.PENDING.size(), "一个玩家同时只能等一项");
        assertSame(second, Inputs.PENDING.get(id), "留下的是最后一次登记的");
        assertFalse(Inputs.PENDING.containsValue(first), "旧状态已被覆盖，不可能再被摘到");
    }

    @Test
    @DisplayName("take 用身份比较：拿旧状态去摘，新状态必须留在表里")
    void takeByIdentity() {
        UUID id = UUID.randomUUID();
        Inputs.Pending old = Inputs.of(id, "旧", new Recorder());
        Inputs.Pending fresh = Inputs.of(id, "新", new Recorder());
        Inputs.put(fresh);

        assertFalse(Inputs.take(old), "旧超时任务到点时不该踢掉新输入");
        assertSame(fresh, Inputs.PENDING.get(id));
        assertTrue(Inputs.take(fresh), "自己摘自己才摘得掉");
        assertNull(Inputs.PENDING.get(id));
    }

    @Test
    @DisplayName("放弃只跑 cancel，不负责摘状态；forget 在没有状态时也不抛")
    void callbacks() {
        Recorder recorder = new Recorder();
        Inputs.Pending pending = Inputs.of(UUID.randomUUID(), "标题", recorder);
        Inputs.put(pending);

        Inputs.abandon(pending);
        assertEquals(Arrays.asList("cancel"), recorder.calls, "放弃只能跑一次取消回调");
        assertSame(pending, Inputs.PENDING.get(pending.key()), "摘状态是调用方的事，abandon 不越权");

        Inputs.forget(null);
    }

    /** 回调记录的先后顺序：取消之后绝不能再来一次 value。 */
    private static final class Recorder implements Inputs.Callback {

        private final List<String> calls = new ArrayList<String>();

        @Override
        public void value(String text) {
            calls.add("value:" + text);
        }

        @Override
        public void cancel() {
            calls.add("cancel");
        }
    }
}
