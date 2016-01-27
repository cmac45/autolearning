package servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import database.database;
import ephesoftBatchXML.XMLParsing;

/**
 * Servlet implementation class SaveVendorInformation
 */
@WebServlet("/SaveVendorInformation")
public class SaveVendorInformation extends HttpServlet {
	
	public static String BATCH_XML = "";
	public static String DOC_ID = "";
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SaveVendorInformation() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
		
		String xml = request.getParameter("batch_xml_path");
		String DOCID = request.getParameter("document_id");
		
		BATCH_XML=xml;
		DOC_ID=DOCID;
		
		response.sendRedirect("NewVendor.html"); 
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//doGet(request, response);
		
		String vendorName = request.getParameter("vendorName");
		//Vendor Number will be a random number for now
		//String vendorNumber = request.getParameter("vendorNumber");
		String address = request.getParameter("address");
		String city = request.getParameter("city");
		String state = request.getParameter("state");
		String zip = request.getParameter("zip");
		String phoneNumber = request.getParameter("phoneNumber");
		String webAddress = request.getParameter("webAddress");
		
		String batchClassID = request.getParameter("bcid");
		
		//random Vendor number
	    Random rand = new Random();
	    int randomNum = rand.nextInt((99999 - 10000) + 1) + 10000;
		
		String vendorNumber = Integer.toString(randomNum);
		
		database db = new database();
		try {
			db.putValuesIntoAutoLearnDB(batchClassID, vendorName, vendorNumber, address, city, state, zip, phoneNumber, webAddress);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
		XMLParsing.setVendorDLFs(vendorName, vendorNumber, address, city, state, zip, phoneNumber, webAddress);

		response.getWriter().println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
		response.getWriter().println("<html>");
		response.getWriter().println("  <head>");
		response.getWriter().println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">");
		response.getWriter().println("    <title>AutoLearning</title>");
		response.getWriter().println("  </head>");
		response.getWriter().println("  <body>");
		response.getWriter().println("    <div id=\"dataheaderbar\">");
		response.getWriter().println("      <div class=\"dataElement\">");
		response.getWriter().println("      <h2 id=\"tableHeader\">Vendor Successfully Created</h2>");
		response.getWriter().println("    </div>");
		response.getWriter().println("  </div>");
		response.getWriter().println("");
		response.getWriter().println(" <div id=\"tablePadding\">");
		response.getWriter().println("  <div class=\"MiddelFullContainer\">");
		response.getWriter().println("      <h3>Vendor Name: "+ vendorName +"</h3>");
		response.getWriter().println("      <br>");
		response.getWriter().println("      <h3>Vendor Number: "+ vendorNumber +"</h3>");
		response.getWriter().println("      <br>");
		response.getWriter().println("      <h3>Address: "+ address +"</h3>");
		response.getWriter().println("      <br>");
		response.getWriter().println("      <h3>City: "+ city +"</h3>");
		response.getWriter().println("      <br>");
		response.getWriter().println("      <h3>State: "+ state +"</h3>");
		response.getWriter().println("      <br>");
		response.getWriter().println("      <h3>Zip: "+ zip +"</h3>");
		response.getWriter().println("      <br>");
		response.getWriter().println("      <h3>Phone: "+ phoneNumber +"</h3>");
		response.getWriter().println("      <br>");
		response.getWriter().println("      <h3>Web Address: "+ webAddress +"</h3>");
		response.getWriter().println("     </div>");
		response.getWriter().println("  </div>");
		response.getWriter().println(" ");
		response.getWriter().println("<div class=\"footer\">");
		response.getWriter().println("  <a class=\"link\" tabindex=\"0\" href=\"http://www.ephesoft.com\">Powered");
		response.getWriter().println("   by Ephesoft</a>");
		response.getWriter().println(" </div>");
		response.getWriter().println("  </body>");
		response.getWriter().println("</html>");
		
		
	}

}
