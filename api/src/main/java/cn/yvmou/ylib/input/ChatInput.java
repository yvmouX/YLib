package cn.yvmou.ylib.input;

import cn.yvmou.ylib.input.internal.InputRegistry;
import cn.yvmou.ylib.scheduler.UniversalScheduler;
import cn.yvmou.ylib.text.TextRenderer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 聊天栏取值：问一个问题，把玩家的下一句话交给回调。
 *
 * <h2>用法</h2>
 * <pre>{@code
 * ChatInput.init(plugin, ylib.getScheduler());   // onEnable 装配一次
 *
 * ChatInput.of(player)
 *         .prompt("&e任务标题 &7(1~32 字) &8输入「取消」放弃")
 *         .onAnswer(title -> Menus.open(menu, player))
 *         .onCancel(() -> Menus.open(menu, player))
 *         .ask();
 * }</pre>
 *
 * <h2>语义</h2>
 * <ul>
 *   <li><b>一问一答</b>：每个玩家同时只挂一个提问；重复 {@code ask} 直接顶掉旧的，
 *       旧回调不会因为这次输入被触发。</li>
 *   <li><b>先关界面</b>：容器开着时聊天键被界面吃掉，{@code ask} 会先关掉玩家的界面再发问；
 *       答完或取消后重开界面由调用方的回调负责。</li>
 *   <li><b>取消与超时</b>：玩家输「取消」或 {@code cancel}（大小写不敏感）放弃；
 *       超过时限（默认 60 秒，{@link Builder#timeout} 可调）之后的下一句也按取消处理。
 *       超时是刻意的惰性结算：挂着的提问不主动打断玩家，只在下一次开口时判过期。</li>
 *   <li><b>线程契约</b>：发问、关界面与全部回调一律落在玩家所属的调度线程上
 *       （Folia/Canvas 是实体区域线程，Paper/Spigot 是主线程）。因此 {@code ask}
 *       可以从任何线程发起——异步事件、别的玩家触发的区域线程都行。</li>
 *   <li><b>退服</b>：挂着的提问直接作废，不跑任何回调。</li>
 * </ul>
 *
 * <p>未调 {@link #init} 时回落到 {@link cn.yvmou.ylib.YLib#instance()} 的调度器；
 * 连它也没有（单测、关服中）就就地执行——提问的意图不该因为拿不到调度器而消失。
 */
public final class ChatInput {

    private ChatInput() {
    }

    /** 宿主装配：登记聊天拦截并注入调度器，onEnable 调一次。 */
    public static void init(Plugin owner, UniversalScheduler scheduler) {
        InputRegistry.init(owner, scheduler);
    }

    /** 玩家此刻挂着的提问作废，不跑任何回调（菜单被重开、玩家改主意时调）。 */
    public static void cancel(Player player) {
        InputRegistry.cancel(player == null ? null : player.getUniqueId());
    }

    /** 宿主禁用：所有挂着的提问作废（回调不跑——菜单都没了，跑了也没意义）。 */
    public static void shutdown() {
        InputRegistry.shutdown();
    }

    /** 开始一次提问；链式配完参数后以 {@link Builder#ask()} 收口。 */
    public static Builder of(Player player) {
        return new Builder(player);
    }

    /** 一次提问的参数集；{@code ask()} 之后即作废，一次一问。 */
    public static final class Builder {

        private static final long DEFAULT_TIMEOUT_MS = 60_000L;

        private final Player player;
        private String prompt;
        private Consumer<String> onAnswer;
        private Runnable onCancel;
        private long timeoutMs = DEFAULT_TIMEOUT_MS;

        private Builder(Player player) {
            this.player = player;
        }

        /** 问题文案，支持传统 {@code &} 色码与 MiniMessage 标签，发送前统一渲染。 */
        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /** 答上来之后要做的事，收到的文本已去首尾空白。 */
        public Builder onAnswer(Consumer<String> onAnswer) {
            this.onAnswer = onAnswer;
            return this;
        }

        /** 输入「取消」或超时后要做的事；不设就是放弃后什么都不做。 */
        public Builder onCancel(Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        /** 惰性超时：超过它之后玩家的下一句按取消处理。必须为正。 */
        public Builder timeout(long duration, TimeUnit unit) {
            if (unit == null) {
                throw new IllegalArgumentException("ChatInput.timeout 的 unit 是 null");
            }
            if (duration <= 0) {
                throw new IllegalArgumentException("ChatInput.timeout 必须为正，收到 " + duration);
            }
            this.timeoutMs = unit.toMillis(duration);
            return this;
        }

        /** 收口：登记提问，关界面，发问。之后这个 builder 不再使用。 */
        public void ask() {
            if (player == null) {
                throw new IllegalArgumentException("ChatInput.of 的 player 是 null");
            }
            if (prompt == null || prompt.trim().isEmpty()) {
                throw new IllegalArgumentException("ChatInput 的 prompt 是空，玩家不知道要答什么");
            }
            InputRegistry.ask(player, TextRenderer.render(prompt), onAnswer, onCancel, timeoutMs);
        }
    }
}
