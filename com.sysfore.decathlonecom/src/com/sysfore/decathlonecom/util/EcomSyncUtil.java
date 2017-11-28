package com.sysfore.decathlonecom.util;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sysfore.decathlonecom.model.EcomAddress;
import com.sysfore.decathlonecom.model.EcomCustomer;

public class EcomSyncUtil {

  public static EcomCustomer parseEcomCustomerXML(Document customerXML) {
	
    EcomCustomer eComCustomer = new EcomCustomer();

    List<EcomAddress> list = new LinkedList<EcomAddress>();
    System.out.println("Parser");
    if (validateEcomCustomerXML(customerXML)) {

      eComCustomer.setGreeting(customerXML.getElementsByTagName("greeting").item(0).getChildNodes()
          .item(0).getNodeValue());
      eComCustomer.setFirstName(customerXML.getElementsByTagName("firstname").item(0)
          .getChildNodes().item(0).getNodeValue());
      eComCustomer.setLastName(customerXML.getElementsByTagName("lastname").item(0).getChildNodes()
          .item(0).getNodeValue());
      eComCustomer.setEmail(customerXML.getElementsByTagName("email").item(0).getChildNodes().item(
          0).getNodeValue());
      eComCustomer.setMobile(customerXML.getElementsByTagName("mobile").item(0).getChildNodes()
          .item(0).getNodeValue());
      eComCustomer.setCompany(customerXML.getElementsByTagName("company").item(0).getChildNodes()
          .item(0).getNodeValue());
      eComCustomer.setOxylane(customerXML.getElementsByTagName("oxylane").item(0).getChildNodes()
          .item(0).getNodeValue());
      eComCustomer.setStatus(customerXML.getElementsByTagName("status").item(0).getChildNodes()
          .item(0).getNodeValue());
      eComCustomer.setSports(customerXML.getElementsByTagName("sports").item(0).getChildNodes()
          .item(0).getNodeValue());
		eComCustomer.setSource(customerXML.getElementsByTagName("source").item(0).getChildNodes()
          .item(0).getNodeValue());
      // eComCustomer.setAddress1(customerXML.getElementsByTagName("address1").item(0).getChildNodes()
      // .item(0).getNodeValue());
      // eComCustomer.setAddress2(customerXML.getElementsByTagName("address2").item(0).getChildNodes()
      // .item(0).getNodeValue());
      // eComCustomer.setAddress3(customerXML.getElementsByTagName("address3").item(0).getChildNodes()
      // .item(0).getNodeValue());
      // eComCustomer.setAddress4(customerXML.getElementsByTagName("address4").item(0).getChildNodes()
      // .item(0).getNodeValue());
      // eComCustomer.setPostalcode(customerXML.getElementsByTagName("postalcode").item(0)
      // .getChildNodes().item(0).getNodeValue());
      // eComCustomer.setCity(customerXML.getElementsByTagName("city").item(0).getChildNodes().item(0)
      // .getNodeValue());
      // eComCustomer.setState(customerXML.getElementsByTagName("state").item(0).getChildNodes().item(
      // 0).getNodeValue());
      // eComCustomer.setCountry(customerXML.getElementsByTagName("country").item(0).getChildNodes()
      // .item(0).getNodeValue());
      eComCustomer.setOptIn(customerXML.getElementsByTagName("optin").item(0).getChildNodes().item(
          0).getNodeValue());
      eComCustomer.setComments(customerXML.getElementsByTagName("comments").item(0).getChildNodes()
          .item(0).getNodeValue());

      NodeList nodeLst = customerXML.getElementsByTagName("ecomuser");
      for (int s = 0; s < nodeLst.getLength(); s++) {
        Node fstNode = nodeLst.item(s);
        NodeList nodeLst_fields = fstNode.getChildNodes();

        for (int i = 0; i < nodeLst_fields.getLength(); i++) {
          Node fstFieldNode = nodeLst_fields.item(i);

          if (fstFieldNode.getNodeType() == Node.ELEMENT_NODE
              && fstFieldNode.getNodeName().equals("address")) {
            EcomAddress eComAddress = new EcomAddress();
            Element fstFieldElmnt = (Element) fstFieldNode;

            NodeList fields = fstFieldElmnt.getElementsByTagName("address_line1");
            Element fieldNameElmnt = (Element) fields.item(0);
            NodeList field = fieldNameElmnt.getChildNodes();
            eComAddress.setAddress1((field.item(0)).getNodeValue());

            NodeList fields1 = fstFieldElmnt.getElementsByTagName("address_line2");
            Element fieldNameElmnt1 = (Element) fields1.item(0);
            NodeList field1 = fieldNameElmnt1.getChildNodes();
            eComAddress.setAddress2((field1.item(0)).getNodeValue());

            NodeList fields2 = fstFieldElmnt.getElementsByTagName("address_line3");
            Element fieldNameElmnt2 = (Element) fields2.item(0);
            NodeList field2 = fieldNameElmnt2.getChildNodes();
            eComAddress.setAddress3((field2.item(0)).getNodeValue());

            NodeList fields3 = fstFieldElmnt.getElementsByTagName("address_line4");
            Element fieldNameElmnt3 = (Element) fields3.item(0);
            NodeList field3 = fieldNameElmnt3.getChildNodes();
            eComAddress.setAddress4((field3.item(0)).getNodeValue());

            NodeList fields4 = fstFieldElmnt.getElementsByTagName("postalcode");
            Element fieldNameElmnt4 = (Element) fields4.item(0);
            NodeList field4 = fieldNameElmnt4.getChildNodes();
            eComAddress.setPostalCode((field4.item(0)).getNodeValue());

            NodeList fields5 = fstFieldElmnt.getElementsByTagName("city");
            Element fieldNameElmnt5 = (Element) fields5.item(0);
            NodeList field5 = fieldNameElmnt5.getChildNodes();
            eComAddress.setCity((field5.item(0)).getNodeValue());

            NodeList fields6 = fstFieldElmnt.getElementsByTagName("state");
            Element fieldNameElmnt6 = (Element) fields6.item(0);
            NodeList field6 = fieldNameElmnt6.getChildNodes();
            eComAddress.setState((field6.item(0)).getNodeValue());

            NodeList fields7 = fstFieldElmnt.getElementsByTagName("country");
            Element fieldNameElmnt7 = (Element) fields7.item(0);
            NodeList field7 = fieldNameElmnt7.getChildNodes();
            eComAddress.setCountry((field7.item(0)).getNodeValue());
            list.add(eComAddress);
            // System.out.println("Adding Addres s      >>>>>>>>>>>>>>>>>>>>>");
          }

        }
      }
      eComCustomer.setEcomAddress(list);
      return eComCustomer;
    }

    eComCustomer = null;
    return eComCustomer;
  }

  public static boolean validateEcomCustomerXML(Document customerXML) {
    Node nodeList = customerXML.getElementsByTagName("greeting").item(0);
    if (nodeList == null) {
      return false;
    } else {
      nodeList = null;
    }
    nodeList = customerXML.getElementsByTagName("firstname").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = customerXML.getElementsByTagName("lastname").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = customerXML.getElementsByTagName("email").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = customerXML.getElementsByTagName("mobile").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = customerXML.getElementsByTagName("company").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = customerXML.getElementsByTagName("oxylane").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = customerXML.getElementsByTagName("status").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = customerXML.getElementsByTagName("sports").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    /**
     * nodeList = customerXML.getElementsByTagName("address1").item(0);
     * 
     * if (nodeList == null) {
     * 
     * return false; } else { nodeList = null; }
     * 
     * nodeList = customerXML.getElementsByTagName("address2").item(0);
     * 
     * if (nodeList == null) {
     * 
     * return false; } else { nodeList = null; }
     * 
     * nodeList = customerXML.getElementsByTagName("address3").item(0);
     * 
     * if (nodeList == null) {
     * 
     * return false; } else { nodeList = null; }
     * 
     * nodeList = customerXML.getElementsByTagName("address4").item(0);
     * 
     * if (nodeList == null) {
     * 
     * return false; } else { nodeList = null; }
     * 
     * nodeList = customerXML.getElementsByTagName("postalcode").item(0);
     * 
     * if (nodeList == null) {
     * 
     * return false; } else { nodeList = null; }
     * 
     * nodeList = customerXML.getElementsByTagName("city").item(0);
     * 
     * if (nodeList == null) {
     * 
     * return false; } else { nodeList = null; }
     * 
     * nodeList = customerXML.getElementsByTagName("state").item(0);
     * 
     * if (nodeList == null) {
     * 
     * return false; } else { nodeList = null; }
     * 
     * nodeList = customerXML.getElementsByTagName("country").item(0);
     * 
     * if (nodeList == null) {
     * 
     * return false; } else { nodeList = null; }
     */
    nodeList = customerXML.getElementsByTagName("optin").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = customerXML.getElementsByTagName("comments").item(0);

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
