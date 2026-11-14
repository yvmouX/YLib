package cn.yvmou.ylib.menu;

/**
 * 一个具体槽位：既是 Bukkit 的槽位号，也带着它在布局表里的位置。
 * <p>
 * {@link #row()} / {@link #col()} 是给**同一个符号占多格**时用的：边框 {@code #} 是 8 个槽位共用一份
 * 物品定义，需要在不同格子上做不同事（只让顶边发光、只让左边响应右键）时，从回调参数拿这个
 * {@code Slot} 判断即可，不必自己去反查坐标。
 * <p>
 * Java 8 没有 {@code record}，因此写成不可变值类。
 */
public final class Slot {

    private final int index;
    private final int row;
    private final int col;

    public Slot(int index, int row, int col) {
        this.index = index;
        this.row = row;
        this.col = col;
    }

    /** Bukkit 槽位号：行主序，{@code row * columns + col}。 */
    public int index() {
        return index;
    }

    /** 布局表里的行号，0 起。 */
    public int row() {
        return row;
    }

    /** 布局表里的列号，0 起。 */
    public int col() {
        return col;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Slot)) {
            return false;
        }
        Slot that = (Slot) other;
        return index == that.index && row == that.row && col == that.col;
    }

    @Override
    public int hashCode() {
        return (index * 31 + row) * 31 + col;
    }

    @Override
    public String toString() {
        return "Slot[" + index + " (" + (row + 1) + "," + (col + 1) + ")]";
    }
}
