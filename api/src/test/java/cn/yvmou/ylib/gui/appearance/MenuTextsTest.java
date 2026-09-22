package cn.yvmou.ylib.gui.appearance;

import cn.yvmou.ylib.message.MessageService;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 菜单文案的取值规则：**语言键存在就用语言文件，没有才用配置里的字面量**，两处都做命名占位符替换。
 * <p>
 * 这条规则要是只能靠开菜单来验，多半会被改错：{@code Menu} 的构造要走 {@code Bukkit.createInventory}，
 * 单测里起不来，所以解析被单独放在 {@link MenuTexts} 里。
 * <p>
 * 这里用**手写的消息服务替身**而不是真的从语言文件加载：本模块刻意不依赖 {@code core}（那是消费方
 * 唯一能看见的编译期表面），而这条规则要的输入只有「某个键存不存在、存在时它是什么串」这一个契约，
 * 替身把它表达得比真加载器更直接。
 */
class MenuTextsTest {

    private static final String KEY = "gui.admin.main.items.quests.name";

    @Test
    @DisplayName("语言文件里没有这个键：用配置里的模板，占位符照常替换")
    void fallsBackToConfigTemplate() {
        MessageService messages = messages(Collections.<String, String>emptyMap());

        String text = MenuTexts.text(messages, null, KEY, "&7共 {count} 个任务", Map.of("count", "3"));

        // 注意这里**没有**被渲染成 §7：MenuTexts 只负责取值与替换，渲染统一由 MenuItem 做一次
        // （先渲染再拼接会让 § 码在二次渲染时被当成普通字符）
        assertEquals("&7共 3 个任务", text);
    }

    @Test
    @DisplayName("语言文件里有这个键：语言文件赢，配置那份让位（服务器不改配置也能翻译）")
    void languageFileWins() {
        MessageService messages = messages(Map.of(KEY, "§aQuests"));

        String text = MenuTexts.text(messages, null, KEY, "&f任务列表", Map.of());

        assertEquals("§aQuests", text, "语言文件里的键必须盖过配置里的字面量");
    }

    @Test
    @DisplayName("语言文件里的占位符同样生效：翻译能重排语序（位置占位符做不到这件事）")
    void languageFileKeepsNamedPlaceholders() {
        MessageService messages = messages(Map.of(KEY, "{count} 个任务"));

        assertEquals("3 个任务", MenuTexts.text(messages, null, KEY, "任务 {count} 个", Map.of("count", "3")));
    }

    @Test
    @DisplayName("lore：语言文件里一个键写成多行就按换行拆开——否则多行 lore 没法翻译")
    void multiLineLoreComesFromBlockScalar() {
        String lore = KEY + ".lore";
        MessageService messages = messages(Map.of(lore, "第一行\n第二行\n第三行"));

        List<String> lines = MenuTexts.lines(messages, null, lore, List.of("fallback"), Map.of());

        assertEquals(Arrays.asList("第一行", "第二行", "第三行"), lines);
    }

    @Test
    @DisplayName("lore：语言键写成空串 = 这台服务器不要这几行，而不是渲染出一行空白")
    void emptyLanguageLoreMeansNoLines() {
        String lore = KEY + ".lore";
        MessageService messages = messages(Map.of(lore, ""));

        assertEquals(Collections.<String>emptyList(), MenuTexts.lines(messages, null, lore, List.of("&7默认文案"), Map.of()));
    }

    @Test
    @DisplayName("lore：语言文件里没有这个键就用配置里的列表，逐行替换占位符")
    void loreFallsBackToConfigList() {
        MessageService messages = messages(Collections.<String, String>emptyMap());

        List<String> lines = MenuTexts.lines(messages, null, KEY + ".lore",
                Arrays.asList("&8{id}", "", "&7类型: &f{type}"), Map.of("id", "mining", "type", "DAILY"));

        assertEquals(Arrays.asList("&8mining", "", "&7类型: &fDAILY"), lines);
    }

    @Test
    @DisplayName("没有消息服务时全部按字面量处理（纯文本标题那种场景）")
    void withoutMessagesEverythingIsLiteral() {
        assertEquals("共 3 个", MenuTexts.text(null, null, KEY, "共 {count} 个", Map.of("count", "3")));
        assertEquals(Arrays.asList("A", "B"), MenuTexts.lines(null, null, KEY + ".lore", Arrays.asList("A", "B"), Map.of()));
    }

    /** 只表达「键存不存在、值是什么串」的消息服务替身；其余方法在这条规则里用不到。 */
    private static MessageService messages(Map<String, String> entries) {
        return new StubMessages(entries);
    }

    private static final class StubMessages implements MessageService {

        private final Map<String, String> entries;

        StubMessages(Map<String, String> entries) {
            this.entries = new HashMap<String, String>(entries);
        }

        @Override
        public String getLanguage() {
            return "zh_CN";
        }

        @Override
        public boolean setLanguage(String languageCode) {
            return false;
        }

        @Override
        public List<String> getAvailableLanguages() {
            return Collections.singletonList("zh_CN");
        }

        @Override
        public boolean has(String key) {
            return entries.containsKey(key);
        }

        @Override
        public String raw(String key, Object... args) {
            String value = entries.get(key);
            return value == null ? "Missing message: " + key : value;
        }

        @Override
        public String raw(CommandSender sender, String key, Object... args) {
            return raw(key, args);
        }

        @Override
        public void send(CommandSender sender, String key, Object... args) {
        }

        @Override
        public void sendRaw(CommandSender sender, String message) {
        }

        @Override
        public String prefix() {
            return "";
        }

        @Override
        public String getMinecraftLanguageCode(String languageCode) {
            return languageCode;
        }

        @Override
        public void reload() {
        }
    }
}
