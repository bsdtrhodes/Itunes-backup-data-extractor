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
 *
 * MIT LicenseCopyright (c) 2020 Maximilian Herczegh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.bsdtrhodes.iExtractor;

/*
 * This file is primarily set up to pull an encrypted file's
 * information and perform the extraction of encrypted data.
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;

import org.xml.sax.SAXException;

import com.dd.plist.NSArray;
import com.dd.plist.NSNumber;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;
import com.dd.plist.UID;

/*
 * Each file has/is an object. Handle accordingly.
 */
public class EncryptedFile {

    public NSDictionary data;
    private NSDictionary properties;
    private NSArray objects;
    private Path contentFile;

    private int protectionClass;
    private byte[] encryptionKey;
    Phone iPhone;

    public EncryptedFile(Path fileID, Phone thePhone, NSDictionary dataBlob) {
    	this.data = dataBlob;
    	this.iPhone = thePhone;
    	this.objects = (NSArray)
			this.data.objectForKey("$objects");
		this.properties = (NSDictionary)
			getObject((UID) ((NSDictionary)
			data.objectForKey("$top")).objectForKey("root"));
		this.protectionClass = ((NSNumber)
		this.properties.objectForKey("ProtectionClass")).intValue();
		this.contentFile = fileID;
		this.encryptionKey = new byte[40];
		ByteBuffer encryptionKeyBuffer =
			ByteBuffer.wrap(this.encryptionKey);
		((NSData) ((NSDictionary) getObject((UID)
			properties.objectForKey("EncryptionKey"))).objectForKey
			("NS.data")).getBytes(encryptionKeyBuffer, 4, 40);
    }

		/* An encrypted file is potentially every file and dir in the backup. */
    public EncryptedFile(Path fileID, Path theDatabase, Phone iPhone)
	    	throws Exception {
	    	this.iPhone = iPhone;
	
	    	/* Try to connect to the DB and get the binary "blob" for the file. */
			try (DatabaseFileManager dbMGR = new
					DatabaseFileManager(theDatabase);
				Connection conn = dbMGR.getConnection();
				PreparedStatement statement = conn
						.prepareStatement(DatabaseQueriesManager
						.getEncFileQuery())) {

				statement.setString(1, "%" + fileID.getFileName()
						.toString() + "%");
				try (ResultSet	result = statement.executeQuery()) {
	
				this.data = (NSDictionary)
					PropertyListParser.parse(result.getBinaryStream(4));
				this.objects = (NSArray)
					this.data.objectForKey("$objects");
				this.properties = (NSDictionary)
					getObject((UID) ((NSDictionary)
					data.objectForKey("$top")).objectForKey("root"));
				this.protectionClass = ((NSNumber)
					this.properties.objectForKey("ProtectionClass")).intValue();
				this.contentFile = fileID;
	
				this.encryptionKey = new byte[40];
				ByteBuffer encryptionKeyBuffer =
					ByteBuffer.wrap(this.encryptionKey);
				((NSData) ((NSDictionary) getObject((UID)
					properties.objectForKey("EncryptionKey"))).objectForKey
					("NS.data")).getBytes(encryptionKeyBuffer, 4, 40);

			} catch (IOException | SQLException | SAXException | ParseException
					e) {
				throw new ExceptionManager("Failure trying to access file"
					+ " encryption data from the database.", e, true);
			}
		}
	}

	    private NSObject getObject(UID uid) {
	    	return this.objects.getArray()[uid.getBytes()[0]];
	    }

    public void extract(Path destination) throws ExceptionManager {
    	try {
            this.iPhone.getKeyBag().get().decryptFile(this.protectionClass,
            	this.encryptionKey, this.contentFile, destination);
        } catch (InvalidKeyException | IOException e) {
            throw new ExceptionManager("Failure extracting the file and file"
            	+ " key for: " + contentFile + " :", e, true);
        }
    }
}