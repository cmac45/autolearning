package servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import database.dbProperties;

/**
 * Servlet implementation class ruleImageDownload
 */
@WebServlet("/ruleImageDownload")
public class ruleImageDownload extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ruleImageDownload() {
        super();
        // TODO Auto-generated constructor stub http://localhost:8090/EIP/ruleImageDownload/?id=BI6_Amount_68486371789561.png
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");  
		PrintWriter out = response.getWriter();  
		String filename = request.getParameter("id");
		String batchClass = request.getParameter("BCid");
		
		dbProperties dbProp = new dbProperties();
		String pathToEphesoft = dbProp.getPropertyValue("EphesoftSharedFolders");
		
		String filepath = pathToEphesoft +"\\"+batchClass+"\\AutoLearnFiles\\";   
		response.setContentType("APPLICATION/OCTET-STREAM");   
		response.setHeader("Content-Disposition","attachment; filename=\"" + filename + "\"");   
		  
		FileInputStream fileInputStream = new FileInputStream(filepath + filename);  
		            
		int i;   
		while ((i=fileInputStream.read()) != -1) {  
		out.write(i);   
		}   
		fileInputStream.close();   
		out.close();   
		}  
		
	}



