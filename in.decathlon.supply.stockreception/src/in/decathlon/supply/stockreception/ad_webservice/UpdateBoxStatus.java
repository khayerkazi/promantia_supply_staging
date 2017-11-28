package in.decathlon.supply.stockreception.ad_webservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.web.WebService;
import org.openbravo.warehouse.packing.PackingBox;

public class UpdateBoxStatus implements WebService {
  static Logger log4j = Logger.getLogger(UpdateBoxStatus.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    StringWriter sw = new StringWriter();
    String strRead = "";
    String message = null;
    String boxno = null;
    Connection con;
    Boolean flag = false;
    JSONArray jsonArray = new JSONArray();
    JSONObject jsonObject = new JSONObject();
    String DocumentnoWS = "";
    try {
      log4j.info("Entering Into UpdateBox Status Part");
      JSONObject jsonDataObject = new JSONObject();

      InputStreamReader isReader = new InputStreamReader(request.getInputStream());
      BufferedReader bReader = new BufferedReader(isReader);
      StringBuilder strBuilder = new StringBuilder();
      strRead = bReader.readLine();
      while (strRead != null) {
        strBuilder.append(strRead);
        strRead = bReader.readLine();
        log4j.info("Parsing Success");
      }
      DocumentnoWS = strBuilder.toString();
      JSONArray geodata = new JSONArray(DocumentnoWS);
      int n = geodata.length();
      for (int i = 0; i < n; ++i) {
        final JSONObject boxNumberObj = geodata.getJSONObject(i);
        log4j.info("Box Update Started");
        String boxnumber = boxNumberObj.getString("boxnumber");
        List<PackingBox> boxlines = new ArrayList<PackingBox>();
        int j = 0;
        String query = "from OBWPACK_Box where trackingNo=:trackingNo";
        Query resultset = OBDal.getInstance().getSession().createQuery(query);
        resultset.setParameter("trackingNo", boxnumber);
        List<PackingBox> resultlist = new ArrayList<PackingBox>(resultset.list());
        for (PackingBox ob : resultlist) {
          if (resultset.list().size() > 0) {
            PackingBox Box = resultlist.get(j);
            Box.setSraBoxstatus("CO");
            Box.setUpdated(new Date());
            Box.setUpdatedBy(Box.getUpdatedBy());
            OBDal.getInstance().save(Box);
            OBDal.getInstance().flush();
            j++;
          } else {
            throw new OBException("TrackingNo " + boxnumber + " not found");
          }
        }

      }
      OBDal.getInstance().commitAndClose();
      JSONArray jsonArray1 = new JSONArray();
      JSONObject jsonObject1 = new JSONObject();
      jsonObject.put("update", "true");
      jsonArray1.put(jsonObject);

      log4j.info("Box Update has been finished");

      response.setContentType("text/xml");
      response.setCharacterEncoding("utf-8");
      final Writer w = response.getWriter();
      w.write(jsonArray1.toString());
      w.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
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
