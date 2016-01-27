package database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;

//Class used to post html data to the autolearning.jsp page
public class database {
	
	//Used in the vendor search to return a query results list
	public String getVendorSeachResults(String search) throws SQLException, JDOMException, IOException
	{
		Statement stmt = null;
		Connection dbConnection = null;	
		try {
			dbConnection = getDBConnection();
				String query = "Select VendorName, VendorID, IndexFieldLearned, DateCreated, Regex, BatchClassID, ImageID From autoLearning Where VendorName LIKE '%"+ search +"'";
					//Run the Select statement
					stmt = dbConnection.createStatement();
					//System.out.println(query);			
					ResultSet rs = stmt.executeQuery(query);
					ResultSetMetaData meta = rs.getMetaData();
					StringBuilder html = new StringBuilder(14);
					int colCount = meta.getColumnCount();
					String BatchClassID ="";					
					while (rs.next())
					{
						html.append("<tr id=\"copy\"></td>");
					    for (int col=1; col <= colCount; col++) 
					    {
					        Object value = rs.getObject(col);
					        if (value != null) 
					        {	
					        	if(col==6) {
					        		BatchClassID=value.toString();
					        	}
					        	//get image file name	
					        	if(col==7){
					        	html.append("<td><a href=\"/EIP/ruleImageDownload?id="+ value.toString()+"&BCid="+BatchClassID +"\" target=\"_blank\">Inspect Rule</a> <a href=\"/EIP/deleteAutoLearningRule?id="+ value.toString()+ "&BCid="+BatchClassID+"\"><input type=\"button\" value=\"Delete Rule\" /></a></td>");		
					        	}else if (col!=6){
					        	html.append("<td>"+value.toString()+"</td>");			  
					        	}
					        }
					    }html.append("</tr>");
					}
					return html.toString();				
		} catch (SQLException e ) {
			System.err.print(e);
		} finally {
			if (stmt != null) { stmt.close(); }
		}
		return null;
	}

	//Default search list of all vendors
	public String getAutoLearnList(String pageNumberBiginning, String pageNumberEnd) throws SQLException, JDOMException, IOException
	{
		Statement stmt = null;
		Connection dbConnection = null;	
		//Check for Pagination
		if (pageNumberBiginning == null || pageNumberEnd == null) {
			pageNumberBiginning ="1";
			pageNumberEnd = "25";
		}
		try {
			dbConnection = getDBConnection();
				String query = "SELECT  VendorName, VendorID, IndexFieldLearned, DateCreated, Regex, BatchClassID, ImageID FROM    ( SELECT    ROW_NUMBER() OVER ( ORDER BY VendorID ) AS RowNum, * FROM autoLearning WHERE VendorID >= 0 ) AS RowConstrainedResult WHERE   RowNum >= " + pageNumberBiginning + "AND RowNum < "+ pageNumberEnd +" ORDER BY RowNum;";
					//Run the Select statement
					stmt = dbConnection.createStatement();
					//System.out.println(query);								
					ResultSet rs = stmt.executeQuery(query);
					ResultSetMetaData meta = rs.getMetaData();
					StringBuilder html = new StringBuilder(14);
					int colCount = meta.getColumnCount();
					String BatchClassID ="";					
					while (rs.next())
					{
						html.append("<tr id=\"copy\"></td>");
					    for (int col=1; col <= colCount; col++) 
					    {
					        Object value = rs.getObject(col);
					        if (value != null) 
					        {	
					        	if(col==6) {
					        		BatchClassID=value.toString();
					        	}
					        	//get image file name	
					        	if(col==7){
					        	html.append("<td><a href=\"/EIP/ruleImageDownload?id="+ value.toString()+"&BCid="+BatchClassID +"\" target=\"_blank\">Inspect Rule</a> <a href=\"/EIP/deleteAutoLearningRule?id="+ value.toString()+ "&BCid="+BatchClassID+"\"><input type=\"button\" value=\"Delete Rule\" /></a></td>");		
					        	}else if (col!=6){
					        	html.append("<td>"+value.toString()+"</td>");			  
					        	}
					        }
					    }html.append("</tr>");
					}
					
					return html.toString();			
					
					
		} catch (SQLException e ) {
			System.err.print(e);
		} finally {
			if (stmt != null) { stmt.close(); }
		}
		return null;
	}

	//Check the to see how many pages for pagination
	public String getNumberOfPages() throws SQLException, JDOMException, IOException
	{
		Statement stmt = null;
		Connection dbConnection = null;	
		try {
			dbConnection = getDBConnection();	
				String query = "SELECT COUNT(*) FROM autoLearning;";
					//Run the Select statement
					stmt = dbConnection.createStatement();
					//System.out.println(query);					
					ResultSet rs = stmt.executeQuery(query);
					String html = "";
					while (rs.next())
					{
						html= rs.getString(1);
					}
					int rowCount = Integer.parseInt(html);
					double pagecount = rowCount / 25.00;
					int pageCountRoundupIntCiel = (int) Math.ceil(pagecount);
					return Integer.toString(pageCountRoundupIntCiel);
								
		} catch (SQLException e ) {
			System.err.print(e);
		} finally {
			if (stmt != null) { stmt.close(); }
		}
		return null;
	}
	
	
	//Get the total number of rules in the auto learning database
	public String getNumberofRules() throws SQLException, JDOMException, IOException
	{	
		Statement stmt = null;
		Connection dbConnection = null;	
		try {
			dbConnection = getDBConnection();	
				String query = "SELECT COUNT(*) FROM autoLearning;";
					//Run the Select statement
					stmt = dbConnection.createStatement();
					//System.out.println(query);			
					ResultSet rs = stmt.executeQuery(query);
					String html = "";
					while (rs.next())
					{
						html= rs.getString(1);
					}
					return html;
		} catch (SQLException e ) {
			System.err.print(e);
		} finally {
			if (stmt != null) { stmt.close(); }
		}
		return null;
	}
	
	//Number of rules created in the last 30 days for info display
	public String getNumberofRulesLast30days() throws SQLException, JDOMException, IOException
	{
		Statement stmt = null;
		Connection dbConnection = null;	
		try {
			dbConnection = getDBConnection();	
				String query = "SELECT COUNT(*) FROM autoLearning WHERE CONVERT(date, DateCreated) >= CONVERT(date, getdate()) AND  CONVERT(date, DateCreated) <DATEADD(DAY,+30,GETDATE())";
					//Run the Select statement
					stmt = dbConnection.createStatement();
					//System.out.println(query);									
					ResultSet rs = stmt.executeQuery(query);
					String html = "";
					while (rs.next())
					{
						html= rs.getString(1);						
					}
					return html;
		} catch (SQLException e ) {
			System.err.print(e);
		} finally {
			if (stmt != null) { stmt.close(); }
		}
		return null;
	}
	
	//Sql statement to delete a reocrd
	public String deleteRecord(String imageName) throws SQLException, JDOMException, IOException
	{
		Statement stmt = null;
		Connection dbConnection = null;	
		try {
			dbConnection = getDBConnection();	
				String query = "DELETE FROM autoLearning WHERE ImageID='"+imageName +"'";
					//Run the Select statement
					stmt = dbConnection.createStatement();
					//System.out.println(query);									
					ResultSet rs = stmt.executeQuery(query);
					String html = "";
					while (rs.next())
					{
						html= rs.getString(1);						
					}
					return html;		
		} catch (SQLException e ) {
			System.err.print(e);
		} finally {
			if (stmt != null) { stmt.close(); }
		}
		return null;
	}

	//Html for pagination 
	public String getNextButton(String pageNumberBiginning, String pageNumberEnd)
	{
		String html = "<a href=\"/EIP/autolearning.jsp\">Next</a>";
		if (pageNumberBiginning == null || pageNumberEnd == null) {
			pageNumberBiginning ="1";
			pageNumberEnd = "25";
			html = "<a href=\"/EIP/autolearning.jsp?recordBeginning=26&recordEnd=50\">Next</a>";
		}	
		int addtoPageBiginning = Integer.parseInt(pageNumberEnd)+1;
		int addtoPageEnd = Integer.parseInt(pageNumberEnd)+25;
		html = "<a href=\"/EIP/autolearning.jsp?recordBeginning="+addtoPageBiginning+"&recordEnd="+addtoPageEnd+"\">Next</a>";
		return html;
	}
	
	//Html for pagination 
	public String getBackButton(String pageNumberBiginning, String pageNumberEnd)
	{
		String html = "<a href=\"/EIP/autolearning.jsp\">Back</a>";
		if (pageNumberBiginning == null || pageNumberEnd == null) {
			pageNumberBiginning ="1";
			pageNumberEnd = "25";
			html = "";
		}
		if (pageNumberBiginning.equals("1") || pageNumberEnd.equals("25")) {
			pageNumberBiginning ="1";
			pageNumberEnd = "25";
			html = "";
		} else {
		
		int addtoPageBiginning = Integer.parseInt(pageNumberBiginning)-25;
		int addtoPageEnd = Integer.parseInt(pageNumberEnd)-25;

		html = "<a href=\"/EIP/autolearning.jsp?recordBeginning="+addtoPageBiginning+"&recordEnd="+addtoPageEnd+"\">Back</a>";
		}
		return html;
	}

	//Used to find the current page the user is on
	public String getCurrentPage(String pageNumberEnd)
	{
		String html = "";
		if (pageNumberEnd == null) {
			
			html = "1";
		} else {
		
		int currentPage = Integer.parseInt(pageNumberEnd) /25;

		html = ""+currentPage;
		}
		
		return html;
	}
	
	public void putValuesIntoAutoLearnDB(String BatchClass, String VendorName, String VendorID, String Address, String City, String State, String Zip, String phone, String webaddress) throws  SQLException, IOException
	{
	Connection dbConnection = null;
	Statement statement = null;
	try
	{	
		dbConnection = getDBConnection();
		statement = dbConnection.createStatement();
		
        String queryString = "  INSERT INTO VendorInformation([BatchClass],[VendorName],[VendorID],[Address],[City],[State],[Zip],[phone],[webaddress],[DateCreated]) VALUES('"+BatchClass+"','"+ VendorName +"','"+ VendorID +"','"+ Address +"','"+ City +"','"+ State +"','"+ Zip +"','"+ phone +"','"+ webaddress +"',GETDATE());";
        statement.executeUpdate(queryString);
			
		}
		catch (SQLException e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			if (statement  != null)
			{
				statement.close();
			}
		}
	}
	
	
	//SQL connection interface
	private static Connection getDBConnection() {
		dbProperties prop = new dbProperties();
		prop.getPropertyValue("DBUserName");
		String SQL_USER = prop.getPropertyValue("DBUserName");
		String SQL_PASSWORD = prop.getPropertyValue("DBPassword");
		String DATABASE_NAME = prop.getPropertyValue("DBDatabaseName");
		String PORTNUMBER = prop.getPropertyValue("DBDatabasePort");
		String SERVERNAME = prop.getPropertyValue("DBServerName");
		String MSSQL_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
		String MSSQL_CONNECTION_STRING = "jdbc:jtds:sqlserver://" + SERVERNAME + ":" + PORTNUMBER + "/" + DATABASE_NAME;
		Connection dbConnection = null;
		try {
			Class.forName(MSSQL_DRIVER);
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
		try {
			dbConnection = DriverManager.getConnection(MSSQL_CONNECTION_STRING, SQL_USER, SQL_PASSWORD);
			return dbConnection;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return dbConnection;
	}	
}
