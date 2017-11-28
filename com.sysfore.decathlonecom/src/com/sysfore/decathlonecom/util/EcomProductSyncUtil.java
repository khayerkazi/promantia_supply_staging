package com.sysfore.decathlonecom.util;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sysfore.decathlonecom.model.EcomOrder;
import com.sysfore.decathlonecom.model.Product;

public class EcomProductSyncUtil {

  public static EcomOrder parseEcomOrderXML(Document orderXML) {

    EcomOrder eComOrder = new EcomOrder();

    List<Product> list = new LinkedList<Product>();
    System.out.println("Parser");
    if (validateeComOrderXML(orderXML)) {
      System.out.println("After Success");
      try {
        eComOrder.setOrgName(orderXML.getElementsByTagName("orgname").item(0).getChildNodes().item(
            0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setOrgName("");
      }
      try {
        eComOrder.setChargeAmt(orderXML.getElementsByTagName("charges").item(0).getChildNodes()
            .item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setChargeAmt("0.0");
      }
			try {
        eComOrder.setFeedback(orderXML.getElementsByTagName("feedback").item(0).getChildNodes()
            .item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setFeedback("");
      }
      try {
        eComOrder.setCustomerId(orderXML.getElementsByTagName("customerid").item(0).getChildNodes()
            .item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setCustomerId("");
      }
      try {
        eComOrder.setDescription(orderXML.getElementsByTagName("description").item(0)
            .getChildNodes().item(0).getNodeValue());
      } catch (Exception e) {

        eComOrder.setDescription("");
      }
      try {
        eComOrder.setBillNo(orderXML.getElementsByTagName("billno").item(0).getChildNodes().item(0)
            .getNodeValue());
      } catch (Exception e) {

        eComOrder.setBillNo("");
      }
      try {
        eComOrder.setOrderDate(orderXML.getElementsByTagName("orderdate").item(0).getChildNodes()
            .item(0).getNodeValue());
      } catch (Exception e) {

        eComOrder.setOrderDate("");
      }
      try {
        eComOrder.setPaymentMode(orderXML.getElementsByTagName("paymentmode").item(0)
            .getChildNodes().item(0).getNodeValue());
      } catch (Exception e) {

        eComOrder.setPaymentMode("");
      }
      try {
        eComOrder.setPaymentIdentifier(orderXML.getElementsByTagName("paymentidentifier").item(0)
            .getChildNodes().item(0).getNodeValue());
      } catch (Exception e) {

        eComOrder.setPaymentIdentifier("");
      }
      try {
        eComOrder.setPaymentTotal(orderXML.getElementsByTagName("paymenttotal").item(0)
            .getChildNodes().item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setPaymentTotal("");
      }
      try {
        eComOrder.setGrantTotal(orderXML.getElementsByTagName("granttotal").item(0).getChildNodes()
            .item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setGrantTotal("");
      }
      try {
        eComOrder.setWarehouseName(orderXML.getElementsByTagName("warehousename").item(0)
            .getChildNodes().item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setWarehouseName("");
      }
      try {
        eComOrder.setAddress1(orderXML.getElementsByTagName("address1").item(0).getChildNodes()
            .item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setAddress1("");
      }
      try {
        eComOrder.setAddress2(orderXML.getElementsByTagName("address2").item(0).getChildNodes()
            .item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setAddress2("");
      }
      try {
        eComOrder.setAddress3(orderXML.getElementsByTagName("address3").item(0).getChildNodes()
            .item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setAddress3("");
      }
      try {
        eComOrder.setAddress4(orderXML.getElementsByTagName("address4").item(0).getChildNodes()
            .item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setAddress4("");
      }
      try {
        eComOrder.setPostal(orderXML.getElementsByTagName("postalcode").item(0).getChildNodes()
            .item(0).getNodeValue());
      } catch (Exception e) {
        eComOrder.setPostal("");

      }
      try {
        eComOrder.setCity(orderXML.getElementsByTagName("city").item(0).getChildNodes().item(0)
            .getNodeValue());
      } catch (Exception e) {
        eComOrder.setCity("");
      }
      try {
        eComOrder.setState(orderXML.getElementsByTagName("state").item(0).getChildNodes().item(0)
            .getNodeValue());
      } catch (Exception e) {
        eComOrder.setState("");
      }
      try {
        eComOrder.setCountry(orderXML.getElementsByTagName("country").item(0).getChildNodes().item(
            0).getNodeValue());
      } catch (Exception e) {

        eComOrder.setCountry("");
      }

      NodeList nodeLst = orderXML.getElementsByTagName("ecomorder");
      for (int s = 0; s < nodeLst.getLength(); s++) {
        Node fstNode = nodeLst.item(s);
        NodeList nodeLst_fields = fstNode.getChildNodes();

        for (int i = 0; i < nodeLst_fields.getLength(); i++) {
          Node fstFieldNode = nodeLst_fields.item(i);

          if (fstFieldNode.getNodeType() == Node.ELEMENT_NODE
              && fstFieldNode.getNodeName().equals("productinfo")) {
            Product eComProduct = new Product();
            Element fstFieldElmnt = (Element) fstFieldNode;

            NodeList fields = fstFieldElmnt.getElementsByTagName("productid");
            Element fieldNameElmnt = (Element) fields.item(0);
            NodeList field = fieldNameElmnt.getChildNodes();
            try {
              eComProduct.setProductId((field.item(0)).getNodeValue());
            } catch (Exception e) {

            }

            NodeList fields1 = fstFieldElmnt.getElementsByTagName("qtyordered");
            Element fieldNameElmnt1 = (Element) fields1.item(0);
            NodeList field1 = fieldNameElmnt1.getChildNodes();
            try {
              eComProduct.setQuantityOrdered((field1.item(0)).getNodeValue());
            } catch (Exception e) {

            }

            NodeList fields2 = fstFieldElmnt.getElementsByTagName("unitqty");
            Element fieldNameElmnt2 = (Element) fields2.item(0);
            NodeList field2 = fieldNameElmnt2.getChildNodes();
            try {
              eComProduct.setUnitQty((field2.item(0)).getNodeValue());
            } catch (Exception e) {
              System.out.println("Exception in unit qty " + e);
            }

            NodeList fields3 = fstFieldElmnt.getElementsByTagName("unitprice");
            Element fieldNameElmnt3 = (Element) fields3.item(0);
            NodeList field3 = fieldNameElmnt3.getChildNodes();
            try {
              eComProduct.setUnitPrice((field3.item(0)).getNodeValue());
            } catch (Exception e) {
              System.out.println("Exception in unit price " + e);
            }

            NodeList fields4 = fstFieldElmnt.getElementsByTagName("ueqty");
            Element fieldNameElmnt4 = (Element) fields4.item(0);
            NodeList field4 = fieldNameElmnt4.getChildNodes();
            try {
              eComProduct.setUeQty((field4.item(0)).getNodeValue());
            } catch (Exception e) {

            }

            NodeList fields5 = fstFieldElmnt.getElementsByTagName("ueprice");
            Element fieldNameElmnt5 = (Element) fields5.item(0);
            NodeList field5 = fieldNameElmnt5.getChildNodes();
            try {
              eComProduct.setUePrice((field5.item(0)).getNodeValue());
            } catch (Exception e) {

            }

            NodeList fields6 = fstFieldElmnt.getElementsByTagName("pcbqty");
            Element fieldNameElmnt6 = (Element) fields6.item(0);
            NodeList field6 = fieldNameElmnt6.getChildNodes();
            try {
              eComProduct.setPcbQty((field6.item(0)).getNodeValue());
            } catch (Exception e) {

            }

            NodeList fields7 = fstFieldElmnt.getElementsByTagName("pcbprice");
            Element fieldNameElmnt7 = (Element) fields7.item(0);
            NodeList field7 = fieldNameElmnt7.getChildNodes();
            try {
              eComProduct.setPcbPrice((field7.item(0)).getNodeValue());
            } catch (Exception e) {

            }

            NodeList fields8 = fstFieldElmnt.getElementsByTagName("taxid");
            Element fieldNameElmnt8 = (Element) fields8.item(0);
            NodeList field8 = fieldNameElmnt8.getChildNodes();
            try {
              eComProduct.setTaxId((field8.item(0)).getNodeValue());
            } catch (Exception e) {

            }
            list.add(eComProduct);
            // System.out.println("Adding Addres s      >>>>>>>>>>>>>>>>>>>>>");
          }

        }
      }
      System.out.println("Complete Success");
      eComOrder.setItemOrdered(list);
      return eComOrder;
    } else {
      System.out.println("Manage Failure");
      eComOrder = null;
      return eComOrder;
    }

  }

  public static boolean validateeComOrderXML(Document orderXML) {
    Node nodeList = orderXML.getElementsByTagName("orgname").item(0);
    if (nodeList == null) {
      return false;
    } else {
      nodeList = null;
    }
    nodeList = orderXML.getElementsByTagName("customerid").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("description").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("billno").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("orderdate").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("paymentmode").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("paymentidentifier").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("paymenttotal").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("granttotal").item(0);

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

    nodeList = orderXML.getElementsByTagName("address1").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("address2").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("address3").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("address4").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("postalcode").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("city").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("state").item(0);

    if (nodeList == null) {

      return false;
    } else {
      nodeList = null;
    }

    nodeList = orderXML.getElementsByTagName("country").item(0);

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
