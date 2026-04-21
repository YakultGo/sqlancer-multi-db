#!/bin/bash
# SQLancer 构建脚本 - 支持自动版本号或手动指定版本号
# 用法:
#   ./scripts/build.sh              # 自动版本 (基于 Git 提交数)
#   ./scripts/build.sh 2.0.100      # 指定版本

VERSION=${1:-"2.0.$(git rev-list --count HEAD)"}

echo "Building SQLancer with version: $VERSION"
mvn clean package -Drevision=$VERSION -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful!"
    echo "Output: target/sqlancer-$VERSION.jar"
    ls -la target/sqlancer-$VERSION.jar
else
    echo "Build failed!"
    exit 1
fi