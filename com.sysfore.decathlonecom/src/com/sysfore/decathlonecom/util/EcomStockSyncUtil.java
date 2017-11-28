package com.sysfore.decathlonecom.util;

import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class EcomStockSyncUtil {

  public static java.util.List<String> parseEcomStockXML(Document orderXML) {

    String[] values = new String[2];

    java.util.List<String> content = new java.util.LinkedList<String>();
    String items = "";
    System.out.println("Parser");
    try {
      items = orderXML.getElementsByTagName("item").item(0).getChildNodes().item(0).getNodeValue();

      StringTokenizer ss = new StringTokenizer(items, "|");
      while (ss.hasMoreTokens()) {
        content.add(ss.nextToken());
      }
    } catch (Exception e) {
      // values[1] = "0";
    }

    try {
      content.add(orderXML.getElementsByTagName("warehousename").item(0).getChildNodes().item(0)
          .getNodeValue());
    } catch (Exception e) {
      // values[1] = "0";
    }

    return content;

  }

  public static boolean validateStockXML(Document orderXML) {
    Node nodeList = orderXML.getElementsByTagName("item").item(0);
    if (nodeList == null) {
      return false;
    } else {
      nodeList = null;
    }
    nodeList = orderXML.getElementsByTagName("warehousename").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    return true;
  }

  public static String doProcess() {

    return "";
  }

}
