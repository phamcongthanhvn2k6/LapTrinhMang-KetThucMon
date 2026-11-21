@echo off
chcp 65001 > NUL
title Quản Lý Sinh Viên PTIT - Đóng Gói và Khởi Chạy Lại

echo ====================================================================
echo   HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG - ĐÓNG GÓI BẢN MỚI NHẤT
echo ====================================================================
echo.

REM 1. Dọn dẹp tiến trình Java cũ chiếm cổng 9876 nếu có
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :9876') do (
    echo [TIẾN TRÌNH] Đang giải phóng cổng mạng 9876 từ PID %%a...
    taskkill /F /PID %%a >nul 2>&1
)

REM 2. Tìm công cụ Maven
set MVN_CMD=mvn
if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" (
    set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd"
)

REM 3. Tìm Java Runtime
where java >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set JAVA_CMD=java
) else if exist "C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe"
) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\jbr\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\jbr\bin\java.exe"
) else (
    echo [LỖI] Không tìm thấy Java Runtime!
    pause
    exit /b 1
)

echo [1/2] Đang biên dịch và đóng gói JAR mới nhất...
call "%MVN_CMD%" clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [LỖI] Quá trình biên dịch thất bại! Vui lòng kiểm tra mã nguồn.
    pause
    exit /b 1
)

echo.
echo [2/2] [THÀNH CÔNG] Đang khởi chạy Hệ thống Quản Lý Sinh Viên PTIT...
"%JAVA_CMD%" -jar "target\LapTrinhMang-KetThucMon-1.0-SNAPSHOT-jar-with-dependencies.jar"
