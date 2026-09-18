package cn.yvmou.ylib.gui;

import cn.yvmou.ylib.scheduler.UniversalTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天输入的状态机：每个玩家同时只等一项（并发收集会让玩家分不清哪句话填的是哪项）。
 * <p>
 * 以玩家 UUID 为键、只碰状态与回调，不碰事件与文本渲染，也不碰 {@code Player}——
 * 因此「哪句话算取消」「重复 ask 保留哪一个」都能在没有服务端的单测里钉住。
 */
final class Inputs {

    /** 取消关键字。 */
    private static final String CANCEL_KEYWORD = "取消";

    /** 每个玩家一份待处理状态；退服、重开输入都从这张表里摘。 */
    static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<UUID, Pending>();

    private Inputs() {
    }

    /**
     * 一次提交的归属：取消关键字、空串都只在这里判定。
     * <p>
     * 做成纯函数而不是塞进 {@link PlayerInput#submit(org.bukkit.entity.Player, String)}，
     * 是为了让「空串算清空」这类语义能直接测——真发一条聊天消息验证它需要一整个服务端。
     */
    enum Decision {
        /** 放弃这次输入。 */
        CANCEL,
        /** 采纳输入：文本已去首尾空白，空串表示清空。 */
        SUBMIT;

        static Decision of(String raw) {
            String text = raw == null ? "" : raw.trim();
            if (text.equals(CANCEL_KEYWORD) || text.equalsIgnoreCase("cancel")) {
                return CANCEL;
            }
            return SUBMIT;
        }
    }

    /** 提交或取消时要跑的回调。 */
    interface Callback {

        void value(String text);

        void cancel();
    }

    /** 登记一份新状态；旧状态由调用方先丢弃（旧回调不能因为新输入被触发）。 */
    static Pending of(UUID key, String label, Callback callback) {
        return new Pending(key, label, callback);
    }

    static Pending get(UUID key) {
        return key == null ? null : PENDING.get(key);
    }

    static void put(Pending pending) {
        PENDING.put(pending.key, pending);
    }

    static boolean awaiting(UUID key) {
        return get(key) != null;
    }

    /** 摘掉该玩家的状态；有就返回（由调用方决定跑不跑回调）。 */
    static Pending take(UUID key) {
        return key == null ? null : PENDING.remove(key);
    }

    /** 按身份摘：玩家中途重开输入时，旧超时任务不该把新状态踢掉。 */
    static boolean take(Pending pending) {
        return PENDING.remove(pending.key, pending);
    }

    /** 退服：丢状态且不跑任何回调。 */
    static void forget(UUID key) {
        Pending pending = take(key);
        if (pending != null) {
            pending.cancelTimeout();
        }
    }

    /** 放弃输入并通知调用方（超时、输入「取消」都走这里）。 */
    static void abandon(Pending pending) {
        pending.cancelTimeout();
        pending.callback.cancel();
    }

    /** 一次等待中的输入：提示文案、回调、超时任务（排上之前是 null）。 */
    static final class Pending {

        private final UUID key;
        private final String label;
        private final Callback callback;

        /** 超时任务；调度器拿不到时排不上，留 null。 */
        private volatile UniversalTask timeout;

        private Pending(UUID key, String label, Callback callback) {
            this.key = key;
            this.label = label;
            this.callback = callback;
        }

        UUID key() {
            return key;
        }

        String label() {
            return label;
        }

        Callback callback() {
            return callback;
        }

        /** 排超时任务；拿不到任务就什么也不做，不能因为没兜底就抛。 */
        void setTimeout(UniversalTask task) {
            if (task == null) {
                return;
            }
            this.timeout = task;
            // 排上之前就已经被提交或取消：补一次取消，否则它到点还会把状态踢掉
            if (!PENDING.containsKey(key)) {
                task.cancel();
            }
        }

        void cancelTimeout() {
            UniversalTask task = timeout;
            // 超时任务自己触发时再取消一次也无害：各平台实现对此都是幂等的
            if (task != null) {
                task.cancel();
            }
        }
    }
}
