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

/*
 * The Media class is used to create objects from media files
 * information. Things like metadata, filename on disk, and
 * more.
 */

/* TODO:
 * o Make pretty. The constructor probably does not need to be so large.
 * o Remove any unused getters.
 * o Ask the user about continuing to work in the face of a copy error.
 * o There is a great deal of additional metadata in another database
 *   file cleverly disguised as a normal file. It should eventually
 *   be defined/used somewhere, maybe a metadata class? Here is the
 *   data:
 *     MetaDataDB = BackupDIR + "12"
 *     + "12b144c0bd44f2b3dffd9186d3f9c05b917cee25";
 */

/* The metadata for files is stored in an odd place, in this location.
 * While I don't like hard coded stuff, this may be subject to change and
 * doing a file type search every time would be IO intensive. Problem is,
 * we need to pass the chosen backup directory here as well!
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class Media extends Phone {
	private String FileID, RelativePath, DirPrefix, md5sum;
	Path BackupDIR;
	private Path saveDIR, refFname, realFileName, fileParentLocation,
	    fromLocation, toLocation;
	private EncryptedFile mediaFile;

	/*
	 * Create media object, set a hash for the file from which
	 * this object is based.
	 */
	Media(String fid, String relpath, Path basedir, Path saveLocation,
			EncryptedFile mediaFile) {
		this.FileID = fid;
		this.RelativePath = relpath;
		this.BackupDIR = basedir;
		this.saveDIR = saveLocation;
		this.mediaFile = mediaFile;
		/*
		 * Set important variables for each media object.
		 */
		this.DirPrefix = fid.substring(0,2);
		refFname = Paths.get(RelativePath);
		this.realFileName = refFname.getFileName();
		this.fileParentLocation = refFname.getParent();
		fromLocation = BackupDIR.resolve(DirPrefix).resolve(FileID);
		toLocation = saveDIR.resolve(fileParentLocation).resolve(realFileName);

		try {
			this.md5sum = super.getFileHash("MD5",
				this.fromLocation.toString());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Some getters for data that is needed to properly parse some of
	 * the media information from the outside.
	 */
	public Path getFromLocation() {
		return fromLocation;
	}

	public Path getToLocation() {
		return toLocation;
	}

	public String getFileID() {
		return this.FileID;
	}

	public String getRelativePath() {
		return this.RelativePath;
	}

	public Path getSaveDir() {
		return this.saveDIR;
	}

	public Path getRefFname() {
		return this.refFname;
	}

	public Path getRealFileName() {
		return this.realFileName;
	}

	public Path getFileParentLocation() {
		return this.fileParentLocation;
	}

	public String getFileRealParent() {
		return fileParentLocation.getFileName().toString();
	}

	public String getFileMD5() {
		return this.md5sum;
	}

	public EncryptedFile getMediaFile() {
		return mediaFile;
	}

}