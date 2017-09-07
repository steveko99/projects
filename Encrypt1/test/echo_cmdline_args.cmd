@echo off
setlocal
set bin=..\dist
set dbRoot=C:\Encrypt1_DB
set metaFile=C:\Encrypt1_DB\META\TreeWalker.out
set dest=C:\Encrypt1_DB\Restore

echo ARGS:-db:%dbRoot% -restore:%metaFile% -dest:%dest%
