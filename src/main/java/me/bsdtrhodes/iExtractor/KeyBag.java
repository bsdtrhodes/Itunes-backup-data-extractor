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
 * MIT License
 * 
 * Copyright (c) 2020 Maximilian Herczegh
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

import com.dd.plist.NSData;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KeyBag {
    private static final Set<String> CLASS_KEY_TAGS = Set.of("CLAS", "WRAP",
    	"WPKY", "KTYP", "PBKY");
    private static final int UUID_LENGTH = 16;
    private static final int WRAP_PASSCODE = 2;
    private Set<String> hashSet;
    public int type;
    public byte[] uuid;
    public byte[] wrap;
    public final Map<ByteBuffer, Map<String, byte[]>> classKeys =
    	new HashMap<>();
    public final Map<String, byte[]> attrs = new HashMap<>();

    private boolean unlocked = false;

    public KeyBag(NSData data) throws ExceptionManager {
        this.parseBinaryBlob(data);
    }

    private void parseBinaryBlob(NSData data) throws ExceptionManager {
        ByteBuffer buffer = ByteBuffer.wrap(data.bytes());
        Map<String, byte[]> currentClassKey = null;

        try {
            while (buffer.hasRemaining()) {
                byte[] tagBytes = new byte[4];
                buffer.get(tagBytes);
                String tag = new String(tagBytes, StandardCharsets.US_ASCII);

                int length = buffer.getInt();
                if (length < 0 || length > buffer.remaining()) {
                	WindowManager.critical(ContextManager.getPrimaryStage(),
                			"Fatal Error in decryption!", "Invalid tag length:"
                					+ length).show();
                }

                byte[] value = new byte[length];
                buffer.get(value);

                switch (tag) {
                    case "TYPE":
                        if (length != 4) {
                        	WindowManager.critical(ContextManager
                        			.getPrimaryStage(), "Fatal Error",
                        			"TYPE length is not four bytes!").show();
                        }
                        this.type = ByteBuffer.wrap(value).getInt();
                        if (this.type > 3) {
                        	WindowManager.critical(ContextManager
                        			.getPrimaryStage(), "Fatal Error",
                        			"TYPE length is incorrect").show();
                        }
                        break;

                    case "UUID":
                        if (this.uuid == null) {
                            if (length != UUID_LENGTH) {
                            	WindowManager.critical(ContextManager
                            			.getPrimaryStage(), "Fatal Error",
                            			"UUID is not 16 bytes").show();
                            }
                            this.uuid = value;
                        } else {
                            if (currentClassKey != null && currentClassKey
                            		.containsKey("CLAS")) {
                                ByteBuffer classKeyId = ByteBuffer
                                	.wrap(currentClassKey.get("CLAS"));
                                this.classKeys.put(classKeyId, currentClassKey);
                            }
                            currentClassKey = new HashMap<>();
                            currentClassKey.put("CLAS", value);
                        }
                        break;

                    case "WRAP":
                        if (this.wrap == null) {
                            this.wrap = value;
                        } else {
                            if (currentClassKey == null) {
                                currentClassKey = new HashMap<>();
                            }
                            currentClassKey.put(tag, value);
                        }
                        break;

                    default:
                        if (CLASS_KEY_TAGS.contains(tag)) {
                            if (currentClassKey == null) {
                                currentClassKey = new HashMap<>();
                            }
                            currentClassKey.put(tag, value);
                        } else {
                            this.attrs.put(tag, value);
                        }
                        break;
                }
            }

            /* Save last class key if it exists */
            if (currentClassKey != null && currentClassKey
            		.containsKey("CLAS")) {
                ByteBuffer classKeyId = ByteBuffer
                		.wrap(currentClassKey.get("CLAS"));
                this.classKeys.put(classKeyId, currentClassKey);
            }

        } catch (BufferUnderflowException e) {
            throw new ExceptionManager("Buffer underflow while parsing key "
            		+ "bag: ", e, true);
        }
    }

    public boolean isLocked() {
        return (! this.unlocked);
    }

    public void pullHashes() throws InvalidKeyException {
    	Set<String> hashSet = new HashSet<String>();

        /* Get values for hashcat array list. */
		String printDPSL = bytesToHex(this.attrs.get("DPSL"));
		String printSALT = bytesToHex(this.attrs.get("SALT"));
		int printDPIC = ByteBuffer.wrap(this.attrs.get("DPIC")).getInt();
		int printITER = ByteBuffer.wrap(this.attrs.get("ITER")).getInt();

		for (Map<String, byte[]> classKey : this.classKeys.values()) {
			String printWPKY = bytesToHex(classKey.get("WPKY"));

			String hashcatFormat =
					String.format("$itunes_backup$*10*%s*%d*%s*%d*%s\n",
					printWPKY, printITER, printSALT, printDPIC, printDPSL);
			hashSet.add(hashcatFormat);
		}
		this.hashSet = hashSet;
    }

    public void unlock(String passcode) throws InvalidKeyException,
    	ExceptionManager {
        try {
            byte[] salt1 = this.attrs.get("DPSL");
            int iterations1 = ByteBuffer.wrap(this.attrs.get("DPIC")).getInt();

            KeySpec spec1 = new PBEKeySpec(passcode.toCharArray(), salt1,
            	iterations1, 32 * 8);

            SecretKeyFactory f1 =
            	SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey key1 = f1.generateSecret(spec1);

            byte[] salt2 = this.attrs.get("SALT");
            int iterations2 = ByteBuffer.wrap(this.attrs.get("ITER")).getInt();

            PKCS5S2ParametersGenerator gen =
            	new PKCS5S2ParametersGenerator(new SHA1Digest());
            gen.init(key1.getEncoded(), salt2, iterations2);
            byte[] keyEncryptionKey = ((KeyParameter)
            	gen.generateDerivedParameters(32 * 8)).getKey();

            Cipher c = Cipher.getInstance("AESWrap");

            for (Map<String, byte[]> classKey : this.classKeys.values()) {
                if (!classKey.containsKey("WPKY")) continue;
                int wrap = ByteBuffer.wrap(classKey.get("WRAP")).getInt();
                if ((wrap & WRAP_PASSCODE) != 0) {
                    c.init(Cipher.UNWRAP_MODE, new
                    	SecretKeySpec(keyEncryptionKey, "AES"));
                    Key contentEncryptionKey = c.unwrap(classKey.get("WPKY"),
                    	"AES", Cipher.SECRET_KEY);

                    if (contentEncryptionKey != null) {
                        classKey.put("KEY", contentEncryptionKey.getEncoded());
                    }
                }
            }
            this.unlocked = true;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException |
        	NoSuchPaddingException e) {
        	throw new ExceptionManager("Issue decrypting backup. System "
        			+ "said: ", e, true);
        }
    }

    public byte[] unwrapKeyForClass(byte[] protectionClass, byte[]
    	persistentKey) throws ExceptionManager, InvalidKeyException {
        if (this.isLocked()) {
        	WindowManager.critical(ContextManager.getPrimaryStage(),
        			"Fatal Error", "Unable to decrypt the phone").show();
        }

        Map<String, byte[]> classKeyMap =
        	this.classKeys.get(ByteBuffer.wrap(protectionClass));
        if (classKeyMap == null) {
        	WindowManager.critical(ContextManager.getPrimaryStage(),
        			"Fatal Error", "Required protection class was not found")
        			.show();
        }

        byte[] classKey = classKeyMap.get("KEY");
        if (classKey == null) {
        	WindowManager.critical(ContextManager.getPrimaryStage(),
        			"Fatal Error", "No class key was found for the"
        			+ " protection class.").show();
        }

        if (persistentKey.length != 0x28) {
        	WindowManager.critical(ContextManager.getPrimaryStage(),
        			"Falal Error", "Invalid class key length").show();
        }

        try {
        	Cipher c = Cipher.getInstance("AESWrap");
        	c.init(Cipher.UNWRAP_MODE, new SecretKeySpec(classKey, "AES"));
        	Key unwrappedKey = c.unwrap(persistentKey, "AES",
        			Cipher.SECRET_KEY);
        	return unwrappedKey.getEncoded();
        	
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new ExceptionManager("Unsupported encryption type found!",
            	e, true);
        }
    }

    public InputStream decryptStream(byte[] protectionClass,
    	byte[] persistentKey, InputStream source) throws ExceptionManager,
    	InvalidKeyException {
        byte[] key = this.unwrapKeyForClass(protectionClass, persistentKey);

        try {
            Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
            	new IvParameterSpec(new byte[16]));
            return new CipherInputStream(source, c);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
        	InvalidAlgorithmParameterException e) {
            throw new ExceptionManager("Unsupported encryption type found!",
            	e, true);
        }
    }

    public void decryptFile(byte[] protectionClass, byte[] persistentKey,
    	Path source, Path destination, long size) throws IOException,
    	ExceptionManager, InvalidKeyException {
        try (InputStream inputStream = Files.newInputStream(source);
        		InputStream decryptStream = decryptStream(protectionClass,
        				persistentKey, inputStream);
        		OutputStream fileOutputStream = Files
        				.newOutputStream(destination);
        		BufferedOutputStream outputStream = new
        				BufferedOutputStream(fileOutputStream)) {
        	    decryptStream.transferTo(outputStream);
        	    outputStream.flush();

                if (size != -1L) {
            	    try (FileChannel channel = ((FileOutputStream)
            	    		fileOutputStream).getChannel()) {
            		    channel.truncate(size);
                        long padding = size - channel.size();
                        if (padding != 0 && padding < Integer.MAX_VALUE) {
                            outputStream.write(new byte[(int) padding]);
                        }
                   }
               }
          }
    }

    /* Return the hashset if !null. */
    public Set<String> getHashSet() {
    	if (this.hashSet != null) {
    		return this.hashSet;
    	} else {
    		return Set.of();
    	}
    }

    public void decryptFile(int protectionClass, byte[] persistentKey,
    	Path source, Path destination, long size) throws ExceptionManager,
    	IOException, InvalidKeyException {
        decryptFile(ByteBuffer.allocate(4).putInt(protectionClass).array(),
        	persistentKey, source, destination, size);
    }

    public void decryptFile(int protectionClass, byte[] persistentKey,
    	Path source, Path destination) throws ExceptionManager, IOException,
    	InvalidKeyException {
        decryptFile(protectionClass, persistentKey, source, destination, -1);
    }

    public InputStream encryptStream(byte[] protectionClass,
    	byte[] persistentKey, InputStream source) throws ExceptionManager,
    	InvalidKeyException {
        byte[] key = this.unwrapKeyForClass(protectionClass, persistentKey);

        try {
            Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
            	new IvParameterSpec(new byte[16]));
            return new CipherInputStream(source, c);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
        	InvalidAlgorithmParameterException e) {
            throw new ExceptionManager("Unsupported encryption type found!",
            	e, true);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}