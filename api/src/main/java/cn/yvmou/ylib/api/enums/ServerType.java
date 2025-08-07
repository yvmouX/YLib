package cn.yvmou.ylib.api.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * 服务器类型枚举，用于自动检测和创建对应的YLib实现
 * 
 * @author yvmou
 * @since 1.0.0-beta5
 */
public enum ServerType {
    
    /**
     * Folia服务器 - 支持区域化多线程
     */
    FOLIA(
        "Folia",
        "io.papermc.paper.threadedregions.RegionizedServer",
        "cn.yvmou.ylib.folia.YLibImplFolia",
        1
    ),
    
    /**
     * Paper服务器 - 高性能Spigot分支
     */
    PAPER(
        "Paper", 
        "com.destroystokyo.paper.PaperConfig",
        "cn.yvmou.ylib.paper.YLibImplPaper",
        2
    ),
    
    /**
     * Spigot服务器 - 标准Bukkit实现
     */
    SPIGOT(
        "Spigot",
        "org.spigotmc.SpigotConfig", 
        "cn.yvmou.ylib.spigot.YLibImplSpigot",
        3
    ),
    
    /**
     * 未知服务器类型
     */
    UNKNOWN(
        "Unknown",
        null,
        null,
        999
    );
    
    private final String displayName;
    private final String detectionClass;
    private final String implementationClass;
    private final int priority;
    private final static Logger logger = Logger.getLogger("YLib-ServerType");
    
    ServerType(@NotNull String displayName, @Nullable String detectionClass, 
               @Nullable String implementationClass, int priority) {
        this.displayName = displayName;
        this.detectionClass = detectionClass;
        this.implementationClass = implementationClass;
        this.priority = priority;
    }
    
    /**
     * 获取显示名称
     * 
     * @return 显示名称
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取检测类名
     * 
     * @return 检测类名，如果为UNKNOWN则返回null
     */
    @Nullable
    public String getDetectionClass() {
        return detectionClass;
    }
    
    /**
     * 获取实现类名
     * 
     * @return 实现类名，如果为UNKNOWN则返回null
     */
    @Nullable
    public String getImplementationClass() {
        return implementationClass;
    }
    
    /**
     * 获取优先级（数字越小优先级越高）
     * 
     * @return 优先级
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * 检查当前服务器是否为此类型
     * 
     * @return 如果是此类型返回true，否则返回false
     */
    public boolean selfCheck() {
        if (detectionClass == null) {
            return false;
        }
        
        try {
            Class.forName(detectionClass);
            logger.info("检测到 " + displayName + " 服务器");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 自动检测当前服务器类型
     * 
     * @return 检测到的服务器类型，如果无法检测则返回UNKNOWN
     */
    @NotNull
    public static ServerType detectServerType() {
        // 按优先级顺序检测
        for (ServerType type : values()) {
            if (type == UNKNOWN) continue;
            
            if (type.selfCheck()) {
                logger.info("服务器类型检测完成: " + type.getDisplayName());
                return type;
            }
        }
        
        logger.warning("无法检测服务器类型，将使用UNKNOWN类型");
        return UNKNOWN;
    }
}