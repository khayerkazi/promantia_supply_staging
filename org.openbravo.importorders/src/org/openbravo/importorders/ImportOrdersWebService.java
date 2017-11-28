/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2009 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.importorders;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The Web Service in charge of calling the import orders web service.
 * 
 * @author aro
 */

public class ImportOrdersWebService implements WebService {

  private static Logger log = Logger.getLogger(ImportOrdersWebService.class);
  private final static String REQ_ID = "110011220033";

  public ImportOrdersWebService() {
  }

  /**
   * Performs the GET REST operation. This service handles the request for the XML Schema of list of
   * Business Objects.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    // Process p = new DummyProcess(); // to be modified
    Process p = new com.sysfore.decathlonimport.ad_process.ImportOrderProcess();

    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        request.getParameter("client"), request.getParameter("organization"),
        request.getParameter("role"));
    DalConnectionProvider conn = new DalConnectionProvider();

    ProcessBundle pb = new ProcessBundle(REQ_ID, vars).init(conn);

    OBError msg;
    try {
      p.execute(pb);
      msg = new OBError();
      msg.setType("Success");
    } catch (Exception e) {
      log.error("Processing Orders", e);
      msg = new OBError();
      msg.setType("Error");
      msg.setMessage(e.getMessage());
    }

    String xml = emptyDocument(msg);
    buildResponse(response, xml);
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  private static String emptyDocument(OBError msg) throws ParserConfigurationException,
      TransformerException {
    // Create Document
    DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
    Document doc = docBuilder.newDocument();
    // Create root element
    Element root = doc.createElement("Openbravo");
    root.setAttribute("type", msg.getType());
    root.setAttribute("title", msg.getTitle());
    root.appendChild(doc.createTextNode(msg.getMessage()));

    doc.appendChild(root);

    return fromDocToString(doc);

  }

  private static String fromDocToString(Document doc) {
    String xml = null;
    try {
      TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer trans = transfac.newTransformer();
      trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      trans.setOutputProperty(OutputKeys.VERSION, "1.0");
      trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      trans.setOutputProperty(OutputKeys.INDENT, "yes");

      // create string from xml tree
      StringWriter sw = new StringWriter();
      StreamResult result = new StreamResult(sw);
      DOMSource source = new DOMSource(doc);
      trans.transform(source, result);

      xml = sw.toString();

    } catch (Exception e) {
      e.printStackTrace();
    }

    return xml;
  }

  private static void buildResponse(HttpServletResponse response, String xml) throws IOException {

    response.setContentType("text/xml");

    response.setCharacterEncoding("utf-8");
    response.setHeader("Content-Encoding", "UTF-8");
    final Writer w = response.getWriter();
    w.write(xml);
    w.close();
  }
}
