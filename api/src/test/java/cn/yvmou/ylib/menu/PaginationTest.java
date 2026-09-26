package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.SymbolState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页与点击类型：那些「不靠服务端也能钉住」的规则。
 * <p>
 * 分页最难的从来不是切片，而是「页码从哪来、翻页按钮看什么、槽位怎么换成下标」——
 * 这三件事都能在没有服务端的环境里算清楚，因此都放在这里。翻页按钮的行为（到底了不动、
 * 没到底才翻）用假的 {@link MenuContext} 直接驱动回调来验。
 */
class PaginationTest {

    @BeforeAll
    static void setUp() {
        TestServer.install();
    }

    // ---------- 测试替身 ----------

    /** 一个只记录页码的 MenuContext 替身。 */
    private static final class FakeContext implements MenuContext {

        private int page;
        private final int perPage;
        private final int pageCount;
        private final int pageSize;
        private final List<Slot> slots;
        private int refreshes;

        FakeContext(int page, int perPage, int pageCount, int slotCount) {
            this(page, perPage, pageCount, slotCount, slotCount);
        }

        /**
         * @param pageSize 这一页**实际取到**的条目数：总页数未知时「下一页」按它判断，
         *                 与 {@code slotCount}（能放几格）是两件事
         */
        FakeContext(int page, int perPage, int pageCount, int slotCount, int pageSize) {
            this.page = page;
            this.perPage = perPage;
            this.pageCount = pageCount;
            this.pageSize = pageSize;
            List<Slot> built = new ArrayList<Slot>();
            for (int index = 0; index < slotCount; index++) {
                built.add(new Slot(index, 0, index));
            }
            this.slots = Collections.unmodifiableList(built);
        }

        @Override
        public Player viewer() {
            return null;
        }

        @Override
        public ClickType clickType() {
            return ClickType.LEFT;
        }

        @Override
        public org.bukkit.event.inventory.InventoryClickEvent event() {
            return null;
        }

        @Override
        public MenuControl menu() {
            return new MenuControl() {
                @Override
                public Player viewer() {
                    return null;
                }

                @Override
                public void close() {
                }

                @Override
                public void refresh() {
                    refreshes++;
                }

                @Override
                public MenuSession open(Player viewer) {
                    return null;
                }

                @Override
                public boolean isOpen(Player viewer) {
                    return false;
                }

                @Override
                public MenuDefinition definition() {
                    return null;
                }

                @Override
                public void resetSession(Player viewer) {
                }
            };
        }

        @Override
        public MenuSession session() {
            return null;
        }

        @Override
        public List<Slot> symbolSlots() {
            return slots;
        }

        @Override
        public Slot slot() {
            return null;
        }

        @Override
        public int page() {
            return page;
        }

        @Override
        public void page(int page) {
            this.page = page;
        }

        @Override
        public int perPage() {
            return perPage;
        }

        @Override
        public int pageSize() {
            return pageSize;
        }

        @Override
        public int pageCount() {
            return pageCount;
        }

        @Override
        public int indexInSymbol(Slot slot) {
            return slots.indexOf(slot);
        }
    }

    private interface Declare {
        void apply(MenuBuilder b);
    }

    private static MenuDefinition build(final String[] layout, final Declare declare) {
        return build(layout, 9, declare);
    }

    private static MenuDefinition build(final String[] layout, final int columns, final Declare declare) {
        MenuTemplate template = new MenuTemplate() {
            @Override
            public String[] layout() {
                return layout;
            }

            @Override
            public int columns() {
                return columns;
            }

            @Override
            public void declare(MenuBuilder b) {
                declare.apply(b);
            }
        };
        MenuBuilder builder = new MenuBuilder();
        template.declare(builder);
        return builder.build(template, null);
    }

    /** 让符号拿到一个非空的槽位表（构建期回填的就是它）。 */
    private static SymbolState pagedSymbol(MenuDefinition definition) {
        return definition.stateOf("l");
    }

    // ---------- 分页构建 ----------

    @Test
    @DisplayName("分页符号：每页条数取布局里的格数")
    void pagedSymbolTakesCapacityFromLayout() {
        MenuDefinition definition = build(new String[]{"ll#######"}, 9, new Declare() {
            @Override
            public void apply(MenuBuilder b) {
                b.symbol('#').of(Items.of(Material.STONE));
                b.symbol('l').item("entry").pageItems(new Fetcher() {
                    @Override
                    public List<ItemStack> fetch(Player viewer, int page, int perPage) {
                        return Collections.emptyList();
                    }
                });
            }
        });
        SymbolState state = pagedSymbol(definition);
        assertTrue(state.isPaged());
        assertEquals(2, state.perPage());
        assertEquals(2, state.slots().size());
        // 槽位是 0 与 1（行首两格）
        assertEquals(0, state.slots().get(0).index());
        assertEquals(1, state.slots().get(1).index());
    }

    @Test
    @DisplayName("分页符号不能再声明占位符（条目由数据决定）")
    void pagedSymbolRejectsPlaceholders() {
        MenuConfigException failure = assertThrows(MenuConfigException.class, () -> build(
                new String[]{"ll#######"}, 9, new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE));
                        b.symbol('l').pageItems(new Fetcher() {
                            @Override
                            public List<ItemStack> fetch(Player viewer, int page, int perPage) {
                                return Collections.emptyList();
                            }
                        }).ph("x", new java.util.function.Function<MenuContext, String>() {
                            @Override
                            public String apply(MenuContext ctx) {
                                return "1";
                            }
                        });
                    }
                }));
        assertTrue(failure.getMessage().contains("分页符号"), failure.getMessage());
    }

    // ---------- 翻页按钮 ----------

    @Test
    @DisplayName("上一页：不在第一页才翻，并触发一次 refresh")
    void previousPageBoundary() {
        MenuDefinition definition = build(new String[]{"p########"}, 9, new Declare() {
            @Override
            public void apply(MenuBuilder b) {
                b.symbol('#').of(Items.of(Material.STONE));
                b.symbol('p').item("prev").of(Items.of(Material.ARROW)).previousPage();
            }
        });
        ClickHandler handler = definition.stateOf("p").resolve(definition.slotsOf("p").get(0), ClickType.LEFT);

        FakeContext middle = new FakeContext(2, 4, 5, 1);
        handler.handle(definition.slotsOf("p").get(0), middle);
        assertEquals(1, middle.page(), "第 2 页点上一页应当到第 1 页");
        assertEquals(1, middle.refreshes, "翻页必须 refresh");

        FakeContext first = new FakeContext(0, 4, 5, 1);
        handler.handle(definition.slotsOf("p").get(0), first);
        assertEquals(0, first.page(), "第一页点上一页应当不动");
        assertEquals(0, first.refreshes, "没翻动就不该 refresh");
    }

    @Test
    @DisplayName("下一页：总页数已知时按它判断，到底了不动")
    void nextPageWithKnownTotal() {
        ClickHandler handler = nextHandler();

        FakeContext middle = new FakeContext(1, 4, 5, 4);
        handler.handle(new Slot(0, 0, 0), middle);
        assertEquals(2, middle.page());
        assertEquals(1, middle.refreshes);

        FakeContext last = new FakeContext(4, 4, 5, 4);
        handler.handle(new Slot(0, 0, 0), last);
        assertEquals(4, last.page(), "最后一页点下一页应当不动");
        assertEquals(0, last.refreshes);
    }

    @Test
    @DisplayName("下一页：总页数未知（-1）时退化为「这一页取满了 perPage 条即还有下一页」")
    void nextPageWithUnknownTotal() {
        ClickHandler handler = nextHandler();

        // 取满 4 条 → 认为还有下一页（格子数与取到的条数是两件事，这里刻意让它们不同）
        FakeContext full = new FakeContext(0, 4, -1, 6, 4);
        handler.handle(new Slot(0, 0, 0), full);
        assertEquals(1, full.page());

        // 只取到 2 条 → 到底了
        FakeContext partial = new FakeContext(0, 4, -1, 6, 2);
        handler.handle(new Slot(0, 0, 0), partial);
        assertEquals(0, partial.page(), "没取满就不该再翻");
        assertEquals(0, partial.refreshes);
    }

    private static ClickHandler nextHandler() {
        MenuDefinition definition = build(new String[]{"n########"}, 9, new Declare() {
            @Override
            public void apply(MenuBuilder b) {
                b.symbol('#').of(Items.of(Material.STONE));
                b.symbol('n').item("next").of(Items.of(Material.ARROW)).nextPage();
            }
        });
        return definition.stateOf("n").resolve(definition.slotsOf("n").get(0), ClickType.LEFT);
    }

    @Test
    @DisplayName("页码指示器：{page} 是 1 起，{pages} 未知时显示 ?")
    void pageIndicator() {
        MenuDefinition definition = build(new String[]{"q########"}, 9, new Declare() {
            @Override
            public void apply(MenuBuilder b) {
                b.symbol('#').of(Items.of(Material.STONE));
                b.symbol('q').item("info").of(Items.of(Material.PAPER).name("&f第 {page}/{pages} 页"))
                        .pageIndicator();
            }
        });
        SymbolState state = definition.stateOf("q");
        assertEquals(2, state.placeholders().size());

        // 第 3 页 / 共 5 页
        FakeContext known = new FakeContext(2, 4, 5, 1);
        assertEquals("3", state.placeholders().get("page").apply(known));
        assertEquals("5", state.placeholders().get("pages").apply(known));

        // 总页数未知
        FakeContext unknown = new FakeContext(0, 4, -1, 1);
        assertEquals("1", state.placeholders().get("page").apply(unknown));
        assertEquals("?", state.placeholders().get("pages").apply(unknown));
    }

    @Test
    @DisplayName("槽位 → 页内下标：全局下标 = page * perPage + indexInSymbol")
    void globalIndexFromSlot() {
        FakeContext context = new FakeContext(3, 4, 5, 4);
        List<Slot> slots = context.symbolSlots();
        assertEquals(0, context.indexInSymbol(slots.get(0)));
        assertEquals(3, context.indexInSymbol(slots.get(3)));
        // 全局下标：第 3 页（0 起）的第 2 条 = 3 * 4 + 1 = 13
        assertEquals(13, context.page() * context.perPage() + context.indexInSymbol(slots.get(1)));
        assertEquals(-1, context.indexInSymbol(new Slot(99, 9, 9)), "不属于这个符号的槽位给 -1");
    }

    // ---------- 点击类型 ----------

    @Test
    @DisplayName("ClickType 映射：键位与修饰键分成两个维度")
    void clickTypeMapping() {
        assertEquals(ClickKind.LEFT, ClickType.of(org.bukkit.event.inventory.ClickType.LEFT).kind());
        assertEquals(ClickKind.RIGHT, ClickType.of(org.bukkit.event.inventory.ClickType.RIGHT).kind());
        assertEquals(ClickKind.MIDDLE, ClickType.of(org.bukkit.event.inventory.ClickType.MIDDLE).kind());
        assertEquals(ClickKind.NUMBER_KEY, ClickType.of(org.bukkit.event.inventory.ClickType.NUMBER_KEY).kind());
        assertEquals(ClickKind.DROP, ClickType.of(org.bukkit.event.inventory.ClickType.DROP).kind());
        assertEquals(ClickKind.SWAP_OFFHAND, ClickType.of(org.bukkit.event.inventory.ClickType.SWAP_OFFHAND).kind());
        assertEquals(ClickKind.DOUBLE_CLICK, ClickType.of(org.bukkit.event.inventory.ClickType.DOUBLE_CLICK).kind());

        // Bukkit 的 ClickType 里**没有** SHIFT_LEFT 这种常量：Shift 由 isShiftClick() 表达。
        // 这正是本库把「键位」与「修饰键」分成两个维度的原因——组合类型由本库自己拼出来
        assertFalse(ClickType.of(org.bukkit.event.inventory.ClickType.LEFT).shift());
        assertFalse(ClickType.of(org.bukkit.event.inventory.ClickType.LEFT).control());
        assertEquals(ClickKind.UNKNOWN, ClickType.of(org.bukkit.event.inventory.ClickType.SHIFT_LEFT).kind(),
                "Bukkit 没有这个常量时应当走兜底，而不是抛");
        assertEquals(ClickType.SHIFT_LEFT, new ClickType(ClickKind.LEFT, true, false));

        // CONTROL_DROP：键位是丢弃，但修饰键维度单独标出来（破坏力不同）
        ClickType controlDrop = ClickType.of(org.bukkit.event.inventory.ClickType.CONTROL_DROP);
        assertEquals(ClickKind.DROP, controlDrop.kind());
        assertTrue(controlDrop.control());

        // 认不出来的点击走兜底，不抛
        assertEquals(ClickKind.UNKNOWN, ClickType.of(null).kind());
        assertEquals(ClickKind.UNKNOWN, ClickType.of(org.bukkit.event.inventory.ClickType.UNKNOWN).kind());
    }

    @Test
    @DisplayName("ClickType 常量与相等性")
    void clickTypeConstants() {
        assertEquals(ClickType.LEFT, new ClickType(ClickKind.LEFT, false, false));
        assertEquals(ClickType.SHIFT_LEFT, new ClickType(ClickKind.LEFT, true, false));
        assertTrue(ClickType.LEFT.isLeft());
        assertFalse(ClickType.LEFT.isRight());
        assertTrue(ClickType.RIGHT.isRight());
        assertEquals("SHIFT+LEFT", ClickType.SHIFT_LEFT.toString());
        assertEquals("LEFT", ClickType.LEFT.toString());
    }

    @Test
    @DisplayName("Shift 组合有单独注册时走它，没注册时退化到普通键位")
    void shiftPrecedence() {
        final List<String> log = new ArrayList<String>();
        MenuDefinition definition = build(new String[]{"#x#######"}, 9, new Declare() {
            @Override
            public void apply(MenuBuilder b) {
                b.symbol('#').of(Items.of(Material.STONE))
                        .onLeftClick(new ClickHandler() {
                            @Override
                            public void handle(Slot slot, MenuContext ctx) {
                                log.add("plain");
                            }
                        });
                b.symbol('x').of(Items.of(Material.STONE))
                        .onLeftClick(new ClickHandler() {
                            @Override
                            public void handle(Slot slot, MenuContext ctx) {
                                log.add("plain-x");
                            }
                        })
                        .onShiftLeftClick(new ClickHandler() {
                            @Override
                            public void handle(Slot slot, MenuContext ctx) {
                                log.add("shift-x");
                            }
                        });
            }
        });

        Slot hash = definition.slotsOf("#").get(0);
        Slot x = definition.slotsOf("x").get(0);
        assertNotNull(definition.stateOf("#").resolve(hash, ClickType.SHIFT_LEFT));
        assertEquals(definition.stateOf("#").resolve(hash, ClickType.LEFT),
                definition.stateOf("#").resolve(hash, ClickType.SHIFT_LEFT),
                "没注册 Shift 组合时退化到普通左键");
        assertNull(definition.stateOf("x").resolve(x, ClickType.RIGHT), "右键没注册即 null");
        assertFalse(definition.stateOf("x").resolve(x, ClickType.LEFT)
                == definition.stateOf("x").resolve(x, ClickType.SHIFT_LEFT),
                "注册了 Shift 组合时应当是不同的回调");
    }
}
