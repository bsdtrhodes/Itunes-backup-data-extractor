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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.dd.plist.NSArray;
import com.dd.plist.NSData;
import com.dd.plist.NSDate;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;

/*
 * Process a "Phone" device in the backup. The phone object contains
 * the important information about the phone, including the last
 * backup date, serial, unique device ID, keybag, etc. and that
 * is needed to get information for both the report and, in the
 * case of an encrypted backup, the stored data.
 * 
 * The encryption code came from Maxi's backup explorer, which is
 * in itself based on a Stackoverflow post from a user (andrewdotn)
 * which had some example code in Python. Looking at both versions,
 * I took Maxi's encryption code along with some other methods and
 * classes which are still included here to not re-invent the wheel.
 * Now, originally I pulled in his manifest and info plist loading
 * code to make it easier to comprehend, and even committed that in
 * Github as a starting point when beginning the encryption parts of
 * this project. That loading code is gone but big thanks to Maxi
 * as a lot of my time was saved with some parts of that project.
 */

public class Phone {
	/* The following are from info.plist */
    public Map<String, ApplicationInfo> applications;
    public String[] installedApplications;
    public Date lastBackupDate;
    public String buildVersion;
    public String deviceName;
    public String displayName;
    public String productName;
    public String productType;
    public String productVersion;
    public String serialNumber;
    public String phoneNumber;
	public String guid;
    public String iccid;
    public String imei;
    public String imei2;
    public String meid;
    public String targetIdentifier;
    public String targetType;
    public String uniqueIdentifier;

    /* The following are from Manifest.plist. */
    protected Boolean isEncrypted;
    public NSDictionary lockedApplications;
    public String lockedVersion;
    public Date lockedDate;
    public NSData manifestKey;
    public boolean passcodeSet;
    public String lockedproductVersion;
    public String lockedproductType;
    public String lockedbuildVersion;
    public String lockedserialNumber;
    public String lockeddeviceName;
    public String uniqueDeviceID;
    private KeyBag keyBag;


    /* Generic constructor for method inheritance. */
    public Phone() {
    }

    /* This REAL constructor is where all the fun happens. */
	public Phone(NSDictionary infoPList, NSDictionary manifestPList) throws
		ExceptionManager {

		/* These are for the Info.plist file. */
		this.applications = new HashMap<>();
        for (Map.Entry<String, NSObject> entry : ((NSDictionary)
        		infoPList.objectForKey("Applications")).entrySet()) {
            NSDictionary info = (NSDictionary) entry.getValue();
            boolean isDemotedApp = info.containsKey("IsDemotedApp") &&
            	((NSNumber) info.objectForKey("IsDemotedApp")).boolValue();
            ApplicationInfo app = new ApplicationInfo((NSData)
            	info.objectForKey("PlaceholderIcon"),
            	(NSData) info.objectForKey("iTunesMetadata"),
            	isDemotedApp, (NSData) info.objectForKey("ApplicationSINF"));
            this.applications.put(entry.getKey(), app);
        }
        this.installedApplications = Arrays.stream(((NSArray)
        	infoPList.objectForKey("Installed Applications")).getArray()).map(
        		NSObject::toString).toArray(String[]::new);
        this.lastBackupDate = ((NSDate)
        	infoPList.objectForKey("Last Backup Date")).getDate();
        this.buildVersion =
        	infoPList.objectForKey("Build Version").toString();
        this.deviceName = infoPList.objectForKey("Device Name").toString();
        this.displayName = infoPList.objectForKey("Display Name").toString();
        this.productName = infoPList.objectForKey("Product Name").toString();
        this.productType = infoPList.objectForKey("Product Type").toString();
        this.productVersion =
        	infoPList.objectForKey("Product Version").toString();
        this.serialNumber =
        		infoPList.objectForKey("Serial Number").toString();
        this.phoneNumber = getStringOrNull(infoPList, "Phone Number");
        this.guid = getStringOrNull(infoPList, "GUID");
        this.iccid = getStringOrNull(infoPList, "ICCID");
        this.imei = getStringOrNull(infoPList, "IMEI");
        this.imei2 = getStringOrNull(infoPList, "IMEI2");
        this.meid = getStringOrNull(infoPList, "MEID");
        this.targetIdentifier = getStringOrNull(infoPList,
        	"Target Identifier");
        this.targetType = getStringOrNull(infoPList, "Target Type");
        this.uniqueIdentifier =
        	getStringOrNull(infoPList, "Unique Identifier");

        /* These are for the manifest.plist file. */
        NSDictionary lockdown = (NSDictionary)
        		manifestPList.objectForKey("Lockdown");

            this.isEncrypted = ((NSNumber)
            	manifestPList.objectForKey("IsEncrypted")).boolValue();
            this.lockedVersion =
            	manifestPList.objectForKey("Version").toString();
            this.lockedDate = ((NSDate)
            	manifestPList.objectForKey("Date")).getDate();
            this.manifestKey = ((NSData)
            		manifestPList.objectForKey("ManifestKey"));
            this.passcodeSet = ((NSNumber)
            	manifestPList.objectForKey("WasPasscodeSet")).boolValue();
            this.lockedproductVersion =
            	lockdown.objectForKey("ProductVersion").toString();
            this.lockedproductType =
            	lockdown.objectForKey("ProductType").toString();
            this.lockedbuildVersion =
            	lockdown.objectForKey("BuildVersion").toString();
            this.uniqueDeviceID =
            	lockdown.objectForKey("UniqueDeviceID").toString();
            this.lockedserialNumber =
            	lockdown.objectForKey("SerialNumber").toString();
            this.lockeddeviceName =
            	lockdown.objectForKey("DeviceName").toString();
            this.lockedApplications =
            	(NSDictionary) manifestPList.objectForKey("Applications");

            /* Set to false. If it turns out otherwise, set to true. */
            if (this.isEncrypted) {
                this.keyBag = new KeyBag((NSData)
                	manifestPList.objectForKey("BackupKeyBag"));
            }
	}

    public Optional<KeyBag> getKeyBag() {
        return Optional.ofNullable(this.keyBag);
    }

    public Set<String> getPhoneHashes() {
    	return this.keyBag.getHashSet();
    }

	private static String getStringOrNull(NSDictionary dict, String key) {
        return dict.containsKey(key) ?
        	dict.objectForKey(key).toString() : null;
    }

    public static class ApplicationInfo {
        //public final NSData placeholderIcon;
        //public final NSData iTunesMetadata;
       // public final boolean isDemotedApp;
        //public final NSData applicationSINF;

        public ApplicationInfo(NSData placeholderIcon, NSData iTunesMetadata,
        	Boolean isDemotedApp, NSData applicationSINF) {
            //this.placeholderIcon = placeholderIcon;
            //this.iTunesMetadata = iTunesMetadata;
           // this.isDemotedApp = isDemotedApp;
            //this.applicationSINF = applicationSINF;
        }
    }

/*
******************************************************************************
* Getters for the various parts of the Phone object.                         *
******************************************************************************
*/
    public Map<String, ApplicationInfo> getApplications() {
		return applications;
	}

	public String[] getInstalledApplications() {
		return installedApplications;
	}

	public Date getLastBackupDate() {
		return lastBackupDate;
	}

	public String getBuildVersion() {
		return buildVersion;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getProductName() {
		return productName;
	}

	public String getProductType() {
		return productType;
	}

	public String getProductVersion() {
		return productVersion;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getGuid() {
		return guid;
	}

	public String getIccid() {
		return iccid;
	}

	public String getImei() {
		return imei;
	}

	public String getImei2() {
		return imei2;
	}

	public String getMeid() {
		return meid;
	}

	public String getTargetIdentifier() {
		return targetIdentifier;
	}

	public String getTargetType() {
		return targetType;
	}

	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	public Boolean IsEncrypted() {
		return isEncrypted;
	}

/*
******************************************************************************
* Verification and validation methods.                                       *
******************************************************************************
*/
	/* Method to get file hash into a string. */
	protected String getFileHash(String algorithm, File f) throws IOException,
		NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(algorithm);

		try (BufferedInputStream in = new BufferedInputStream((new
		    	FileInputStream(f)));
		        DigestOutputStream out =
		        	new DigestOutputStream(OutputStream.nullOutputStream(),
		        		md)) {
		        in.transferTo(out);
		    }

		    String fx = "%0" + (md.getDigestLength()*2) + "x";
		    return String.format(fx, new BigInteger(1, md.digest()));
	}

	/* Method to get hash from file as a string. */
	protected String getFileHash(String algorithm, String floc) throws
		IOException, NoSuchAlgorithmException {
		File f = new File(floc);
		MessageDigest md = MessageDigest.getInstance(algorithm);

		try (BufferedInputStream in = new BufferedInputStream((new
			FileInputStream(f)));
			DigestOutputStream out =
	        new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
			in.transferTo(out);
	    }

		String fx = "%0" + (md.getDigestLength()*2) + "x";
		return String.format(fx, new BigInteger(1, md.digest()));
	}
}
