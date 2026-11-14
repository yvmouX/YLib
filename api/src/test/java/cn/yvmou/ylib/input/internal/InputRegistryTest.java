package cn.yvmou.ylib.input.internal;

import cn.yvmou.ylib.scheduler.UniversalScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Bukkit 一侧的契约，这里钉四条：
 * <ol>
 *   <li>ask 会把「关界面 + 发问」交给调度器的实体重载，绝不就地跑；</li>
 *   <li>挂着提问时聊天一律被吞，答案/取消经调度器送到对应回调（文本已去首尾空白）；</li>
 *   <li>超时按取消处理；退服丢状态且不跑回调；</li>
 *   <li>没有挂提问的消息原样放行，不碰事件也不碰调度器。</li>
 * </ol>
 */
class InputRegistryTest {

    private Plugin plugin;
    private UniversalScheduler scheduler;
    private Player player;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        scheduler = mock(UniversalScheduler.class);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        InputRegistry.init(plugin, scheduler);
    }

    @AfterEach
    void tearDown() {
        InputRegistry.shutdown();
    }

    @Test
    @DisplayName("ask：关界面与发问交给调度器的实体重载，登记时不就地执行")
    void askDefersCloseAndPrompt() {
        InputRegistry.ask(player, "提示", answer -> { }, null, 60_000L);

        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(1)).runTask(same(plugin), same(player), tasks.capture());
        verify(player, never()).closeInventory();

        tasks.getValue().run();
        verify(player).closeInventory();
        verify(player).sendMessage("提示");
    }

    @Test
    @DisplayName("取值：消息被吞，答案经调度器送到 onAnswer（已去首尾空白）")
    void submitDeliversAnswerViaScheduler() {
        AtomicReference<String> answer = new AtomicReference<>();
        InputRegistry.ask(player, "提示", answer::set, null, 60_000L);

        AsyncPlayerChatEvent event = chat("  答案  ");
        new InputRegistry().onChat(event);

        verify(event).setCancelled(true);
        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(2)).runTask(same(plugin), same(player), tasks.capture());
        List<Runnable> dispatched = tasks.getAllValues();
        dispatched.get(0).run();   // ask 派出的关界面 + 发问
        dispatched.get(1).run();   // onChat 派出的答案回调
        assertEquals("答案", answer.get(), "答案应去首尾空白后送达");
    }

    @Test
    @DisplayName("取消词：走 onCancel，onAnswer 不被触发")
    void cancelWordGoesToOnCancel() {
        AtomicInteger cancelled = new AtomicInteger();
        AtomicInteger answered = new AtomicInteger();
        InputRegistry.ask(player, "提示", text -> answered.incrementAndGet(),
                cancelled::incrementAndGet, 60_000L);

        new InputRegistry().onChat(chat("取消"));

        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(2)).runTask(same(plugin), same(player), tasks.capture());
        tasks.getAllValues().get(1).run();
        assertEquals(1, cancelled.get());
        assertEquals(0, answered.get());
    }

    @Test
    @DisplayName("过期：时限之后的第一句话按取消处理")
    void expiredPendingActsAsCancel() {
        AtomicInteger cancelled = new AtomicInteger();
        InputRegistry.ask(player, "提示", answer -> { }, cancelled::incrementAndGet, -1L);

        new InputRegistry().onChat(chat("迟到的答案"));

        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(2)).runTask(same(plugin), same(player), tasks.capture());
        tasks.getAllValues().get(1).run();
        assertEquals(1, cancelled.get());
    }

    @Test
    @DisplayName("退服：状态直接作废，之后的聊天原样放行、不跑回调")
    void quitDropsPendingSilently() {
        AtomicReference<String> answer = new AtomicReference<>();
        InputRegistry.ask(player, "提示", answer::set, null, 60_000L);

        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(player);
        new InputRegistry().onQuit(quit);

        AsyncPlayerChatEvent event = chat("答案");
        new InputRegistry().onChat(event);

        verify(event, never()).setCancelled(true);
        assertNull(answer.get());
        // ask 之后的 onQuit 不再产生派发；onChat 也没有（无 pending 即放行）
        verify(scheduler, times(1)).runTask(same(plugin), same(player), any(Runnable.class));
    }

    @Test
    @DisplayName("无提问：消息原样放行，不吞、不派发")
    void noPendingMessagePassesThrough() {
        new InputRegistry().onChat(chat("闲聊一句"));

        verifyNoInteractions(scheduler);
    }

    @Test
    @DisplayName("dispatch：装配齐全时按实体调度，回调不就地执行")
    void dispatchGoesThroughScheduler() {
        Runnable action = () -> { };
        InputRegistry.dispatch(player, action);

        verify(scheduler).runTask(same(plugin), same(player), same(action));
    }

    @Test
    @DisplayName("dispatch：没注入调度器时（单测、YLib 未初始化）就地执行")
    void dispatchFallsBackToInline() {
        InputRegistry.init(plugin, null);   // 清掉注入，模拟未装配
        AtomicInteger ran = new AtomicInteger();

        InputRegistry.dispatch(player, ran::incrementAndGet);

        assertEquals(1, ran.get(), "拿不到调度器时应就地跑，而不是把回调吞掉");
        verifyNoInteractions(scheduler);
    }

    @Test
    @DisplayName("cancel：摘掉挂着的提问，不跑任何回调")
    void cancelDropsPendingSilently() {
        AtomicInteger answered = new AtomicInteger();
        InputRegistry.ask(player, "提示", text -> answered.incrementAndGet(), null, 60_000L);

        InputRegistry.cancel(player.getUniqueId());
        AsyncPlayerChatEvent event = chat("答案");
        new InputRegistry().onChat(event);

        assertEquals(0, answered.get());
        verify(event, never()).setCancelled(true);
    }

    private AsyncPlayerChatEvent chat(String message) {
        AsyncPlayerChatEvent event = mock(AsyncPlayerChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getMessage()).thenReturn(message);
        return event;
    }
}
