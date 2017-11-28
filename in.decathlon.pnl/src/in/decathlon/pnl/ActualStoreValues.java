package in.decathlon.pnl;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.joda.time.DateTime;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.web.WebService;

public class ActualStoreValues implements WebService {
  public static final Logger log = Logger.getLogger(ActualStoreValues.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    String storeOrgId[] = null;
    String storeDate[] = null;
    String storeDept[] = null;

    if (request.getParameter("orgid") == null) {
      throw new Exception("Store orgid is empty");
    } else {
      storeOrgId = request.getParameter("orgid").split(",");
    }

    if (request.getParameter("paramdate[]") == null) {
      throw new Exception("date is empty");
    } else {

      String reqParamDate = request.getParameter("paramdate[]");
      storeDate = reqParamDate.split(",");
    }

    if (request.getParameter("paramdept[]") != null) {
      storeDept = request.getParameter("paramdept[]").split(",");
    }

    JSONObject jsonObj = new JSONObject();
    JSONArray jsonResponseObj = getJsonOrderLineData(storeDate, storeOrgId, storeDept);
    jsonObj.put("data", jsonResponseObj);
    writeResult(response, jsonObj.toString());

  }

  private JSONArray getJsonOrderLineData(String storeDates[], String[] storeOrgIds,
      String storeDeptmnts[]) throws JSONException, ParseException {
    String docType = "511A9371A0F74195AA3F6D66C722729D";

    JSONArray jsonArray = new JSONArray();

    String startDate = storeDates[0];
    String endDate = storeDates[1];

    DateTime sDate = new DateTime(startDate);
    DateTime eDate = new DateTime(endDate);

    for (DateTime sd = sDate; sd.compareTo(eDate) <= 0; sd = sd.plusDays(1)) {

      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

      Date formDate = simpleDateFormat.parse(sd.toString("yyyy-MM-dd"));
      Date toDate = simpleDateFormat.parse(sd.toString("yyyy-MM-dd"));

      String stDate = simpleDateFormat.format(formDate);
      String edDate = simpleDateFormat.format(toDate);

      String storeQuery = "";
      if (storeDeptmnts != null && storeDeptmnts.length > 0) {
        for (String storeOrgId : storeOrgIds) {
          for (String brands : storeDeptmnts) {
            storeQuery = "select ol.salesOrder.organization.name, model.brand.name,"
                + " coalesce(sum(ol.orderedQuantity),0) as qtyordered,coalesce(round((sum(ol.dsLinenetamt)),2),0) as turnover,"
                + " coalesce(round(sum(to_number(ol.dsMarginamt))),0) as marginamt, ol.salesOrder.organization.id as orgid,"
                + " model.brand.id as brandid,trunc(ol.orderDate) from OrderLine ol"
                + " join ol.salesOrder " + " join ol.product.clModel as model "
                + " where  ol.salesOrder.salesTransaction ='Y'" + " and ol.orderDate >= :fromdate "
                + " and ol.orderDate <= :todate "
                + " and ol.salesOrder.organization.id = :storeOrgId "
                + " and model.brand.id = :modelBrand "
                + " and  ol.salesOrder.documentType.id= :docType"
                + " and  ol.salesOrder.documentStatus in('CO','DR')"
                + " group by ol.salesOrder.organization.id, ol.salesOrder.organization.name,"
                + " model.brand.id, model.brand.name,trunc(ol.orderDate)";

            Query qry = OBDal.getInstance().getSession().createQuery(storeQuery);
            qry.setDate("fromdate", formDate);
            qry.setDate("todate", toDate);
            qry.setParameter("storeOrgId", storeOrgId);
            qry.setParameter("modelBrand", brands);
            qry.setParameter("docType", docType);
            List<Object[]> qryResult = qry.list();
            addToJsonArray(jsonArray, qryResult, stDate, edDate);

          }
        }

      } else {
        for (String storeOrgId : storeOrgIds) {
          storeQuery = "select ol.salesOrder.organization.name, model.brand.name,"
              + " coalesce(sum(ol.orderedQuantity),0) as qtyordered,coalesce(round((sum(ol.dsLinenetamt)),2),0) as turnover,"
              + " coalesce(round(sum(to_number(ol.dsMarginamt))),0) as marginamt, ol.salesOrder.organization.id as orgid,"
              + " model.brand.id as brandid,trunc(ol.orderDate) from OrderLine ol"
              + " join ol.salesOrder " + " join ol.product.clModel as model "
              + " where ol.salesOrder.salesTransaction ='Y'" + " and ol.orderDate >= :fromdate "
              + " and ol.orderDate <= :todate "
              + " and ol.salesOrder.organization.id = :storeOrgId "
              + " and ol.salesOrder.documentType.id= :docType"
              + " and  ol.salesOrder.documentStatus in('CO','DR')"
              + " group by ol.salesOrder.organization.id, ol.salesOrder.organization.name,"
              + " model.brand.id, model.brand.name,trunc(ol.orderDate)";

          Query qry = OBDal.getInstance().getSession().createQuery(storeQuery);
          qry.setDate("fromdate", formDate);
          qry.setDate("todate", toDate);
          qry.setParameter("storeOrgId", storeOrgId);
          qry.setParameter("docType", docType);
          List<Object[]> qryResult = qry.list();
          addToJsonArray(jsonArray, qryResult, stDate, edDate);
        }
      }
    }

    return jsonArray;
  }

  private void addToJsonArray(JSONArray jsonArray, List<Object[]> qryResult, String startdate,
      String enddate) {

    if (qryResult != null && qryResult.size() > 0) {

      for (Object[] obj : qryResult) {
        JSONObject jsonOrderLineObj = new JSONObject();
        try {
          jsonOrderLineObj.put("fromdate", startdate);
          jsonOrderLineObj.put("todate", enddate);
          jsonOrderLineObj.put("org", obj[0].toString());
          jsonOrderLineObj.put("dept", obj[1].toString());
          jsonOrderLineObj.put("qtyordered", obj[2].toString());
          jsonOrderLineObj.put("turnover", obj[3].toString());
          jsonOrderLineObj.put("marginamt", obj[4].toString());
          jsonOrderLineObj.put("orgId", obj[5].toString());
          jsonOrderLineObj.put("deptId", obj[6].toString());
          jsonArray.put(jsonOrderLineObj);
        } catch (JSONException e) {
          log.error("Exception while creating json array for the Extraction of data for PNL project-->"
              + e.getMessage());
          e.printStackTrace();
        }
      }

    } else {
      log.info(" qryResult is empty" + qryResult.size());
    }
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

}
