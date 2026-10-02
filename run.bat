@echo off
chcp 65001 > NUL
title Quản Lý Sinh Viên UDP - App Launcher
echo =========================================================
echo ĐỀ TÀI 12: CHƯƠNG TRÌNH QUẢN LÝ SINH VIÊN CLIENT - SERVER UDP
echo =========================================================

REM Check if java is on PATH, else search JetBrains JDK
where java >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set JAVA_CMD=java
) else (
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\jbr\bin\java.exe" (
        set "JAVA_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\jbr\bin\java.exe"
    ) else (
        echo Không tìm thấy Java Runtime. Vui lòng kiểm tra lại môi trường Java!
        pause
        exit /b 1
    )
)

"%JAVA_CMD%" -jar "target\LapTrinhMang-KetThucMon-1.0-SNAPSHOT-jar-with-dependencies.jar"
