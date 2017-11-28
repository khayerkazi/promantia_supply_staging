package in.decathlon.supply.stockreception.ad_webservice;

import in.decathlon.integration.PassiveDB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class GetAllBoxDetails implements WebService {
  static Logger log4j = Logger.getLogger(GetAllBoxDetails.class);
  private static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

  private static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String BoxDetailsFromWS = "";
    String strRead = "";
    String orgname = "";
    String boxnumber = "";
    StringWriter sw = new StringWriter();
    String hql = "";
    String GetDate = "";
    String productHql = "";
    String departmentHql = "";
    int flag = 0;
    Connection con;
    ResultSet getDateResultSet = null;
    ResultSet getBoxDetailsResultSet = null;
    ResultSet getDepartmentResultSet = null;
    log4j.info("Entering Into ScanWhole box Part");
    JSONObject jsonDataObject = new JSONObject();
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
      orgname = document.getElementsByTagName("orgname").item(0).getChildNodes().item(0)
          .getNodeValue().toString();
      boxnumber = document.getElementsByTagName("boxnumber").item(0).getChildNodes().item(0)
          .getNodeValue().toString();

      con = PassiveDB.getInstance().getSupplyDBConnection();
      PreparedStatement pst = null;
      log4j.info("Search by Date");
      GetDate = "select (header.packdate+1-1) as startdate ,(header.packdate+1) as enddate from obwpack_packingh header,obwpack_box box, c_bpartner partner,OBWPACK_Movlinebox prod, c_orderline col,c_order co where box.obwpack_packingh_id=header.obwpack_packingh_id and partner.c_bpartner_id=header.c_bpartner_id and col.c_orderline_id=prod.c_orderline_id and co.c_order_id=col.c_order_id and prod.obwpack_box_id =box.obwpack_box_id and (box.em_sra_boxstatus is null OR box.em_sra_boxstatus <>'CO') and co.docstatus in ('IBDO_SH','IBDO_PSH','CL') and partner.name=?  and box.trackingNo=? group by header.packdate";
      pst = con.prepareStatement(GetDate);
      pst.setString(1, orgname);
      pst.setString(2, boxnumber);
      getDateResultSet = pst.executeQuery();
      if (getDateResultSet.next()) {
        log4j.info("Quering all info based on Date");
        Date startDate = getDateResultSet.getDate("startdate");
        Date endDate = getDateResultSet.getDate("enddate");
        log4j.info("Starting date:" + startDate);
        log4j.info("End Date:" + endDate);
        // hql =
        // "select box.trackingNo as trackingno ,count(distinct(prod.m_product_id)) as quantity,co.documentNo as documentno,header.packdate as packeddate  from OBWPACK_Box box ,OBWPACK_Movlinebox prod, obwpack_packingh header,c_bpartner partner,m_product mproduct ,c_orderline col ,c_order co,m_inout minout where box.obwpack_packingh_id =header.obwpack_packingh_id  and col.c_orderline_id=prod.c_orderline_id and co.c_order_id=col.c_order_id and prod.obwpack_box_id=box.obwpack_box_id and partner.c_bpartner_id=header.c_bpartner_id and mproduct.m_product_id=prod.m_product_id and co.c_order_id=minout.c_order_id and header.packdate>=? and header.packdate<? and partner.name=? and box.trackingNo is not null and (box.em_sra_boxstatus is null OR box.em_sra_boxstatus <>'CO') and co.docstatus in ('IBDO_SH','IBDO_PSH','CL') group by box.trackingNo,co.documentNo,header.packdate order by co.documentNo";
        // hql =
        // "select box.trackingNo as trackingno ,count(distinct(prod.m_product_id)) as count,sum(prod.quantity) as quantity,co.documentNo as documentno, header.packdate as packeddate from OBWPACK_Box box ,OBWPACK_Movlinebox prod, obwpack_packingh header, c_bpartner partner,m_product mproduct ,c_orderline col ,c_order co,m_inout minout where  box.obwpack_packingh_id =header.obwpack_packingh_id  and col.c_orderline_id=prod.c_orderline_id and   co.c_order_id=col.c_order_id and prod.obwpack_box_id=box.obwpack_box_id and partner.c_bpartner_id=header.c_bpartner_id and mproduct.m_product_id=prod.m_product_id and co.c_order_id=minout.c_order_id and header.packdate>=? and header.packdate<? and partner.name=? and box.trackingNo is not null and (box.em_sra_boxstatus is null OR box.em_sra_boxstatus <>'CO') and co.docstatus in ('IBDO_SH','IBDO_PSH','CL') group by box.trackingNo,co.documentNo,header.packdate order by co.documentNo";
        hql = "select box.trackingNo as trackingno ,count(distinct(prod.m_product_id)) as count,sum(prod.quantity) as quantity from OBWPACK_Box box,OBWPACK_Movlinebox prod, obwpack_packingh header,c_bpartner partner,m_product mproduct ,c_orderline col ,  c_order co,m_inout minout where box.obwpack_packingh_id =header.obwpack_packingh_id  and  col.c_orderline_id=prod.c_orderline_id and co.c_order_id=col.c_order_id and prod.obwpack_box_id=box.obwpack_box_id and partner.c_bpartner_id=header.c_bpartner_id and mproduct.m_product_id=prod.m_product_id and co.c_order_id=minout.c_order_id and header.packdate>=? and header.packdate<? and partner.name=? and (box.em_sra_boxstatus is null OR box.em_sra_boxstatus <>'CO') AND box.trackingNo<>'' and co.docstatus in ('IBDO_SH','IBDO_PSH','CL') group by box.trackingNo";
        pst = con.prepareStatement(hql);
        pst.setDate(1, startDate);
        pst.setDate(2, endDate);
        pst.setString(3, orgname);
        getBoxDetailsResultSet = pst.executeQuery();
        JSONArray jsonArray = new JSONArray();

        while (getBoxDetailsResultSet.next()) {

          log4j.info("Iterate Quiried Info into Json");
          String trackingNumber = getBoxDetailsResultSet.getString("trackingno");
          String count = getBoxDetailsResultSet.getString("count");
          String quantity = getBoxDetailsResultSet.getString("quantity");
          // String documentno = getBoxDetailsResultSet.getString("documentno");
          // String packeddate = getBoxDetailsResultSet.getString("packeddate");
          JSONObject jsonObject = new JSONObject();

          departmentHql = "select brand.name as name from cl_brand brand join cl_model model on brand.cl_brand_id=model.cl_brand_id join  m_product product on product.em_cl_model_id=model.cl_model_id join obwpack_movlinebox boxproduct  on  boxproduct.m_product_id=product.m_product_id  join obwpack_box box on box.obwpack_box_id=boxproduct.obwpack_box_id  where box.trackingno=? limit 1";
          pst = con.prepareStatement(departmentHql);
          pst.setString(1, trackingNumber);
          getDepartmentResultSet = pst.executeQuery();
          if (getDepartmentResultSet.next()) {
            log4j.info("Quering all info based on Date and append Store dept");
            jsonObject.put("boxnumber", trackingNumber);
            jsonObject.put("count", count);
            jsonObject.put("quantity", quantity);
            // jsonObject.put("documentno", documentno);
            // jsonObject.put("packeddate", packeddate);
            jsonObject.put("store_dept", getDepartmentResultSet.getString("name"));

            jsonArray.put(jsonObject);
          }
          flag++;
        }
        if (flag > 0) {
          jsonDataObject.put("data", jsonArray);
          jsonDataObject.put("status", "Success");
        } else {
          log4j.info("No Result Found because of Shipment no created/SO already closed");
          jsonDataObject.put("data", "noresult");
          jsonDataObject.put("status", "Success");
        }

      } else {
        log4j.info("No Result Found");
        jsonDataObject.put("data", "noresult");
        jsonDataObject.put("status", "Success");
      }

    } catch (SQLException exception) {
      jsonDataObject.put("data", "noresult");
      jsonDataObject.put("status", "Failure");
      exception.printStackTrace();
    } catch (ClassNotFoundException e) {
      jsonDataObject.put("data", "noresult");
      jsonDataObject.put("status", "Failure");
      e.printStackTrace();
    } catch (Exception e) {
      jsonDataObject.put("data", "noresult");
      jsonDataObject.put("status", "Failure");
      e.printStackTrace();

    }
    log4j.info(jsonDataObject);
    final String xml = sw.toString();
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonDataObject.toString());

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
