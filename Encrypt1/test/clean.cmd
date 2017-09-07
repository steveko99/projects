@echo off
setlocal
set dbRoot=C:\Encrypt1_DB

rd /s %dbRoot%

md %dbRoot%
md %dbRoot%\DAT
md %dbRoot%\KEY
md %dbRoot%\META
md %dbRoot%\Restore

echo ROOT,C:\users\user0\Documents> c:\Encrypt1_DB\config
