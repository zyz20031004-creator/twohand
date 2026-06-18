# GitHub 操作手册

本文档适合 Windows 用户，从安装 Git 开始，介绍如何把本地项目上传到 GitHub，以及后续如何更新 GitHub 上的项目。

## 目录

- [一、准备工作](#一准备工作)
- [二、安装 Git](#二安装-git)
- [三、首次配置 Git](#三首次配置-git)
- [四、创建 GitHub 仓库](#四创建-github-仓库)
- [五、把本地项目第一次上传到 GitHub](#五把本地项目第一次上传到-github)
- [六、以后如何更新 GitHub 上的项目](#六以后如何更新-github-上的项目)
- [七、从 GitHub 拉取最新代码](#七从-github-拉取最新代码)
- [八、克隆 GitHub 项目到本地](#八克隆-github-项目到本地)
- [九、查看 Git 状态和提交记录](#九查看-git-状态和提交记录)
- [十、常用分支操作](#十常用分支操作)
- [十一、代理设置](#十一代理设置)
- [十二、常见问题](#十二常见问题)
- [十三、本项目常用命令](#十三本项目常用命令)

## 一、准备工作

你需要准备：

- 一个 GitHub 账号
- 一台已经安装 Git 的电脑
- 一个要上传的本地项目文件夹
- 可以访问 GitHub 的网络环境

本项目路径示例：

```powershell
E:\dm\twohand
```

GitHub 仓库地址示例：

```text
https://github.com/zyz20031004-creator/twohand.git
```

## 二、安装 Git

### 1. 下载 Git

打开 Git 官网：

```text
https://git-scm.com/download/win
```

下载 Windows 版本安装包。

### 2. 安装 Git

安装时大部分选项保持默认即可。比较重要的选项：

- 默认编辑器：可以选择 Vim、VS Code 或其他编辑器。
- PATH 环境变量：建议选择 `Git from the command line and also from 3rd-party software`。
- 换行符处理：Windows 用户通常保持默认即可。

### 3. 验证是否安装成功

打开 PowerShell，输入：

```powershell
git --version
```

如果看到类似下面的输出，说明安装成功：

```text
git version 2.xx.x.windows.x
```

## 三、首次配置 Git

Git 需要知道提交代码的人是谁。

### 1. 配置用户名

```powershell
git config --global user.name "你的 GitHub 用户名"
```

例如：

```powershell
git config --global user.name "zyz20031004-creator"
```

### 2. 配置邮箱

推荐使用 GitHub 提供的 noreply 邮箱，保护个人邮箱隐私：

```powershell
git config --global user.email "你的用户名@users.noreply.github.com"
```

例如：

```powershell
git config --global user.email "zyz20031004-creator@users.noreply.github.com"
```

### 3. 查看配置

```powershell
git config --global --list
```

如果能看到 `user.name` 和 `user.email`，说明配置成功。

## 四、创建 GitHub 仓库

### 1. 打开 GitHub

访问：

```text
https://github.com
```

登录你的账号。

### 2. 新建仓库

点击右上角 `+`，选择 `New repository`。

常用配置：

| 配置项 | 建议 |
| --- | --- |
| Repository name | 项目名称，例如 `twohand` |
| Description | 项目简介，可选 |
| Public / Private | 简历项目建议选 `Public` |
| Add a README file | 如果本地已有项目，建议不要勾选 |
| Add .gitignore | 如果本地已有 `.gitignore`，建议不要勾选 |
| Choose a license | 按需选择 |

创建完成后，GitHub 会给你一个仓库地址，例如：

```text
https://github.com/zyz20031004-creator/twohand.git
```

## 五、把本地项目第一次上传到 GitHub

假设你的项目在：

```powershell
E:\dm\twohand
```

### 1. 进入项目目录

```powershell
cd E:\dm\twohand
```

### 2. 初始化 Git 仓库

如果项目还不是 Git 仓库，执行：

```powershell
git init -b main
```

如果提示已经是 Git 仓库，可以跳过这一步。

### 3. 检查文件状态

```powershell
git status
```

常见状态说明：

| 状态 | 含义 |
| --- | --- |
| Untracked files | 新文件，还没有加入 Git |
| modified | 文件已修改 |
| staged | 文件已加入暂存区，准备提交 |
| clean | 工作区干净，没有待提交内容 |

### 4. 添加忽略规则

项目根目录建议有 `.gitignore` 文件，用来避免上传依赖、构建产物、日志、临时文件和敏感配置。

Java + Vue 项目常见忽略内容：

```gitignore
target/
node_modules/
dist/
*.log
.idea/
.vscode/
.env
.env.*
upload/
```

说明：

- `node_modules/` 是前端依赖，不能上传。
- `target/` 是后端 Maven 构建产物，不能上传。
- `dist/` 是前端打包产物，一般不上传源码仓库。
- `.env` 可能包含密码、Token 等敏感信息。
- `upload/` 通常是运行时上传文件，不建议上传。

### 5. 添加文件到暂存区

```powershell
git add .
```

如果只想添加某个文件：

```powershell
git add README.md
```

### 6. 提交代码

```powershell
git commit -m "Initial commit"
```

提交信息要简短清楚，例如：

```powershell
git commit -m "Add login page"
git commit -m "Fix order status update"
git commit -m "Update README"
```

### 7. 绑定 GitHub 远程仓库

```powershell
git remote add origin https://github.com/你的用户名/你的仓库名.git
```

例如：

```powershell
git remote add origin https://github.com/zyz20031004-creator/twohand.git
```

如果已经绑定过远程仓库，可以查看：

```powershell
git remote -v
```

如果远程地址写错了，可以修改：

```powershell
git remote set-url origin https://github.com/你的用户名/你的仓库名.git
```

### 8. 第一次推送到 GitHub

```powershell
git push -u origin main
```

第一次推送需要登录 GitHub。Git 可能会弹出 GitHub 登录窗口，按提示登录即可。

成功后，刷新 GitHub 仓库页面，就能看到项目代码。

## 六、以后如何更新 GitHub 上的项目

以后每次修改完代码，只需要重复下面三步：

### 1. 查看修改

```powershell
git status
```

### 2. 添加修改

添加全部修改：

```powershell
git add .
```

只添加指定文件：

```powershell
git add README.md
```

### 3. 提交修改

```powershell
git commit -m "本次修改说明"
```

例如：

```powershell
git commit -m "Update project screenshots"
```

### 4. 推送到 GitHub

```powershell
git push
```

完整流程示例：

```powershell
cd E:\dm\twohand
git status
git add .
git commit -m "Update README"
git push
```

## 七、从 GitHub 拉取最新代码

如果你在另一台电脑上修改了代码，或者 GitHub 上有新内容，本地需要先拉取：

```powershell
git pull
```

建议每次开始写代码前先执行：

```powershell
git pull
```

这样可以减少冲突。

## 八、克隆 GitHub 项目到本地

如果你换电脑，或者想重新下载项目：

```powershell
git clone https://github.com/你的用户名/你的仓库名.git
```

例如：

```powershell
git clone https://github.com/zyz20031004-creator/twohand.git
```

克隆完成后进入项目：

```powershell
cd twohand
```

## 九、查看 Git 状态和提交记录

### 查看当前状态

```powershell
git status
```

### 查看简短状态

```powershell
git status --short --branch
```

### 查看提交记录

```powershell
git log --oneline
```

### 查看最近 5 次提交

```powershell
git log --oneline -5
```

### 查看某个文件的修改内容

```powershell
git diff README.md
```

### 查看已暂存的修改

```powershell
git diff --cached
```

## 十、常用分支操作

### 查看分支

```powershell
git branch
```

### 新建并切换分支

```powershell
git switch -c feature/login
```

### 切回 main 分支

```powershell
git switch main
```

### 合并分支

先切回 main：

```powershell
git switch main
```

再合并功能分支：

```powershell
git merge feature/login
```

### 删除本地分支

```powershell
git branch -d feature/login
```

个人毕业设计项目通常直接在 `main` 分支提交即可。如果多人协作，建议使用分支开发。

## 十一、代理设置

如果你的电脑访问 GitHub 很慢，或者出现连接失败，可以给 Git 设置代理。

### 1. 查看当前代理

```powershell
git config --global --get http.proxy
git config --global --get https.proxy
```

### 2. 设置全局代理

如果你使用 Clash Verge，常见 HTTP 代理端口可能是 `7890` 或 `7897`。

你的电脑之前显示的代理地址是：

```text
127.0.0.1:7897
```

可以设置：

```powershell
git config --global http.proxy http://127.0.0.1:7897
git config --global https.proxy http://127.0.0.1:7897
```

### 3. 只给当前项目设置代理

进入项目目录后执行：

```powershell
git config http.proxy http://127.0.0.1:7897
git config https.proxy http://127.0.0.1:7897
```

这样只影响当前项目，不影响其他项目。

### 4. 取消代理

取消全局代理：

```powershell
git config --global --unset http.proxy
git config --global --unset https.proxy
```

取消当前项目代理：

```powershell
git config --unset http.proxy
git config --unset https.proxy
```

### 5. 测试 GitHub 连接

```powershell
Test-NetConnection github.com -Port 443
```

如果看到：

```text
TcpTestSucceeded : True
```

说明网络可以连接 GitHub。

如果看到：

```text
TcpTestSucceeded : False
```

说明 GitHub 端口连接失败，需要检查网络、代理或 VPN。

## 十二、常见问题

### 1. fatal: not a git repository

错误示例：

```text
fatal: not a git repository (or any of the parent directories): .git
```

原因：当前目录不是 Git 仓库。

解决：

```powershell
cd 你的项目目录
git init -b main
```

### 2. remote origin already exists

错误示例：

```text
error: remote origin already exists.
```

原因：已经设置过远程仓库。

解决：

```powershell
git remote -v
git remote set-url origin https://github.com/你的用户名/你的仓库名.git
```

### 3. Failed to connect to github.com port 443

错误示例：

```text
Failed to connect to github.com port 443
```

原因：网络无法连接 GitHub。

解决：

- 检查浏览器是否能打开 GitHub。
- 打开 Clash、VPN 或代理软件。
- 设置 Git 代理。
- 换网络，例如手机热点。

Clash Verge 代理示例：

```powershell
git config --global http.proxy http://127.0.0.1:7897
git config --global https.proxy http://127.0.0.1:7897
```

### 4. Authentication failed

原因：GitHub 登录失败或凭据失效。

解决：

- 重新登录 GitHub 弹窗。
- 使用 GitHub Token。
- 清理 Windows 凭据管理器里的旧 GitHub 凭据后重新登录。

Windows 凭据管理器路径：

```text
控制面板 -> 用户账户 -> 凭据管理器 -> Windows 凭据
```

找到 GitHub 相关凭据后删除，再重新执行：

```powershell
git push
```

### 5. rejected because the remote contains work

错误示例：

```text
Updates were rejected because the remote contains work that you do not have locally.
```

原因：GitHub 上有本地没有的提交。

解决：

```powershell
git pull --rebase
git push
```

### 6. nothing to commit, working tree clean

提示示例：

```text
nothing to commit, working tree clean
```

说明：当前没有新的修改需要提交。

如果你已经提交过，但还没推送，可以执行：

```powershell
git push
```

### 7. accidentally added node_modules

如果误把 `node_modules` 加入暂存区，先取消暂存：

```powershell
git restore --staged node_modules
```

然后确认 `.gitignore` 包含：

```gitignore
node_modules/
```

## 十三、本项目常用命令

本项目路径：

```powershell
E:\dm\twohand
```

GitHub 地址：

```text
https://github.com/zyz20031004-creator/twohand
```

### 进入项目

```powershell
cd E:\dm\twohand
```

### 查看状态

```powershell
git status --short --branch
```

### 更新 GitHub 项目

```powershell
git add .
git commit -m "Update project"
git push
```

### 只提交 README

```powershell
git add README.md
git commit -m "Update README"
git push
```

### 只提交文档和图片

```powershell
git add README.md docs/images
git commit -m "Update documentation"
git push
```

### 不建议提交的内容

提交前请确认不要上传以下内容：

- 数据库密码
- 账号 Token
- `.env` 文件
- `node_modules/`
- `target/`
- `dist/`
- `upload/`
- 日志文件

### 推荐提交信息

| 场景 | 提交信息示例 |
| --- | --- |
| 更新 README | `Update README` |
| 添加截图 | `Add project screenshots` |
| 修复 Bug | `Fix order status bug` |
| 新增功能 | `Add product favorite feature` |
| 调整样式 | `Improve user center layout` |

## 十四、推荐工作习惯

每次修改项目时，建议按照下面顺序操作：

1. 进入项目目录。
2. 执行 `git pull` 获取 GitHub 最新代码。
3. 修改代码。
4. 执行 `git status` 查看变更。
5. 执行 `git add` 添加要提交的文件。
6. 执行 `git commit -m "说明"` 创建提交。
7. 执行 `git push` 上传到 GitHub。

完整示例：

```powershell
cd E:\dm\twohand
git pull
git status
git add .
git commit -m "Update project"
git push
```

养成这个流程后，GitHub 更新就会很稳定。
