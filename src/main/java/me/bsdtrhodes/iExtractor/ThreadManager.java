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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/*
 * In theory, provide a management interface for threading various
 * tasts. In practice, well, this is my first time doing threads
 * outside of pthreads in C.
 */
public class ThreadManager {

	private final ExecutorService execSVS;
	private final Map<Integer, Future<?>> jobMap;

	public ThreadManager(int poolSize) {
		execSVS = Executors.newFixedThreadPool(poolSize);
		jobMap = new HashMap<>();
	}

	/* Submit a thread project here. */
	public void submitJob(Runnable job, int jid) {
		Future<?> future = execSVS.submit(job);
		jobMap.put(jid, future);
	}

	/* Cleanup and return done, or permit waiting. */
	public boolean isJobComplete(int jid) {
		Future<?> future = jobMap.get(jid);
		if (future != null && future.isDone()) {
			jobMap.remove(jid);
			return future.isDone();
		}
		return false;
	}

	/* Shutdown thread pool */
	public void shutdown() {
		execSVS.shutdown();
	}

	/* Await termination. */
	public boolean awaitTermination(long timeout, TimeUnit unit) throws
		InterruptedException {
	    return execSVS.awaitTermination(timeout, unit);
	}
}
