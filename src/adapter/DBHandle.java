package adapter;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
// import org.springframework.core.serializer.Serializer;

public class DBHandle implements Serializable {

  public static final String DB_HANDLE_FILE = "db_handle.ser";

  private static final transient Logger logger = Logger.getLogger(DBHandle.class.getName());

  private static transient Connection connPortfolio_;
  private static final String SQL_DRIVER = "jdbc:sqlserver://eessql.gss.scotia-capital.com:5150";
  private static final String SQL_LOGIN = "dmamso";
  private static final String SQL_PASSWORD = "abc1234$6";

  public static Map<String, String> nameMap = new HashMap<String, String>();

  /** SQL QUERIES */
  private static final String SQL_FULL_NAME =
      "SELECT [ID_EXCH_SYMBOL], [RIC], [LONG_COMP_NAME] FROM [SecurityMaster].[dbo].[IEST_MSD_BASE] WHERE (RIC LIKE '%.TO' OR RIC LIKE '%.V') ORDER BY RIC";

  static {
    try {
      logger.info("Loading DBHandle...");
      Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
      connPortfolio_ = DriverManager.getConnection(SQL_DRIVER, SQL_LOGIN, SQL_PASSWORD);
      loadCache();
      logger.info("Loading DBHandle - complete");
    } catch (Exception e) {
      logger.warn(e);
    }
  }

  public static void loadCache() throws IllegalStateException {
    logger.info("Loading params and prices...");
    try {
      Statement stmt = connPortfolio_.createStatement();

      // load full names

      logger.info("Loading full names");
      ResultSet rsResultSet = stmt.executeQuery(SQL_FULL_NAME);
      Map<String, String> tempName = new HashMap<String, String>();
      while (rsResultSet.next()) {
        String ricSym = rsResultSet.getString(2);
        String fullName = rsResultSet.getString(3);
        tempName.put(ricSym, fullName);
      }
      nameMap = tempName;
    } catch (SQLException sqle) {
      logger.warn("Failed loading cached TSX params from SQL!", sqle);
    }
  }

  public static String getAltRIC(String ric) {
    String altRIC = "";
    try {
      Statement cmd = connPortfolio_.createStatement();
      String cmdString =
          "SELECT [RIC] FROM [SecurityMaster].[dbo].[IEST_MSD_BASE] WHERE (([EQY_PRIM_EXCH] = 'CT' AND [REUTERS_SUFFIX] = 'TO') OR ([EQY_PRIM_EXCH] = 'CV' AND [REUTERS_SUFFIX] = 'V') OR ([EQY_PRIM_EXCH] = 'UN' AND [REUTERS_SUFFIX] = 'N') OR ([EQY_PRIM_EXCH] = 'UP' AND [REUTERS_SUFFIX] = 'P') OR ([EQY_PRIM_EXCH] = 'UQ' AND [REUTERS_SUFFIX] = 'O') OR ([EQY_PRIM_EXCH] = 'UR' AND [REUTERS_SUFFIX] = 'O') OR ([EQY_PRIM_EXCH] = 'UW' AND [REUTERS_SUFFIX] = 'O') OR ([EQY_PRIM_EXCH] = 'UA' AND [REUTERS_SUFFIX] = 'A')) AND [RIC] IS NOT NULL AND [ID_EXCH_SYMBOL] IS NOT NULL AND [ID_CUSIP]= (SELECT [ID_CUSIP] FROM [SecurityMaster].[dbo].[IEST_MSD_BASE] WHERE (([EQY_PRIM_EXCH] = 'CT' AND [REUTERS_SUFFIX] = 'TO') OR ([EQY_PRIM_EXCH] = 'CV' AND [REUTERS_SUFFIX] = 'V') OR ([EQY_PRIM_EXCH] = 'UN' AND [REUTERS_SUFFIX] = 'N') OR ([EQY_PRIM_EXCH] = 'UP' AND [REUTERS_SUFFIX] = 'P') OR ([EQY_PRIM_EXCH] = 'UQ' AND [REUTERS_SUFFIX] = 'O') OR ([EQY_PRIM_EXCH] = 'UR' AND [REUTERS_SUFFIX] = 'O') OR ([EQY_PRIM_EXCH] = 'UW' AND [REUTERS_SUFFIX] = 'O') OR ([EQY_PRIM_EXCH] = 'UA' AND [REUTERS_SUFFIX] = 'A')) AND [RIC] IS NOT NULL AND [ID_EXCH_SYMBOL] IS NOT NULL and RIC = "
              + "'"
              + ric
              + "')"
              + " and RIC != "
              + "'"
              + ric
              + "'";
      ResultSet rs = cmd.executeQuery(cmdString);
      while (rs.next()) {
        // ... get column values from this record
        altRIC = rs.getString("RIC");
      }
    } catch (SQLException s) {
      logger.warn(s);
    }
    return altRIC;
  }
}
