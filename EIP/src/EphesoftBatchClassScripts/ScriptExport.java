package EphesoftBatchClassScripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import com.ephesoft.dcma.script.IJDomScript;

public class ScriptExport implements IJDomScript {

	private static final String BATCH_LOCAL_PATH = "BatchLocalPath";
	private static final String BATCH_INSTANCE_ID = "BatchInstanceIdentifier";
	private static final String EXT_BATCH_XML_FILE = "_batch.xml";
	private static String ZIP_FILE_EXT = ".zip";
	private static Properties INVOICE_PROPERTIES = null;
	private static Properties EIP_PROPERTIES = null;
	private static String PATH_TO_INV_PROPERTIES_FILE = "";
	private static String SQL_USER;
	private static String SQL_PASSWORD;
	private static String DATABASE_NAME; 
	private static String PORTNUMBER; 
	private static String SERVERNAME;
	private static String MSSQL_DRIVER; 
	private static String MSSQL_CONNECTION_STRING;
	private static String[] FIELDS_TO_AUTOLEARN;

	public Object execute(Document document, String methodName, String docIdentifier) {
		Exception exception = null;
		try {
			System.out.println("*************  Inside ExportScript scripts.");

			System.out.println("*************  Start execution of the ExportScript scripts.");

			
			documentTypeDecision(document);
			
			
			if (null == document) {
				System.out.println("Input document is null.");
				return null;

			}
			System.out.println("*************  End execution of the ScriptExport scripts.");
		} catch (Exception e) {
			System.out.println("*************  Error occurred in scripts." + e.getMessage());
			exception = e;
		}
		return null;
	}

	
	/**
	 * Code to Add Batch Level Information to Auto Learning database 
	 */
	
	private void documentTypeDecision(Document document) throws SQLException
	{
		//Get the property information for db connection 
		getpropertyFileInformationEIP();
		
		//Gets the document root element.
		Element docRoot = document.getRootElement();
		
		
		//String variables containing information regarding the current batch class excuting this script.
		String batchClassId = docRoot.getChildText("BatchClassIdentifier");
		String batchLocalPath = docRoot.getChildText("BatchLocalPath");	
		String batchInstanceId = docRoot.getChildText("BatchInstanceIdentifier");
   
		//get the batch class properties
		PATH_TO_INV_PROPERTIES_FILE = batchLocalPath.substring(0, batchLocalPath.lastIndexOf('\\')) + "\\"+ batchClassId +"\\script-config\\InvoiceBatchClass.properties";
		
		
		getBatchClassProperties(batchLocalPath, batchClassId);
		
		//Get and traverse through documents list.
		List<Element> docList = docRoot.getChild("Documents").getChildren("Document");
		for (Element doc : docList)
		{
			//String variables containing information regarding the individual documents in the documents list.
			String docId = doc.getChildText("Identifier");
			String docType = doc.getChildText("Type");	
			
			//Check if doc is Invoice
			if (docType.equals("INVOICE"))
			{
				//put the Invoice document level fields into auto learning db.
				try {
					putValuesIntoAutoLearnDB(doc,batchClassId,batchLocalPath, batchInstanceId);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	//This method is used to get the regex from the properties file since regex slashes are not escaped 
	public static String getRgexfromPropFile (String dlfName) throws IOException{
		
		BufferedReader br = new BufferedReader(new FileReader(PATH_TO_INV_PROPERTIES_FILE));
		try {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		        if (line.startsWith(dlfName)) {
		        	return line.substring(line.indexOf('=')+1, line.length());
		        }
		    }
		} finally {
		    br.close();
		}
		return null;
	}
	
	
	
	private void getBatchClassProperties(String pathToBatch, String batchClassID) {
		String pathToBCPropertiesFile = null;
		 
		pathToBCPropertiesFile = pathToBatch.substring(0, pathToBatch.lastIndexOf('\\')) + "\\"+ batchClassID +"\\script-config\\InvoiceBatchClass.properties";
   
		Properties prop = new Properties();
		InputStream input = null;
		try
		{
			input = new FileInputStream(pathToBCPropertiesFile);
			prop.load(input);
			INVOICE_PROPERTIES = prop;
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		
		FIELDS_TO_AUTOLEARN = getPropertyBCPropValue("AutoLearningFields").split("\\;");
	}
		
	//get the property file information contained in the EIP auto learning module
	private void getpropertyFileInformationEIP() {

		String pathToEPIPropertiesFile = null;
		 Map<String, String> env = System.getenv();
	        for (String envName : env.keySet()) {
	           if (envName.equalsIgnoreCase("DCMA_HOME")) {
	        	
	        	pathToEPIPropertiesFile = env.get(envName).substring(0, env.get(envName).lastIndexOf('\\')) + "\\JavaAppServer\\webapps\\EIP\\WEB-INF\\classes\\database\\EIP-Prop.properties";
	        	pathToEPIPropertiesFile= pathToEPIPropertiesFile.replaceAll("(\\\\)\\\\", "$1");
	           }
	        }
		Properties prop = new Properties();
		InputStream input = null;
		try
		{
			input = new FileInputStream(pathToEPIPropertiesFile);
			prop.load(input);
			EIP_PROPERTIES = prop;
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	        setPropertie();
	}
	
	public static String getPropertyEIPValue(String propertykey)
	{
		String propertyValue = EIP_PROPERTIES.getProperty(propertykey);
		return propertyValue;
	}

	public static String getPropertyBCPropValue(String propertykey)
	{
		String propertyValue = INVOICE_PROPERTIES.getProperty(propertykey);
		return propertyValue;
	}
	
	public static void setPropertie() {
		
		SQL_USER = getPropertyEIPValue("DBUserName");
		SQL_PASSWORD = getPropertyEIPValue("DBPassword");
		DATABASE_NAME = getPropertyEIPValue("DBDatabaseName");
		PORTNUMBER = getPropertyEIPValue("DBDatabasePort");
		SERVERNAME = getPropertyEIPValue("DBServerName");
		MSSQL_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
		MSSQL_CONNECTION_STRING = "jdbc:jtds:sqlserver://" + SERVERNAME + ":" + PORTNUMBER + "/" + DATABASE_NAME;		
	}
		
	private void putValuesIntoAutoLearnDB(Element Ephedoc, String BatchClassID, String BatchLocalPath, String batchInstanceId) throws SQLException, IOException
	{
	Connection dbConnection = null;
	Statement statement = null;
	try
	{	
		dbConnection = getDBConnection();
		statement = dbConnection.createStatement();
		//Get and traverse through document level fields list.
		List<Element> dlfList = Ephedoc.getChild("DocumentLevelFields").getChildren("DocumentLevelField");
		for (Element dlf: dlfList)
			{
				//String variables containing the name of the document level field, and the value in that field.
				//Note: If there is an empty tag in the batch xml, such as <Value/>, the text for that tag will be an empty string "", not null. 
				String dlfName = dlf.getChildText("Name");    
				
				//check to see if dlf is in the learning config
				if (Arrays.asList(FIELDS_TO_AUTOLEARN).contains(dlfName)){
					String confidance = dlf.getChildText("Confidence");
					//check to see if the field has been extracted with Key Value or if clicked by user
					if(confidance.equals("0.0")) {
						
						String VendorName = getVendorName(dlfList);
						String VendorID = getVendorID(dlfList);
						
						//Check if Rule has been Learned already
						Boolean isRuleLearned = checkIfRuleIsLearned(BatchClassID, VendorID, dlfName);
						if (isRuleLearned == false) {		
			                
			                int x0 = 0;
			                int y0 = 0;
			                int x1 = 0;
			                int y1 = 0;
			                String pageNum = "";
			 
			                Element CordList = dlf.getChild("CoordinatesList");
			                if (CordList !=null) {
				                Element Cord = CordList.getChild("Coordinates");
				                x0 = Integer.parseInt(Cord.getChild("x0").getText());
				                y0 = Integer.parseInt(Cord.getChild("y0").getText());
				                x1 = Integer.parseInt(Cord.getChild("x1").getText());
				                y1 = Integer.parseInt(Cord.getChild("y1").getText());
				                pageNum = dlf.getChild("Page").getText();
			                }
			                //Create the Image for the Auto Learning field
			                String imageName =createAutoLearningImage(BatchLocalPath, batchInstanceId, pageNum, dlfName, x0 , y0 ,x1 ,y1 ,BatchClassID);
			                String queryString = "INSERT INTO autoLearning([VendorName],[VendorID],[IndexFieldLearned],[DateCreated],[Regex],[ImageID],[BatchClassID],[FieldValueX0],[FieldValueY0],[FieldValueX1],[FieldValueY1]) VALUES('"+ VendorName +"','"+ VendorID +"','"+ dlfName +"',GETDATE(),'"+ getRgexfromPropFile(dlfName)+"','"+ imageName +"','"+BatchClassID+"',"+ x0 +","+ y0 +","+ x1 +","+ y1 +")";
			                System.out.println(queryString);
			                statement.executeUpdate(queryString);
						} 
					}
				}
			}
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

	//loops throught the list of DLF fields returns the Vendor Name
	private static String getVendorName(List<Element> dlfList) {
		String vendorName ="";
		for (Element dlf: dlfList) {
			String dlfName = dlf.getChildText("Name");
			if (dlfName.equals("VendorName")){
				vendorName=dlf.getChildText("Value");
				return vendorName;
			}				
		}
		return null;
	}
	
	//loops throught the list of DLF fields returns the Vendor Name
		private static String getVendorID(List<Element> dlfList) {		
			String vendorID ="";
			for (Element dlf: dlfList) {
				String dlfName = dlf.getChildText("Name");
				if (dlfName.equals("VendorNumber")){
					vendorID=dlf.getChildText("Value");
					return vendorID;
				}				
			}
			return null;
		}

	//Create connection to Auto Learning DB
	private static Connection getDBConnection() {
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
		

	//create Auto Learn image for auto learning rule set
	private String createAutoLearningImage(String BatchLocalPath, String batchInstanceId, String pageNum, String DlfName, int x0, int y0, int x1, int y1, String BatchClassID) {
		String PathToImageStore = BatchLocalPath.substring(0,BatchLocalPath.lastIndexOf("\\")+1) + BatchClassID +"\\AutoLearnFiles\\";
		
		String pathToEPIPropertiesFile = null;
		 Map<String, String> env = System.getenv();
	        for (String envName : env.keySet()) {
	           if (envName.equalsIgnoreCase("DCMA_HOME")) {	        	
	        	pathToEPIPropertiesFile = env.get(envName).substring(0, env.get(envName).lastIndexOf('\\'));	
	           }
	        }
	        String IMpath = pathToEPIPropertiesFile.replace("\\\\", "\\") + "Dependencies\\ImageMagick\\";
	        
		 
		 long time = System.nanoTime();        
         String command = IMpath +"convert " +  BatchLocalPath + "\\"+ batchInstanceId +"\\" +batchInstanceId +"_"+ pageNum +"_displayImage.png -strokewidth 0 -fill \"rgba( 255, 215, 0 , 0.5 )\" -draw \"rectangle " + x0 +"," + y0 + " " + x1 +"," + y1 + " \" " + PathToImageStore + batchInstanceId +"_"+ DlfName+ "_"+ time +".png";
              try {
                 Process p = Runtime.getRuntime().exec(command);
                 p.waitFor();
                 }   
         catch(IOException e1) {
                 e1.printStackTrace();
                 }
         catch(InterruptedException e2) {
                System.out.println(e2.getMessage());
                 }
		return batchInstanceId +"_"+ DlfName+ "_"+ time +".png";
	}
		
   
	
	public boolean checkIfRuleIsLearned(String BatchClassID, String VendorID, String dlfName)
	{
		Boolean isRuleCreacted = false;
		Statement stmt = null;
		Connection dbConnection = null;
		try {

			dbConnection = getDBConnection();
			String query = "SELECT COUNT(*) FROM autoLearning WHERE VendorID = '"+ VendorID +"' AND IndexFieldLearned = '"+dlfName+"' AND BatchClassID = '"+ BatchClassID +"'";
		
			//Run the Select statement
			stmt = dbConnection.createStatement();
			//System.out.println(query);					
			ResultSet rs = stmt.executeQuery(query);
			String ruleCount = "";
				while (rs.next())
				{
					ruleCount= rs.getString(1);
				}
			
				int rowCount = Integer.parseInt(ruleCount);
				if(rowCount >= 1){
					isRuleCreacted = true;
				}
				
			return isRuleCreacted;				
		} catch (SQLException e ) {
			System.err.print(e);
		} finally {
			if (stmt != null) { try {
				stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} }
		}
		return isRuleCreacted;
	}
	

	/**
	 * The <code>writeToXML</code> method will write the state document to the XML file.
	 * 
	 * @param document {@link Document}.
	 */
	private void writeToXML(Document document) {
		String batchLocalPath = null;
		List<?> batchLocalPathList = document.getRootElement().getChildren(BATCH_LOCAL_PATH);
		if (null != batchLocalPathList) {
			batchLocalPath = ((Element) batchLocalPathList.get(0)).getText();
		}

		if (null == batchLocalPath) {
			System.err.println("Unable to find the local folder path in batch xml file.");
			return;
		}

		String batchInstanceID = null;
		List<?> batchInstanceIDList = document.getRootElement().getChildren(BATCH_INSTANCE_ID);
		if (null != batchInstanceIDList) {
			batchInstanceID = ((Element) batchInstanceIDList.get(0)).getText();

		}

		if (null == batchInstanceID) {
			System.err.println("Unable to find the batch instance ID in batch xml file.");
			return;
		}

		String batchXMLPath = batchLocalPath.trim() + File.separator + batchInstanceID + File.separator + batchInstanceID
				+ EXT_BATCH_XML_FILE;

		String batchXMLZipPath = batchXMLPath + ZIP_FILE_EXT;

		System.out.println("batchXMLZipPath************" + batchXMLZipPath);

		OutputStream outputStream = null;
		File zipFile = new File(batchXMLZipPath);
		FileWriter writer = null;
		XMLOutputter out = new XMLOutputter();
		try {
			if (zipFile.exists()) {
				System.out.println("Found the batch xml zip file.");
				outputStream = getOutputStreamFromZip(batchXMLPath, batchInstanceID + EXT_BATCH_XML_FILE);
				out.output(document, outputStream);
			} else {
				writer = new java.io.FileWriter(batchXMLPath);
				out.output(document, writer);
				writer.flush();
				writer.close();
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static OutputStream getOutputStreamFromZip(final String zipName, final String fileName) throws FileNotFoundException,
			IOException {
		ZipOutputStream stream = null;
		stream = new ZipOutputStream(new FileOutputStream(new File(zipName + ZIP_FILE_EXT)));
		ZipEntry zipEntry = new ZipEntry(fileName);
		stream.putNextEntry(zipEntry);
		return stream;
	}

	public static void main(String args[])
	{
		String filePath = "C:\\Ephesoft\\SharedFolders\\ephesoft-system-folder\\BI1A\\BI1A_batch.xml";
		try
		{
			SAXBuilder sb = new SAXBuilder();
			Document doc = sb.build(filePath);
			ScriptExport se = new ScriptExport();
			se.execute(doc, null, null);
		}
		catch (Exception x)
		{
			System.out.println(x);
		}
	} 
}

