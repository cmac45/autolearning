<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="database.database"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="style.css">
    <title>AutoLearning</title>
   <script type="text/javascript" src="jquery-1.11.3.min.js"></script>
  </head>
  <body>
    
     <div class="header">
      <div class="logo">
       <a href="/dcma/home.html"><img src="images/ephesoft-logo.png" alt="Logo" name="img_logo" border="0" id="img_logo"height="61" ></a>
     </div>
     <div id="search_bar" class="search-section">
       <form id="search_bar_form" class="frmsearch" action="/EIP/autolearning.jsp">
        <input type="text" class="txtsearch" name="SearchVendor" placeholder="Search Vendor Name">
        <input type="submit" class="button" value="Search Here">
      </form>
     
<!--        <p>Select Batch Class</p>
        <select>
		  <option value="BC4">BC4</option>
		  <option value="BC5">BC5</option>
		</select>
		-->  
      </div>
    </div>
   
    
    
 

<%database db = new database();  %>

<%String recordBeginning = request.getParameter("recordBeginning");%>
<%String recordEnd = request.getParameter("recordEnd");%>


    <div id="dataheaderbar">
      <div class="dataElement">
       <h3>Total Rules Created</h3>
      <p><%String ruleCount = db.getNumberofRules(); out.print(ruleCount);%></p>
     </div>

     <div class="dataElement">
      <h3>New Rules Created In Last 30 Days</h3>
     <p><%out.print(db.getNumberofRulesLast30days());%></p>
    </div>
  </div>

 <div id="tablePadding">
  <div class="MiddelFullContainer">
  	<%String vendorSearch = request.getParameter("SearchVendor"); %>
  	<%if (vendorSearch !=null) {%>
      <h2 id="tableHeader">Search Results for "<%out.print(vendorSearch);%>"</h2>
      <%} else {%> 
    <h2 id="tableHeader">Auto Created Rules List</h2>
    <%} %>
    <br/>

    <table id="invoiceTableHead">
      <tr>    
        <th>Vendor Name</th>
        <th>Vendor ID</th>
        <th>Index Field</th>
        <th>Date Created</th>
        <th>Regex</th>
        <th>Action</th>
      </tr>
    </table>

    <div id ="inner_table">
    <table>
      <%if (vendorSearch !=null) {%>
      <%out.print(db.getVendorSeachResults(vendorSearch)); %>
      <%} else { %>
      <%out.print(db.getAutoLearnList(recordBeginning, recordEnd));%>
      <%} %>
    </table>
    </div>

    <br/>
    <br/>
    <div id="leftfloterElement">
      
       </div>
       <div id="rightfloterElement">
        <a href="/EIP/autolearning.jsp">First Page</a>
        Page <%out.print(db.getCurrentPage(recordEnd));%> of <%out.print(db.getNumberOfPages());%> <%out.print(db.getBackButton(recordBeginning, recordEnd)); %>|<%out.print(db.getNextButton(recordBeginning, recordEnd)); %>
       </div>
     </div>
  </div>
 

<div class="footer">
  <a class="link" tabindex="0" href="http://www.ephesoft.com">Powered
   by Ephesoft</a>
 </div>

  </body>
</html>