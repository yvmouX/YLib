package cn.yvmou.ylib.api.command;

public enum CompleteType {
    AUTO("自动"),
    CUSTOM("自定义"),
    PRESET("预设");

    private final String desc;

    CompleteType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
