```
YLib/
├── common/              # 基础模块：枚举、异常 (Java 8)
├── core/                # 核心逻辑：API 定义、具体实现 (Java 8)
│   ├── api/             # 对外暴露的接口 (Scheduler, Config, Command)
│   └── impl/            # 接口的具体逻辑实现
├── platform/            # 平台适配层
│   ├── folia/           # Folia 专用实现 (Java 17)
│   ├── paper/           # Paper 专用实现 (Java 17)
│   └── spigot/          # Spigot 基础实现 (Java 8)
└── build.gradle.kts     # 统一管理版本和发布逻辑
```