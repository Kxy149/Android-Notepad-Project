# NotePad 记事本应用

## 项目简介  
NotePad 是一款轻量级 Android 记事本应用，基于 Android 官方数据库操作教程扩展实现。该应用聚焦于**数据持久化**与**内容提供者（Content Provider）** 设计模式的实践，通过简洁的界面与核心功能，为 Android 初学者提供直观的学习案例，帮助理解SQLite 数据库操作、组件间数据共享及基础 UI 交互逻辑。


## 功能特点  
- **基础笔记管理**：支持创建、编辑、删除笔记，操作流程简洁直观
 <img width="304" height="588" alt="Screenshot_20251127-103813" src="https://github.com/user-attachments/assets/5df364b4-4a15-4f61-b914-0e87f292428c" />
 
- **时间排序**：笔记默认按修改时间降序排列，最新编辑内容优先展示
 <img width="304" height="588" alt="Screenshot_20251127-103813" src="https://github.com/user-attachments/assets/5df364b4-4a15-4f61-b914-0e87f292428c" />
   
- **分类与置顶**：可对笔记进行分类管理，并支持置顶重要内容
<img width="304" height="588" alt="Screenshot_20251127-103952" src="https://github.com/user-attachments/assets/866983c4-3d44-40dd-b446-8997c77f9905" />

- **实时保存**：编辑过程中自动保存内容，避免意外丢失
  
- **搜索与筛选**：支持按关键词搜索笔记，或按分类快速筛选内容
<img width="304" height="588" alt="Screenshot_20251127-104106" src="https://github.com/user-attachments/assets/bcd973bb-37d8-4a70-836b-8de1ecb8a198" />

- **修改标题**：支持随时更改标题，可按标题搜索笔记  
<img width="304" height="588" alt="Screenshot_20251127-112730" src="https://github.com/user-attachments/assets/aead00bf-9248-4cd4-8072-e5f25830369a" />


## 技术架构  
### 核心技术栈  
- **数据存储**：基于 SQLite 数据库存储笔记数据，通过自定义 `NotePadProvider` 实现跨组件数据访问  
- **架构设计**：采用内容提供者（Content Provider）模式，通过契约类 `NotePad` 统一定义数据结构与访问接口，降低模块耦合  
- **UI 组件**：  
  - `NotesList`：笔记列表页面，负责数据展示与操作入口  
  - `NoteEditor`：笔记编辑页面，集成自定义 `LinedEditText` 实现带行线的编辑体验  
  - `NotesLiveFolder`：支持桌面动态文件夹，快速访问笔记内容  


## 项目结构  
```  
NotePad/  
├── app/  
│   ├── src/main/  
│   │   ├── java/com/example/android/notepad/  
│   │   │   ├── NotePad.java           // 数据契约类（定义表结构、字段常量）  
│   │   │   ├── NotePadProvider.java   // 自定义内容提供者（处理数据 CRUD 操作）  
│   │   │   ├── NotesList.java         // 笔记列表 Activity（展示与管理笔记）  
│   │   │   ├── NoteEditor.java        // 笔记编辑 Activity（创建/修改笔记内容）  
│   │   │   └── NotesLiveFolder.java   // 动态文件夹支持类  
│   │   └── res/                       // 资源文件（布局、字符串、样式等）  
│   └── build.gradle                   // 模块构建配置（依赖、编译版本等）  
├── build.gradle                       // 项目全局构建配置  
├── README.md                          // 项目说明文档  
└── LICENSE                            // 开源许可证文件  
```  


## 数据库设计  
核心表 `notes` 结构如下：  

| 字段名    | 类型       | 说明                     |  
|-----------|------------|--------------------------|  
| `_ID`     | INTEGER    | 笔记唯一标识（主键，自增） |  
| `title`   | TEXT       | 笔记标题                 |  
| `note`    | TEXT       | 笔记详细内容             |  
| `created` | INTEGER    | 创建时间戳（毫秒）       |  
| `modified`| INTEGER    | 最后修改时间戳（毫秒）   |  
| `category`| TEXT       | 笔记分类（如「工作」「生活」） |  
| `pinned`  | INTEGER    | 是否置顶（0=不置顶，1=置顶） |  


## 安装与运行  
1. 克隆仓库到本地：  
   ```bash  
   git clone https://github.com/你的用户名/NotePad.git  
   ```  
2. 用 Android Studio 打开项目，等待 Gradle 同步完成（首次打开可能需要下载依赖）。  
3. 连接 Android 设备（开启 USB 调试）或启动模拟器（建议 API 21+）。  
4. 点击工具栏的「Run 'app'」按钮（▶️ 图标），等待应用安装并启动。  


## 学习价值  
- 理解 Content Provider 的设计思想与实现方式（跨应用数据共享的核心机制）。  
- 掌握 SQLite 数据库的基本操作（CRUD）及通过 `ContentResolver` 访问数据的流程。  
- 学习 Android 基础组件（Activity、自定义 View）的协作方式。  


## 参考资料  
- [Android 官方 Content Provider 文档](https://developer.android.com/guide/topics/providers/content-providers)  
- [SQLite 数据库操作指南](https://developer.android.com/training/data-storage/sqlite)  

