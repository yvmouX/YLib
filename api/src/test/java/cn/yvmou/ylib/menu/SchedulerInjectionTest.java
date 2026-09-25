package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.MenuScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 调度注入点：菜单包**唯一的**对外耦合，也是它能不能整包搬走的唯一前提。
 * <p>
 * 契约有三条，都在这里钉住：
 * <ol>
 *   <li>宿主注入后，{@code delayedRefresh} 的重画走宿主那条路；</li>
 *   <li>宿主没注入时回退到 Bukkit 调度器，且在「没有服务端」的情况下**就地执行**——
 *       刷新界面的意图不该因为拿不到调度器而消失；</li>
 *   <li>宿主自己的实现抛异常时，同样回退，而不是把这次刷新丢掉。</li>
 * </ol>
 * 顺带守住一条架构约束：<b>菜单包不 import 任何外部 YLib 类型</b>。
 * {@link MenuScheduler} 只依赖 Bukkit，因此 {@code plugin} 传 {@code null} 也必须能工作。
 */
class SchedulerInjectionTest {

    @BeforeAll
    static void setUp() {
        TestServer.install();
    }

    @AfterEach
    void tearDown() {
        MenuScheduler.register(null);   // 恢复默认回退，免得影响后面的用例
        assertFalse(MenuScheduler.injected(), "用例之间必须复位");
    }

    @Test
    @DisplayName("注入后：任务交给宿主，不再自己执行")
    void injectedSinkReceivesTask() {
        final List<String> ran = new ArrayList<String>();
        MenuScheduler.register(new java.util.function.Consumer<Runnable>() {
            @Override
            public void accept(Runnable task) {
                ran.add("deferred");
                // 故意不执行：宿主要自己决定什么时候跑
            }
        });
        assertTrue(MenuScheduler.injected());

        MenuScheduler.runNextTick(null, new Runnable() {
            @Override
            public void run() {
                ran.add("ran");
            }
        });

        assertEquals(java.util.Collections.singletonList("deferred"), ran,
                "任务应当交给宿主，而不是就地执行");
    }

    @Test
    @DisplayName("宿主没注入：没有服务端时就地执行（不回退成「什么都不做」）")
    void fallsBackToInlineWhenNoServer() {
        final List<String> ran = new ArrayList<String>();
        MenuScheduler.runNextTick(null, new Runnable() {
            @Override
            public void run() {
                ran.add("ran");
            }
        });
        assertEquals(java.util.Collections.singletonList("ran"), ran,
                "拿不到调度器时应当就地跑，而不是把刷新吞掉");
    }

    @Test
    @DisplayName("宿主实现抛异常：回退，不把这次刷新丢掉")
    void brokenSinkFallsBack() {
        MenuScheduler.register(new java.util.function.Consumer<Runnable>() {
            @Override
            public void accept(Runnable task) {
                throw new IllegalStateException("宿主自己的调度器坏了");
            }
        });
        final List<String> ran = new ArrayList<String>();
        MenuScheduler.runNextTick(null, new Runnable() {
            @Override
            public void run() {
                ran.add("ran");
            }
        });
        assertEquals(java.util.Collections.singletonList("ran"), ran);
    }

    @Test
    @DisplayName("null 任务：什么也不做（不抛）")
    void nullTaskIsIgnored() {
        MenuScheduler.register(new java.util.function.Consumer<Runnable>() {
            @Override
            public void accept(Runnable task) {
                throw new AssertionError("不该被调用");
            }
        });
        MenuScheduler.runNextTick(null, null);
    }

    @Test
    @DisplayName("注入 null 恢复默认回退")
    void registerNullRestoresFallback() {
        MenuScheduler.register(new java.util.function.Consumer<Runnable>() {
            @Override
            public void accept(Runnable task) {
            }
        });
        assertTrue(MenuScheduler.injected());
        MenuScheduler.register(null);
        assertFalse(MenuScheduler.injected());
    }

    @Test
    @DisplayName("Menus.scheduler(...) 转发到这个注入点")
    void menusForwardsToSink() {
        final List<String> ran = new ArrayList<String>();
        Menus.scheduler(new java.util.function.Consumer<Runnable>() {
            @Override
            public void accept(Runnable task) {
                ran.add("via-menus");
            }
        });
        MenuScheduler.runNextTick(null, new Runnable() {
            @Override
            public void run() {
                ran.add("ran");
            }
        });
        assertEquals(java.util.Collections.singletonList("via-menus"), ran);
    }
}
