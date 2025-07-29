@echo off
echo Starting git upload process...

REM 初始化git仓库（如果需要）
if not exist .git (
    echo Initializing git repository...
    git init
)

REM 添加远程仓库（如果需要）
git remote -v | findstr "origin" >nul
if errorlevel 1 (
    echo Adding remote origin...
    git remote add origin git@github.com:fzhiy/tobacco_app.git
)

REM 创建并切换到fy分支
echo Creating/checking out fy branch...
git checkout -b fy 2>nul || git checkout fy

REM 添加所有文件
echo Adding files...
git add .

REM 提交更改
echo Committing changes...
git commit -m "Add tobacco_weight_windows project - Windows version of tobacco weighing system"

REM 推送到远程
echo Pushing to remote fy branch...
git push -u origin fy

echo Git upload completed!
pause 