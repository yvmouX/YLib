package cn.yvmou.ylib.common.utils;

import cn.yvmou.ylib.api.exception.YLibException;
import org.jetbrains.annotations.NotNull;

/**
 * 验证工具类 - 提供输入验证功能
 *
 * @author yvmoux
 * @since 1.0.0
 */
public final class ValidationUtils {
    
    private ValidationUtils() {
        // 工具类不允许实例化
    }
    
    /**
     * 验证对象不为null
     * @param obj 要验证的对象
     * @param name 参数名称
     * @throws YLibException 如果对象为null
     */
    public static void notNull(Object obj, @NotNull String name) {
        if (obj == null) {
            throw new YLibException(name + " cannot be null");
        }
    }
    
    /**
     * 验证字符串不为空
     * @param str 要验证的字符串
     * @param name 参数名称
     * @throws YLibException 如果字符串为空
     */
    public static void notEmpty(String str, @NotNull String name) {
        if (str == null || str.trim().isEmpty()) {
            throw new YLibException(name + " cannot be empty");
        }
    }
    
    /**
     * 验证命令名称是否有效
     * @param commandName 命令名称
     * @return boolean 如果命令名称有效返回true
     */
    public static boolean isValidCommandName(@NotNull String commandName) {
        if (commandName == null || commandName.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含非法字符
        return commandName.matches("^[a-zA-Z0-9_-]+$");
    }
    
    /**
     * 验证延迟时间是否有效
     * @param delay 延迟时间
     * @return boolean 如果延迟时间有效返回true
     */
    public static boolean isValidDelay(long delay) {
        return delay >= 0;
    }
    
    /**
     * 验证周期时间是否有效
     * @param period 周期时间
     * @return boolean 如果周期时间有效返回true
     */
    public static boolean isValidPeriod(long period) {
        return period > 0;
    }
    
    /**
     * 验证索引是否在有效范围内
     * @param index 索引
     * @param size 数组大小
     * @return boolean 如果索引有效返回true
     */
    public static boolean isValidIndex(int index, int size) {
        return index >= 0 && index < size;
    }
    
    /**
     * 验证端口号是否有效
     * @param port 端口号
     * @return boolean 如果端口号有效返回true
     */
    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }
    
    /**
     * 验证IP地址是否有效
     * @param ip IP地址
     * @return boolean 如果IP地址有效返回true
     */
    public static boolean isValidIpAddress(@NotNull String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        final int expectedParts = 4;
        if (parts.length != expectedParts) {
            return false;
        }
        
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 验证邮箱地址是否有效
     * @param email 邮箱地址
     * @return boolean 如果邮箱地址有效返回true
     */
    public static boolean isValidEmail(@NotNull String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // 简单的邮箱验证正则表达式
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * 验证URL是否有效
     * @param url URL地址
     * @return boolean 如果URL有效返回true
     */
    public static boolean isValidUrl(@NotNull String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            java.net.URL urlObj = new java.net.URL(url);
            // 检查是否有协议和主机
            return urlObj.getProtocol() != null && urlObj.getHost() != null && !urlObj.getHost().isEmpty();
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }
} 