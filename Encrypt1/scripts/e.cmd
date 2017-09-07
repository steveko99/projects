@echo off
cd /d %~dp0
call init.cmd

@echo.
set ans=n
set /p ans="Continue? (y/n) "
if %ans%==y goto :doit
goto :eof

:doit
java -jar %bin%\Encrypt1.jar -db:%dbRoot% -src:%srcRoot%
