# TA 端（Teaching Assistant）静态原型说明

本目录是 **BUPT TA 招聘系统（TA 角色）** 的前端静态原型页面集合，主要用于演示交互与 UI 评审。

技术栈：

- **页面**：HTML
- **样式**：Tailwind CSS（CDN）+ 少量页面内联样式
- **图标**：Iconify（CDN）
- **数据**：本目录以 **前端 Mock + 本地存储** 为主（`localStorage` / `IndexedDB`），不依赖真实后端即可演示

业务数据与完整流程以仓库的 `NLC_test`（Java）为准（见 `NLC_test/README.md`）。

---

## 页面清单（按常用使用路径）

建议从 `index.html` 开始体验整体流程。

| 页面 | 用途 | 常见入口/去向 |
|------|------|---------------|
| `index.html` | TA 登录页（前端 mock 登录） | 登录后进入 `dashboard.html` |
| `dashboard.html` | 工作台 / 概览 | 侧边栏可进入招聘、申请、资料、通知 |
| `personal-information.html` | 个人资料（用于后续申请表预填）+ 修改密码弹窗 | 保存后用于自动填充 `ta-recruitment-application-form.html` |
| `notifications.html` | 系统通知/公告展示 | 侧边栏入口 |
| `ta-recruitment-list.html` | TA 招聘岗位列表 | 点击岗位进入 `ta-recruitment-detail.html` |
| `ta-recruitment-detail.html` | 岗位详情 | 点击 Apply 进入 `ta-recruitment-application-form.html` |
| `ta-recruitment-application-form.html` | 申请表单（校验 + 草稿 + 简历 PDF 上传预览 + 自动预填） | Submit → `application-success.html`；Save progress → 返回 `ta-recruitment-detail.html` |
| `application-success.html` | 提交成功页（引导去“我的申请/招聘列表”） | 按钮跳转 `my-ta-applications.html` / `ta-recruitment-list.html` |
| `my-ta-applications.html` | 我的申请记录列表（原型展示） | 可进入 `application-review-detail.html` |
| `application-review-detail.html` | 申请/审核详情（界面原型） | 返回 `my-ta-applications.html` |
| `forgot-password.html` | 忘记密码：提交重置请求（前端 mock） | 供登录页/修改密码弹窗跳转 |

---

## 关键交互与本地存储说明（与演示相关）

### 1) 个人资料保存（用于申请表自动预填）

- **页面**：`personal-information.html`
- **存储**：`localStorage`
  - Key：`ta_profile_v1`
  - 保存内容（用于申请表预填）：
    - QMplus account
    - BUPT Student ID Number（左侧固定展示的 Student ID，会被保存但不可编辑）
    - surname / forename / Chinese name
    - mobile/cell phone number
    - gender
    - BUPT school

### 2) 申请表自动预填（从个人资料读取）

- **页面**：`ta-recruitment-application-form.html`
- **行为**：页面加载时读取 `ta_profile_v1`，并对表单字段进行“空才填”的自动预填。
- **优先级**：
  - 若你已存在申请草稿（draft）或当前字段已有值，则 **不会被个人资料覆盖**。
  - 这样既能保障草稿不会丢，也能让第一次填写更省事。

### 3) 申请草稿保存 / 自动恢复

- **页面**：`ta-recruitment-application-form.html`
- **表单草稿**：`localStorage`
  - Key：`ta_application_draft_v1`
  - 触发：输入/选择后会自动保存（定时防抖），“Save progress”也会手动保存
- **简历 PDF**：`IndexedDB`
  - DB：`ta_application_resume_db_v1`
  - Store：`resume`
  - Key：`current`
  - 用途：支持刷新页面/重进页面后恢复简历文件并继续预览

### 4) 简历 PDF 上传与预览

- **页面**：`ta-recruitment-application-form.html`
- **限制**：仅允许 PDF，最大 20MB
- **预览**：使用浏览器内置 PDF Viewer（iframe），并提供 “Open in new tab”

### 5) 提交校验与提交成功跳转

- **页面**：`ta-recruitment-application-form.html`
- **校验**：必填项 + 手机号格式 + 学号格式（前端校验）
- **提交成功**：跳转到 `application-success.html`

### 6) 密码相关流程（原型）

- **修改密码（登录态自助）**：`personal-information.html` 的弹窗（mock 校验与提示）
- **忘记密码（提交重置请求）**：`forgot-password.html`（mock 提交流程，强调需要管理员审核）

---

## 本地预览方式

### 方式一：直接打开（最快）

在资源管理器中双击 **`index.html`** 用浏览器打开即可。

### 方式二：本地静态服务器（推荐）

在 `TA` 目录下执行：

```powershell
cd TA
python -m http.server 5500
```

浏览器访问：

- http://localhost:5500/
- 或直接访问 http://localhost:5500/index.html

---

## 与后端（`NLC_test`）的关系

- 仓库中的 **`NLC_test`** 提供 Java 服务、REST API 以及可用于联调的静态资源页面。
- 本目录 **`TA/`** 为独立静态原型，当前交互多为前端 mock 与本地存储。
- 若需要与真实 API 联调：
  - 需要按项目约定接入 `fetch` 请求并处理鉴权/数据结构
  - 或将本目录页面整合到 `NLC_test/src/main/resources/static` 中统一托管

---

## 技术与维护提示

- **CDN 依赖**：Tailwind / Iconify 使用 CDN，离线环境下样式和图标可能不可用。
- **浏览器建议**：Chrome / Edge / Firefox 最新版。
- **维护建议**：
  - 新增/重命名 `.html` 时同步更新侧边栏链接与本 README。
  - 若调整表单字段 `id/name`，注意同步更新：
    - 申请草稿保存/恢复逻辑
    - 个人资料预填映射逻辑
