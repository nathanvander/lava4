package lava4.db;
/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 *
 * NOTE:  Downloaded from https://github.com/gwenn/sqlite-jna and modified.
 */

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
* These are the only functions I want.
*
* Connection:
*	sqlite3_open
* 	sqlite3_close_v2
* 	sqlite3_exec
* 	sqlite3_changes
* 	sqlite3_last_insert_rowid
* 	sqlite3_libversion_number
*
* Statement:
* 	sqlite3_prepare_v2
* 	sqlite3_finalize
*	sqlite3_step
*	sqlite3_stmt_busy
*	sqlite3_column_int
*	sqlite3_column_int64
*	sqlite3_column_double
*	sqlite3_column_text
*	sqlite3_column_type
 *	sqlite3_column_count
*	sqlite3_column_name
*/

public final class SQLite implements Library {
	public static final String JNA_LIBRARY_NAME = "sqlite3";

	static {
		Native.register(JNA_LIBRARY_NAME);
	}

	public static final int SQLITE_OK = 0;
	public static final int SQLITE_ROW = 100;
	public static final int SQLITE_DONE = 101;
	public static final int SQLITE_TRANSIENT = -1;

	public static native String sqlite3_libversion(); // no copy needed

	public static native int sqlite3_libversion_number();

	public static native String sqlite3_errmsg(SQLite3 pDb); // copy needed: the error string might be overwritten or deallocated
								//by subsequent calls to other SQLite interface functions.
	public static native int sqlite3_errcode(SQLite3 pDb);

		//int sqlite3_open(
		//  const char *filename,   /* Database filename (UTF-8) */
		//  sqlite3 **ppDb          /* OUT: SQLite db handle */
	public static native int sqlite3_open(String filename,PointerByReference ppDb);

	public static native int sqlite3_open_v2(String filename, PointerByReference ppDb, int flags, String vfs); // no copy needed

	public static native int sqlite3_close(SQLite3 pDb);

	//The sqlite3_close_v2() interface is intended for use with host languages that are garbage collected
	public static native int sqlite3_close_v2(SQLite3 pDb); // since 3.7.14

	public static native void sqlite3_interrupt(SQLite3 pDb);

	public static native int sqlite3_changes(SQLite3 pDb);

	public static native int sqlite3_total_changes(SQLite3 pDb);

	public static native long sqlite3_last_insert_rowid(SQLite3 pDb);

	public static native int sqlite3_exec(SQLite3 pDb, String cmd, Callback c, Pointer udp, PointerByReference errMsg);

	//---------------------------
	//Statements

	public static native int sqlite3_prepare_v2(SQLite3 pDb, Pointer sql, int nByte, PointerByReference ppStmt,
			PointerByReference pTail);

	public static native int sqlite3_finalize(SQLite3Stmt pStmt);

	public static native int sqlite3_step(SQLite3Stmt pStmt);

	public static native boolean sqlite3_stmt_busy(SQLite3Stmt pStmt);

	public static native int sqlite3_column_count(SQLite3Stmt pStmt);

	public static native int sqlite3_column_type(SQLite3Stmt pStmt, int iCol);

	public static native String sqlite3_column_name(SQLite3Stmt pStmt, int iCol); // copy needed: The returned string pointer is valid until
		//either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared
		//by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or
		//sqlite3_column_name16() on the same column.

	public static native int sqlite3_column_bytes(SQLite3Stmt pStmt, int iCol);

	public static native double sqlite3_column_double(SQLite3Stmt pStmt, int iCol);

	public static native int sqlite3_column_int(SQLite3Stmt pStmt, int iCol);

	public static native long sqlite3_column_int64(SQLite3Stmt pStmt, int iCol);

	public static native String sqlite3_column_text(SQLite3Stmt pStmt, int iCol); // copy needed: The pointers
		//returned are valid until a type conversion occurs as described above, or until sqlite3_step()
		//or sqlite3_reset() or sqlite3_finalize() is called.

	/**
	 * Database connection handle
	 * @see <a href="http://sqlite.org/c3ref/sqlite3.html">sqlite3</a>
	 */
	public static class SQLite3 extends PointerType {
		public SQLite3() {
		}
		public SQLite3(Pointer p) {
			super(p);
		}
	}

	/**
	 * Prepared statement object
	 * @see <a href="http://sqlite.org/c3ref/stmt.html">sqlite3_stmt</a>
	 */
	public static class SQLite3Stmt extends PointerType {
		public SQLite3Stmt() {
		}
		public SQLite3Stmt(Pointer p) {
			super(p);
		}
	}

	//===========================
	//bind and blob methods
	public static native int sqlite3_bind_int(SQLite3Stmt pStmt, int i, int value);

	public static native int sqlite3_bind_blob(SQLite3Stmt pStmt, int i, byte[] value, int n, long xDel);
		// no copy needed when xDel == SQLITE_TRANSIENT == -1

	public static native int sqlite3_bind_text(SQLite3Stmt pStmt, int i, String value, int n, long xDel);
	// no copy needed when xDel == SQLITE_TRANSIENT == -1

	//get a blob pointer from a statement
	//the number of bytes is in sqlite3_column_bytes()
	public static native Pointer sqlite3_column_blob(SQLite3Stmt pStmt, int iCol);
}
