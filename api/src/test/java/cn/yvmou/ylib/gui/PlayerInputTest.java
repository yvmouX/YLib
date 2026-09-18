package cn.yvmou.ylib.gui;

import cn.yvmou.ylib.YLib;
import cn.yvmou.ylib.scheduler.UniversalScheduler;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 没有插件实例时 {@link PlayerInput} 的公开 API 契约，以及「回调一律回主线程」这条线程契约。
 * <p>
 * 「不抛」这几条不是凑数：测试环境里没有服务端，YLib 也没初始化，调用任何一个方法抛出去都会让宿主
 * 在关服流程里连带炸掉。真实玩家对象造不出来（{@code Player} 要整个 CraftPlayer），因此这一层用假身。
 * <p>
 * 线程契约必须在这里钉住：取消与提交一样是从**异步**聊天事件里进来的，回调里但凡有一支就地执行，
 * 调用方（典型实现是「重开刚才那个菜单」）就会在异步线程里 {@code openInventory}，
 * 服务端直接抛 {@code Thread failed main thread check: Cannot init menu async} —— 不是偶发，是必崩。
 */
class PlayerInputTest {

    private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @AfterEach
    void clearState() throws Exception {
        Inputs.PENDING.clear();
        installYLib(null);
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

    @Test
    @DisplayName("输入「取消」：onCancel 交给调度器执行，不在异步聊天线程里就地跑")
    void cancelIsDispatchedNotRunInline() throws Exception {
        AtomicBoolean cancelled = new AtomicBoolean();
        Player player = waitingPlayer(cancelled, null);
        UniversalScheduler scheduler = installScheduler();

        assertTrue(PlayerInput.submit(player, "取消"), "这句话算这次输入的内容，应当被拦下");
        assertFalse(cancelled.get(), "回调必须等主线程，不能在聊天线程里就地执行（那会让调用方异步开界面）");

        runDispatched(scheduler, player);
        assertTrue(cancelled.get(), "轮到主线程时 onCancel 必须执行");
    }

    @Test
    @DisplayName("提交值：onValue 同样交给调度器执行，且拿到的文本已去空白")
    void valueIsDispatchedWithTrimmedText() throws Exception {
        AtomicReference<String> value = new AtomicReference<>();
        Player player = waitingPlayer(null, value);
        UniversalScheduler scheduler = installScheduler();

        assertTrue(PlayerInput.submit(player, "  STONE  "));
        assertNull(value.get(), "回调必须等主线程");

        runDispatched(scheduler, player);
        assertEquals("STONE", value.get());
    }

    // ---------- 假身与工具 ----------

    /** 登记一次等待中的输入；{@code cancelled} / {@code value} 用来观察回调有没有跑。 */
    private static Player waitingPlayer(final AtomicBoolean cancelled, final AtomicReference<String> value) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER);
        Inputs.put(Inputs.of(PLAYER, "标题", new Inputs.Callback() {
            @Override
            public void value(String text) {
                if (value != null) {
                    value.set(text);
                }
            }

            @Override
            public void cancel() {
                if (cancelled != null) {
                    cancelled.set(true);
                }
            }
        }));
        return player;
    }

    /** 装一个假 YLib 与它的假调度器（单测里 ServiceLoader 找不到平台实现，真调度器拿不到）。 */
    private static UniversalScheduler installScheduler() throws Exception {
        UniversalScheduler scheduler = mock(UniversalScheduler.class);
        YLib ylib = mock(YLib.class);
        when(ylib.getScheduler()).thenReturn(scheduler);
        installYLib(ylib);
        return scheduler;
    }

    /** 取出交给调度器的那一个任务并执行它——正常运行时是主线程在下一个 tick 做这件事。 */
    private static void runDispatched(UniversalScheduler scheduler, Player player) {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runLater(eq(player), task.capture(), eq(1L));
        task.getValue().run();
    }

    /** 直接写 YLib 的单例字段：库没有给测试用的注入点，而这里要的只是「调度器存在」这一件事。 */
    private static void installYLib(YLib ylib) throws Exception {
        Field instance = YLib.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, ylib);
    }
}
