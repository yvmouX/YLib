package cn.yvmou.ylib.menu;

import org.bukkit.entity.Player;

/**
 * yml 里 {@code actions} 的**宿主侧实现**：{@link ActionSpec} 里那些本库不该替你决定的事都落在这里。
 *
 * <pre>{@code
 * Menus.actions(new MenuActions() {
 *     @Override
 *     public void message(Player viewer, String text) {
 *         viewer.sendMessage(LegacyCodes.toSection(text));
 *     }
 *
 *     @Override
 *     public void playerCommand(Player viewer, String command) {
 *         Bukkit.dispatchCommand(viewer, command);
 *     }
 *
 *     @Override
 *     public void consoleCommand(String command) {
 *         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
 *     }
 *
 *     @Override
 *     public void openMenu(Player viewer, String menuName) {
 *         for (MenuDefinition definition : Menus.definitions()) {
 *             if (definition.template().getClass().getSimpleName().equals(menuName)) {
 *                 Menus.open(definition, viewer);
 *                 return;
 *             }
 *         }
 *     }
 * });
 * }</pre>
 *
 * <h2>为什么这几个动作必须由宿主实现</h2>
 * 它们都要碰到本库之外的东西：文本渲染（{@code &} 色码还是 MiniMessage？前缀从哪来？）、
 * 命令分派（以玩家身份还是控制台？要不要先做权限判断？）、按名字找菜单（菜单注册在哪张表里？）。
 * 框架硬编码任何一个答案都会和宿主的既有做法打架，因此这里只定义签名，不提供默认实现。
 * <p>
 * {@code message} / {@code player_command} / {@code console_command} / {@code open_menu} 四个方法
 * 只在 yml 里真的配了对应动作时才会被调用，因此只用了其中几种的宿主可以留空其余几个。
 *
 * <h2>安全边界</h2>
 * <ul>
 *   <li>{@code player_command} 一律**不带前导斜杠**（yml 里写了也会剥掉），交由宿主决定怎么派发；</li>
 *   <li>{@code {player}} 在命令里被替换成玩家名。名字由服务端保证唯一性与合法性，因此不做额外转义；
 *       但宿主**应当**在 {@link #playerCommand} 里做权限判断，不要指望 yml 里的动作是安全的；</li>
 *   <li>{@code console_command} 是提权动作，服主写了什么就会以控制台身份执行什么——文档里必须写明这一点。</li>
 * </ul>
 */
public interface MenuActions {

    /** {@code type: message}：给玩家发一条文本（{@code {player}}、占位符已替换）。 */
    default void message(Player viewer, String text) {
    }

    /** {@code type: player_command}：以玩家身份执行一条命令（**已去掉前导斜杠**）。 */
    default void playerCommand(Player viewer, String command) {
    }

    /** {@code type: console_command}：以控制台身份执行一条命令（提权动作）。 */
    default void consoleCommand(String command) {
    }

    /** {@code type: open_menu}：打开另一个菜单；{@code menu} 是 yml 里写的那个名字。 */
    default void openMenu(Player viewer, String menu) {
    }
}
