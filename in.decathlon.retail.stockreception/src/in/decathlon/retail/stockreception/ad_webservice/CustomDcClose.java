package in.decathlon.retail.stockreception.ad_webservice;

import in.decathlon.ibud.transfer.ad_process.EnhancedProcessGoods;
import in.decathlon.ibud.transfer.ad_process.Transfer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class CustomDcClose implements WebService {
  final private static Logger log4j = Logger.getLogger(Transfer.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String BoxDetailsFromWS = "";
    String strRead = "";
    String boxnumber = "";
    StringWriter sw = new StringWriter();
    int flag = 0;
    log4j.info("Calling Custom Dc Closing Webservice");
    try {
      InputStreamReader isReader = new InputStreamReader(request.getInputStream());
      BufferedReader bReader = new BufferedReader(isReader);
      StringBuilder strBuilder = new StringBuilder();
      strRead = bReader.readLine();
      while (strRead != null) {
        strBuilder.append(strRead);
        strRead = bReader.readLine();
        log4j.info("Parsing Success");
      }
      BoxDetailsFromWS = strBuilder.toString();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(BoxDetailsFromWS));
      Document document = builder.parse(is);

      boxnumber = document.getElementsByTagName("boxnumber").item(0).getChildNodes().item(0)
          .getNodeValue().toString();

      List<ShipmentInOut> inoutList = null;
      ShipmentInOut inoutObj;
      List<ShipmentInOut> ShipInOutList = new ArrayList<ShipmentInOut>();

      OBCriteria<ShipmentInOut> criteriashipmentinout = OBDal.getInstance().createCriteria(
          ShipmentInOut.class);
      criteriashipmentinout.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTSTATUS, "DR"));
      criteriashipmentinout.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, boxnumber));
      inoutList = criteriashipmentinout.list();

      Integer inoutCount1 = inoutList.size();
      if (inoutCount1 > 0) {
        for (ShipmentInOut InoutIterator : inoutList) {
          try {
            inoutObj = InoutIterator;
            System.out.println(inoutObj);
            ShipInOutList.add(inoutObj);
            EnhancedProcessGoods epg = new EnhancedProcessGoods();
            epg.processReceipt(inoutObj);
          } catch (Exception e) {

            e.printStackTrace();

          }
        }
      } else {
        log4j.info("Goods Receipt already closed for " + boxnumber);
      }

      if (flag == 0) {
        log4j.info("No Document Found for" + boxnumber + "number to close");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    final String xml = sw.toString();
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(response.toString());
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

}
