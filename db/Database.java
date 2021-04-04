package lava4.db;
import lava4.Cell;
import lava4.MethodInfo;

/**
* The Database stores info in Sqlite.
*/
public class Database {
	public static final String dbname = "lava4.sqlite";
	public static final int T_INT = 3;
	public static final int T_CLASS = 7;		//from CONSTANT_Class value
	public static final int T_STRING = 8;
	public static final int T_FIELD = 9;
	public static final int T_METHOD = 10;
	public static final int T_IFACE_METHOD = 11;
	public static final int T_OBJECT = 20;		//arbitrary
	public static final int T_INT_ARRAY = 21;	//arbitrary
	public static final int T_STR_ARRAY = 22;	//arbitrary
	public static final int T_OBJ_ARRAY = 23;	//arbitrary
	public static final String INT_ARRAY_CLASS = "[I";
	public static final String OBJ_ARRAY_CLASS = "[Ljava/lang/Object;";
	public static final String STR_ARRAY_CLASS = "[Ljava/lang/String;";

	//It's ok to call this more than once
	public static void init() {
		Connection c = new Connection(dbname);
		Item.createItemTable(c);
		Name.createNameTable(c);
		MethodTable.createMethodTable(c);
		ItemNameHasValue.createItemNameHasValueTable(c);
		c.close();
	}

	//======================================
	// item table operations
	public static class Item {

		/**
		* The Item table has 5 columns:
		*	id
		*	type
		*	class_name
		*	class_id
		*	length - for arrays
		*/
		protected static void createItemTable(Connection c) {
			String sql = "CREATE TABLE IF NOT EXISTS item ( "
				+"id INTEGER PRIMARY KEY, "
				+"type INTEGER NOT NULL, "
				+"class_name TEXT UNIQUE, "	//class_name may be null
				+"class_id INTEGER, "
				+"length INTEGER) ";
			int rc = c.exec(sql);
			if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}

			//create an index on class_name
			String sql2 = "CREATE UNIQUE INDEX IF NOT EXISTS idx_item_cname "
				+"ON item (class_name) "
				+"WHERE class_name IS NOT NULL";
			rc = c.exec(sql2);
			if (rc!=0) {throw new IllegalStateException(rc+": "+sql2);}
		}

		/**
		* Create a new class and return the class_id
		*/
		public static int newClass(String jClassName) {
			if (jClassName==null) return 0;
			int id = getClassId(jClassName);
			if (id!=0) {
				return id;
			} else {
				String sql = "INSERT INTO item (type,class_name) VALUES ("+T_CLASS+",'"+jClassName+"')";
				Connection c = new Connection(dbname);
				int rc = c.exec(sql);
				if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
				int cid = c.last_insert_rowid();
				c.close();
				return cid;
			}
		}

		public static int getClassId(String className) {
			String sql = "SELECT id FROM item WHERE class_name='"+className+"'";
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			int id=0;
			if (st.step()) {
				id=st.getInt(0);
			}
			st.close();
			c.close();
			return id;
		}

		//given the object id, get the class of it
		public static int getClassId(int oid) {
			String sql = "SELECT class_id FROM item WHERE id="+oid;
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			int cid=0;
			if (st.step()) {
				cid=st.getInt(0);
			}
			st.close();
			c.close();
			return cid;
		}

		public static String getClassName(int cid) {
			String sql = "SELECT class_name FROM item WHERE id="+cid;
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			String cname = null;
			if (st.step()) {
				cname=st.getString(0);
			}
			st.close();
			c.close();
			return cname;
		}

		/**
		* Create a new object and return the object id
		*/
		public static int newObject(int classId) {
			if (classId==0) return 0;
			String sql = "INSERT INTO item (type,class_id) VALUES ("+T_OBJECT+","+classId+")";
			Connection c = new Connection(dbname);
			int rc = c.exec(sql);
			if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
			int rowid = c.last_insert_rowid();
			c.close();
			return rowid;
		}

		//at this point,we only have int array, String array and Object array
		public static int newArray(int atype,int length) {
			if (length<1 || length>255) throw new IllegalStateException("length is out of range: "+length);
			String claz = null;
			if (atype==T_INT_ARRAY) {claz=INT_ARRAY_CLASS;}
			else if (atype==T_STR_ARRAY) {claz=STR_ARRAY_CLASS;}
			else if (atype==T_OBJ_ARRAY) {claz=OBJ_ARRAY_CLASS;}
			else {
				//I will need to handle char and byte arrays. To do
				throw new IllegalStateException("invalid array type: "+atype);
			}

			int cid = getClassId(claz);
			if (cid==0) {
				cid = newClass(claz);
			}

			String sql = "INSERT INTO item (type,class_id,length) VALUES ("+atype+","+cid+","+length+")";
			Connection c = new Connection(dbname);
			int rc = c.exec(sql);
			if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
			int rowid = c.last_insert_rowid();
			c.close();
			return rowid;
		}

		public static int getLength(int arrayid) {
			String sql = "SELECT length FROM item WHERE id="+arrayid;
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			int len=0;
			if (st.step()) {
				len=st.getInt(0);
			}
			st.close();
			c.close();
			return len;
		}

		//for debugging
		public static String dump() {
			StringBuilder sb = new StringBuilder();
			String sql = "SELECT id,class_name from item where type=7";
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			sb.append("Item Table:\r\n");
			while(st.step()) {
				sb.append(st.getInt(0)+ ": "+st.getString(1)+"\r\n");
			}
			st.close();
			c.close();
			return sb.toString();
		}
	} //end Item
	//===================================
	// Name
	/**
	* Name comes from NameAndType.  It can either be a field or a method.
	* The name table has 5 fields:
	*	nid
	*	type
	*	class_id
	*	jname
	*	is_static
	*	//static_ival
	*	//static_sval
	*
	* Even though each Name will only have
	*/
	public static class Name {

		protected static void createNameTable(Connection c) {
			//the autoincrement will make an entry in the sqlite_sequence table
			String sql = "CREATE TABLE IF NOT EXISTS name ( "
				+"nid INTEGER PRIMARY KEY AUTOINCREMENT, "
				+"type INTEGER NOT NULL, "
				+"class_id INTEGER NOT NULL, "
				+"jname TEXT NOT NULL, "
				+"is_static INTEGER NOT NULL) ";

				//+"jname TEXT NOT NULL, "
				//+"static_ival INTEGER, "
				//+"static_sval TEXT) ";
			int rc = c.exec(sql);
			if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}

			//change the start of the nid
			//get the value of the autoincrement field
			String sql2 = "SELECT seq FROM sqlite_sequence WHERE name = 'name'";
			Statement s = new Statement(c,sql2);
			int name_seq=0;
			if (s.step()) {
				name_seq=s.getInt(0);
			}
			s.close();

			if (name_seq<2) {
				String sql3 = "UPDATE sqlite_sequence SET seq=256 WHERE name = 'name'";
				int rc2 = c.exec(sql3);
				if (rc2!=0) {throw new IllegalStateException(rc2+": "+sql);}
			}

			//create an index on jname
			String sql4 = "CREATE UNIQUE INDEX IF NOT EXISTS idx_name_jname "
				+"ON name (jname) ";
			rc = c.exec(sql4);
			if (rc!=0) {throw new IllegalStateException(rc+": "+sql4);}
		}

		/**
		* newName
		* Names have to be unique so this checks that they exist before adding
		* type is one of:
		*	T_FIELD
		*	T_METHOD
		*	T_IFACE_METHOD
		* Insert a new name and return the nameid
		* The isStatic field is informational only.  The default is false (0).
		*/
		public static int newName(int type,int classId,String jname,boolean isStatic) {
			if (type==0 || classId==0) throw new IllegalArgumentException("type and classId must have a value");
			int nid = getNameId(jname);
			if (nid>0) return nid;
			int is_static = isStatic ? 1 : 0;
			String sql = "insert into name (type,class_id,jname,is_static) values ("
				+type+","+classId+",'"+jname+"',"+is_static+")";

			Connection c = new Connection(dbname);
			int rc = c.exec(sql);
			if (rc!=0) {
				String error = c.getLastError();
				throw new IllegalStateException(error+" ["+rc+"]: "+sql);
			}
			int rowid = c.last_insert_rowid();
			c.close();
			return rowid;
		}

		/**
		* If the jname is valid, it will have a number starting with 256
		*/
		public static int getNameId(String jname) {
			String sql = "select nid from name where jname = '"+jname+"'";
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			int nid=0;
			if (st.step()) {
				nid=st.getInt(0);
			}
			st.close();
			c.close();
			return nid;
		}

		public static String getName(int nid) {
			String sql = "select jname from name where nid="+nid;
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			String jname = null;
			if (st.step()) {
				jname=st.getString(0);
			}
			st.close();
			c.close();
			return jname;
		}

		//protected int getStaticInt(Connection c,int nid) {
		//	String sql = "select ival from name where nid = "+nid;
		//	Statement st = new Statement(c,sql);
		//	int ival = 0;
		//	if (st.step()) {
		//		ival = st.getInt(0);
		//	}
		//	st.close();
		//	return ival;
		//}

		//protected String getStaticString(Connection c,int nid) {
		//	String sql = "select sval from name where nid = "+nid;
		//	Statement st = new Statement(c,sql);
		//	String sval = null
		//	if (st.step()) {
		//		sval = st.getString(0);
		//	}
		//	st.close();
		//	return sval;
		//}

		//protected static void updateStaticInt(Connection c, int nid,int ival) {
		//	String sql = "update name set static_ival = "+ival+" where nid = "+nid;
		//	int rc = c.exec(sql);
		//	if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
		//}

		//protected static void updateStaticString(Connection c,int nid,String sval) {
		//	//there may be a reason to set this to null, but I want to see the case for it
		//	if (sval==null) throw new IllegalArgumentException("String may not be null");
		//	String sql = "update name set static_sval = '"+sval+"' where nid = "+nid;
		//	int rc = c.exec(sql);
		//	if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
		//}

		/**
		* Static fields are stored in the static_value of the Name object
		*/
		//public static void putStatic(int fieldId,Cell value) {
		//	Connection c = new Connection(dbname);
		//	if (value.length()>2) {
		//		updateStaticString(c,fieldId,value.toString());
		//	} else {
		//		updateStaticInt(c,fieldId,value.toInt());
		//	}
		//	c.close();
		//}

		//is this a String or an int? Is there a better way of doing this?
		//public static Cell getStatic(int fieldId) {
		//	Connection c = new Connection(dbname);
		//	String sql = "select static_ival,static_sval from name where nid = "+fieldId;
		//	Statement st = new Statement(c,sql);
		//	Cell v = null;
		//	int ival = 0;
		//	String sval = null;
		//	if (st.step()) {
		//		ival=st.getInt(0);
		//		sval=st.getString(1);
		//		if (sval!=null && sval.length()>1) {
		//			v = new Cell(sval);
		//		} else {
		//			v = new Cell(ival);
		//		}
		//	}
		//	st.close();
		//	c.close();
		//	return v;
		//}

		public static String dump() {
			StringBuilder sb = new StringBuilder();
			String sql = "SELECT nid,jname from name order by nid";
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			sb.append("Name Table:\r\n");
			while(st.step()) {
				sb.append(st.getInt(0)+ ": "+st.getString(1)+"\r\n");
			}
			st.close();
			c.close();
			return sb.toString();
		}
	}

	//==============================
	// Method

	public static class MethodTable {

	/**
	* The method table is an extension of the name table.
	* The nid and class_id are the same.  The nid is not auto-assigned, it must
	* be passed in.
	*
	* The method table has 5 fields:
	*	nid
	*	class_id
	*	is_static
	*	params
	*	code
	*/
		protected static void createMethodTable(Connection c) {
			String sql ="CREATE TABLE IF NOT EXISTS method ( "
				+"nid INTEGER PRIMARY KEY, "
				+"class_id INTEGER NOT NULL, "
				+"is_static INTEGER, "		//can null, 1 if static
				+"params INTEGER NOT NULL, "
				+"code BLOB NOT NULL)";
			int rc = c.exec(sql);
			if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
		}


		/**
		* Insert a new method into the database.  Return the id if successful.
		* The nid comes from the Name table
		*/
		public static int newMethod(MethodInfo m) {
			String sql = "insert into method (nid,class_id,is_static,params,code) values ("
				+ m.getNameId()+","+m.getClassId()+","+m.getStatic()+","+m.params()+ ", ?)";
			Connection c = new Connection(dbname);
			PreparedStatement ps = new PreparedStatement(c,sql);
			ps.setBlob(1,m.getCode());
			int rc=ps.executeUpdate();
			if (rc!=101) {
				String err = c.getLastError();
				throw new IllegalStateException("Database.newMethod: "+err+" ["+rc+"]: "+sql);
			}
			ps.close();
			c.close();
			return m.getNameId();
		}

		/**
		* This will return null if not found
		*/
		public static MethodInfo getMethod(int nameId) {
			String sql = "select nid, class_id, is_static, params, code from method where nid = "+nameId;
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			MethodRow m = null;
			if (st.step()) {
				m = new MethodRow();
				m.nid = nameId;
				m.class_id = st.getInt(1);
				//is_static is a number in the database but a boolean in the object
				m.setStatic(st.getInt(2));
				m.params = st.getInt(3);
				m.code = st.getBlob(4);
			}
			st.close();
			c.close();
			return (MethodInfo)m;
		}

		//for debugging
		public static String dump() {
			StringBuilder sb = new StringBuilder();
			String sql = "SELECT n.nid,n.jname FROM name n,method m WHERE n.nid=m.nid";
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			sb.append("Method Table:\r\n");
			while(st.step()) {
				sb.append(st.getInt(0)+ ": "+st.getString(1)+"\r\n");
			}
			st.close();
			c.close();
			return sb.toString();
		}
	}	//end class Method
	//====================================
	/**
	* item_name_has_value
	* Store value. This has multiple uses:
	*	1. store a constant (int or string) in a constant pool
	*  	2. store a name in a constant pool, including a method name
	*	3. store a static value
	*	4. store a value in an object field
	*	5. store a value in an array
	*
	* The value is ambiguous.  It could be an int value, but this could be an object id or
	* a number.  You can't tell by looking.
	*
	* Static values are fields that are associated with a class, not an object
	*/
	public static class ItemNameHasValue {
		/**
		* The item_name_has_value table has 6 fields:
		*	id
		*	item_id
		*	name_id
		*	vtype
		*	ival
		*	sval
		* The calling method has the connection.
		*/
		protected static void createItemNameHasValueTable(Connection c) {
			String sql = "CREATE TABLE IF NOT EXISTS item_name_has_value ( "
				+"id INTEGER PRIMARY KEY, "
				+"item_id INTEGER NOT NULL, "
				+"name_id INTEGER NOT NULL, "
				+"vtype INTEGER NOT NULL, "
				+"ival INTEGER, "
				+"sval TEXT)";
			int rc = c.exec(sql);
			if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}

			//create an index on class_name
			String sql2 = "CREATE INDEX IF NOT EXISTS idx_inhv "
				+"ON item_name_has_value (item_id,name_id) ";
			rc = c.exec(sql2);
			if (rc!=0) {throw new IllegalStateException(rc+": "+sql2);}
		}

		/**
		* FindRow
		* Return the rowid which has the item_id and name_id (or index).
		* A return value of 0 means it was not found
		*/
		protected static int findRow(Connection c,int item,int x) {
			String sql = "select id from item_name_has_value where item_id="+item+" and name_id="+x;
			Statement st = new Statement(c,sql);
			int id = 0;
			if (st.step()) {
				id = st.getInt(0);
			}
			return id;
		}

		/**
		* InsertValue.  This is used internally. You need to call findRow() before this to see
		* if the row already exists.
		* VType is usually T_INT or T_STRING but it could be other values such as T_OBJECT
		*/
		protected static int insertValue(Connection c,int item,int x,int vtype,Cell value) {
			//determine type of cell by looking at the length
			int clen = value.length();
			if (clen>2) {
				String sval = value.toString();
				String sql = "insert into item_name_has_value ( item_id,name_id,vtype,sval ) values ( "
					+ item + "," + x + "," +T_STRING+ ",'"+ sval+ "')";
				int rc = c.exec(sql);
				if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
			} else {
				int ival = 0;
				if (clen==1) {
					ival = (int)value.toChar();
				} else if (clen==2) {
					ival = value.toInt();
				} else {
					//this should never happen
					throw new IllegalStateException("insertValue clen = "+clen);
				}
				String sql = "insert into item_name_has_value ( item_id,name_id,vtype,ival) values ( "
					+ item + "," + x + ","+vtype+","+ ival+")";
				int rc = c.exec(sql);
				if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
			}
			return c.last_insert_rowid();
		}

		/**
		* UpdateValue.
		* This changes the value, for example the field of an object.  You can't change the type.
		*/
		protected static int updateValue(Connection c,int rowid,Cell value) {
			//determine type of cell by looking at the length
			int clen = value.length();
			if (clen>2) {
				String sql = "update item_name_has_value set sval = '"+value.toString()+"' where id = "+rowid;
				int rc = c.exec(sql);
				if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
			} else {
				String sql = "update item_name_has_value set ival = '"+value.toInt()+"' where id = "+rowid;
				int rc = c.exec(sql);
				if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
			}
			return rowid;
		}

		protected static Cell selectValue(Connection c,int item,int x) {
			Cell value = null;
			String sql = "select ival,sval from item_name_has_value where item_id = "+item+" and name_id = "+x;
			Statement st = new Statement(c,sql);
			int ival = 0;
			String sval = null;
			if (st.step()) {
				ival=st.getInt(0);
				sval=st.getString(1);
				if (sval!=null && sval.length()>1) {
					value = new Cell(sval);
				} else {
					value = new Cell(ival);
				}
			}
			st.close();
			return value;
		}

		public static String dump() {
			StringBuilder sb = new StringBuilder();
			String sql = "select item_id,name_id,ival,sval from item_name_has_value order by item_id,name_id";
			Connection c = new Connection(dbname);
			Statement st = new Statement(c,sql);
			sb.append("item_name_has_value:\r\n");
			while(st.step()) {
				sb.append(st.getInt(0)+ "# "+st.getInt(1)+" = "+st.getInt(2)+","+st.getString(3)+"\r\n");
			}
			st.close();
			c.close();
			return sb.toString();
		}

		//protected static void updateStaticInt(Connection c, int nid,int ival) {
		//	String sql = "update name set static_ival = "+ival+" where nid = "+nid;
		//	int rc = c.exec(sql);
		//	if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
		//}

		//protected static void updateStaticString(Connection c,int nid,String sval) {
		//	//there may be a reason to set this to null, but I want to see the case for it
		//	if (sval==null) throw new IllegalArgumentException("String may not be null");
		//	String sql = "update name set static_sval = '"+sval+"' where nid = "+nid;
		//	int rc = c.exec(sql);
		//	if (rc!=0) {throw new IllegalStateException(rc+": "+sql);}
		//}

		/**
		* Static fields are stored in the static_value of the Name object
		*/
		//public static void putStatic(int fieldId,Cell value) {
		//	Connection c = new Connection(dbname);
		//	if (value.length()>2) {
		//		updateStaticString(c,fieldId,value.toString());
		//	} else {
		//		updateStaticInt(c,fieldId,value.toInt());
		//	}
		//	c.close();
		//}

		//is this a String or an int? Is there a better way of doing this?
		//public static Cell getStatic(int fieldId) {
		//	Connection c = new Connection(dbname);
		//	String sql = "select static_ival,static_sval from name where nid = "+fieldId;
		//	Statement st = new Statement(c,sql);
		//	Cell v = null;
		//	int ival = 0;
		//	String sval = null;
		//	if (st.step()) {
		//		ival=st.getInt(0);
		//		sval=st.getString(1);
		//		if (sval!=null && sval.length()>1) {
		//			v = new Cell(sval);
		//		} else {
		//			v = new Cell(ival);
		//		}
		//	}
		//	st.close();
		//	c.close();
		//	return v;
		//}

	} //end class ItemNameHasValue

	//============================================
	//methods directly on Database

	/**
	* This is a public method to be used outside the class.
	* vtype is the type of data, usually int or String.
	*/
	public static int store(int item,int x,int vtype,Cell value) {
			Connection c = new Connection(dbname);
			int id = ItemNameHasValue.findRow(c,item,x);
			if (id==0) {
				id=ItemNameHasValue.insertValue(c, item, x, vtype, value);
			} else {
				ItemNameHasValue.updateValue(c, id, value);
			}
			c.close();
			return id;
	}

	/**
	* load().  Returns Cell.  If there is no value in the database, this will be null
	* so check it.
	*/
	public static Cell load(int item,int x) {
			Connection c = new Connection(dbname);
			Cell v = ItemNameHasValue.selectValue(c,item,x);
			c.close();
			return v;
	}



	/**
	* put a value in a field. vType is optional and can be 0
	*/
	public static void putField(int objId,int fieldId,int vtype,Cell value) {
		store(objId,fieldId,vtype,value);
	}

	public static Cell getField(int objId,int fieldId) {
		return load(objId,fieldId);
	}

	/**
	* The static value is associated with the class not the object.
	*/
	public static void putStatic(int classId,int fieldId,int vtype,Cell value) {
		store(classId,fieldId,vtype,value);
	}

	public static Cell getStatic(int classId,int fieldId) {
		return load(classId,fieldId);
	}

	/**
	* aastore
	* iastore
	* Note the familiar method signature
	*/
	public static void arrayStore(int arrayId,int index,int vtype,Cell value) {
		store(arrayId,index,vtype,value);
	}

	/**
	* aaload
	* iaload
	* Note the familiar method signature
	*/
	public static Cell arrayLoad(int arrayId,int index) {
		return load(arrayId,index);
	}
	//==============================================

	// get a list of tables in the database.
	public String[] getTables() {
		Connection c = new Connection(dbname);
		String sql = "select count(*) from sqlite_master where type='table'";
		Statement s = new Statement(c,sql);
		int rows = 0;
		if (s.step()) {
			rows = s.getInt(0);
		}
		s.close();
		String[] tables = new String[rows];
		String sql2= "select name from sqlite_master where type='table'";
		Statement s2 = new Statement(c,sql2);
		int count = 0;
		while (s2.step()) {
			tables[count] = s.getString(0);
			count++;
		}
		s2.close();
		c.close();
		return tables;
	}

}