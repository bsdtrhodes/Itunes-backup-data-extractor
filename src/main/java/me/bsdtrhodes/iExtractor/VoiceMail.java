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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/*
 * Voicemails object
 * 
 * Voicemail information is stored in three places, two in the
 * database. The first one is the database about saved voicemails
 * is in the file: 99\992df473bbb9e132f4b3b6e4d33f72171e97bc7a,
 * and there is a ROWID that lines up with the AMR file on disk
 * and stored in the Manifest DB.
 * 
 */
public class VoiceMail extends Phone {
	private String ROWID, AppleDate, sender, expiration, trashed_date,
		receiver, arrival_date, duration, MD5;

	VoiceMail(String row, String Date, String sender, String expiration,
		String trashdt, String receiver, String arrdate, String len, String
		MD5) throws NoSuchAlgorithmException, IOException {

		this.ROWID = row;
		this.AppleDate = Date;
		this.sender = sender;
		this.expiration = expiration;
		this.trashed_date = trashdt;
		this.receiver = receiver;
		this.arrival_date = arrdate;
		this.duration = len;
		this.MD5 = MD5;
	}

	public String getROWID() {
		return ROWID;
	}

	public String getDuration() {
		return duration;
	}

	public String getAppleDate() {
		return AppleDate;
	}

	public String getSender() {
		return sender;
	}

	public String getMD5() {
		return MD5;
	}

	public String getExpiration() {
		return expiration;
	}

	public String getTrashed_date() {
		return trashed_date;
	}

	public String getReceiver() {
		return receiver;
	}

	public String getArrival_date() {
		return arrival_date;
	}
}
