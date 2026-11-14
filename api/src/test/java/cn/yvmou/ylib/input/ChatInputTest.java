package cn.yvmou.ylib.input;

import cn.yvmou.ylib.scheduler.UniversalScheduler;
import cn.yvmou.ylib.text.TextRenderer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 门面层的契约：参数校验在前端 fail-fast，色码渲染在 {@code ask} 收口处完成。
 * 状态机与调度路由的细则在 {@code internal} 包里钉。
 */
class ChatInputTest {

    private Plugin plugin;
    private UniversalScheduler scheduler;
    private Player player;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        scheduler = mock(UniversalScheduler.class);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        ChatInput.init(plugin, scheduler);
    }

    @AfterEach
    void tearDown() {
        ChatInput.shutdown();
    }

    @Test
    @DisplayName("prompt 为空：ask 拒绝，玩家不能被问一句空话")
    void blankPromptRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ChatInput.of(player).prompt("  ").onAnswer(text -> { }).ask());
        assertThrows(IllegalArgumentException.class,
                () -> ChatInput.of(player).onAnswer(text -> { }).ask());
    }

    @Test
    @DisplayName("player 为 null：ask 拒绝")
    void nullPlayerRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ChatInput.of(null).prompt("问题").ask());
    }

    @Test
    @DisplayName("timeout：必须为正，unit 不能为 null")
    void timeoutValidated() {
        ChatInput.Builder builder = ChatInput.of(player);
        assertThrows(IllegalArgumentException.class, () -> builder.timeout(0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> builder.timeout(-5, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> builder.timeout(1, null));
    }

    @Test
    @DisplayName("ask：发出去的提示语已按 & 色码渲染")
    void promptRenderedBeforeSending() {
        AtomicReference<String> answer = new AtomicReference<>();
        ChatInput.of(player)
                .prompt("&e任务标题")
                .onAnswer(answer::set)
                .ask();

        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(same(plugin), same(player), tasks.capture());
        tasks.getValue().run();

        verify(player).sendMessage(TextRenderer.render("&e任务标题"));
        verify(player).closeInventory();
    }
}
