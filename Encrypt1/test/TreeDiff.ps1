# Set-ExecutionPolicy RemoteSigned
$fso = Get-ChildItem -Recurse -path C:\Encrypt1_DB\Restore
$fsoBU = Get-ChildItem -Recurse -path C:\users\user0\Documents
Compare-Object -ReferenceObject $fso -DifferenceObject $fsoBU
Set-ExecutionPolicy Restricted
