package cn.yvmou.ylib.common.utils;

import cn.yvmou.ylib.api.exception.YLibException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ValidationUtils测试类
 *
 * @author yvmoux
 * @since 1.0.0
 */
@DisplayName("ValidationUtils测试")
class ValidationUtilsTest {
    
    @Test
    @DisplayName("测试notNull方法 - 正常情况")
    void testNotNull_Valid() {
        // 不应该抛出异常
        assertDoesNotThrow(() -> ValidationUtils.notNull("test", "Test"));
        assertDoesNotThrow(() -> ValidationUtils.notNull(123, "Number"));
        assertDoesNotThrow(() -> ValidationUtils.notNull(new Object(), "Object"));
    }
    
    @Test
    @DisplayName("测试notNull方法 - null值")
    void testNotNull_Null() {
        // 应该抛出异常
        assertThrows(YLibException.class, () -> ValidationUtils.notNull(null, "Test"));
    }
    
    @Test
    @DisplayName("测试notEmpty方法 - 正常情况")
    void testNotEmpty_Valid() {
        // 不应该抛出异常
        assertDoesNotThrow(() -> ValidationUtils.notEmpty("test", "Test"));
        assertDoesNotThrow(() -> ValidationUtils.notEmpty("hello world", "Message"));
    }
    
    @Test
    @DisplayName("测试notEmpty方法 - 空字符串")
    void testNotEmpty_Empty() {
        // 应该抛出异常
        assertThrows(YLibException.class, () -> ValidationUtils.notEmpty("", "Test"));
        assertThrows(YLibException.class, () -> ValidationUtils.notEmpty("   ", "Test"));
    }
    
    @Test
    @DisplayName("测试notEmpty方法 - null值")
    void testNotEmpty_Null() {
        // 应该抛出异常
        assertThrows(YLibException.class, () -> ValidationUtils.notEmpty(null, "Test"));
    }
    
    @Test
    @DisplayName("测试isValidCommandName方法 - 有效命令名")
    void testIsValidCommandName_Valid() {
        assertTrue(ValidationUtils.isValidCommandName("test"));
        assertTrue(ValidationUtils.isValidCommandName("mycommand"));
        assertTrue(ValidationUtils.isValidCommandName("command123"));
        assertTrue(ValidationUtils.isValidCommandName("test-command"));
        assertTrue(ValidationUtils.isValidCommandName("test_command"));
    }
    
    @Test
    @DisplayName("测试isValidCommandName方法 - 无效命令名")
    void testIsValidCommandName_Invalid() {
        assertFalse(ValidationUtils.isValidCommandName(""));
        assertFalse(ValidationUtils.isValidCommandName(null));
        assertFalse(ValidationUtils.isValidCommandName("test command")); // 包含空格
        assertFalse(ValidationUtils.isValidCommandName("test/command")); // 包含斜杠
        assertFalse(ValidationUtils.isValidCommandName("test:command")); // 包含冒号
    }
    
    @Test
    @DisplayName("测试isValidDelay方法 - 有效延迟")
    void testIsValidDelay_Valid() {
        assertTrue(ValidationUtils.isValidDelay(0));
        assertTrue(ValidationUtils.isValidDelay(1000));
        assertTrue(ValidationUtils.isValidDelay(Long.MAX_VALUE));
    }
    
    @Test
    @DisplayName("测试isValidDelay方法 - 无效延迟")
    void testIsValidDelay_Invalid() {
        assertFalse(ValidationUtils.isValidDelay(-1));
        assertFalse(ValidationUtils.isValidDelay(-1000));
        assertFalse(ValidationUtils.isValidDelay(Long.MIN_VALUE));
    }
    
    @Test
    @DisplayName("测试isValidPeriod方法 - 有效周期")
    void testIsValidPeriod_Valid() {
        assertTrue(ValidationUtils.isValidPeriod(1));
        assertTrue(ValidationUtils.isValidPeriod(1000));
        assertTrue(ValidationUtils.isValidPeriod(Long.MAX_VALUE));
    }
    
    @Test
    @DisplayName("测试isValidPeriod方法 - 无效周期")
    void testIsValidPeriod_Invalid() {
        assertFalse(ValidationUtils.isValidPeriod(0));
        assertFalse(ValidationUtils.isValidPeriod(-1));
        assertFalse(ValidationUtils.isValidPeriod(-1000));
        assertFalse(ValidationUtils.isValidPeriod(Long.MIN_VALUE));
    }
    
    @Test
    @DisplayName("测试isValidIndex方法 - 有效索引")
    void testIsValidIndex_Valid() {
        assertTrue(ValidationUtils.isValidIndex(0, 10));
        assertTrue(ValidationUtils.isValidIndex(5, 10));
        assertTrue(ValidationUtils.isValidIndex(9, 10));
    }
    
    @Test
    @DisplayName("测试isValidIndex方法 - 无效索引")
    void testIsValidIndex_Invalid() {
        assertFalse(ValidationUtils.isValidIndex(-1, 10));
        assertFalse(ValidationUtils.isValidIndex(10, 10));
        assertFalse(ValidationUtils.isValidIndex(15, 10));
    }
    
    @Test
    @DisplayName("测试isValidPort方法 - 有效端口")
    void testIsValidPort_Valid() {
        assertTrue(ValidationUtils.isValidPort(1));
        assertTrue(ValidationUtils.isValidPort(1024));
        assertTrue(ValidationUtils.isValidPort(65535));
    }
    
    @Test
    @DisplayName("测试isValidPort方法 - 无效端口")
    void testIsValidPort_Invalid() {
        assertFalse(ValidationUtils.isValidPort(0));
        assertFalse(ValidationUtils.isValidPort(-1));
        assertFalse(ValidationUtils.isValidPort(65536));
    }
    
    @Test
    @DisplayName("测试isValidIpAddress方法 - 有效IP地址")
    void testIsValidIpAddress_Valid() {
        assertTrue(ValidationUtils.isValidIpAddress("127.0.0.1"));
        assertTrue(ValidationUtils.isValidIpAddress("192.168.1.1"));
        assertTrue(ValidationUtils.isValidIpAddress("10.0.0.1"));
        assertTrue(ValidationUtils.isValidIpAddress("0.0.0.0"));
        assertTrue(ValidationUtils.isValidIpAddress("255.255.255.255"));
    }
    
    @Test
    @DisplayName("测试isValidIpAddress方法 - 无效IP地址")
    void testIsValidIpAddress_Invalid() {
        assertFalse(ValidationUtils.isValidIpAddress(""));
        assertFalse(ValidationUtils.isValidIpAddress(null));
        assertFalse(ValidationUtils.isValidIpAddress("256.0.0.1"));
        assertFalse(ValidationUtils.isValidIpAddress("192.168.1"));
        assertFalse(ValidationUtils.isValidIpAddress("192.168.1.1.1"));
        assertFalse(ValidationUtils.isValidIpAddress("192.168.1.256"));
    }
    
    @Test
    @DisplayName("测试isValidEmail方法 - 有效邮箱")
    void testIsValidEmail_Valid() {
        assertTrue(ValidationUtils.isValidEmail("test@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name@domain.co.uk"));
        assertTrue(ValidationUtils.isValidEmail("test+tag@example.org"));
    }
    
    @Test
    @DisplayName("测试isValidEmail方法 - 无效邮箱")
    void testIsValidEmail_Invalid() {
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        assertFalse(ValidationUtils.isValidEmail("@example.com"));
        assertFalse(ValidationUtils.isValidEmail("test@"));
    }
    
    @Test
    @DisplayName("测试isValidUrl方法 - 有效URL")
    void testIsValidUrl_Valid() {
        assertTrue(ValidationUtils.isValidUrl("https://example.com"));
        assertTrue(ValidationUtils.isValidUrl("http://www.example.org"));
        assertTrue(ValidationUtils.isValidUrl("https://example.com/path"));
        assertTrue(ValidationUtils.isValidUrl("https://example.com:8080"));
    }
    
    @Test
    @DisplayName("测试isValidUrl方法 - 无效URL")
    void testIsValidUrl_Invalid() {
        assertFalse(ValidationUtils.isValidUrl(""));
        assertFalse(ValidationUtils.isValidUrl(null));
        assertFalse(ValidationUtils.isValidUrl("not-a-url"));
        assertFalse(ValidationUtils.isValidUrl("invalid://"));
        assertFalse(ValidationUtils.isValidUrl("http://"));
    }
} 