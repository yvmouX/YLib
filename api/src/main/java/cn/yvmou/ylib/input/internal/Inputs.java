package cn.yvmou.ylib.input.internal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 聊天输入的状态机：每个玩家同时只等一项（并发收集会让玩家分不清哪句话填的是哪项）。
 *
 * <h2>为什么它是独立的一层</h2>
 * 以玩家 UUID 为键、只碰状态与判定，不碰事件、调度与 {@code Player}——
 * 「哪句话算取消」「超时怎么判」「重复 ask 保留哪一个」都能在没有服务端的单测里钉住。
 */
final class Inputs {

    /** 中文取消词。 */
    private static final String CANCEL_ZH = "取消";
    /** 英文取消词；与中文一样整句相等才算，不放宽到前后缀。 */
    private static final String CANCEL_EN = "cancel";

    /** 每个玩家一份待处理状态；退服、重开提问、取值完成都从这张表里摘。 */
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private Inputs() {
    }

    /** 这句话（已去首尾空白）是否是取消词。 */
    static boolean isCancelWord(String text) {
        return CANCEL_ZH.equals(text) || CANCEL_EN.equalsIgnoreCase(text);
    }

    /** 登记一份新的等待，顶掉旧的并返回旧状态（调用方丢弃即可：旧回调不能因新输入被触发）。 */
    static Pending put(Pending pending) {
        return PENDING.put(pending.playerId(), pending);
    }

    /** 摘掉该玩家的等待；没有就返回 {@code null}。 */
    static Pending take(UUID playerId) {
        return playerId == null ? null : PENDING.remove(playerId);
    }

    /** 宿主禁用：全部作废。回调不在这里跑（菜单都没了，跑了也没意义）。 */
    static void clear() {
        PENDING.clear();
    }

    /** 一次提交的归属：取消词与超时都在这里判定，让语义可单测。 */
    enum Decision {
        /** 放弃这次输入：取消词，或已过时限之后的下一句。 */
        CANCEL,
        /** 采纳输入：文本已去首尾空白，空串也照常提交（怎么理解交给调用方）。 */
        SUBMIT;

        static Decision of(String raw, long deadline) {
            String text = raw == null ? "" : raw.trim();
            if (isCancelWord(text) || System.currentTimeMillis() > deadline) {
                return CANCEL;
            }
            return SUBMIT;
        }
    }

    /** 一次等待中的输入：回调与惰性时限，全部不可变。 */
    static final class Pending {

        private final UUID playerId;
        private final Consumer<String> onAnswer;
        private final Runnable onCancel;
        private final long deadline;

        Pending(UUID playerId, Consumer<String> onAnswer, Runnable onCancel, long deadline) {
            this.playerId = playerId;
            this.onAnswer = onAnswer;
            this.onCancel = onCancel;
            this.deadline = deadline;
        }

        UUID playerId() {
            return playerId;
        }

        Consumer<String> onAnswer() {
            return onAnswer;
        }

        Runnable onCancel() {
            return onCancel;
        }

        long deadline() {
            return deadline;
        }
    }
}
