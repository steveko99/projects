Step 1:

This is the recommended tree ... dbRoot => C:\Encrypt1\SomeName

            md C:\Encrypt1\SomeName
            cd C:\Encrypt1\SomeName
            copy <x>\init.cmd .
            copy <x>\clean.cmd .
            copy <x>\e.cmd .
            copy <x>\d.cmd .

Step 2:
    Edit init.cmd
        set dbRoot=C:\Encrypt1\SomeName\DB
        set srcRoot=<_YOUR_FOLDER_>
        set destRoot=<_CAN_CHANGE_TO_DIFFERNT_DRIVE_>
        set restoreFile=<leave the default>

    Run clean.cmd, it will create an empty tree like so
        C:\Encrypt1\Name\Name\DB\config     (file)
        C:\Encrypt1\Name\Name\DB\DAT
        C:\Encrypt1\Name\Name\DB\KEY
        C:\Encrypt1\Name\Name\DB\META

Step 3:
    Run it once and setup a password hash
    It will report the hash of you password, cut and paste it
    Now edit DB\config and paste it in over beefbeefbeef...
    Now make a backup of the config file (has the password hash)

            copy DB\config .\config.backup

Step 4:
    run e.cmd to encrypt
    run d.cmd to decrypt

Step 5:
    run clean.cmd if you want to zap the entire DB, it will restore
    your password hash so long as you made config.backup

Note:
- Every time you run d.cmd you have the chance to empty out the Restore folder
  first or not.

- The restoreFile is a listing of all the files to restore.  You can save them
  to create a restore point and then restore that manifest into an empty
  restore folder.  Could be used to retrieve deleted or modified files.

- The DB folder can be located on a cloud sync folder.  But the parent scripts
  folder should not by synced.  Everything in DB is encrypted.

- To put DB on the clould ...

    md C:\Encrypt1\SomeName
    mklink /j c:\Encrypt1\SomeName\DBLink z:\dropbox\SomeName

  Then adjust dbRoot in init.cmd

- Recommend a hint.txt when you set the password

