package lava4.db;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.ptr.PointerByReference;

public class Connection {

	public static SQLite.SQLite3 open(String filename) {
		PointerByReference ppDb=new PointerByReference();
		int rc=SQLite.sqlite3_open(filename,ppDb);
		if (rc!=0) {
			throw new IllegalStateException("("+rc+") unable to open database file: "+filename);
		} else {
			Pointer pdb=ppDb.getValue();
			return new SQLite.SQLite3(pdb);
		}
	}

	public static int libversion_number() {
		return SQLite.sqlite3_libversion_number();
	}

	//=========================
	SQLite.SQLite3 conn;

	public Connection(String filename) {
		conn=open(filename);
	}

	protected SQLite.SQLite3 getPointer() {return conn;}

	public void close() {
		SQLite.sqlite3_close_v2(conn);
	}

	/**
	* Execute the given sql code.  This is used for all sql commands except select.
	* Returns 0 if success, otherwise returns the error code.
	*/
	public int exec(String sql) {
		System.out.println(sql);
		//the pbr will hold an error string if any
		PointerByReference pbr=new PointerByReference();
		return SQLite.sqlite3_exec(conn,sql,null,null,pbr);
	}

	/**
	* returns the number of rows modified, inserted or deleted by the most recently completed
	* INSERT, UPDATE or DELETE statement on the database connection
	*/
	public int changes() {
		return SQLite.sqlite3_changes(conn);
	}

	public int last_insert_rowid() {
		return (int)SQLite.sqlite3_last_insert_rowid(conn);
	}

	/**
	* begin a new transaction.  Call before exec
	*/
	public void begin() {
		int rc=exec("BEGIN IMMEDIATE TRANSACTION");
		if (rc!=0) throw new IllegalStateException("("+rc+") unable to begin transaction");
	}

	/**
	* commit a transaction.
	*/
	public void commit() {
		int rc=exec("COMMIT TRANSACTION");
		if (rc!=0) throw new IllegalStateException("("+rc+") unable to commit transaction");
	}

	/**
	* This both rollsback the transaction and closes the connection.
	*/
	public void rollback() {
		int rc=exec("ROLLBACK TRANSACTION");
		if (rc!=0) throw new IllegalStateException("("+rc+") unable to rollback transaction");
	}

	/**
	* Get the last error message, if any.
	*/
	public String getLastError() {
		return SQLite.sqlite3_errmsg(conn);
	}

	//======================
	public static void main(String[] args) {
		System.out.println(libversion_number());
		Connection c = new Connection("test.sqlite");
		String sql = "CREATE TEMP TABLE Products( "
		  + "ProductId, "
		  + "ProductName, "
		  + "Price);";
		int rc = c.exec(sql);
		if (rc==0) {
			System.out.println("success");
		} else {
			System.out.println("error: "+rc);
		}
		c.close();
	}

}