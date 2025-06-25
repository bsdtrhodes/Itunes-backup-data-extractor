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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/*
 * The Window manager handles dialogs. Exceptions are handled here. They are
 * either caught and logged, or caught and displayed. If the error is
 * considered fatal, we call the window manager to inform the user and
 * call exit.
 */

/*
 * Self note: RuntimeException helps reduce boilerplate, and, from my
 * reading, gives me cleaner code at the loss of, in my case, meaningful
 * safety. If we need more checked v. unchecked error handling, it
 * can be reverted to Exception.
 */
public class ExceptionManager extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(ExceptionManager
			.class.getName());

	static {
		try {
			Path logFilePath = Paths.get(System.getProperty("user.home"),
					"iExtractor.log");
			FileHandler logFile = new FileHandler(logFilePath.toString(),
					true);
			logFile.setFormatter(new SimpleFormatter());
			logFile.setLevel(Level.ALL);
			LOGGER.addHandler(logFile);
			LOGGER.setUseParentHandlers(false);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	public ExceptionManager(String message) {
	        super("The backup manager encountered an error: " + message);
	        LOGGER.log(Level.WARNING, message);
	}

	public ExceptionManager(String message, Throwable cause,
		boolean fatal) {
	        super("The backup manager encountered an error: " + message,
	        		cause);
	        LOGGER.log(Level.SEVERE, cause.getMessage());
	        if (fatal) {
	        	WindowManager.critical(ContextManager.getPrimaryStage(),
	        			message, cause.getMessage());
	        }
	}
	public static void testLogging() {
        LOGGER.log(Level.INFO, "Test log: Is logging working?");
    }
}