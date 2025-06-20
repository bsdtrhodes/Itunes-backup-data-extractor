# iExtractor
A JavaFX application to extract heaps of data from encrypted and non-encrypted backups created by iTunes. May also be used to extract known password hashes (including the backup password itself) into a block for hashcat. Supports RFC 3394 AES keywraps used by Apple.

Requires Java to be installed. Built on jdk24 so that is the minimal requirement. The required libraries for Linux (tested on Kali) and Windows (tested on Windows 10) are included (see LICENSE files and the ThirdPartyLicenses file here).

It works in two ways. IF you can get it to start (I'm partially joking here), clicking on the drop down menus to select backup and restore locations, setting a report option, and then selecting the perform extraction option will start (see the help document).

The other method is just clicking a button will begin the workflow, but without a report.

If the backup is encrypted, a password prompt will pop up for the password entry.

Not really ready for prime time yet, but I have some people doing alpha testing and I'm certainly open to feedback as I literally just started learning Java and Java FX stuff and there are certainly things I don't know. For example, how come my "fat jar" doesn't "just work" on Windows, it needs some DLLs, what?! Why is Linux mad about the location of libglass.so?! Is that error self caused, a Maven feature, little of column A little of column B? I just wanted my data back!

For real though, once I get some feedback and figure out how Java dev's do their builds, I'll put out some kind of release for people to try out. Until then, hey, here's some java files and my not good at all look and feel stuff. :)

So here is the layout, it's very simple and clicking any of these options will bring up the dialogs to select a backup directory and a restore directory.
![image](https://github.com/user-attachments/assets/d3ce723c-6976-4473-8c0f-507680a8d566)

Alternatively, this may be done manually by selecting the drop down options and setting each location:
![image](https://github.com/user-attachments/assets/8e8b8d45-c58d-451b-9b79-5cfd9c084af5)

Finally, to dump hashes found in the iTunes backup, use the following option (Note: These will be in hashcat format):
![image](https://github.com/user-attachments/assets/4eccd2ec-51a2-4202-a564-ad936e62dd25)

You will have output like this, but pertinent to your own files (censored, of course):
![image](https://github.com/user-attachments/assets/1200371d-fd0a-48b9-9e7c-0ad03f386cdd)

Some todo items I have including expanding the data for which we have access, cleanup to be more Java-styled, and maybe launch workers on individual threads, maybe include more charsets as I'm just defaulting to UTF-8. Big thanks to Maxi Herczegh from who my decryption code is borrowed/based. And the countless Java docs I read because, well, I don't always rtfm. Now that this is committed, other people can hate for me.

This application includes JavaFX libraries under the terms of the GPL v2 with Classpath Exception. See the legal/ directory and ThirdPartyLicenses.txt file for details.
