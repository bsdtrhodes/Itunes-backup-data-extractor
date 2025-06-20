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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

/*
 * Class and methods to clean up after a completed or partially completed
 * backup. As the extraction happens, files are added to the list for
 * removal. Throw ExceptionManager.
 * 
 * XXX: Hook this into the JVM:
 * public CleanupManager() {
 * Runtime.getRuntime().addShutdownHook(new Thread(this::runCleanup));
 * }
 * Example of use: cleanupManager.markForCleanup(decMessagesDB);
 * Eventually: boolean removeNow(File f) if you want immediate cleanup.
 * Eventually: void clearQueue() if we need to cancel a batch of deletions.
 * 
 * BackupManager should be the only consumer of this. Improve logic if this
 * ever needs to be passed around.
 * 
 * 
 * XXX: TODO: Take a DEBUG action, if DEBUG is true, leave these files.
 */
public class CleanupManager {
	private final Queue<Path> cleanupQueue = new LinkedList<>();

	/* This SHOULD hook into the JVM so it can be called on exit. */
	public CleanupManager() throws IOException {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				runCleanup();
			} catch (ExceptionManager e) {
				/*
				 * Throw not allowed, called on exit, print the trace in
				 * case anyone is interested in the error.
				 */
				e.printStackTrace();
			}
		}));
	}

	public void markForCleanup(Path file) {
	    if (file != null && Files.exists(file)) {
	        cleanupQueue.add(file);
	    }
	}

	public void runCleanup() throws ExceptionManager {
	    while (!cleanupQueue.isEmpty()) {
	        Path file = cleanupQueue.poll();
	        try {
	            Files.deleteIfExists(file);
	        } catch (IOException e) {
	            throw new ExceptionManager("Failed to delete: " + file, e,
	            	false);
	        }
	    }
	}
}