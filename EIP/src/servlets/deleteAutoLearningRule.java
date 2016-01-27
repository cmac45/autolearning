package servlets;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.JDOMException;

import ImageServices.autoLearnImages;
import database.database;

/**
 * Servlet implementation class deleteAutoLearningRule
 */
@WebServlet("/deleteAutoLearningRule")

public class deleteAutoLearningRule extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public deleteAutoLearningRule() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
		
		System.out.println(request.getRemoteUser().toString()); 
		
		String imageName = request.getParameter("id");
		String BatchClassID = request.getParameter("BCid");
		
		//Remove the record from the DB
		database db = new database();
		try {
			db.deleteRecord(imageName);
		} catch (SQLException | JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Delete Image from Learn folders directory
		autoLearnImages.deleteImage(imageName, BatchClassID);
		
		response.sendRedirect("autolearning.jsp"); 
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
