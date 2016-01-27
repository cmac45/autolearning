package EphesoftBatchClassScripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

/**
 * The <code>ScriptEtractionr</code> class represents the script execute
 * structure. Writer of scripts plug-in should implement this IScript interface
 * to execute it from the scripting plug-in. Via implementing this interface
 * writer can change its java file at run time. Before the actual call of the
 * java Scripting plug-in will compile the java and run the new class file.
 * 
 * 
 * @version 1.0
 */

public class ScriptExtraction implements IJDomScript
{
	private static final String BATCH_LOCAL_PATH = "BatchLocalPath";
	private static final String BATCH_INSTANCE_ID = "BatchInstanceIdentifier";
	private static final String EXT_BATCH_XML_FILE = "_batch.xml";
	private static final String ZIP_FILE_EXT = ".zip";
	private static Properties INVOICE_PROPERTIES = null;
	private static Properties EIP_PROPERTIES = null;
	private static String SQL_USER;
	private static String SQL_PASSWORD;
	private static String DATABASE_NAME; 
	private static String PORTNUMBER; 
	private static String SERVERNAME;
	private static String MSSQL_DRIVER; 
	private static String MSSQL_CONNECTION_STRING;
	private static String[] FIELDS_TO_AUTOLEARN ;

	@Override
	public Object execute(Document document, String methodName, String docIdentifier)
	{
		Exception exception = null;
		try
		{
			System.out.println("*************  Inside ScriptExtraction scripts.");
			System.out.println("*************  Start execution of the ScriptExtraction scripts.");
			
			if (null == document)
			{
				System.out.println("Input document is null.");
				return exception;
			}
			
			getpropertyFileInformationEIP();
			documentTypeDecision(document);
			
			
			writeToXML(document);
			
			System.out.println("*************  End execution of the ScriptExtraction scripts.");
		}
		catch (Exception e)
		{
			System.out.println("*************  Error occurred in scripts." + e.getMessage());
			e.printStackTrace();
			exception = e;
		}
		return exception;
	}
	
	//Auto Learning Extraction
	
	private void documentTypeDecision(Document document) throws SQLException, IOException, JDOMException
	{
		//Gets the document root element.
		Element docRoot = document.getRootElement();
		
		
		//String variables containing information regarding the current batch class excuting this script.
		String batchClassId = docRoot.getChildText("BatchClassIdentifier");
		String batchLocalPath = docRoot.getChildText("BatchLocalPath");	
		String batchInstanceId = docRoot.getChildText("BatchInstanceIdentifier");
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
				//get the Invoice document level fields auto learning db.
				getValuesFromAutoLearnDB(doc, batchClassId, batchLocalPath, batchInstanceId);
			}
		}
	}
	
	
	private void getValuesFromAutoLearnDB(Element Ephedoc, String batchClassId, String batchLocalPath, String batchID) throws SQLException, IOException, JDOMException 
	{
		Statement stmt = null;
		Connection dbConnection = null;
		try {

			dbConnection = getDBConnection();
			//Get and traverse through document level fields list.
			List<Element> dlfList = Ephedoc.getChild("DocumentLevelFields").getChildren("DocumentLevelField");
			for (Element dlf: dlfList)
			{
				//String variables containing the name of the document level field, and the value in that field.
				//Note: If there is an empty tag in the batch xml, such as <Value/>, the text for that tag will be an empty string "", not null. 
				String dlfName = dlf.getChildText("Name");                                              	                
				
				//check to see if dlf is in the learning config
				if (Arrays.asList(FIELDS_TO_AUTOLEARN).contains(dlfName)){
					
					//check to see if the field is blank
					if(dlf.getChildText("Value")==null) {
						
						//String VendorName = getVendorName(dlfList);
						String VendorID = getVendorID(dlfList);
						String query = "SELECT * FROM autoLearning WHERE VendorID = '"+ VendorID +"' AND IndexFieldLearned = '"+dlfName+"'";
						
						//info from DB	
						int FieldValueX0 =0;
						int FieldValueY0 =0;
						int FieldValueX1 =0;
						int FieldValueY1 =0;
						String regexString = ".+";

						//Run the Select statement
						stmt = dbConnection.createStatement();
						//System.out.println(query);
	
						ResultSet rs = stmt.executeQuery(query);
						while (rs.next()) {
							FieldValueX0 = rs.getInt("FieldValueX0");
							FieldValueY0 = rs.getInt("FieldValueY0");
							FieldValueX1 = rs.getInt("FieldValueX1");
							FieldValueY1 = rs.getInt("FieldValueY1");
							regexString = rs.getString("Regex");
						}
						
						String valueFromHOCRFile ="null";
						
						//get the first page of the document
						Element Pages = Ephedoc.getChild("Pages");
						Element firstPage = Pages.getChild("Page");
						String firstPageid = firstPage.getChildText("HocrFileName");
						String hocrPath = batchLocalPath + "\\" + batchID + "\\" + firstPageid;

						valueFromHOCRFile = getValueFromCoords(hocrPath,FieldValueX0,FieldValueY0,FieldValueX1,FieldValueY1);
						//Remove the space at the end of the string
						if (!valueFromHOCRFile.isEmpty()){
						valueFromHOCRFile = valueFromHOCRFile.substring(0,valueFromHOCRFile.length()-1);
						}
						//System.out.println("+++++++++++++++++++++ " + hocrPath+ " " +FieldValueX0+ " " +FieldValueY0+ " " +FieldValueX1+ " " +FieldValueY1 );
						//System.out.println("+++++++++++++++VALUE: " + valueFromHOCRFile );
					

					//Regex Matching 
				      // Create a Pattern object
				      Pattern r = Pattern.compile(regexString);
	
				      // Now create matcher object.
				      Matcher m = r.matcher(valueFromHOCRFile);
				      if (m.find( )) {
				         System.out.println("Found value: " + m.group(0) +" MATCHES WITH " + regexString);
				         valueFromHOCRFile = m.group(0);
				      } else {
				         System.out.println("NO MATCH REGEX MATCH ON " + VendorID + dlfName );
				         valueFromHOCRFile="";
				      }
						
					//Put New Data in the DLF Fields	    	        
					Element ValueTag = new Element ("Value");
					
					ValueTag.setText(valueFromHOCRFile);
					dlf.addContent(ValueTag);

					Element CoordinatesList = new Element("CoordinatesList");
					Element Coordinates = new Element("Coordinates");
					Element x0 = new Element("x0");
					x0.setText(Integer.toString(FieldValueX0));
					Element y0 = new Element("y0");
					y0.setText(Integer.toString(FieldValueY0));
					Element x1 = new Element("x1");
					x1.setText(Integer.toString(FieldValueX1));
					Element y1 = new Element("y1");
					y1.setText(Integer.toString(FieldValueY1));

					Coordinates.addContent(x0);
					Coordinates.addContent(y0);
					Coordinates.addContent(x1);
					Coordinates.addContent(y1);

					CoordinatesList.addContent(Coordinates);
					dlf.addContent(CoordinatesList);

					//Get first Page of the doc and add it to document level field
					Element pageTag = new Element ("Page");
					pageTag.setText(firstPage.getChildText("Identifier"));
					dlf.addContent(pageTag);
					
					Element OCRConfidance = dlf.getChild("OcrConfidence");
					
					//set confidence to 100 if we find a match
					OCRConfidance.setText("100.0");
					}
				}

			}
		} catch (SQLException e ) {
			System.err.print(e);
		} finally {
			if (stmt != null) { stmt.close(); }
		}
	}
			
			
	public String getValueFromCoords(String hocrPath, int x0, int y0, int x1, int y1) throws JDOMException, IOException
	{
		// adding buffer regions to location zone
		int bufferPX_X = 30;
		int bufferPX_Y = 20;
		x0 = x0 - bufferPX_X;
		x1 = x1 + bufferPX_X;
		y0 = y0 - bufferPX_Y;
		y1 = y1 + bufferPX_Y;

		String value = "";
		SAXBuilder sb = new SAXBuilder();
		Document hocrDoc = sb.build(hocrPath);
		Element hocrRoot = hocrDoc.getRootElement();

		List<Element> spans = hocrRoot.getChild("HocrPage").getChild("Spans").getChildren("Span");
		for(Element span : spans)
		{
			Element coordinates = span.getChild("Coordinates");
			int hocrX0 = Integer.valueOf(coordinates.getChildText("x0"));
			int hocrY0 = Integer.valueOf(coordinates.getChildText("y0"));
			int hocrX1 = Integer.valueOf(coordinates.getChildText("x1"));
			int hocrY1 = Integer.valueOf(coordinates.getChildText("y1"));

			if(hocrX0 >= x0 + 20 && hocrX1 <= x1 )
			{
				if(hocrY0 >= y0 && hocrY1 <= y1)
				{
					value += span.getChildText("Value") + " ";
				}
			}
		}
		return value;
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
	
	public static void setPropertie() {

		SQL_USER = getPropertyEIPValue("DBUserName");
		SQL_PASSWORD = getPropertyEIPValue("DBPassword");
		DATABASE_NAME = getPropertyEIPValue("DBDatabaseName");
		PORTNUMBER = getPropertyEIPValue("DBDatabasePort");
		SERVERNAME = getPropertyEIPValue("DBServerName");
		MSSQL_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
		MSSQL_CONNECTION_STRING = "jdbc:jtds:sqlserver://" + SERVERNAME + ":" + PORTNUMBER + "/" + DATABASE_NAME;		
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

	/**
	 * The <code>writeToXML</code> method will write the state document to the
	 * XML file.
	 * 
	 * @param document
	 *            {@link Document}.
	 */
	private void writeToXML(Document document)
	{
		String batchLocalPath = null;
		Element batchLocalPathElement = document.getRootElement().getChild(BATCH_LOCAL_PATH);
		if (null != batchLocalPathElement)
		{
			batchLocalPath = batchLocalPathElement.getText();
		}
		if (null == batchLocalPath)
		{
			System.err.println("Unable to find the local folder path in batch xml file.");
			return;
		}
		String batchInstanceID = null;
		Element batchInstanceIDElement = document.getRootElement().getChild(BATCH_INSTANCE_ID);
		if (null != batchInstanceIDElement)
		{
			batchInstanceID = batchInstanceIDElement.getText();
		}
		if (null == batchInstanceID)
		{
			System.err.println("Unable to find the batch instance ID in batch xml file.");
			return;
		}
		String batchXMLPath = batchLocalPath.trim() + File.separator + batchInstanceID + File.separator + batchInstanceID + EXT_BATCH_XML_FILE;
		String batchXMLZipPath = batchXMLPath + ZIP_FILE_EXT;
		System.out.println("batchXMLZipPath************" + batchXMLZipPath);
		OutputStream outputStream = null;
		File zipFile = new File(batchXMLZipPath);
		FileWriter writer = null;
		XMLOutputter out = new XMLOutputter();
		try
		{
			if (zipFile.exists())
			{
				System.out.println("Found the batch xml zip file.");
				outputStream = getOutputStreamFromZip(batchXMLPath, batchInstanceID + EXT_BATCH_XML_FILE);
				out.output(document, outputStream);
			}
			else
			{
				writer = new java.io.FileWriter(batchXMLPath);
				out.output(document, writer);
				writer.flush();
				writer.close();
			}
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
		finally
		{
			if (outputStream != null)
			{
				try
				{
					outputStream.close();
				}
				catch (IOException e)
				{
				}
			}
		}
	}

	public static OutputStream getOutputStreamFromZip(final String zipName, final String fileName) throws FileNotFoundException, IOException
	{
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
			ScriptExtraction se = new ScriptExtraction();
			se.execute(doc, null, null);
		}
		catch (Exception x)
		{
			System.out.println(x);
		}
	} 
}
