# iExtractor
A JavaFX application to extract heaps of data from encrypted and non-encrypted backups created by iTunes. May also be used to extract known password hashes (including the backup password itself) into a block for hashcat. Supports RFC 3394 AES keywraps used by Apple.

Requires Java to be installed. Built on jdk24.

It works in two ways. IF you can get it to start (I'm partially joking here), clicking on the drop down menus to select backup and restore locations, setting a report option, and then selecting the perform extraction option will start (see the help document).

The other method is just clicking a button will begin the workflow, but without a report.

If the backup is encrypted, a password prompt will pop up for the password entry.

Not really ready for prime time yet, but I have some people doing alpha testing and I'm certainly open to feedback as I literally just started learning Java and Java FX stuff and there are certainly things I don't know. For example, how come my "fat jar" doesn't "just work" on Windows, it needs some DLLs, what?! Why is Linux mad about the location of libglass.so?! Is that error self caused, a Maven feature, little of column A little of column B? I just wanted my data back!

For real though, once I get some feedback and figure out how Java dev's do their builds, I'll put out some kind of release for people to try out. Until then, hey, here's some java files and my not good at all look and feel stuff. :)
