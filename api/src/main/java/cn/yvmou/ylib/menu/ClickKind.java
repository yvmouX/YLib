package cn.yvmou.ylib.menu;

import org.bukkit.event.inventory.ClickType;

/**
 * 点击的**键位**维度。
 * <p>
 * 键位与修饰键（Shift / Ctrl）刻意分成两个维度，不塞进一个枚举——否则 {@code ShiftClick} 到底是
 * 左键还是右键都说不清，而「Shift+左键」与「Shift+右键」在菜单里几乎总是两件不同的事
 * （整理 / 删除）。修饰键见 {@link ClickType#shift()} 与 {@link ClickType#control()}。
 */
public enum ClickKind {

    /** 左键。 */
    LEFT,
    /** 右键。 */
    RIGHT,
    /** 中键（创造模式的复制键，也是「选取方块」）。 */
    MIDDLE,
    /** 数字键 1-9（把快捷栏第 N 格换到光标 / 与点击的格子交换）。 */
    NUMBER_KEY,
    /** 丢弃键（Q）：光标上的物品被丢出，或整格被丢出。 */
    DROP,
    /** F 键换手（副手交换）。 */
    SWAP_OFFHAND,
    /** 双击（收集同类物品到光标）。 */
    DOUBLE_CLICK,
    /** 认不出来的点击：新版本可能新增点击类型，那时走「兜底处理」而不是把点击变成异常。 */
    UNKNOWN;

    /**
     * Bukkit 的点击类型 → 键位。
     * <p>
     * 用 {@code name()} 比对而不是 {@code switch} 枚举常量：常量在跨版本时会被改名/删除，
     * 直接写常量名会把本库钉死在某一个服务端版本上（例如 {@code CONTROL_DROP}）。
     */
    static ClickKind of(ClickType type) {
        if (type == null) {
            return UNKNOWN;
        }
        String name = type.name();
        if ("LEFT".equals(name)) {
            return LEFT;
        }
        if ("RIGHT".equals(name)) {
            return RIGHT;
        }
        if ("MIDDLE".equals(name)) {
            return MIDDLE;
        }
        if ("NUMBER_KEY".equals(name)) {
            return NUMBER_KEY;
        }
        if ("DROP".equals(name) || "CONTROL_DROP".equals(name)) {
            return DROP;
        }
        if ("SWAP_OFFHAND".equals(name)) {
            return SWAP_OFFHAND;
        }
        if ("DOUBLE_CLICK".equals(name)) {
            return DOUBLE_CLICK;
        }
        return UNKNOWN;
    }
}
