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
public class ScriptDocumentAssembler implements IJDomScript {

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
        //boolean write = true;
        if (null == documentFile) {
            System.out.println("Input document is null.");
        }
        Exception exception = null;

        // Method calls
        mergeDocs(documentFile);

        // Write the document object to the xml file. Currently following IF block is commented for performance improvement.
         /*if (write) {					
			writeToXML(document);
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

        Boolean[] mergeTypes = new Boolean[]{false, false, false, false, false};
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
            mergeDocsByPageNumber(document, mergeNonMatching);
            removeDocs(toRemove);
        }
        if (mergeTypes[3]) {
            mergeDocsByKVPage(document, mergeNonMatching);
            removeDocs(toRemove);
        }
        if (mergeTypes[4]) {
            mergeDocsByKVPageAV(document, mergeNonMatching);
            removeDocs(toRemove);
        }
    }

    // Merge Documents Based on KV Page Numbers and Alternate Values
    public void mergeDocsByKVPageAV(Document document, Boolean mergeNonMatching) {
        List<Element> currentList = new ArrayList<Element>();
        Element currentDoc = new Element("this");
        Element root = document.getRootElement();
        boolean match = false;
        int currentPage = 0;
        String pageNumber = "";

        List<Element> docList = getDocList(root);

        for (int doc = 0; doc < docList.size(); doc++) {
            List<Element> plfList = getFirstPLF(docList.get(doc));

            for (int plf = 0; plf < plfList.size(); plf++) {

                if (plfList.get(plf).getChildText("Name").equals("Page_Number") && doc == 0) {

                    // Code for using AlternateValues
                    plfList = getLastPLF(docList.get(doc));
                    List<Element> avList = getCurrentAVList(plfList);

                    currentDoc = (Element) docList.get(doc);
                    break;

                } else if (doc != 0 && plfList.get(plf).getChildText("Name").equals("Page_Number")) {

                    // Code for using AlternateValues
                    if (currentList.size() == 0) {
                        plfList = getLastPLF(docList.get(doc));
                        currentList = getCurrentAVList(plfList);
                        currentDoc = (Element) docList.get(doc);
                        break;
                    } else {

                        List<Element> avList = getAVList(plfList.get(plf));
                        match = consecutiveCompareList(currentList, avList);

                        if (match) {
                            if (!mergeNonMatching && !checkInvoiceMatch(currentDoc, docList.get(doc))) {

                                plfList = getLastPLF(docList.get(doc));
                                currentList = getCurrentAVList(plfList);
                                currentDoc = (Element) docList.get(doc);
                                break;
                            }

                            List<Element> tempList = docList.get(doc).getChild("Pages").getChildren("Page");
                            for (Element page : tempList) {
                                Element temp = (Element) page.clone();
                                currentDoc.getChild("Pages").addContent(temp);
                            }
                            plfList = getLastPLF(currentDoc);

                            toRemove.add(docList.get(doc));

                        } else {
                            plfList = getLastPLF(docList.get(doc));
                            currentList = getCurrentAVList(plfList);
                            currentDoc = (Element) docList.get(doc);
                        }
                    }
                }
            }
        }
    }

    // Method to set current avList
    public List<Element> getCurrentAVList(List<Element> plfList) {
        List<Element> avList = new ArrayList<Element>();
        for (int plf = 0; plf < plfList.size(); plf++) {
            if (plfList.get(plf).getChildText("Name").equals("Page_Number")) {
                return getAVList(plfList.get(plf));

            }
        }
        return avList;
    }

    // Method to get merge method from custom-merge-documents.properties file
    public Boolean[] getMergeTypes(Document document) {

        Boolean[] mergeTypes = new Boolean[5];

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
            mergeTypes[2] = Boolean.parseBoolean(prop.getProperty("MergeByPageNumber"));
            mergeTypes[3] = Boolean.parseBoolean(prop.getProperty("MergeByPageNumberUsingKV"));
            mergeTypes[4] = Boolean.parseBoolean(prop.getProperty("MergeByPageNumberUsingKVandAV"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mergeTypes;
    }

    // Method to compare if numbers in one list are consecutive to the other
    public boolean consecutiveCompareList(List<Element> list1, List<Element> list2) {

        for (Element value1 : list1) {
            for (Element value2 : list2) {
                if (Integer.parseInt(value1.getChildText("Value"))
                        == Integer.parseInt(value2.getChildText("Value")) - 1) {
                    return true;
                }
            }
        }

        return false;
    }

    // Merge Documents Based on KV Page Numbers extracted by KV_PAGE_PROCESS plugin and PageLevelField
    public void mergeDocsByKVPage(Document document, Boolean mergeNonMatching) {
        Element currentDoc = new Element("this");
        Element root = document.getRootElement();

        String pageNumber = "";
        int currentPage = 0;

        List<Element> docList = getDocList(root);

        for (int doc = 0; doc < docList.size(); doc++) {
            List<Element> plfList = getFirstPLF(docList.get(doc));

            for (int plf = 0; plf < plfList.size(); plf++) {

                if (plfList.get(plf).getChildText("Name").equals("Page_Number") && doc == 0) {

                    plfList = getLastPLF(docList.get(doc));
                    pageNumber = plfList.get(plf).getChildText("Value");
                    if (pageNumber.equals("")) {
                        pageNumber = "empty";
                    } else {

                        currentPage = Integer.parseInt(pageNumber);
                    }
                    currentDoc = (Element) docList.get(doc);
                } else if (doc != 0 && plfList.get(plf).getChildText("Name").equals("Page_Number")
                        && Integer.parseInt(plfList.get(plf).getChildText("Value")) == currentPage + 1) {
                    if (!mergeNonMatching && !checkInvoiceMatch(currentDoc, docList.get(doc))) {
                        plfList = getLastPLF(docList.get(doc));
                        pageNumber = plfList.get(plf).getChildText("Value");
                        currentPage = Integer.parseInt(pageNumber);
                        currentDoc = (Element) docList.get(doc);
                        break;
                    }

                    List<Element> tempList = docList.get(doc).getChild("Pages").getChildren("Page");
                    for (Element temp : tempList) {
                        Element temp2 = (Element) temp.clone();
                        currentDoc.getChild("Pages").addContent(temp2);
                    }

                    toRemove.add(docList.get(doc));
                } else if (doc != 0 && plfList.get(plf).getChildText("Name").equals("Page_Number")
                        && Integer.parseInt(plfList.get(plf).getChildText("Value")) != currentPage + 1) {
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

    public boolean checkInvoiceMatch(Element doc1, Element doc2) {
        
        boolean containsInvoice = false;
        List<Element> plfList1 = getLastPLF(doc1);
        List<Element> plfList2 = getFirstPLF(doc2);
        List<Element> avList1 = new ArrayList<Element>();
        List<Element> avList2 = new ArrayList<Element>();

        for (Element plf1 : plfList1) {
            if (plf1.getChildText("Name").equals("Invoice_Number")) {
                containsInvoice = true;
                avList1 = getAVList(plf1);
                if (avList1.size() == 0) {
                    return false;
                }

            }
            if (!containsInvoice) {
            return true;
        }
        }
        containsInvoice = false;
        for (Element plf2 : plfList2) {
            if (plf2.getChildText("Name").equals("Invoice_Number")) {
                containsInvoice = true;
                avList2 = getAVList(plf2);
                if (avList2.size() == 0) {
                    return false;
                }

            }
            if (!containsInvoice) {
            return true;
            }
        }
        

        return compareList(avList1, avList2);
    }

    // Merge Documents Based on Page Numbers using full page OCR
    public void mergeDocsByPageNumber(Document document, Boolean mergeNonMatching) {
        try {

            Element root = document.getRootElement();
            List<Element> currentList = new ArrayList<Element>();
            Element currentDoc = new Element("this");
            List<Element> docList = getDocList(root);
            SAXBuilder sax = new SAXBuilder();
            String patternString;
            int pageGroupCount = 0;
            int pageCount = 0;
            patternString = "(?<=\\s)(?i)((Page|pg\\.|pg|p\\.|p)\\s?0?\\d{1,2}\\s?(\\/|(of))\\s?0?\\d{1,2}|(Page|pg|p\\.|p)\\s?0?\\d{1,2})(?=\\s)";

            for (int doc = 0; doc < docList.size(); doc++) {
                List<Element> pageList = docList.get(doc).getChild("Pages").getChildren("Page");
                String hocr = pageList.get(0).getChildText("HocrFileName");

                String path = (root.getChildText("BatchLocalPath").trim() + File.separator
                        + root.getChildText("BatchInstanceIdentifier").trim()
                        + File.separator + hocr.trim());
                Document hocrDoc = sax.build(path);

                String hocrContent = hocrDoc.getRootElement().getChild("HocrPage").getChildText("HocrContent");
                List<String> matchList = regexer(patternString, hocrContent);

                if (matchList.size() == 1) { // Case for 1 page number (ex. page 1)
                    if (doc == 0) {
                        currentDoc = docList.get(doc);
                        pageCount = Integer.parseInt(matchList.get(0));
                    } else if (pageCount == Integer.parseInt(matchList.get(0)) - 1
                            && pageCount != 0) {
                        if (!mergeNonMatching && !checkInvoiceMatch(currentDoc, docList.get(doc))) {

                            hocr = pageList.get(pageList.size() - 1).getChildText("HocrFileName");
                            path = (root.getChildText("BatchLocalPath").trim() + File.separator
                                    + root.getChildText("BatchInstanceIdentifier").trim()
                                    + File.separator + hocr.trim());
                            hocrDoc = sax.build(path);
                            hocrDoc.getRootElement().getChild("HocrPage").getChildText("HocrContent");
                            matchList = regexer(patternString, hocrContent);
                            if (matchList.size() == 0) {
                                pageCount = 0;
                                pageGroupCount = 0;
                            } else if (matchList.size() == 1) {
                                pageGroupCount = 0;
                                pageCount = Integer.parseInt(matchList.get(0));
                            } else if (matchList.size() == 2) {
                                pageCount = Integer.parseInt(matchList.get(0));
                                pageGroupCount = Integer.parseInt(matchList.get(1));
                            }
                            continue;
                        }

                        pageCount = Integer.parseInt(matchList.get(0));
                        pageGroupCount = 0;
                        List<Element> temp = docList.get(doc).getChild("Pages").getChildren("Page");
                        Element tempClone = new Element("This");
                        for (int i = 0; i < temp.size(); i++) {
                            tempClone = (Element) temp.get(i).clone();
                            currentDoc.getChild("Pages").addContent(tempClone);
                        }
                        hocr = pageList.get(pageList.size() - 1).getChildText("HocrFileName");
                        path = (root.getChildText("BatchLocalPath").trim() + File.separator
                                + root.getChildText("BatchInstanceIdentifier").trim()
                                + File.separator + hocr.trim());
                        hocrDoc = sax.build(path);
                        hocrDoc.getRootElement().getChild("HocrPage").getChildText("HocrContent");
                        matchList = regexer(patternString, hocrContent);
                        if (matchList.size() == 0) {
                            pageCount = 0;
                            pageGroupCount = 0;
                        } else if (matchList.size() == 1) {
                            pageGroupCount = 0;
                            pageCount = Integer.parseInt(matchList.get(0));
                        } else if (matchList.size() == 2) {
                            pageCount = Integer.parseInt(matchList.get(0));
                            pageGroupCount = Integer.parseInt(matchList.get(1));
                        }
                        toRemove.add(docList.get(doc));

                    } else if (pageCount != Integer.parseInt(matchList.get(0)) - 1) {
                        pageCount = Integer.parseInt(matchList.get(0));
                        pageGroupCount = 0;
                        currentDoc = docList.get(doc);
                    } else if (pageCount == Integer.parseInt(matchList.get(0)) - 1
                            && pageCount == 0) {

                        currentDoc = docList.get(doc);
                        pageCount = Integer.parseInt(matchList.get(0));
                        pageGroupCount = 0;
                    } else {
                        pageCount = 0;
                        pageGroupCount = 0;
                    }

                } else if (matchList.size() == 2) { // Case for 2 page numbers (ex page 1 of 2)

                    if (doc == 0) {
                        currentDoc = docList.get(doc);
                        pageCount = Integer.parseInt(matchList.get(0));
                        pageGroupCount = Integer.parseInt(matchList.get(1));

                    } else if (pageCount == 0 && pageGroupCount == 0) {
                        currentDoc = docList.get(doc);
                        pageCount = Integer.parseInt(matchList.get(0));
                        pageGroupCount = Integer.parseInt(matchList.get(1));
                    } else if (pageCount == Integer.parseInt(matchList.get(0)) - 1
                            && pageGroupCount == Integer.parseInt(matchList.get(1))
                            && pageCount != 0) {

                        if (!mergeNonMatching && !checkInvoiceMatch(currentDoc, docList.get(doc))) {
                            hocr = pageList.get(pageList.size() - 1).getChildText("HocrFileName");
                            path = (root.getChildText("BatchLocalPath").trim() + File.separator
                                    + root.getChildText("BatchInstanceIdentifier").trim()
                                    + File.separator + hocr.trim());
                            hocrDoc = sax.build(path);
                            hocrDoc.getRootElement().getChild("HocrPage").getChildText("HocrContent");
                            matchList = regexer(patternString, hocrContent);
                            if (matchList.size() == 0) {
                                pageCount = 0;
                                pageGroupCount = 0;
                            } else if (matchList.size() == 1) {
                                pageGroupCount = 0;
                                pageCount = Integer.parseInt(matchList.get(0));
                            } else if (matchList.size() == 2) {
                                pageCount = Integer.parseInt(matchList.get(0));
                                pageGroupCount = Integer.parseInt(matchList.get(1));
                            }

                            continue;
                        }
                        pageCount = Integer.parseInt(matchList.get(0));

                        if (pageCount == pageGroupCount) {
                            pageCount = 0;
                            pageGroupCount = 0;
                        }

                        List<Element> temp = docList.get(doc).getChild("Pages").getChildren("Page");
                        Element tempClone = new Element("This");
                        for (int i = 0; i < temp.size(); i++) {
                            tempClone = (Element) temp.get(i).clone();
                            currentDoc.getChild("Pages").addContent(tempClone);
                        }
                        hocr = pageList.get(pageList.size() - 1).getChildText("HocrFileName");
                        path = (root.getChildText("BatchLocalPath").trim() + File.separator
                                + root.getChildText("BatchInstanceIdentifier").trim()
                                + File.separator + hocr.trim());
                        hocrDoc = sax.build(path);
                        hocrDoc.getRootElement().getChild("HocrPage").getChildText("HocrContent");
                        matchList = regexer(patternString, hocrContent);
                        if (matchList.size() == 0) {
                            pageCount = 0;
                            pageGroupCount = 0;
                        } else if (matchList.size() == 1) {
                            pageGroupCount = 0;
                            pageCount = Integer.parseInt(matchList.get(0));
                        } else if (matchList.size() == 2) {
                            pageCount = Integer.parseInt(matchList.get(0));
                            pageGroupCount = Integer.parseInt(matchList.get(1));
                        }
                        toRemove.add(docList.get(doc));
                    } else if (pageCount == Integer.parseInt(matchList.get(0)) - 1
                            && pageGroupCount != Integer.parseInt(matchList.get(1))
                            && pageCount == 0) {

                        currentDoc = docList.get(doc);
                        pageCount = Integer.parseInt(matchList.get(0));
                        pageGroupCount = Integer.parseInt(matchList.get(1));
                        if (pageCount == pageGroupCount) {
                            pageCount = 0;
                            pageGroupCount = 0;
                        }
                    }

                } else {
                    pageCount = 0;
                    pageGroupCount = 0;
                }

            }

        } catch (JDOMException ex) {
            Logger.getLogger(ScriptDocumentAssembler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ScriptDocumentAssembler.class.getName()).log(Level.SEVERE, null, ex);
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

    // Regex matcher for the HOCR content file
    private List<String> regexer(String patternString, String hocrContent) {
        List<String> matchList = new ArrayList<String>();

        Pattern sourcePattern = Pattern.compile(patternString);
        Matcher matcher = sourcePattern.matcher(hocrContent);

        if (matcher.find()) {
            String match = matcher.group();
            boolean test = false;
            boolean next = false;
            String numString1 = "";
            String numString2 = "";

            for (int i = 0; i < match.length(); i++) {
                int temp = Character.getNumericValue(match.charAt(i));
                if (temp < 0 && temp > 9 && numString1.equals("")) {

                } else if (temp >= 0 && temp <= 9 && numString1.equals("")) {
                    numString1 += match.charAt(i);
                    test = true;
                } else if (temp < 0 && temp > 9 && !numString1.equals("")) {
                    if (test && !next) {
                        numString1 += match.charAt(i);
                    } else if (!test && next) {
                        numString2 += match.charAt(i);
                        test = true;
                    } else if (test && next) {
                        numString2 += match.charAt(i);
                    }
                } else {
                    test = false;
                    next = true;
                }
            }
            if (!numString1.equals("")) {
                matchList.add(numString1);
            }
            if (!numString2.equals("")) {
                matchList.add(numString2);
            }

        }

        return matchList;
    }

    //Main method for testing. Uncomment for testing
    public static void main(String args[]) {
        String filePath = "C:\\Ephesoft\\SharedFolders\\ephesoft-system-folder\\BI1E\\docass\\BI1E_batch.xml";
        try {
            SAXBuilder sb = new SAXBuilder();
            Document doc = sb.build(filePath);
            ScriptDocumentAssembler sda = new ScriptDocumentAssembler();
            sda.execute(doc, null, null);
        } catch (Exception x) {
            System.out.println(x);
            PrintStream s = null;
            x.printStackTrace(s);
        }
    }
}
