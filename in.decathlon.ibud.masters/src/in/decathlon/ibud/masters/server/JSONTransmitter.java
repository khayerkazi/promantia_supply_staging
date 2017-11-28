package in.decathlon.ibud.masters.server;

import in.decathlon.ibud.commons.JSONHelper;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.service.json.DataToJsonConverter;

/*
 * 
 * Writes the data into response
 * helps in pushing the master data from supply to store
 * 
 */

public class JSONTransmitter {
  PrintWriter writer;
  DataToJsonConverter toJson;
  HttpServletResponse response;

  JSONTransmitter(HttpServletResponse response) throws IOException {
    this.writer = response.getWriter();
    this.response = response;
    this.toJson = new DataToJsonConverter();
    response.setBufferSize(1024);
  }

  public void sendData(BaseOBObject bob) throws Exception {
    JSONObject masterJson = JSONHelper.convetBobToJson(bob);
    writer.write(masterJson.toString());
    writer.flush();
  }
}
