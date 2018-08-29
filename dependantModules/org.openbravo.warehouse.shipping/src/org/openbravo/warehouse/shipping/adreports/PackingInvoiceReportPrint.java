package org.openbravo.warehouse.shipping.adreports;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
/*import net.sf.jasperreports.engine.JRException;
 import net.sf.jasperreports.engine.JasperCompileManager;
 import net.sf.jasperreports.engine.JasperReport;
 import net.sf.jasperreports.engine.design.JasperDesign;
 import net.sf.jasperreports.engine.xml.JRXmlLoader;*/
import org.openbravo.base.exception.OBException;

public class PackingInvoiceReportPrint extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strMovementId = vars.getSessionValue("Goods_Movements_Invoice_Print.inpmMovementId");
      // GoodsMovementsInvoicePrint
      log4j.debug("strShippingId------------>" + strMovementId);
      if (strMovementId.equals(""))
        strMovementId = vars.getSessionValue("Goods_Movements_Invoice_Print.inpmMovementId");

      String trimmedmovementID = strMovementId.substring(2, strMovementId.length() - 2);

      InternalMovement movementObj = OBDal.getInstance().get(InternalMovement.class,
          trimmedmovementID);
      try {
        String reportName = "GoodsMovementReport_";
        if (movementObj != null) {
          if (movementObj.getObwshipUniqueno() != null) {
            reportName = reportName + movementObj.getObwshipUniqueno();
          } else {
            reportName = reportName + movementObj.getDocumentNo();

          }
          reportName = reportName + new Date();
          if (!movementObj.getSWMovementtypegm().equalsIgnoreCase("Saleable Fixture WH-WH"))
            throw new OBException(
                "Goods movement transaction can be printed only for records having Movement Type as 'Fixture movement WH to WH'");
          else
            printPagePartePDF(response, vars, strMovementId, reportName);
          // update the hsncode
          for (InternalMovementLine movementlineObj : movementObj
              .getMaterialMgmtInternalMovementLineList()) {
            if (movementlineObj.getOBWSHIPHSNCode() == null) {
              if (movementlineObj.getProduct() != null) {
                if (movementlineObj.getProduct().getIngstGstproductcode() != null) {
                  if (movementlineObj.getProduct().getIngstGstproductcode().getValue() != null) {
                    movementlineObj.setOBWSHIPHSNCode(movementlineObj.getProduct()
                        .getIngstGstproductcode().getValue());
                    OBDal.getInstance().save(movementlineObj);
                    OBDal.getInstance().flush();
                  }
                }
              }
            }
          }
        }
      } catch (Exception e) {
        throw new OBException(
            "Goods movement transaction can be printed only for records having Movement Type as 'Fixture movement WH to WH'");
      }
    } else
      pageError(response);
  }

  private void printPagePartePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strShippingId, String reportName) throws IOException, ServletException {

    String strBaseDesign = getBaseDesignPath(vars.getLanguage());
    HashMap<String, Object> parameters = new HashMap<String, Object>();

    /*
     * JasperReport jasperReportLines; try { JasperDesign jasperDesignLines =
     * JRXmlLoader.load(strBaseDesign +
     * "/org/openbravo/warehouse/shipping/adreports/FixtureInvoiceRe.jrxml"); jasperReportLines =
     * JasperCompileManager.compileReport(jasperDesignLines); } catch (JRException e) {
     * e.printStackTrace(); throw new ServletException(e.getMessage()); }
     */

    strShippingId = strShippingId.replaceAll("\\(|\\)|'", "");
    parameters.put("BASE_DESIGN", strBaseDesign);
    parameters.put("DOCUMENT_ID", strShippingId);
    renderJR(vars, response, null, "pdf", parameters, null, null);
  }

  public String getServletInfo() {
    return "Servlet that presents the RptMRequisitions seeker";
  } // End of getServletInfo() method
}