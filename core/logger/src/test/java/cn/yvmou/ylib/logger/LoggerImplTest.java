package cn.yvmou.ylib.logger;

import cn.yvmou.ylib.PluginInfo;
import cn.yvmou.ylib.TestServer;
import cn.yvmou.ylib.utils.LegacyCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 日志实现：占位符、级别映射、色码剥离，以及「玩家提示不该有控制台副作用」。
 *
 * <h2>怎么在不建服务端的情况下断言输出</h2>
 * 往 {@code Bukkit.getLogger()} 上挂一个内存 {@link Handler}，把写进去的 {@link LogRecord}
 * 收集起来。这比 mock 更有价值——它连**级别**和**条数**都能验，
 * 而这次重构的核心恰恰是这两件事（四条方法统一级别、异常单独占一条）。
 */
class LoggerImplTest {

    /** 收集日志记录的内存 handler。 */
    private static final class Collector extends Handler {
        final List<LogRecord> records = new ArrayList<LogRecord>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        String text() {
            StringBuilder text = new StringBuilder();
            for (LogRecord record : records) {
                text.append(record.getLevel()).append('|').append(record.getMessage()).append('\n');
            }
            return text.toString();
        }
    }

    private static Collector collector;

    @BeforeAll
    static void setUp() {
        TestServer.install();
        PluginInfo.setLoggerPrefix("MyPlugin");
        collector = new Collector();
        java.util.logging.Logger.getLogger("").addHandler(collector);
    }

    @AfterEach
    void reset() {
        collector.records.clear();
        PluginInfo.setLoggerDebug(true);   // 默认值是 true，用例之间复位
    }

    private static String last() {
        assertFalse(collector.records.isEmpty(), "没有写出任何日志");
        return collector.records.get(collector.records.size() - 1).getMessage();
    }

    // ---------- 级别与格式 ----------

    @Test
    @DisplayName("四条方法映射到标准日志级别")
    void levels() {
        Logger logger = new LoggerImpl();
        logger.debug("d");
        logger.info("i");
        logger.warn("w");
        logger.error("e");

        assertEquals(4, collector.records.size(), collector.text());
        // debug 与 info 同为 INFO：日志级别过滤不出 debug（见 LoggerImpl 的说明），
        // 区分靠 [DEBUG] 标签
        assertEquals(Level.INFO, collector.records.get(0).getLevel());
        assertEquals(Level.INFO, collector.records.get(1).getLevel());
        assertEquals(Level.WARNING, collector.records.get(2).getLevel());
        assertEquals(Level.SEVERE, collector.records.get(3).getLevel());
    }

    @Test
    @DisplayName("开了调试就一定要看得见：debug 不会被日志级别静默吃掉")
    void debugIsActuallyVisible() {
        Logger logger = new LoggerImpl();
        logger.debug("一条调试");
        assertEquals(1, collector.records.size(),
                "debug 被日志级别过滤掉了——那样用户开了开关却什么都看不到");
        assertTrue(last().contains("[DEBUG]"), last());
    }

    @Test
    @DisplayName("四条方法格式一致：插件前缀 + [级别] + 正文")
    void uniformFormat() {
        Logger logger = new LoggerImpl();
        logger.info("一条信息");
        assertTrue(last().startsWith("MyPlugin[INFO] "), last());
        assertTrue(last().endsWith("一条信息"), last());

        collector.records.clear();
        logger.warn("一条警告");
        assertTrue(last().startsWith("MyPlugin[WARN] "), last());
    }

    @Test
    @DisplayName("色码在写进日志前被剥掉（日志文件里不该出现 §a）")
    void stripsColorCodes() {
        Logger logger = new LoggerImpl();
        logger.info("&a绿色 &l加粗 &7灰色");
        String written = last();
        assertFalse(written.contains(LegacyCodes.section() + ""), "日志里不该残留段落符：" + written);
        assertFalse(written.contains("&a"), "日志里不该残留 & 码：" + written);
        assertTrue(written.endsWith("绿色 加粗 灰色"), written);
    }

    // ---------- 占位符 ----------

    @Test
    @DisplayName("{} 占位符按顺序替换")
    void placeholders() {
        Logger logger = new LoggerImpl();
        logger.info("玩家 {} 在 {} 执行了 {}", "Steve", "world", "buy");
        assertTrue(last().endsWith("玩家 Steve 在 world 执行了 buy"), last());
    }

    @Test
    @DisplayName("参数多于占位符：忽略多余的，不抛")
    void extraArgsIgnored() {
        Logger logger = new LoggerImpl();
        logger.info("只有 {} 个占位符", "一", "二", "三");
        assertTrue(last().endsWith("只有 一 个占位符"), last());
    }

    @Test
    @DisplayName("占位符多于参数：剩下的原样保留")
    void missingArgsKeepPlaceholder() {
        Logger logger = new LoggerImpl();
        logger.info("{} 和 {}", "一");
        assertTrue(last().endsWith("一 和 {}"), last());
    }

    @Test
    @DisplayName("没有占位符也传参数：正文不受影响")
    void noPlaceholder() {
        Logger logger = new LoggerImpl();
        logger.info("没有占位符", "多余的");
        assertTrue(last().endsWith("没有占位符"), last());
    }

    // ---------- debug 开关 ----------

    @Test
    @DisplayName("debug 关掉时连格式化都不做，一条都不写")
    void debugCanBeDisabled() {
        Logger logger = new LoggerImpl();
        PluginInfo.setLoggerDebug(false);
        logger.debug("不该出现 {}", "x");
        assertTrue(collector.records.isEmpty(), "关掉调试后不该有任何记录：" + collector.text());
    }

    @Test
    @DisplayName("debug 只影响 debug，不影响其它级别")
    void debugSwitchDoesNotAffectOthers() {
        Logger logger = new LoggerImpl();
        PluginInfo.setLoggerDebug(false);
        logger.debug("d");
        logger.info("i");
        logger.warn("w");
        assertEquals(2, collector.records.size(), collector.text());
    }

    // ---------- 异常 ----------

    @Test
    @DisplayName("error(Throwable, ...) 写出两条：一条正文 + 一条带堆栈的异常")
    void throwableGetsOwnRecord() {
        Logger logger = new LoggerImpl();
        IllegalStateException failure = new IllegalStateException("坏了");
        logger.error(failure, "执行失败: {}", "任务A");

        assertEquals(2, collector.records.size(), collector.text());
        assertTrue(collector.records.get(0).getMessage().endsWith("执行失败: 任务A"),
                collector.records.get(0).getMessage());
        // 第二条必须真的**带着**异常对象，否则日志系统渲染不出堆栈
        assertNotNull(collector.records.get(1).getThrown(), "异常必须挂在 LogRecord 上，日志系统才能打印堆栈");
        assertEquals(failure, collector.records.get(1).getThrown());
    }

    @Test
    @DisplayName("error(Throwable=null, ...) 只写正文，不写空堆栈")
    void nullThrowableWritesOneRecord() {
        Logger logger = new LoggerImpl();
        // 必须强转：字面量 null 在 error(String, Object...) 与
        // error(Throwable, String, Object...) 之间是有歧义的（见接口注释里的说明）
        logger.error((Throwable) null, "只是错误");
        assertEquals(1, collector.records.size(), collector.text());
        assertTrue(last().endsWith("只是错误"), last());
    }

    @Test
    @DisplayName("不带异常的 error(...) 只写一条")
    void plainErrorWritesOneRecord() {
        Logger logger = new LoggerImpl();
        logger.error("出错了: {}", "原因");
        assertEquals(1, collector.records.size(), collector.text());
    }
}
