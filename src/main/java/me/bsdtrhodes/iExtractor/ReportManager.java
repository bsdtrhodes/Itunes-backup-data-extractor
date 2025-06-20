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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.bsdtrhodes.iExtractor.Phone.ApplicationInfo;

/*
 * XXX: Please PLEASE PLEASE change the report closer to an actual
 * close option OR try-with-resources. I think it's not done now
 * because we have a chicken and egg problem, so additional validate
 * is needed.
 */


/*
 * This class will handle creating a report including headers, installed
 * applications, text message statistics, etc..
 */

public class ReportManager {
    private final BufferedWriter report;
    private final Phone thePhone;

    public ReportManager(Path reportFileName, Phone thePhone) throws
    	IOException {
        this.report = Files.newBufferedWriter(reportFileName,
        		StandardCharsets.UTF_8);
        this.thePhone = thePhone;
        writeHeader();
    }

	/* This will write the header and data from the phone. */
    private void writeHeader() throws IOException {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern
			("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		report.write(WebPageManager.header("Phone Report generated on: "
				+ now));
		report.write(WebPageManager.sectionBlockStart("General Phone"
				+ " Information"));
		report.write(WebPageManager.sectionBlockEntry("Report generation"
				+ "date/time: (Local Time) " + dtf.format(now)));
		report.write(WebPageManager.sectionBlockEntry("Device Name:"
				+ thePhone.getDeviceName()));
		report.write(WebPageManager.sectionBlockEntry("Device Phone"
				+ " Number: " + thePhone.getPhoneNumber()));
		report.write(WebPageManager.sectionBlockEntry("Display Name:"
				+ thePhone.getDisplayName()));
		report.write(WebPageManager.sectionBlockEntry("Device Serial"
				+ " Number: " + thePhone.getSerialNumber()));
		report.write(WebPageManager.sectionBlockEntry("Last backup"
				+ " date: " + thePhone.getLastBackupDate()));
		report.write(WebPageManager.sectionBlockEntry("Software Build"
				+ " Version: " + thePhone.getBuildVersion()));
		report.write(WebPageManager.sectionBlockEntry("Product Name: "
				+ thePhone.getProductName()));
		report.write(WebPageManager.sectionBlockEntry("Product Type: "
				+ thePhone.getProductType()));
		report.write(WebPageManager.sectionBlockEntry("Phone GUID (Global"
				+ " Unique Identifier: " + thePhone.getGuid()));
		report.write(WebPageManager.sectionBlockEntry("Device IMEI"
				+ " (International Mobile Equipment Identify: "
				+ thePhone.getImei()));
		report.write(WebPageManager.sectionBlockEntry("Device IMEI2 (for"
				+ " dual-SIM phones: " + thePhone.getImei2()));
		report.write(WebPageManager.sectionBlockEntry("Device Mobile"
				+ " Equipment Identifier (legacy CDMA tracking): "
				+ thePhone.getMeid()));
		report.write(WebPageManager.sectionBlockEntry("Integrated"
				+ " Circuit Card Identifier: " + thePhone.getIccid()));

		report.write(WebPageManager
				.sectionBlockStart("Known/found installed"
						+ " applications:\n"));

		Map<String, ApplicationInfo> appMap = thePhone.getApplications();
			for (String name: appMap.keySet()) {
				String key = name;
				report.write(WebPageManager.sectionBlockEntry(key));
		}
		report.write(WebPageManager.sectionBlockEnd());
    }

    public void processMessages(ArrayList<Message> messages) throws
    	IOException {
        Map<String, InteractionStats> statsMap = new HashMap<>();

        for (Message msg : messages) {
        	boolean isFromMe = msg.getMessageIsFromMe();
            String number;
            
            if (isFromMe) {
            	number = msg.getMessageToPhone();
            } else {
            	number = msg.getMessageFromPhone();
            }

            if (number == null || number.isBlank()) {
            	continue;
            }

            statsMap.putIfAbsent(number, new InteractionStats(number));
            if (isFromMe) {
                statsMap.get(number).incrementSent();
            } else {
                statsMap.get(number).incrementReceived();
            }
        }

        report.write(WebPageManager.sectionBlockStart("Message Interaction Summary:"));
        report.write(WebPageManager.beginTable("Phone Number", "Sent", "Received", "Total"));
        for (InteractionStats stats : statsMap.values()) {
        	report.write(WebPageManager.row(stats.phoneNumber,
        			String.valueOf(stats.sent),
        			String.valueOf(stats.received),
        			String.valueOf(stats.total())));
        }
        report.write(WebPageManager.endTable());
        report.close();
    }

    /* Helper class to track various bits of list data. */
    private static class InteractionStats {
        private final String phoneNumber;
        private int sent = 0;
        private int received = 0;

        public InteractionStats(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public void incrementSent() {
            sent++;
        }

        public void incrementReceived() {
            received++;
        }

        public int total() {
            return sent + received;
        }
    }
}