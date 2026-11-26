@echo off
chcp 65001 > NUL
title Quản Lý Sinh Viên PTIT - App Launcher

REM Giải phóng cổng 9876 nếu có tiến trình treo cũ
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :9876') do (
    taskkill /F /PID %%a >nul 2>&1
)

REM Check if java is on PATH, else search known JDK locations
where java >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set JAVA_CMD=java
) else if exist "C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe"
) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\jbr\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\jbr\bin\java.exe"
) else (
    echo [LỖI] Không tìm thấy Java Runtime. Vui lòng kiểm tra lại môi trường Java!
    pause
    exit /b 1
)

REM Check if Jar file exists, else build it with maven
if not exist "target\LapTrinhMang-KetThucMon-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    echo [THÔNG BÁO] Chưa tìm thấy file JAR đóng gói, đang tiến hành biên dịch dự án...
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" (
        call "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" package -DskipTests
    ) else (
        call mvn package -DskipTests
    )
)

echo [THÀNH CÔNG] Đang khởi chạy hệ thống Quản lý sinh viên PTIT...
"%JAVA_CMD%" -cp "target\classes;target\LapTrinhMang-KetThucMon-1.0-SNAPSHOT-jar-with-dependencies.jar" com.qlsv.AppLauncher
