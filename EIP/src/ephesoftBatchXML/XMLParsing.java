package ephesoftBatchXML;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import servlets.SaveVendorInformation;

public class XMLParsing {
	
	private static String INPUT_ZIP_FILE = "";
	private static String BATCHXMLFILE = "";
    private static String OUTPUT_FOLDER = "";
    private static final String BATCH_LOCAL_PATH = "BatchLocalPath";
	private static final String BATCH_INSTANCE_ID = "BatchInstanceIdentifier";
	private static final String EXT_BATCH_XML_FILE = "_batch.xml";
	private static final String ZIP_FILE_EXT = ".zip";

	public static Document getBatchClassXmlDOM() {
		//Define a path to the Batch XML. 
		String filePath = SaveVendorInformation.BATCH_XML + "\\" + SaveVendorInformation.BATCH_XML.substring(SaveVendorInformation.BATCH_XML.lastIndexOf("\\") +1,SaveVendorInformation.BATCH_XML.length()) +"_batch.xml";
		BATCHXMLFILE =filePath;
		try 
        {                          
        	SAXBuilder sb = new SAXBuilder();
        	Document doc = sb.build(filePath);
            return doc;                 
        } catch (Exception x){}
        return null;
	}
	
	public static Document getBatchClassXmlDOMIfZipped() {
		//Define a path to the Batch XML. 
		String ZipfilePath = SaveVendorInformation.BATCH_XML + "\\" + SaveVendorInformation.BATCH_XML.substring(SaveVendorInformation.BATCH_XML.lastIndexOf("\\") +1,SaveVendorInformation.BATCH_XML.length()) +"_batch.xml.zip";
		String filePath = SaveVendorInformation.BATCH_XML + "\\" + SaveVendorInformation.BATCH_XML.substring(SaveVendorInformation.BATCH_XML.lastIndexOf("\\") +1,SaveVendorInformation.BATCH_XML.length()) +"_batch.xml";
		
		INPUT_ZIP_FILE = ZipfilePath;
		OUTPUT_FOLDER = SaveVendorInformation.BATCH_XML;
		
    	unZipIt(INPUT_ZIP_FILE,OUTPUT_FOLDER);
		
		try 
        {                          
        	SAXBuilder sb = new SAXBuilder();
        	Document doc = sb.build(filePath);
            return doc;                 
        } catch (Exception x){}
        return null;
	}
	
	
	static public void setVendorDLFs(String vendorName, String vendorNumber, String address, String city, String state, String zip, String phoneNumber, String webAddress)
	{	
		Document document = getBatchClassXmlDOM();		
		//if the batch xml is zipped 
		if(document == null){
			document = getBatchClassXmlDOMIfZipped();	
			//remove the unziped File 
			try{
	    		File file = new File(BATCHXMLFILE);
	    		if(file.delete()){			
	    		}else{
	    			System.out.println("Delete operation is failed.");
	    		}
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}
		}
		
		//Gets the document root element.
		Element docRoot = document.getRootElement();

		//Get and traverse through documents list.
		List<Element> docList = docRoot.getChild("Documents").getChildren("Document");		
		for (Element doc : docList)
		{
			//String variables containing information regarding the individual documents in the documents list.
			String docId = doc.getChildText("Identifier");	
			
			//go to the doc that has the correct doc id
			if(docId.equalsIgnoreCase(SaveVendorInformation.DOC_ID)) {
				
				//Get and traverse through document level fields list.
				List<Element> dlfList = doc.getChild("DocumentLevelFields").getChildren("DocumentLevelField");
				for (Element dlf: dlfList)
				{
					//String variables containing the name of the document level field, and the value in that field.
					//Note: If there is an empty tag in the batch xml, such as <Value/>, the text for that tag will be an empty string "", not null.                                       
	                String dlfValue = dlf.getChildText("Value");
	                String dlfFName = dlf.getChildText("Name");
	                
	                //VendorName
	                if (dlfFName.equals("VendorName")) {           
	                	Element valueTag = dlf.getChild("Value");
	                	if(valueTag ==null){
	                		Element value = new Element("Value");
	                		value.setText(vendorName);
	                		dlf.addContent(value);
	                	} else {
	                		valueTag.setText(vendorName);
	                	}
	                	
	                }
	                //vendorNumber
	                if (dlfFName.equals("VendorNumber")) {           
	                	Element valueTag = dlf.getChild("Value");
	                	if(valueTag ==null){
	                		Element value = new Element("Value");
	                		value.setText(vendorNumber);
	                		dlf.addContent(value);
	                	} else {
	                		valueTag.setText(vendorNumber);
	                	}
	                }
	                //VendorAddress
	                if (dlfFName.equals("VendorAddress")) {           
	                	Element valueTag = dlf.getChild("Value");
	                	if(valueTag ==null){
	                		Element value = new Element("Value");
	                		value.setText(address);
	                		dlf.addContent(value);
	                	} else {
	                		valueTag.setText(address);
	                	}
	                }
	                //VendorCity
	                if (dlfFName.equals("VendorCity")) {           
	                	Element valueTag = dlf.getChild("Value");
	                	if(valueTag ==null){
	                		Element value = new Element("Value");
	                		value.setText(city);
	                		dlf.addContent(value);
	                	} else {
	                		valueTag.setText(city);
	                	}
	                }
	                //VendorState
	                if (dlfFName.equals("VendorState")) {           
	                	Element valueTag = dlf.getChild("Value");
	                	if(valueTag ==null){
	                		Element value = new Element("Value");
	                		value.setText(state);
	                		dlf.addContent(value);
	                	} else {
	                		valueTag.setText(state);
	                	}
	                }
	                //VendorZip
	                if (dlfFName.equals("VendorZip")) {           
	                	Element valueTag = dlf.getChild("Value");
	                	if(valueTag ==null){
	                		Element value = new Element("Value");
	                		value.setText(zip);
	                		dlf.addContent(value);
	                	} else {
	                		valueTag.setText(zip);
	                	}
	                }
	                //VendorPhone
	                if (dlfFName.equals("VendorPhone")) {           
	                	Element valueTag = dlf.getChild("Value");
	                	if(valueTag ==null){
	                		Element value = new Element("Value");
	                		value.setText(phoneNumber);
	                		dlf.addContent(value);
	                	} else {
	                		valueTag.setText(phoneNumber);
	                	}
	                }
	                //VendorWebAddress
	                if (dlfFName.equals("VendorWebAddress")) {           
	                	Element valueTag = dlf.getChild("Value");
	                	if(valueTag ==null){
	                		Element value = new Element("Value");
	                		value.setText(phoneNumber);
	                		dlf.addContent(webAddress);
	                	} else {
	                		valueTag.setText(webAddress);
	                	}
	                }
			}
		}
	}
		writeToXML(document);
	}
	
	public static void unZipIt(String zipFile, String outputFolder){

	     byte[] buffer = new byte[1024];
	    	
	     try{
	    		
	    	//create output directory is not exists
	    	File folder = new File(OUTPUT_FOLDER);
	    	if(!folder.exists()){
	    		folder.mkdir();
	    	}
	    		
	    	//get the zip file content
	    	ZipInputStream zis = 
	    		new ZipInputStream(new FileInputStream(zipFile));
	    	//get the zipped file list entry
	    	ZipEntry ze = zis.getNextEntry();
	    		
	    	while(ze!=null){
	    			
	    	   String fileName = ze.getName();
	           File newFile = new File(outputFolder + File.separator + fileName);
	                
	           //System.out.println("file unzip : "+ newFile.getAbsoluteFile());
	                
	            //create all non exists folders
	            //else you will hit FileNotFoundException for compressed folder
	            new File(newFile.getParent()).mkdirs();
	              
	            FileOutputStream fos = new FileOutputStream(newFile);             

	            int len;
	            while ((len = zis.read(buffer)) > 0) {
	       		fos.write(buffer, 0, len);
	            }
	        		
	            fos.close();   
	            ze = zis.getNextEntry();
	    	}
	    	
	        zis.closeEntry();
	    	zis.close();
	    		
	    	//System.out.println("Done");
	    		
	    }catch(IOException ex){
	       ex.printStackTrace(); 
	    }
	   }    
	
	/**
	 * The <code>writeToXML</code> method will write the state document to the
	 * XML file.
	 * 
	 * @param document
	 *            {@link Document}.
	 */
	private static void writeToXML(Document document)
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
		//System.out.println("batchXMLZipPath************" + batchXMLZipPath);
		OutputStream outputStream = null;
		File zipFile = new File(batchXMLZipPath);
		FileWriter writer = null;
		XMLOutputter out = new XMLOutputter();
		try
		{
			if (zipFile.exists())
			{
				//System.out.println("Found the batch xml zip file.");
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
}