package com.promantia.supply.gstcustomizations.adreports;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;








import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

public class GsInvoiceReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strShippingId = vars.getSessionValue("GS_InvoiceReport.inpobwshipShippingId");
      // GS_InvoiceReport
      log4j.debug("strShippingId------------>" + strShippingId);
      if (strShippingId.equals(""))
        strShippingId = vars.getSessionValue("GS_InvoiceReport.inpobwshipShippingId");
      // strcBpartnerId = vars.getSessionValue("PrintRfQ.inpcBpartnerId");
      printPagePartePDF(response, vars, strShippingId);
    } else
      pageError(response);
  }

  private void printPagePartePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strShippingId) throws IOException, ServletException {
    
	  
	  
	  
    String strBaseDesign = getBaseDesignPath(vars.getLanguage());
    HashMap<String, Object> parameters = new HashMap<String, Object>();

    JasperReport jasperReportLines;
    try {
 /*JasperCompileManager.compileReportToFile(strBaseDesign
                + "/com/promantia/supply/gstcustomizations/adreports/InvoiceRe.jrxml");
*/
     JasperDesign jasperDesignLines = JRXmlLoader.load(strBaseDesign
          + "/com/promantia/supply/gstcustomizations/adreports/InvoiceRe.jrxml");
      jasperReportLines = JasperCompileManager.compileReport(jasperDesignLines); 
    } catch (JRException e) {
      e.printStackTrace();
      throw new ServletException(e.getMessage());
    }

    strShippingId = strShippingId.replaceAll("\\(|\\)|'", "");
    parameters.put("BASE_DESIGN", strBaseDesign);
    parameters.put("DOCUMENT_ID", strShippingId);
    renderJR(vars, response, null, "pdf", parameters, null, null);
    
    
    
    
  }

  public String getServletInfo() {
    return "Servlet that presents the RptMRequisitions seeker";
  } // End of getServletInfo() method
  
  
  
}