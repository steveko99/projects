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
@echo -----------------------------------------------------------------------
@echo REMOVE %dbRoot%
@echo -----------------------------------------------------------------------
@echo.
if exist %dbRoot% rd /s %dbRoot%

if not exist %dbRoot% mklink /j %dbRoot% D:\CloudSync\DropBox\silverhp_user0_documents\DB
if not exist %dbRoot%\DAT md %dbRoot%\DAT
if not exist %dbRoot%\KEY md %dbRoot%\KEY
if not exist %dbRoot%\META md %dbRoot%\META

if exist config.backup copy config.backup db\config >nul

@echo.
@echo Done - set a password and make config.backup
