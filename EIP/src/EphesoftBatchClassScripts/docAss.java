package EphesoftBatchClassScripts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import com.ephesoft.dcma.script.IJDomScript;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.input.SAXBuilder;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.jdom.JDOMException;

/**
 * The <code>ScriptDocumentAssembler</code> class represents the script execute
 * structure. Writer of scripts plug-in should implement this IScript interface
 * to execute it from the scripting plug-in. Via implementing this interface
 * writer can change its java file at run time. Before the actual call of the
 * java Scripting plug-in will compile the java and run the new class file.
 *
 * @author Ephesoft
 * @version 1.0
 */
public class docAss implements IJDomScript {
//public class ScriptDocumentAssembler implements IJDomScript {

    private static String BATCH_LOCAL_PATH = "BatchLocalPath";
    private static String BATCH_INSTANCE_ID = "BatchInstanceIdentifier";
    private static String EXT_BATCH_XML_FILE = "_batch.xml";
    private static String ZIP_FILE_EXT = ".zip";
    private List<Element> toRemove = new ArrayList<Element>();

    /**
     * The <code>execute</code> method will execute the script written by the
     * writer at run time with new compilation of java file. It will execute the
     * java file dynamically after new compilation.
     *
     * @param document {@link Document}
     */
    public Object execute(Document documentFile, String methodName, String documentIdentifier) {
        System.out.println("*************  Inside ScriptDocumentAssembler scripts.");
        System.out.println("*************  Start execution of the ScriptDocumentAssembler scripts.");
        boolean write = false;
        if (null == documentFile) {
            System.out.println("Input document is null.");
        }
        Exception exception = null;

        classifyDocument(documentFile);
        mergeDocs(documentFile);
        // Method calls
        //mergeDocs(documentFile);

        // Write the document object to the xml file. Currently following IF block is commented for performance improvement.
         /*if (write) {					
			writeToXML(documentFile);
			System.out.println("*************  Successfully write the xml file for the ScriptDocumentAssembler scripts.");
		}*/
        System.out.println("*************  End execution of the ScriptDocumentAssembler scripts.");
        return exception;
    }

    //Remove documents
    public void removeDocs(List<Element> toremove) {
        for (Element rem : toremove) {
            rem.detach();
        }
    }

    public void mergeDocs(Document document) {

        Boolean[] mergeTypes = new Boolean[]{false, false, false};
        Boolean mergeNonMatching = false;
        mergeTypes = getMergeTypes(document);
        if (mergeTypes[0]) {
            mergeDocsByInvoice(document);
            removeDocs(toRemove);
        }
        if (mergeTypes[1]) {
            mergeNonMatching = true;
        }      
        if (mergeTypes[2]) {
        	//mergeDocsByKVPage(document);
            removeDocs(toRemove);
        }
    }
    
    
    //Classify Documents on Key Words. This method will classify the low confidant documents to the correct document.
    public void classifyDocument(Document document) {
    	 
         Element root = document.getRootElement();
         List<Element> docList = getDocList(root);

         for (int doc = 0; doc < docList.size(); doc++) {
             List<Element> plfList = getFirstPLF(docList.get(doc));
             
             Element currentDoc = docList.get(doc);
             Element docType = currentDoc.getChild("Type");
             Element docDescription = currentDoc.getChild("Description");
             Element docReviewed = currentDoc.getChild("Reviewed");
             
              //check if the doc is confidant or not
             boolean isDocConfident = true;
             Element confidenceTag = currentDoc.getChild("Confidence");
             double confidence = Double.parseDouble(confidenceTag.getText());
             double confidenceThreshold = Double.parseDouble(currentDoc.getChildText("ConfidenceThreshold"));
             if (confidence < confidenceThreshold){
            	 isDocConfident = false;
             }
             
             //if document is not confident continue 
             if(!isDocConfident) {
	             for (int plf = 0; plf < plfList.size(); plf++) {
	            	 Element currentPlf = plfList.get(plf);
	            	 if (currentPlf.getChildText("Name").equals("DOCTYPE_INVOICE")) {
	                	 Element plfValue = currentPlf.getChild("Value");
	                	 if (!plfValue.getText().equals("")){
	                		 docType.setText("INVOICE");
	                		 docDescription.setText("INVOICE");
							 System.out.println("Made it " + docType.getText() + doc);
							 //Add 12 points to the confidence score for finding an invoice key word
							 confidenceTag.setText( Double.toHexString(confidence + 12.0));
	                	 	}
	            	 	}
	                	 if (currentPlf.getChildText("Name").equals("DOCTYPE_CREDIT_MEMO")) {
		                	 Element CMplfValue = currentPlf.getChild("Value");
		                	 if (!CMplfValue.getText().equals("")){
		                		 docType.setText("CREDIT_MEMO");
		                		 docDescription.setText("CREDIT MEMO");
								 System.out.println("Made it " + docType.getText() + doc);
		                	 }
	                	 
	                	 } 
	             }
             }
         }
    	
    	
    }

    // Method to get merge properties from custom-merge-documents.properties file
    public Boolean[] getMergeTypes(Document document) {

        Boolean[] mergeTypes = new Boolean[3];

        Properties prop = new Properties();
        String blp = document.getRootElement().getChildText("BatchLocalPath");
        String bci = document.getRootElement().getChildText("BatchClassIdentifier");
        String path = blp + "\\..\\" + bci + "\\script-config\\custom-merge-documents.properties";
        InputStream in = null;
        try {
            in = new FileInputStream(path);
            prop.load(in);
            mergeTypes[0] = Boolean.parseBoolean(prop.getProperty("MergeByInvoice"));
            mergeTypes[1] = Boolean.parseBoolean(prop.getProperty("MergeNonMatchingInvoiceNumber"));
            mergeTypes[2] = Boolean.parseBoolean(prop.getProperty("MergeByPageNumberUsingKVAndAV"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mergeTypes;
    }

   

    
 // Merge Documents Based on KV Page Numbers extracted by KV_PAGE_PROCESS plugin and PageLevelField
    public void mergeDocsByKVPage(Document document) {
        Element currentDoc = new Element("this");
        Element root = document.getRootElement();

        String pageNumber = "";
        int currentPage = 0;

        List<Element> docList = getDocList(root);

        for (int doc = 0; doc < docList.size(); doc++) {
            List<Element> plfList = getFirstPLF(docList.get(doc));
            
            for (int plf = 0; plf < plfList.size(); plf++) {

                if (plfList.get(plf).getChildText("Name").equals("INVOICE_Page_Number")) {
                	
                    plfList = getLastPLF(docList.get(doc));
                    pageNumber = plfList.get(plf).getChildText("Value");
                    if (pageNumber.equals("")) {
                        pageNumber = "empty";
                    } else {

                        currentPage = Integer.parseInt(pageNumber);
                    }
                    currentDoc = (Element) docList.get(doc);
                } else if (doc != 0 && plfList.get(plf).getChildText("Name").equals("INVOICE_Page_Number") && Integer.parseInt(plfList.get(plf).getChildText("Value")) == currentPage + 1) {
                   
                    List<Element> tempList = docList.get(doc).getChild("Pages").getChildren("Page");
                    for (Element temp : tempList) {
                        Element temp2 = (Element) temp.clone();
                        System.out.println("parse for Page number");
                        currentDoc.getChild("Pages").addContent(temp2);
                    }

                    toRemove.add(docList.get(doc));
                } else if (doc != 0 && plfList.get(plf).getChildText("Name").equals("INVOICE_Page_Number") && Integer.parseInt(plfList.get(plf).getChildText("Value")) != currentPage + 1) {
                    currentDoc = (Element) docList.get(doc);
                    plfList = getLastPLF(docList.get(doc));
                    currentPage = Integer.parseInt(plfList.get(plf).getChildText("Value"));
                    pageNumber = plfList.get(plf).getChildText("Value");
                    if (pageNumber.equals("")) {
                        pageNumber = "empty";
                    }
                }

            }

        }

    }
    
    

    //Merge Documents Based on InvoiceNumber extracted from KV_PAGE_PROCESS plugin
    public void mergeDocsByInvoice(Document document) {

        List<Element> currentList = new ArrayList<Element>();
        Element currentDoc = new Element("this");
        Element root = document.getRootElement();
        boolean match = false;

        String invoiceNumber = "";

        List<Element> docList = getDocList(root);

        for (int doc = 0; doc < docList.size(); doc++) {
            List<Element> plfList = getFirstPLF(docList.get(doc));

            for (int plf = 0; plf < plfList.size(); plf++) {

                if (plfList.get(plf).getChildText("Name").equals("MERGE_Invoice_Number") && doc == 0) {

                    // Code for using AlternateValues
                    List<Element> avList = getAVList(plfList.get(plf));
                    if (avList.size() != 0) {
                        currentList = avList;
                    }
                    currentDoc = (Element) docList.get(doc);

                } else if (doc != 0 && plfList.get(plf).getChildText("Name").equals("MERGE_Invoice_Number")) {

                    // Code for using AlternateValues
                    if (currentList.size() == 0) {
                        currentList = getAVList(plfList.get(plf));
                        currentDoc = (Element) docList.get(doc);
                    } else {

                        List<Element> avList = getAVList(plfList.get(plf));
                        match = compareList(currentList, avList);

                        if (match) {

                            Element temp = docList.get(doc).getChild("Pages").getChild("Page");
                            Element temp2 = (Element) temp.clone();
                            currentDoc.getChild("Pages").addContent(temp2);
                            toRemove.add(docList.get(doc));

                        } else {
                            currentList = avList;
                            currentDoc = (Element) docList.get(doc);
                        }
                    }
                }
            }
        }
    }

    // Returns the list of documents
    public List<Element> getDocList(Element root) {
        List<Element> docList = new ArrayList();
        if (root.getChild("Documents") != null) {
            if (root.getChild("Documents").getChildren("Document") != null) {
                docList = root.getChild("Documents").getChildren("Document");
                return docList;
            } else {
                return docList;
            }
        } else {
            return null;
        }

    }

    // Returns the list of PageLevelFields from the last page of the documents
    public List<Element> getLastPLF(Element doc) {
        List<Element> pageList = doc.getChild("Pages").getChildren("Page");
        List<Element> plfList = pageList.get(pageList.size() - 1).getChild("PageLevelFields").getChildren("PageLevelField");

        return plfList;
    }

    // Return the list of PageLevelFields from the first page of the documents
    public List<Element> getFirstPLF(Element doc) {
        Element page = doc.getChild("Pages").getChild("Page");
        List<Element> plfList = page.getChild("PageLevelFields").getChildren("PageLevelField");
        return plfList;
    }

    // Returns a list of all the AlternateValues in a PageLevelField
    public List<Element> getAVList(Element plf) {

        if (!plf.getChildText("Value").equals("")) {
            return plf.getChild("AlternateValues").getChildren("AlternateValue");
        }

        return new ArrayList();
    }

    // Compare two lists and return true if there is a match, false if not
    public boolean compareList(List<Element> list1, List<Element> list2) {
        boolean matchFound = false;

        for (Element value1 : list1) {
            for (Element value2 : list2) {
                if (value1.getChildText("Value").equals(value2.getChildText("Value"))
                        && !value1.getChildText("Value").equals("")
                        && !value2.getChildText("Value").equals("")) {
                    matchFound = true;
                }
            }
        }
        return matchFound;
    }

    /**
     * The <code>writeToXML</code> method will write the state document to the
     * XML file.
     *
     * @param document {@link Document}.
     */
    private void writeToXML(Document document) {
        String batchLocalPath = null;
        List batchLocalPathList = document.getRootElement().getChildren(BATCH_LOCAL_PATH);
        if (null != batchLocalPathList) {
            batchLocalPath = ((Element) batchLocalPathList.get(0)).getText();
        }

        if (null == batchLocalPath) {
            System.err.println("Unable to find the local folder path in batch xml file.");
            return;
        }

        String batchInstanceID = null;
        List batchInstanceIDList = document.getRootElement().getChildren(BATCH_INSTANCE_ID);
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
        XMLOutputter out = new com.ephesoft.dcma.batch.encryption.util.BatchInstanceXmlOutputter(batchInstanceID);
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

   

    //Main method for testing. Uncomment for testing
    public static void main(String args[]) {
        String filePath = "C:\\Ephesoft\\SharedFolders\\ephesoft-system-folder\\BI20\\docass\\BI20_batch.xml";
        try {
            SAXBuilder sb = new SAXBuilder();
            Document doc = sb.build(filePath);
            docAss sda = new docAss();
            sda.execute(doc, null, null);
        } catch (Exception x) {
            System.out.println(x);
            PrintStream s = null;
            x.printStackTrace(s);
        }
    }
}
