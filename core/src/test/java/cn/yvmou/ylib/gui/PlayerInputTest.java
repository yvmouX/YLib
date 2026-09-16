package cn.yvmou.ylib.gui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 没有插件实例时 {@link PlayerInput} 的公开 API 契约。
 * <p>
 * 这几条不是凑数：测试环境里没有服务端，YLib 也没初始化，调用任何一个方法抛出去都会让宿主
 * 在关服流程里连带炸掉。真实玩家对象造不出来（{@code Player} 要整个 CraftPlayer），
 * 因此只测「不抛」这一层，状态机本身在 {@link InputsTest} 里测。
 */
class PlayerInputTest {

    @AfterEach
    void clearState() {
        Inputs.PENDING.clear();
    }

    @Test
    @DisplayName("没有插件实例：ask / awaiting / submit / cancel / forget 都不抛")
    void noPluginInstance() {
        // 全传 null 就是「没有玩家、没有插件实例」这两个现实场景（单测、关服清场）
        PlayerInput.ask(null, "标题", "提示", "当前值", text -> {
        }, null);
        PlayerInput.awaiting(null);
        PlayerInput.submit(null, "随便说点什么");
        PlayerInput.cancel(null);
        PlayerInput.forget(null);
    }

    @Test
    @DisplayName("没有待处理输入时 submit 返回 false：那句话该照常进公屏")
    void submitWithoutPending() {
        assertFalse(PlayerInput.submit(null, "cancel"), "没有等输入就不算被拦截");
    }

    @Test
    @DisplayName("超时秒数是公开契约：改它等于改玩家的等待体验")
    void timeoutContract() {
        assertEquals(90, PlayerInput.TIMEOUT_SECONDS);
    }
}
