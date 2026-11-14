package cn.yvmou.ylib.utils;

public class StringUtil {

    /**
     * 判断字符串是否为空或者全空白字符
     * @param str 待检测字符串
     * @return null / "" / "   " → true；有实际字符 → false
     */
    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        return str.trim().isEmpty();
    }

    /**
     * 如果字符串是空或者全空白字符就返回第二个参数，否则返回原字符串
     * @param str 待检测字符串
     * @param yes 如果字符串是空或者全空白字符就返回
     * @return String
     */
    public static String ifBlank(String str, String yes) {
        return isBlank(str) ? yes : str;
    }
}
