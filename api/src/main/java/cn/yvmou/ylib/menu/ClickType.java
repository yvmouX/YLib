package cn.yvmou.ylib.menu;

/**
 * 一次点击的完整描述：**键位 + 修饰键**。
 * <p>
 * 两个维度分开的理由见 {@link ClickKind}。{@code control} 单独留一个维度是因为
 * {@code CONTROL_DROP}（Ctrl+Q 丢一整叠）与普通丢弃是不同的破坏力，而它并不改变键位。
 */
public final class ClickType {

    /** 左键。 */
    public static final ClickType LEFT = new ClickType(ClickKind.LEFT, false, false);
    /** 右键。 */
    public static final ClickType RIGHT = new ClickType(ClickKind.RIGHT, false, false);
    /** 中键。 */
    public static final ClickType MIDDLE = new ClickType(ClickKind.MIDDLE, false, false);
    /** 丢弃键。 */
    public static final ClickType DROP = new ClickType(ClickKind.DROP, false, false);
    /** F 键换手。 */
    public static final ClickType SWAP_OFFHAND = new ClickType(ClickKind.SWAP_OFFHAND, false, false);
    /** 双击。 */
    public static final ClickType DOUBLE_CLICK = new ClickType(ClickKind.DOUBLE_CLICK, false, false);
    /** Shift+左键。 */
    public static final ClickType SHIFT_LEFT = new ClickType(ClickKind.LEFT, true, false);
    /** Shift+右键。 */
    public static final ClickType SHIFT_RIGHT = new ClickType(ClickKind.RIGHT, true, false);

    private final ClickKind kind;
    private final boolean shift;
    private final boolean control;

    public ClickType(ClickKind kind, boolean shift, boolean control) {
        this.kind = kind == null ? ClickKind.UNKNOWN : kind;
        this.shift = shift;
        this.control = control;
    }

    /**
     * Bukkit 的点击类型 → 本类型。
     * <p>
     * {@code control} 用名字比对而不是引用 {@code ClickType.CONTROL_DROP} 常量：那个常量在跨版本时
     * 可能被改名或删除，写死它会把本库钉死在某一个服务端版本上。
     */
    public static ClickType of(org.bukkit.event.inventory.ClickType type) {
        if (type == null) {
            return new ClickType(ClickKind.UNKNOWN, false, false);
        }
        return new ClickType(ClickKind.of(type), type.isShiftClick(),
                "CONTROL_DROP".equals(type.name()));
    }

    public ClickKind kind() {
        return kind;
    }

    /** 是否按着 Shift。 */
    public boolean shift() {
        return shift;
    }

    /** 是否按着 Ctrl（目前只用于区分 {@code CONTROL_DROP}）。 */
    public boolean control() {
        return control;
    }

    /** 左键（含 Shift+左键）——与 Bukkit 的语义一致，需要精确判断时用 {@link #kind()}。 */
    public boolean isLeft() {
        return kind == ClickKind.LEFT;
    }

    /** 右键（含 Shift+右键）。 */
    public boolean isRight() {
        return kind == ClickKind.RIGHT;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClickType)) {
            return false;
        }
        ClickType that = (ClickType) other;
        return shift == that.shift && control == that.control && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return (kind.hashCode() * 31 + (shift ? 1 : 0)) * 31 + (control ? 1 : 0);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        if (shift) {
            text.append("SHIFT+");
        }
        if (control) {
            text.append("CTRL+");
        }
        return text.append(kind).toString();
    }
}
