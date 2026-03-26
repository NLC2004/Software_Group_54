# MO — EduMO / TA Recruitment MO System

本目录为 **助教（TA）招聘管理端（MO）** 的静态页面原型，产品名为 **EduMO**，面向教师或院系管理员，用于发布与管理课程助教岗位、查看数据与通知。

## 功能与页面

| 文件 | 说明 |
|------|------|
| `my-job-list.html` | 岗位列表 / 工作台（Dashboard），汇总已发布岗位与快捷操作 |
| `job-create.html` | 发布新 TA 岗位（课程信息、要求等表单） |
| `job-edit.html` | 编辑已有岗位 |
| `job-detail.html` | 单个岗位详情 |
| `statistics-analysis.html` | 招聘数据统计与图表（使用 ECharts） |
| `notifications-messages.html` | 系统通知与消息 |
| `personal-center.html` | 个人中心 / 资料与设置 |

页面间通过侧栏导航与面包屑相互链接，可从 **`my-job-list.html`** 或 **`job-create.html`** 开始浏览。

## 技术栈

- **HTML5** + **Tailwind CSS**（CDN）
- **Iconify** 图标
- **Google Fonts**：Inter
- **ECharts**（仅统计页 `statistics-analysis.html`）

当前为 **纯前端展示**，无后端接口；表单与交互为布局与演示用途。

## 本地预览

在 `MO` 目录下用任意静态服务器打开即可，例如：

```bash
# Python 3
python -m http.server 8080
```

浏览器访问 `http://localhost:8080/my-job-list.html`。

也可直接用浏览器打开各 `.html` 文件（部分环境下相对路径与 CDN 行为一致）。

## 依赖说明

页面依赖公网 CDN（Tailwind、Iconify、字体、ECharts 等），**需要联网** 才能完整加载样式与脚本。
