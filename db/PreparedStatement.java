package lava4.db;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Memory;
import com.sun.jna.ptr.PointerByReference;

/**
* SQLite doesn't distinguish a statement from a prepared statement, but it is easier to think of
* this as separate. See java.sql.PreparedStatement
*
* This requires bind parameters.  I only have these for ints and blobs but the others could be added
*
* This extends Statement.  Be sure to close it when done.
*/
public class PreparedStatement extends Statement {
	public final static int SQLITE_STATIC = 0;

	/**
	* Create a new PreparedStatement, given the connection and the sql.
	*
	* The SQL must have bind parameters.  See https://www.sqlite.org/c3ref/bind_blob.html
	*/
	public PreparedStatement(Connection c,String sql) {
		super(c,sql);
	}

	/**
	* Bind an int to the statement using the parameter index, which starts with 1.
	* Returns 0 for success or an error code.
	*/
	public int setInt(int parameterIndex, int value) {
		return SQLite.sqlite3_bind_int(stmt, parameterIndex, value);
	}

	/**
	* Bind a blob to the statement.
	*/
	public int setBlob(int px,byte[] blob) {
		return SQLite.sqlite3_bind_blob(stmt, px, blob, blob.length, SQLITE_STATIC);
	}

	public int setString(int px,String sval) {
		return SQLite.sqlite3_bind_text(stmt, px, sval, sval.length(), SQLITE_STATIC);
	}

	/**
	* The return code must be SQLITE_DONE (101), or else an error has occurred
	*/
	public int executeUpdate() {
		return SQLite.sqlite3_step(stmt);
	}


	//===================
	//test code
	public static void main(String[] args) {
		Connection c = new Connection("test.sqlite");
		//create a temp table
		String sql = "CREATE TEMP TABLE Products( "
				  + "ProductId INTEGER PRIMARY KEY, "
				  + "ProductName TEXT, "
				  + "SKU BLOB);";
		int rc = c.exec(sql);

		//do an insert using a prepared statement
		String productName = "pig";
		String sku = "1234~!@#$%^&*";
		String sql2 = "insert into Products (ProductName,SKU) values (?,?)";
		PreparedStatement ps = new PreparedStatement(c,sql2);
		ps.setString(1,productName);
		ps.setBlob(2,sku.getBytes());
		int rc2=ps.executeUpdate();
		if (rc2!=101) {
			System.out.println("prepared statement returned "+rc);
		}
		int pid = c.last_insert_rowid();
		System.out.println("product "+productName+" inserted as ProductId "+pid);
		ps.close();

		//now do a select
		String sql3 = "select ProductName,SKU from Products where ProductId = "+pid;
		Statement s = new Statement(c,sql3);
		if (s.step()) {
			String pname = s.getString(0);
			byte[] bsku = s.getBlob(1);
			System.out.println(pname+" | "+new String(bsku));
		}
		s.close();
		c.close();
	}
}
