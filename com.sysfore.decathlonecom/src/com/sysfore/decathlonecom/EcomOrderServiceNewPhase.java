package com.sysfore.decathlonecom;

import in.decathlon.b2c.eCommerce.util.ECommerceUtil;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.orders.process.ImmediateSOonPO;

import java.io.StringReader;
import java.io.Writer;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.sysfore.decathlonecom.model.EcomOrder;
import com.sysfore.decathlonecom.model.Product;
import com.sysfore.decathlonecom.util.EcomProductSyncUtil;

public class EcomOrderServiceNewPhase implements WebService {

  // Properties Singleton class instantiation
  final Properties p = ECommerceUtil.getInstance().getProperties();
  final DecimalFormat df = new DecimalFormat("#.##");
  private static Logger log = Logger.getLogger(EcomOrderServiceNewPhase.class);

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();

  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
	  
    try {
      String xml = request.getParameter("ecomOrder");
      String msg = "none";
      Order orderHeader=null;
      String documentstatus = null;
      StringBuilder xmlBuilder = new StringBuilder();

      System.out.println("Reading XML logic");
      // Following code converting the String into XML format
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(xml));
      Document document = builder.parse(is);

      // Validating the XML. Verify all the necessary tag and values are present;
      EcomOrder eComOrder = EcomProductSyncUtil.parseEcomOrderXML(document);

      if (eComOrder != null) {
        System.out.println("New logic started");
        String bPartnerSO = "";
        final OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(
            BusinessPartner.class, "as bp where bp.rCOxylane='" + eComOrder.getCustomerId() + "'");
        if (bpQuery.list().size() > 0) {
          System.out.println("Business partner exists");
          final BusinessPartner bPartner = bpQuery.list().get(0);
          bPartnerSO = bPartner.getId();
        }

        // create PO header based on OrderID
        final JSONObject job = new JSONObject(
            "{\"_entityName\":\"Order\",\"client\":\""
                + p.getProperty("ecomsupplyClient")
                + "\",\"client._identifier\":\"\",\"organization\":\""
                + p.getProperty("ecomsupplyOrg")
                + "\",\"organization._identifier\":\"\",\"createdBy\":\""
                + OBContext.getOBContext().getUser().getId()
                + "\",\"updatedBy\":\""
                + OBContext.getOBContext().getUser().getId()
                + "\",\"salesTransaction\":\"false\",\"documentNo\":\"\",\"documentStatus\":\"DR\",\"documentAction\":\"CO\",\"documentType\":\""
                + p.getProperty("ecomsupplydocumentType")
                + "\",\"documentType._identifier\":\"\",\"transactionDocument\":\""
                + p.getProperty("ecomsupplydocumentType")
                + "\",\"transactionDocument._identifier\":\"\",\"description\":\"Online Order\",\"orderDate\":\""
                + new Timestamp(new Date().getTime())
                + "\",\"scheduledDeliveryDate\":\"\",\"accountingDate\":\""
                + new Timestamp(new Date().getTime())
                + "\",\"businessPartner\":\""
                + p.getProperty("ecomsupplybusinessPartner")
                + "\",\"businessPartner._identifier\":\"\",\"invoiceAddress\":\""
                + p.getProperty("ecomsupplyinvoiceAddress")
                + "\",\"invoiceAddress._identifier\":\"\",\"partnerAddress\":\""
                + p.getProperty("ecomsupplyinvoiceAddress")
                + "\",\"partnerAddress._identifier\":\"\",\"printDiscount\":\"false\",\"currency\":\""
                + p.getProperty("ecomcurrency")
                + "\",\"currency._identifier\":\"\",\"formOfPayment\":\"B\",\"paymentTerms\":\""
                + p.getProperty("ecomsupplypaymentTerms")
                + "\",\"summedLineAmount\":\"\",\"paymentTerms._identifier\":\"\",\"invoiceTerms\":\"D\",\"deliveryTerms\":\"A\",\"freightCostRule\":\"I\",\"deliveryMethod\":\"P\",\"priority\":\"5\",\"warehouse\":\""
                + p.getProperty("ecomsupplywarehouse")
                + "\",\"warehouse._identifier\":\"\",\"priceList\":\""
                + p.getProperty("ecomsupplypriceList")
                + "\",\"priceList._identifier\":\"\",\"userContact\":\""
                + OBContext.getOBContext().getUser().getId()
                + "\",\"paymentMethod\":\""
                + p.getProperty("ecomsupplypaymentMethod")
                + "\",\"paymentMethod._identifier\":\""
                + "\",\"dSTotalpriceadj\":0,\"dSPosno\":0,\"dSEMDsRatesatisfaction\":\""
                + eComOrder.getFeedback()
                + "\",\"clBrand\":\""
                + p.getProperty("ecombrandstar")                
                + "\",\"dSChargeAmt\":"
                + eComOrder.getChargeAmt()
                + ",\"generateTemplate\":\"\",\"copyFromPO\":\"\",\"pickFromShipment\":\"\",\"receiveMaterials\":\"\",\"createInvoice\":\"\",\"addOrphanLine\":\"\",\"calculatePromotions\":\"\",\"quotation\":\"\",\"reservationStatus\":\"NR\",\"dSReceiptno\":\""
                + eComOrder.getBillNo()
                + "\",\"swSendorder\":\"\",\"swModorder\":\"\",\"swDelorder\":\"\",\"obwplGeneratepicking\":\"\",\"sWEMSwPostatus\":\"\",\"grandTotalAmount\":\"\",\"summedLineAmount\":\"\",\"dSEMDsGrandTotalAmt\":\"\",\"dsBpartner\":\""
                + bPartnerSO + "\",\"rCOxylaneno\":\""
                + eComOrder.getCustomerId() + "\",\"salesRepresentative\":\""
                + OBContext.getOBContext().getUser().getId() + "\",\"ibdoCreateso\":\"true\"}");
        final JsonToDataConverter toBaseOBObject = OBProvider.getInstance().get(
            JsonToDataConverter.class);
        // Converting JSON Object to Openbravo Business object
        final BaseOBObject bob = toBaseOBObject.toBaseOBObject(job);

        // Database Connections and Database insertions
        OBDal.getInstance().save(bob);
       OBDal.getInstance().flush();

        final String poHeaderId = (String) bob.getId();
        OBDal.getInstance().commitAndClose();
        System.out.println("PO created " + poHeaderId);
        int lineNumPO = 0;
        for (Product product : eComOrder.getItemOrdered()) {
          System.out.println("Lines creation logic starts");
          lineNumPO++;
          double taxRate = 0.0;
          String taxId = product.getTaxId();
          final TaxRate tax = OBDal.getInstance().get(TaxRate.class, taxId);
          if (tax != null) {
            taxRate = tax.getRate().doubleValue();
          }
          /*
           * if(orderItem.getShipTax().equals("0.055")) { taxId = p.getProperty("ecomsupplytax550");
           * taxRate = 5.5; } else if(orderItem.getShipTax().equals("0.145")) { taxId =
           * p.getProperty("ecomsupplytax1450"); taxRate = 14.5; }
           */
          double unitWoTax = (Double.parseDouble(product.getUnitPrice()) * 100) / (100 + taxRate);
          unitWoTax = Double.parseDouble(df.format(unitWoTax));
          final JSONObject jobi = new JSONObject(
              "{\"_entityName\":\"OrderLine\",\"client\":\""
                  + p.getProperty("ecomsupplyClient")
                  + "\",\"client._identifier\":\"\",\"organization\":\""
                  + p.getProperty("ecomsupplyOrg")
                  + "\",\"organization._identifier\":\"\",\"createdBy\":\""
                  + OBContext.getOBContext().getUser().getId()
                  + "\",\"updatedBy\":\""
                  + OBContext.getOBContext().getUser().getId()
                  + "\",\"active\":\"true\",\"salesOrder\":\""
                  + poHeaderId
                  + "\",\"salesOrder._identifier\":\"\",\"lineNo\":"
                  + lineNumPO
                  + ",\"businessPartner\":\""
                  + p.getProperty("ecomsupplybusinessPartner")
                  + "\",\"partnerAddress\":\""
                  + p.getProperty("ecomsupplyinvoiceAddress")
                  + "\",\"orderDate\":\""
                  + new Timestamp(new Date().getTime())
                  + "\",\"product\":\""
                  + product.getProductId()
                  + "\",\"product._identifier\":\"\",\"warehouse\":\""
                  + p.getProperty("ecomsupplywarehouse")
                  + "\",\"warehouse._identifier\":\"\",\"uOM\":\"100\",\"uOM._identifier\":\"\",\"orderedQuantity\":"
                  + product.getQuantityOrdered()
                  + ",\"reservedQuantity\":0,\"reservationStatus\":\"NR\",\"manageReservation\":\"\",\"managePrereservation\":\"\",\"currency\":\""
                  + p.getProperty("ecomcurrency")
                  + "\",\"currency._identifier\":\"\",\"unitPrice\":"
                  + unitWoTax
                  + ",\"attributeSetValue\":null,\"tax\":\""
                  + taxId
                  + "\",\"tax._identifier\":\"\",\"dSLotqty\":0,\"dSLotprice\":0,\"dSBoxqty\":0,\"dSBoxprice\":0,\"dSEMDSUnitQty\":"
                  + product.getQuantityOrdered()
                  + ",\"dsLinenetamt\":"
                  + (unitWoTax * Double.parseDouble(product.getQuantityOrdered()))
                  + ",\"chargeAmount\":\"\",\"lineNetAmount\":"
                  + (unitWoTax * Double.parseDouble(product.getQuantityOrdered()))
                  + ",\"FreightAmt\":\"\",\"grossUnitPrice\":"
                  + df.format((Double.parseDouble(product.getUnitPrice())))
                  + ",\"PriceStd\":"
                  + df.format((Double.parseDouble(product.getUnitPrice())))
                  + ",\"priceList\":\""
                  + p.getProperty("ecomstorepriceList")
                  + "\",\"lineGrossAmount\":"
                  + df.format(Double.parseDouble(product.getUnitPrice())
                      * Double.parseDouble(product.getQuantityOrdered())) + "}");
          final BaseOBObject bobi = toBaseOBObject.toBaseOBObject(jobi);
          // Database Connections and Database insertions
          OBDal.getInstance().save(bobi);
          OBDal.getInstance().flush();
        }
        OBDal.getInstance().commitAndClose();
        System.out.println("Lines created");
        // Calling Stored Procedure c_order_post1
        final List parameters = new ArrayList();
        parameters.add(null);
        parameters.add(poHeaderId);
        System.out.println(poHeaderId);
        /*
         * final String procedureName = "c_order_post1";
         * if((CallStoredProcedure.getInstance().call(procedureName, parameters, null)).equals("1"))
         * { log.info("Procedure called successfully for the PO "+poHeaderId); }
         * System.out.println("Procedure called successfully for the PO "+poHeaderId);
         */

       
        orderHeader = OBDal.getInstance().get(Order.class, poHeaderId);
        // doPost(request, response);
       

        System.out.println("Sending request to supply logic starts");
        ImmediateSOonPO soOnPO = new ImmediateSOonPO();
        ArrayList<String> pOids = new ArrayList<String>();
        if (orderHeader != null) {
          if (orderHeader.getOrderLineList().size() > 0) {
            processOrder(soOnPO, pOids, orderHeader);
          }
        }
      } else {
        msg = "corrupt data";
      }
      String getPOreference="select swPoReference from Order where id=:orderid";
      
			Query getPOReference=OBDal.getInstance().getSession().createQuery(getPOreference);
			getPOReference.setParameter("orderid", orderHeader.getId());
			List<Object[]> getPOStatus=getPOReference.list();
if(getPOStatus.get(0)!=null)
{
			String GetDocstatus="select sum(line.orderedQuantity),sum(line.swConfirmedqty) from OrderLine line, Order corder where line.salesOrder=corder.id and corder.id=:orderid  group by corder.documentNo";
     
		    Query getDocResultset = OBDal.getInstance().getSession().createQuery(GetDocstatus);
		    getDocResultset.setParameter("orderid",orderHeader.getId());
		    List<Object[]> docstatus = getDocResultset.list();
      if(docstatus.size()>0)
      {
    	  for (Object[] ob : docstatus) {
     String orderedqty=ob[0].toString();
     String confirmedqty=ob[1].toString();
					    	  if(orderedqty.equals(confirmedqty))
					    	  {
					    		  documentstatus="Booked";
					    	  }
					    	  else if (confirmedqty=="0")
					    	  {
					    		  documentstatus="Not Booked";  
					    	  }
					    	  else
					    	  {
					    		  documentstatus="Partially Booked";
					    	  }
    	  								}
    	  msg = "success";
    }
      String GetItemCodes="select mproduct.name,line.swConfirmedqty from OrderLine line, Order corder, Product mproduct where line.salesOrder=corder.id and line.product=mproduct.id and corder.id=:orderid";
      Query getItemResulset = OBDal.getInstance().getSession().createQuery(GetItemCodes);
      getItemResulset.setParameter("orderid",orderHeader.getId());
      List<Object[]> itemCodes = getItemResulset.list();
      
      xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
      xmlBuilder.append("<root>");
      xmlBuilder.append("<message>").append(msg).append("</message>");
      xmlBuilder.append("<documentno>" + orderHeader.getDocumentNo() + "</documentno>");
      xmlBuilder.append("<billno>" + eComOrder.getBillNo() + "</billno>");
      xmlBuilder.append("<docstatus>" + documentstatus + "</docstatus>");
      xmlBuilder.append("<lines>");
if(itemCodes.size()>0)
{
for(Object[] object:itemCodes)
{      
	  xmlBuilder.append("<line>");
	  xmlBuilder.append("<itemcode>" + object[0]+ "</itemcode>");
	  xmlBuilder.append("<quantity>" + object[1] + "</quantity>");
	  xmlBuilder.append("</line>");
}
		  xmlBuilder.append("</lines>");
}
      xmlBuilder.append("</root>");
}
else
{
	
					  msg="Failure";
					  documentstatus="Not Booked";
					  xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
				      xmlBuilder.append("<root>");
				      xmlBuilder.append("<message>").append(msg).append("</message>");
				      xmlBuilder.append("<documentno>" + orderHeader.getDocumentNo() + "</documentno>");
				      xmlBuilder.append("<billno>" + eComOrder.getBillNo() + "</billno>");
				      xmlBuilder.append("<docstatus>" + documentstatus + "</docstatus>");
				      xmlBuilder.append("</root>");
}
				
				      response.setContentType("text/xml");
				      response.setCharacterEncoding("utf-8");
				      final Writer w = response.getWriter();
				      w.write(xmlBuilder.toString());
				      w.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();

  }

  private static void processOrder(ImmediateSOonPO soOnPO, ArrayList<String> pOids,
      Order orderHeader) throws Exception {

    log.info(",, " + SOConstants.performanceTest + " before process ,," + new Date());
    soOnPO.processRequest(orderHeader);
    log.info(",, " + SOConstants.performanceTest + " after process ,," + new Date());
    pOids.add(orderHeader.getId());
  }

}