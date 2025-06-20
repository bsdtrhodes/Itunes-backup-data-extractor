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
 * This file will describe the messages class. Everything is extracted
 * into an ArrayList and then written to files, but for that, each
 * conversation needs to be broken down into distinct objects.
 * 
 * Information comes from the file
 * backupdir/3d/3d0d7e5fb2ce288813306e4d4636395e047a3d28
 *
 * NOTE: We do not pull in contact information and assign any
 * number the nickname or person assigned that number in the
 * phone since that is easily disguised by just putting in a
 * random name to a number.
 * 
 * BUGS:
 * o I'm torn on keeping the ThreadID, on one hand, it helps to identify
 *   the chat thread, but on the other hand, I'm not sure it really helps
 *   to discern the identify of the recipients/senders.
 */

public class Message {
	private String ThreadID, FromPhone, ToPhone, ServiceType,
	    MessageDate, MessageData;
	private Boolean isFromMe;

	Message(String ThreadID, Boolean fromMe, String fromPhone, String
			toPhone, String Service, String MessageDate, String Message) {
		this.ThreadID = ThreadID;
		this.isFromMe = fromMe;
		this.FromPhone = fromPhone;
		this.ToPhone = toPhone;
		this.ServiceType = Service;
		this.MessageDate = MessageDate;
		this.MessageData = Message;
	}

	/* Getters are needed for this data. */
	public String getMessageThreadID() {
		return this.ThreadID;
	}

	public Boolean getMessageIsFromMe() {
		return isFromMe;
	}

	public String getMessageFromPhone() {
		return this.FromPhone;
	}

	public String getMessageToPhone() {
		return this.ToPhone;
	}

	public String getMessageService() {
		return this.ServiceType;
	}

	public String getMessageDate() {
		return this.MessageDate;
	}

	public String getMessageData() {
		return this.MessageData;
	}
}
