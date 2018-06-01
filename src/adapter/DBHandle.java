package adapter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
//import org.springframework.core.serializer.Serializer;

import user.User;
import data.BlockDetails;
//import user.UserMap;
//import utils.DateUtils;
//import utils.Helpers;
import utils.SymbolConverter;
import blockdata.BlockMetric;
import blockdata.BlockTrade;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import data.Comment;
import data.CorporateEvent;

public class DBHandle implements Serializable
{

	public final static String DB_HANDLE_FILE = "db_handle.ser";
	
	private final static transient Logger logger = Logger.getLogger(DBHandle.class.getName());
	
	private transient Connection connPortfolio_;
	private static final String SQL_DRIVER = "jdbc:sqlserver://eessql.gss.scotia-capital.com:5150";
	private static final String SQL_LOGIN = "dmamso";
	private static final String SQL_PASSWORD = "abc1234$6";
	
	public Map<String, String> nameMap = new HashMap<String, String>();

	/**
	 * SQL QUERIES
	 */
	private final static String SQL_FULL_NAME = "SELECT [ID_EXCH_SYMBOL], [RIC], [LONG_COMP_NAME] FROM [SecurityMaster].[dbo].[IEST_MSD_BASE] WHERE (RIC LIKE '%.TO' OR RIC LIKE '%.V') ORDER BY RIC";
	
	public DBHandle() throws Exception
	{
		logger.info( "Loading DBHandle..." );
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		connPortfolio_ = DriverManager.getConnection(SQL_DRIVER, SQL_LOGIN, SQL_PASSWORD);
		loadCache();
		logger.info( "Loading DBHandle - complete" );
	}

	public void loadCache() throws IllegalStateException
	{
		logger.info( "Loading params and prices..." );
		try
		{
			Statement stmt = connPortfolio_.createStatement();	
			
			//load full names
			
			logger.info("Loading full names");
			ResultSet rsResultSet = stmt.executeQuery(SQL_FULL_NAME);
			Map<String, String> tempName = new HashMap<String, String>();
			while (rsResultSet.next()) {
				String ricSym = rsResultSet.getString(2);
				String fullName = rsResultSet.getString(3);
				tempName.put(ricSym, fullName);
			}
			this.nameMap = tempName;
		}
		catch(SQLException sqle) { 
			logger.warn("Failed loading cached TSX params from SQL!", sqle);
		}
	}
	
}

