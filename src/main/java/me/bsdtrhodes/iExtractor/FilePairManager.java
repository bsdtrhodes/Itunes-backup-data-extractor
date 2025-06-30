/*-
 * Copyright (c) 2025 Tom Rhodes. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package me.bsdtrhodes.iExtractor;

import java.nio.file.Path;

/*
 * Each backup contains a file that stores information about the various
 * backup objects. When the backup is not encrypted, it is a single
 * file. When it's encrypted, we have two files. This class will help
 * the manager deal with these files.
 */
public enum FilePairManager {
    CONTACTS("31", "31bb7ba8914766d4ba40d6dfb6113c8b614be442",
    		"contacts.db"),
    MESSAGES("3d", "3d0d7e5fb2ce288813306e4d4636395e047a3d28",
    		"messages.db"),
    VOICEMAILS("99", "992df473bbb9e132f4b3b6e4d33f72171e97bc7a",
    		"voicemails.db"),
    CALLS("5a", "5a4935c78a5255723f707230a451d79c540d2741",
    		"callhistory.db"),
    SAFARI("1a", "1a0e7afc19d307da602ccdcece51af33afe92c53",
    		"safarihistory.db"),
    MANIFEST(null, "Manifest.db", "Manifest.db"),
    CONTACTSCSV(null, "Contacts.csv", "Contacts.csv"),
    CONTACTSHTML(null, "Contacts.html", "Contacts.html"),
    MESSAGESCSV(null, "Messages.csv", "Messages.csv"),
    MESSAGESHTML(null, "Messages.html", "Messages.html"),
    CALLSCSV(null, "CallsHistory.csv", "CallsHistory.csv"),
    CALLSHTML(null, "CallHistory.html", "CallHistory.html"),
    VOICEMAILSCSV(null, "Voicemails.csv", "Voicemails.csv"),
    VOICEMAILSHTML(null, "Voicemails.html", "Voicemails.html"),
    SAFARICSV(null, "Safarihistory.csv", "Safarihistory.csv"),
    SAFARIHTML(null, "Safarihistory.html", "Safarihistory.html");

	/*
	 * This works as:
	 * subDir - This is where our file is found.
	 * encDBFile - The encrypted file, hash name is stored here.
	 * decDBFile - Friendly, decrypted filename on disk.
	 */
    private final String subDir;
    private final String encDBFile;
    private final String decDBFile;

    /* 
     * When called, specify the enum string to get the file you want based
     * on name argument. For example:
     * FilePairManager.CONTACTS.getEncryptedPath(backupLocation)
     * will translate to the encrypted Contacts file. And giving it
     * the restore location will provide "contacts.db" which makes
     * this interface very simple and extendible. Want to add the
     * FacebookMessenger encrypted file, add it above with the
     * same format and use it everywhere.
     */
    FilePairManager(String subDir, String encDBFile, String decDBFile) {
        this.subDir = subDir;
        this.encDBFile = encDBFile;
        this.decDBFile = decDBFile;
    }

    public Path getEncryptedPath(Path backupLocation) {
        if (subDir == null) {
            return backupLocation.resolve(encDBFile);
        } else {
            return backupLocation.resolve(subDir).resolve(encDBFile);
        }
    }

    public Path getDecryptedPath(Path restoreLocation) {
        return restoreLocation.resolve(decDBFile);
    }

    public String getEncryptedDB() {
        return encDBFile;
    }

    public String getDecryptedDB() {
        return decDBFile;
    }
}