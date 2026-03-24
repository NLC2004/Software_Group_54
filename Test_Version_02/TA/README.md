# TA 端（Teaching Assistant Portal）

本目录为 **助教（TA）招聘系统 — TA（申请人）角色** 的前端静态原型，技术栈为 **HTML + Tailwind CSS（CDN）+ Iconify**。用于课程展示、原型评审与小组分工说明。  
**业务数据、鉴权与完整业务流程** 以仓库内 **`NLC_test`** 的 Java 服务为准（见 [`../NLC_test/README.md`](../NLC_test/README.md)）。

---

## TA 角色能做什么（与后端设计对齐）

| 能力 | 说明 |
|------|------|
| 注册 / 登录 | 独立账号，角色为 TA（后端 `POST /api/auth/register`、`/api/auth/login`） |
| 个人资料 | 查看与编辑个人信息（`GET/PUT /api/auth/profile` 等，以实际实现为准） |
| 浏览岗位 | 查看开放的课程 TA / 活动等岗位 |
| 投递申请 | 填写申请表、上传 CV（`POST /api/upload` + 申请接口，需携带 Token） |
| 跟踪状态 | 查看申请进度；可对 **待处理** 申请执行撤回等操作（以 API 为准） |

**建议演示路径（全系统）：** 注册 TA → 浏览岗位 → 申请 → MO 审核 → Admin 查看工作量（见 `NLC_test` README）。

---

## 目录与页面说明

```
TA/
├── index.html                         # 登录入口
├── forgot-password.html               # 忘记密码（界面）
├── dashboard.html                     # TA 工作台
├── personal-information.html          # 个人信息 / 资料
├── ta-recruitment-list.html           # 招聘岗位列表
├── ta-recruitment-detail.html         # 岗位详情
├── ta-recruitment-application-form.html  # 申请表单
├── my-ta-applications.html            # 我的申请
├── application-review-detail.html     # 申请/审核详情（界面）
└── README.md
```

### 页面一览

| 文件 | 说明 |
|------|------|
| `index.html` | TA 登录页 |
| `forgot-password.html` | 忘记密码（界面原型，是否接后端依项目而定） |
| `dashboard.html` | TA 工作台 / 仪表盘 |
| `personal-information.html` | 个人信息与资料维护 |
| `ta-recruitment-list.html` | 岗位浏览与筛选入口 |
| `ta-recruitment-detail.html` | 单岗位详情 |
| `ta-recruitment-application-form.html` | 在线投递 / 申请表 |
| `my-ta-applications.html` | 我的申请列表与状态 |
| `application-review-detail.html` | 单条申请详情或审核视角（界面占位） |

侧栏在各页通过 `<a href="...">` 串联；修改文件名时请全局替换链接并更新本表。

---

## 导航结构（侧栏逻辑）

典型侧栏包含：**Dashboard** → **TA Recruitment（岗位列表）** → **My Applications** → **Profile**；部分页面含 **Management** 等区域。具体以各页 HTML 为准。

---

## 本地预览

### 方式一：直接打开

双击 **`index.html`** 用浏览器打开（部分环境对 `file://` 有限制，若脚本异常请用方式二）。

### 方式二：本地静态服务器（推荐）

在 **`Software_Group_54-main/TA`** 目录下执行：

```powershell
cd TA
python -m http.server 5500
```

浏览器访问：**http://localhost:5500/index.html**

---

## 与后端、其他前端目录的关系

| 路径 | 说明 |
|------|------|
| **`../NLC_test/`** | 嵌入式 HTTP + REST API；静态资源在 `src/main/resources/static/`，内含与 SPA/页面联调的登录与仪表盘等 |
| **`../MO/`** | 课程负责人（MO）端静态原型 |
| **`../admin/`** | 管理员端静态原型 |

本 **`TA/`** 为**独立静态页**：若要与真实 API 联调，需统一 `fetch` 的 Base URL（如 `http://localhost:8080/api`）、处理 **Bearer Token** 与跨域策略，或将页面合并进 `NLC_test` 的 `static` 目录并由组内规范路由。

---

## 技术说明

| 项 | 说明 |
|----|------|
| 样式 | Tailwind CSS（CDN）；部分页面含内联样式 |
| 图标 | Iconify（CDN） |
| 依赖 | 预览 **需联网** 以加载 CDN |
| 浏览器 | 建议使用最新 Chrome / Edge / Firefox |

---

## 常见问题

| 现象 | 建议 |
|------|------|
| 页面样式丢失 | 检查网络是否能访问 CDN；或改用本地构建的 Tailwind（若项目组有配置） |
| 登录/提交无反应 | 当前多为静态原型，需接 `NLC_test` 或 Mock 数据 |
| 接口 401/403 | 确认已登录且请求头携带 `Authorization: Bearer <token>`（以后端实现为准） |

---

## 维护清单

- [ ] 新增/重命名 `.html` 时更新**侧栏链接**与本 README 表格  
- [ ] 与后端字段、路由变更时同步更新表单与列表页文案  
- [ ] 大作业提交前确认截图与录屏使用的入口页（建议从 `index.html` 或 `dashboard.html` 开始）

---

*Course project — BUPT International School TA Recruitment System (TA applicant UI prototype).*
