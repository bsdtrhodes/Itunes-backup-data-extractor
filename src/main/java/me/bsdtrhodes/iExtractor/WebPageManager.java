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
 * I guess Java 15 introduced text blocks. That was around 2018 I think.
 * I've left my comparability to be with older versions for no other reason
 * than avoiding the project compliance update. Everyone should be using
 * the latest version of Java for security but I want to be cautious.
 */
public class WebPageManager {

	/* This is a static string provider, create generic constructor. */
	public WebPageManager() {
		return;
	}

    public static String header(String title) {
        return "<!DOCTYPE html> <html lang=\"en\"><head><meta"
        		+ " charset=\"UTF-8\"><title>" + title
        		+ "</title><style>"
        		+ "body { font-family: Arial, sans-serif; padding: 20px; }"
                + " table { border-collapse: collapse; width: 100%%; }"
                + " th, td { border: 1px solid #ddd; padding: 8px; }"
                + " th { background-color: #f2f2f2; }"
                + "</style></head><body><h1>" + title + "</h1>";
	}

    public static String footer() {
        return "</body></html>";

    }

    public static String beginTable(String... headers) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n<tr>");
        for (String h : headers) {
            sb.append("<th>").append(h).append("</th>");
        }
        sb.append("</tr>\n");
        return sb.toString();
    }

    public static String row(String... cells) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        for (String c : cells) {
            sb.append("<td>").append(c).append("</td>");
        }
        sb.append("</tr>\n");
        return sb.toString();
    }

    public static String endTable() {
        return "</table>\n";
    }

    public static String sectionBlockStart(String title) {
        return "<div style=\"margin-top: 30px; text-align: center;"
        		+ " padding: 20px; background-color: #f9f9f9; border: 2px"
        		+ " solid #ddd;\">" + "<h2>" + title + "</h2>"
                + " <pre style=\"white-space: pre-wrap; word-wrap:"
        		+ " break-word; text-align: left;\">";
    }

    public static String sectionBlockEntry(String entry) {
        /* Return an escaped string for safe HTML rendering. */
        return escapeHtml(entry) + "\n";
    }

    public static String sectionBlockEnd() {
        return "</pre></div>";
    }

    /* Safety first! */
    public static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
