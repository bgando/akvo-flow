/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package com.gallatinsystems.survey.device.exception;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.os.Environment;
import android.util.Log;

import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.FileUtil;

/**
 * This exception handler will log all exceptions it handles to the filesystem
 * so they can be processed later. This sets the default uncaught exception
 * handler. It will delegate processing to the previously installed uncaught
 * exception handler (if there is one) to preserve normal system operation. This
 * class is a singleton to preserve the chain of exception handlers.
 * 
 * @author Christopher Fagiani
 * 
 */
public class PersistentUncaughtExceptionHandler implements
		UncaughtExceptionHandler {

	private static final String TAG = "UNCAUGHT_EXCEPTION_HANDLER";
	private static PersistentUncaughtExceptionHandler instance;

	private UncaughtExceptionHandler oldHandler;

	public static final PersistentUncaughtExceptionHandler getInstance() {
		if (instance == null) {
			instance = new PersistentUncaughtExceptionHandler();
		}
		return instance;
	}

	/**
	 * installs the old uncaught exception handler in a member variable so it
	 * can be invoked later
	 */
	private PersistentUncaughtExceptionHandler() {
		if (Thread.getDefaultUncaughtExceptionHandler() != null
				&& !(Thread.getDefaultUncaughtExceptionHandler() instanceof PersistentUncaughtExceptionHandler)) {
			oldHandler = Thread.getDefaultUncaughtExceptionHandler();
		}

	}

	/**
	 * saves the exception to the filesystem. Processing will then be delegated
	 * to the previously installed uncaught exception handler
	 */
	@Override
	public void uncaughtException(Thread sourceThread, Throwable exception) {

		recordException(exception);

		// Still process the exception with the default handler so we don't
		// change system behavior
		if (oldHandler != null) {
			oldHandler.uncaughtException(sourceThread, exception);
		}
	}

	/**
	 * checks against a white-list of exceptions we ignore (mainly communication
	 * errors that can arise if we're offline).
	 * 
	 * @param exception
	 * @return
	 */
	private static boolean ignoreException(Throwable exception) {
		if (exception instanceof UnknownHostException) {
			return true;
		} else if (exception instanceof SocketException) {
			return true;
		} else if (exception instanceof IllegalStateException) {
			if (exception.getMessage() != null
					&& exception
							.getMessage()
							.toLowerCase()
							.contains("sqlitedatabase created and never closed")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * saves the exception to the filesystem. this can be used to save otherwise
	 * handled exceptions so they can be reported to the server.
	 * 
	 * @param exception
	 */
	public static void recordException(Throwable exception) {
		if (!ignoreException(exception)) {
			// save the error
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			exception.printStackTrace(printWriter);
			if (exception.getMessage() != null) {
				printWriter.print("\n" + exception.getMessage());
			}

			try {
				FileOutputStream out = null;
				if (Environment.getExternalStorageState().equals(
						Environment.MEDIA_MOUNTED)) {
					out = FileUtil.getFileOutputStream(
							ConstantUtil.STACKTRACE_FILENAME
									+ Long.toString(System.currentTimeMillis())
									+ ConstantUtil.STACKTRACE_SUFFIX,
							ConstantUtil.STACKTRACE_DIR, false, null);
				} else {
					out = FileUtil.getFileOutputStream(
							ConstantUtil.STACKTRACE_FILENAME
									+ Long.toString(System.currentTimeMillis())
									+ ConstantUtil.STACKTRACE_SUFFIX,
							ConstantUtil.STACKTRACE_DIR, true, null);
				}

				FileUtil.writeStringToFile(result.toString(), out);
			} catch (IOException e) {
				Log.e(TAG, "Couldn't save trace file", e);
			} finally {
				if (result != null) {
					try {
						result.close();
					} catch (IOException e) {
						Log.w(TAG, "Can't close print writer object", e);
					}
				}
			}
		}
	}
}
