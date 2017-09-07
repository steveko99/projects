@echo off
cd /d %~dp0
call init.cmd

@echo.
set ans=n
set /p ans="Continue? (y/n) "
if %ans%==y goto :doit
goto :eof

:doit
@echo.
@echo ---------------------------------------------------------------------------
@echo   Warning: will delete %destRoot%
@echo ---------------------------------------------------------------------------
@echo.
rd /s %destRoot%
if not exist %destRoot% md %destRoot%
java -jar %bin%\Encrypt1.jar -db:%dbRoot% -restore:%restoreFile% -dest:%destRoot%
