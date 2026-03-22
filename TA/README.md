# TA 端（Teaching Assistant）界面说明

本目录为 **助教（TA）招聘系统 — TA 角色** 的前端静态原型页面，使用 **HTML + Tailwind CSS（CDN）+ Iconify**，用于课程展示与 UI 评审。业务数据与完整流程以 `NLC_test` 中的 Java 服务为准（见仓库内 `NLC_test/README.md`）。

---

## 目录内页面一览

| 文件 | 说明 |
|------|------|
| `index.html` | TA 登录页 |
| `forgot-password.html` | 忘记密码（界面） |
| `dashboard.html` | TA 工作台 / 仪表盘 |
| `personal-information.html` | 个人信息 / 资料 |
| `ta-recruitment-list.html` | TA 招聘岗位列表 |
| `ta-recruitment-detail.html` | 岗位详情 |
| `ta-recruitment-application-form.html` | 投递申请表单 |
| `my-ta-applications.html` | 我的申请记录 |
| `application-review-detail.html` | 申请/审核相关详情（界面） |

侧栏导航在各页之间通过 `<a href="...">` 串联，可从任意页进入其他关联页。

---

## 本地如何预览

### 方式一：直接打开（最快）

在资源管理器中双击 **`index.html`**，用浏览器打开即可。

### 方式二：本地静态服务器（推荐）

在 `TA` 目录下执行：

```powershell
cd TA
python -m http.server 5500
```

浏览器访问：**http://localhost:5500/**（默认进入目录列表，点开 `index.html` 或访问 **http://localhost:5500/index.html**）。

---

## 与后端的关系

- 仓库中的 **`NLC_test`** 提供基于 Java 的内嵌 HTTP 服务、REST API 与 `src/main/resources/static` 下的联调界面。
- 本 **`TA/`** 目录为**独立静态页面**，若需与真实 API 联调，需按项目约定修改 `fetch` 地址或合并进 `NLC_test` 的静态资源，具体以组内架构为准。

---

## 技术说明

- **样式**：Tailwind CSS、部分页面内联样式；依赖 CDN，预览时需联网。
- **图标**：Iconify（CDN）。
- **浏览器**：建议使用最新版 Chrome / Edge / Firefox。

---

## 维护提示

新增或重命名 `.html` 时，请同步更新侧栏 `href` 与本 README 中的表格，避免断链。
