package lava4.db;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Memory;
import com.sun.jna.ptr.PointerByReference;

public class Statement {
	SQLite.SQLite3Stmt stmt;

	/**
	* Create a new Statement, given the connection and the sql.
	*/
	public Statement(Connection c,String sql) {
		if (sql==null) throw new IllegalArgumentException("sql is null");
		System.out.println(sql);
		PointerByReference ppStmt=new PointerByReference();
		PointerByReference pzTail=new PointerByReference();

		Pointer psql = new Memory(sql.length() + 1); // WARNING: assumes ascii-only string
		psql.setString(0, sql);

		int rc=SQLite.sqlite3_prepare_v2(
			c.getPointer(),
			psql,
			sql.length(),
			ppStmt,
			pzTail
		);

		if (rc==0) {
			Pointer pstmt=ppStmt.getValue();
			stmt=new SQLite.SQLite3Stmt(pstmt);
		} else {
			throw new IllegalStateException("("+rc+") unable to create statement with "+sql);
		}
	}

	//close the statement.  always do this
	public int close() {
		return SQLite.sqlite3_finalize(stmt);
	}

	public boolean step() {
		int rc = SQLite.sqlite3_step(stmt);
		if (rc==SQLite.SQLITE_ROW) {
			return true;
		} else if (rc==SQLite.SQLITE_DONE) {
			return false;
		} else {
			throw new IllegalStateException("step returns "+rc);
		}
	}

	/**
	* returns true is the statement has stepped at least once
	* but not run until completion.
	*/
	public boolean isBusy() {
		return SQLite.sqlite3_stmt_busy(stmt);
	}

	public int getColumnCount() {
		return SQLite.sqlite3_column_count(stmt);
	}

	//save the string right away
	public String getColumnName(int i) {
		return SQLite.sqlite3_column_name(stmt, i);
	}

	//The returned value is one of:
	//	SQLITE_INTEGER 1
	//	SQLITE_FLOAT 2
	//	SQLITE_TEXT 3
	//	SQLITE_BLOB 4
	//	SQLITE_NULL 5
	public int getColumnType(int i) {
		return SQLite.sqlite3_column_type(stmt,i);
	}

	public int column_bytes(int iCol) {
		return SQLite.sqlite3_column_bytes(stmt,iCol);
	}

	public double getDouble(int columnIndex) {
		return SQLite.sqlite3_column_double(stmt, columnIndex);
	}

	public int getInt(int columnIndex) {
		return SQLite.sqlite3_column_int(stmt, columnIndex);
	}

	public long getLong(int columnIndex)  {
		return SQLite.sqlite3_column_int64(stmt, columnIndex);
	}

	public String getString(int columnIndex) {
		return SQLite.sqlite3_column_text(stmt, columnIndex);
	}

	public byte[] getBlob(int columnIndex) {
		int size = SQLite.sqlite3_column_bytes(stmt, columnIndex);
		Pointer ptr = SQLite.sqlite3_column_blob(stmt, columnIndex);
		if (ptr==null) {
			throw new IllegalStateException("sqlite3_column_blob returned null");
		}
		return ptr.getByteArray( (long)0,size);
	}

	//===================
	public static void main(String[] args) {
		Connection c = new Connection("test.sqlite");
		String sql = "CREATE TEMP TABLE Products( "
				  + "ProductId, "
				  + "ProductName, "
				  + "Price);";
		int rc = c.exec(sql);

		String sql2 = "select type,tbl_name from sqlite_temp_master";
		Statement s = new Statement(c,sql2);
		while (s.step()) {
			System.out.println(s.getString(0)+" | "+s.getString(1));
		}
		s.close();
		c.close();
	}
}
