# SQLancer 构建脚本 - 支持自动版本号或手动指定版本号
# 用法:
#   .\scripts\build.ps1                    # 自动版本 (基于 Git 提交数)
#   .\scripts\build.ps1 -Version 2.0.100   # 指定版本

param(
    [string]$Version
)

if (-not $Version) {
    $commitCount = git rev-list --count HEAD
    $Version = "2.0.$commitCount"
}

Write-Host "Building SQLancer with version: $Version" -ForegroundColor Green
mvn clean package -Drevision=$Version -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host "Output: target/sqlancer-$Version.jar"
    Get-ChildItem "target\sqlancer-$Version.jar"
} else {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}