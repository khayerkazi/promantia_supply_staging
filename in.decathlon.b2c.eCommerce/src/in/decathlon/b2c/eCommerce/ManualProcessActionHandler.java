package in.decathlon.b2c.eCommerce;

import in.decathlon.b2c.eCommerce.util.ECommerceUtil;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.geography.Location;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.w3c.dom.NodeList;

import com.sysfore.sankalpcrm.RCCompany;

public class ManualProcessActionHandler extends BaseActionHandler {

	// Properties Singleton class instantiation
	Properties p = ECommerceUtil.getInstance().getProperties();
	
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
	  JSONObject result = new JSONObject();
	  
		  final String vendorId = p.getProperty("avettivendor");
		  final String userID = p.getProperty("avettiwsuser");
		  final String pwd = p.getProperty("avettipwd");
		  
		  final DecimalFormat df = new DecimalFormat("#.##");
	  
		  try {
			final JSONObject jsonData = new JSONObject(data);
			final String recordId = jsonData.getString("recordId");
			final String action = jsonData.getString("action");
	
			final OBQuery<Invoice> categoryQuery = OBDal.getInstance().createQuery(Invoice.class,
		     "id=:id");
			categoryQuery.setNamedParameter("id", recordId);
			final Invoice invoice = categoryQuery.list().get(0);
			
			final OBQuery<InvoiceLine> invlinCriteria = OBDal.getInstance().createQuery(InvoiceLine.class, "as o where o.invoice='"+invoice.getId()+"'");
			
			//check if organization is Ecommerce
			org.openbravo.model.common.enterprise.Organization org = invoice.getOrganization();
			String orgname= org.getName();
			
			if(orgname.equals("Ecommerce")){
			final SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			final SOAPConnection soapConnection = soapConnectionFactory.createConnection();
			
			final SOAPMessage findByIdResponse = soapConnection.call(findById(vendorId, userID, pwd, invoice.getDescription()), p.getProperty("serverURL")+"orderservice");
			final SOAPBody findByIdResponseeBody = findByIdResponse.getSOAPBody();
			final NodeList findByIdList = findByIdResponseeBody.getElementsByTagName("ns1:out");
			
			final SOAPMessage findOrderItemsResponse = soapConnection.call(findOrderItems(vendorId, userID, pwd, getNodeValue("orderdataid", findByIdList.item(0).getChildNodes())), p.getProperty("serverURL")+"orderservice");
			final SOAPBody findOrderItemsResponseBody = findOrderItemsResponse.getSOAPBody();
			final NodeList findOrderItemsList = findOrderItemsResponseBody.getElementsByTagName("ns2:OrderItem");
			
			final SOAPMessage findCustomerByEmailResponse = soapConnection.call(findCustomerByEmail(vendorId, userID, pwd, getNodeValue("loginname", findByIdList.item(0).getChildNodes())), p.getProperty("serverURL")+"customerservice");
			final SOAPBody findCustomerByEmailResponseBodyBilling = findCustomerByEmailResponse.getSOAPBody();
			final NodeList findCustomerBillingList = findCustomerByEmailResponseBodyBilling.getElementsByTagName("Address");
			
			final SOAPBody findCustomerByEmailResponseBody = findCustomerByEmailResponse.getSOAPBody();
			final NodeList findCustomerByEmailList = findCustomerByEmailResponseBody.getElementsByTagName("Customerprops");
			
			//Billing Address from rc_company table
			Location location2 = OBDal.getInstance().get(Location.class, invoice.getBusinessPartner().getRCCompany().getLocationAddress().getId());
			
			// StringBuilder for preparing the Shipping Address html code
			final StringBuilder billingAddress = new StringBuilder();
			
			if (location2 != null) {System.out.println(findCustomerBillingList.getLength());
				billingAddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;");
				if (invoice.getBusinessPartner().getName() != null) {
					billingAddress.append(invoice.getBusinessPartner().getName());
				}
				if (invoice.getBusinessPartner().getName2() != null) {
					billingAddress.append("&nbsp;" + invoice.getBusinessPartner().getName2());
				}
				billingAddress.append("</td></tr>");
				billingAddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
				if (invoice.getBusinessPartner().getRCCompany().getCompanyName() != null) {
					billingAddress.append(invoice.getBusinessPartner().getRCCompany().getCompanyName());
				}
				billingAddress.append("</td></tr>");
				billingAddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
				if (location2.getRegion().getName() != null) {
					billingAddress.append(location2.getRegion().getName());
				}
				billingAddress.append("</td></tr>");
				billingAddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
				if (location2.getCountry().getName() != null) {
					billingAddress.append(location2.getCountry().getName());
				}
				billingAddress.append("</td></tr>");
				billingAddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
				if (location2.getAddressLine1() != null) {
					billingAddress.append(location2.getAddressLine1());
				}
				if (location2.getAddressLine2() != null) {
					billingAddress.append(",&nbsp;" + location2.getAddressLine2());
				}
				if (location2.getRCAddress3() != null) {
					billingAddress.append(",&nbsp;" + location2.getRCAddress3());
				}
				billingAddress.append("</td></tr>");
				billingAddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
				if (location2.getCityName() != null) {
					billingAddress.append(location2.getCityName());
				}
				billingAddress.append("</td></tr>");
				billingAddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
				if (location2.getPostalCode() != null) {
					billingAddress.append(location2.getPostalCode());
				}
				billingAddress.append("</td></tr>");
				billingAddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
				if (invoice.getBusinessPartner().getRCMobile() != null) {
					billingAddress.append(invoice.getBusinessPartner().getRCMobile());
				}
				billingAddress.append("</td></tr>");
				billingAddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
				if (invoice.getBusinessPartner().getRCEmail() != null) {
					billingAddress.append(invoice.getBusinessPartner().getRCEmail());
				}
				billingAddress.append("</td></tr>");
				billingAddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;TIN NO&nbsp;:&nbsp;");
				String tinNum="";
				if (invoice.getBusinessPartner().getRCCompany().getLicenseNo() != null) {
					billingAddress.append(invoice.getBusinessPartner().getRCCompany().getLicenseNo());
				}
				billingAddress.append("</td></tr>");
			}
			
			// StringBuilder for preparing the Shipping Address html code
			final StringBuilder shippingAddress = new StringBuilder();
			shippingAddress.append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;");
			if (!getNodeValue("firstname", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("firstname", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			if (!getNodeValue("lastname", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append("&nbsp;" + getNodeValue("lastname", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("</th></tr>");
			shippingAddress.append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
			if (invoice.getBusinessPartner().getRCCompany().getCompanyName() != null) {
				shippingAddress.append(invoice.getBusinessPartner().getRCCompany().getCompanyName());
			}
			shippingAddress.append("</th></tr>");
			shippingAddress.append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
			shippingAddress.append(getNodeValue("name", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes().item(31).getChildNodes()));
			shippingAddress.append("</th></tr>");
			shippingAddress.append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
			shippingAddress.append(getNodeValue("name", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes().item(10).getChildNodes()));
			shippingAddress.append("</th></tr>");
			shippingAddress.append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
			if (!getNodeValue("address1", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("address1", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			} 
			if (!getNodeValue("address2", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(",&nbsp;" + getNodeValue("address2", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			if (!getNodeValue("address3", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(",&nbsp;" + getNodeValue("address3", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("</th></tr>");
			shippingAddress.append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
			if (!getNodeValue("city", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("city", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("</th></tr>");
			shippingAddress.append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
			if (!getNodeValue("postal", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("postal", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("</th></tr>");
			shippingAddress.append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
			if (!getNodeValue("phone", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("phone", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("</th></tr>");
			shippingAddress.append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
			if (!getNodeValue("email", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("email", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("</th></tr>");
			shippingAddress.append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;TIN NO&nbsp;:&nbsp;");
			String tinNum="";
			if (findCustomerByEmailList.getLength() > 0) {System.out.println(findCustomerByEmailList.getLength());
				for (int k = 0; k < findCustomerByEmailList.getLength(); k++) {
					if(getNodeValue("propname", findCustomerByEmailList.item(k).getChildNodes()).equals("TAXID")) {
						tinNum = getNodeValue("propvalue", findCustomerByEmailList.item(k).getChildNodes());
					}
				}
			}
			shippingAddress.append(tinNum);
			shippingAddress.append("</th></tr>");
			
			// StringBuilder for preparing the Item Info html code
			final StringBuilder itemsInfo = new StringBuilder();
			double subTotal = 0.0;
			double shipTotal = 0.0;
			double tax14 = 0.0;
			double tax5 = 0.0;
			int part=0;
			int totalQty=0;
			if(invlinCriteria.list().size() > 0) {
				for (InvoiceLine iLine : invlinCriteria.list()) {
					part++;
					BigDecimal qty = iLine.getInvoicedQuantity();
					BigDecimal unitPrice = iLine.getUnitPrice();
					String desc = "";
					String qtyAvetti;
					Double delCharges = 0.0;
					for (int k = 0; k < findOrderItemsList.getLength(); k++) {
						if(getNodeValue("itemcode", findOrderItemsList.item(k).getChildNodes()).equals(iLine.getProduct().getName()))
						{
							desc = getNodeValue("title", findOrderItemsList.item(k).getChildNodes());
							qtyAvetti = getNodeValue("qty", findOrderItemsList.item(k).getChildNodes());
							delCharges = Double.parseDouble(iLine.getChargeAmount().toString()) / Double.parseDouble(qtyAvetti);
	 					}
					}
					totalQty = totalQty + iLine.getInvoicedQuantity().intValue();
					String Qty;
					if(iLine.getInvoicedQuantity().intValue() == 0) {
						Qty = "0*";
					} else {
						Qty = ""+iLine.getInvoicedQuantity().intValue();
					}
					itemsInfo.append("<tr>");
					itemsInfo.append("<td style='width:15%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+iLine.getProduct().getName()+"</td>");
					itemsInfo.append("<td style='width:5%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+part+"</td>");
					itemsInfo.append("<td style='width:20%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+desc+"</td>");
					itemsInfo.append("<td style='width:3%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+Qty+"</td>");
					itemsInfo.append("<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+df.format(iLine.getUnitPrice())+"</td>");
					itemsInfo.append("<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+df.format(unitPrice.multiply(qty))+"</td>");
					itemsInfo.append("<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+iLine.getTax().getName()+"</td>");
					itemsInfo.append("<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+df.format(iLine.getTaxAmount())+"</td>");
					itemsInfo.append("<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-bottom:1px solid #000;'>"+df.format(iLine.getTaxAmount().add(unitPrice.multiply(qty)))+"</td>");
					itemsInfo.append("</tr>");
					subTotal = subTotal + Double.parseDouble(iLine.getTaxAmount().add(unitPrice.multiply(qty)).toString());
					shipTotal = shipTotal + Double.parseDouble(iLine.getChargeAmount().toString());
					if(iLine.getTax().getName().equals("VAT 5.50%")) {
						tax5 = Math.round(tax5 + (delCharges * Double.parseDouble(qty.toString())));
					} else if(iLine.getTax().getName().equals("VAT 14.50%")) {
						tax14 = Math.round(tax14 + (delCharges * Double.parseDouble(qty.toString())));
					}
				}
			}
			
			double invtotal = subTotal + tax14 + tax5;
			result.put("logourl", p.getProperty("logoUrl"));
			result.put("invoiceNum", invoice.getDocumentNo());
			result.put("orderDate", invoice.getCreationDate().toString().substring(0, 16));
			result.put("orderId", getNodeValue("orderid", findByIdList.item(0).getChildNodes()));
			result.put("orderDataId", getNodeValue("orderdataid", findByIdList.item(0).getChildNodes()));
			result.put("noOfItems", findOrderItemsList.getLength());
			result.put("billAddress", billingAddress.toString());
			result.put("shipAddress", shippingAddress.toString());
			result.put("itemInfo", itemsInfo.toString());
			result.put("subTotal", subTotal);
			result.put("tax14", tax14);
			result.put("tax5", tax5);
			result.put("shipTotal", Math.round(tax14 + tax5));
			result.put("invTotal", Math.round(invtotal));
			result.put("totalInvQty", totalQty);
			String dateOfIssue = invoice.getInvoiceDate().toString().substring(0, 10); 
			result.put("dateOfIssue", dateOfIssue);
			result.put("place", dateOfIssue);
			
		} else if (orgname.equals("B2B")) {
				OBContext.setAdminMode(true);
				 String orderid = invoice.getSalesOrder().getDocumentNo();
        OBQuery<RCCompany> rccompanyquery = null;
        String b2bid;

        if (orderid.contains("A")) {
          b2bid = orderid.substring(0, 7);

          rccompanyquery = OBDal.getInstance().createQuery(RCCompany.class,
              "as rc where rc.searchKey='" + b2bid + "'");
        } else {
          b2bid = orderid.substring(0, 5);
          rccompanyquery = OBDal.getInstance().createQuery(RCCompany.class,
              "as rc where rc.searchKey='" + b2bid + "'");
        }
				if(rccompanyquery.list().size()!=0){
				RCCompany rcCompany=rccompanyquery.list().get(0);
	
				String dateOfIssue = invoice.getInvoiceDate().toString().substring(0, 10); 
	
				String fullAddress = rcCompany.getCompanyAddress();
				fullAddress=fullAddress.replace("-addr3", "");
				fullAddress=fullAddress.replace("-addr4", "");
				fullAddress=fullAddress.replace("-", ",");
				
				StringBuilder billingaddress= new StringBuilder();
	
				billingaddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>Company:");
				billingaddress.append(rcCompany.getCompanyName());
				billingaddress.append("</td></tr>");
	
				billingaddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>Address:");
				billingaddress.append(fullAddress);
				billingaddress.append("</td></tr>");
	
				billingaddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>Contact Person:");
				billingaddress.append(rcCompany.getRCCompContactList().get(0).getName());
				billingaddress.append("   Phone:");
				billingaddress.append(rcCompany.getRCCompContactList().get(0).getMobile());
				billingaddress.append("</td></tr>");
	
				billingaddress.append("<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>TIN:");
				billingaddress.append("29710661016");
				billingaddress.append("</td></tr>");
			
				StringBuilder itemsInfo = new StringBuilder();
				StringBuilder description =null;
				BigDecimal unitprice;
				BigDecimal qty;
				double subTotal = 0.0;
				double shipTotal = 0.0;
				double tax14 = 0.0;
				double tax5 = 0.0;
				double delivercharge=0.0;
				double taxamount=0.0; 
				double taxrate=0.0;
				DecimalFormat twoDForm = new DecimalFormat("0.00");
				HashMap<String, InvoiceLine> items= null;
				HashMap<String, BigDecimal> qtyinfo= null;
				InvoiceLine inline=null;
				
				delivercharge=Double.parseDouble(invoice.getSalesOrder().getDSChargeAmt().toString());
				
				if(invlinCriteria.list().size() > 0) {
					items=new HashMap<String, InvoiceLine>();
					qtyinfo=new HashMap<String, BigDecimal>();
					
				for (InvoiceLine iLine : invlinCriteria.list()) {
						qty=iLine.getInvoicedQuantity();
						
						if(null != items.get(iLine.getProduct().getName()) && null != qtyinfo.get(iLine.getProduct().getName())){
							
							inline=items.get(iLine.getProduct().getName()); 
							qty=qty.add(qtyinfo.get(iLine.getProduct().getName()));
							qtyinfo.put(inline.getProduct().getName(), qty);
							
							items.remove(items.get(iLine.getProduct().getName()));  // remove old object
							items.put(inline.getProduct().getName(), inline); //put new object
						}
						else
						{
							items.put(iLine.getProduct().getName(), iLine); //put object
							qtyinfo.put(iLine.getProduct().getName(), iLine.getInvoicedQuantity());
						}
						qty=null;
						inline=null;
						iLine=null;
						
					} //merge multiple itemcodes from invoicelines
					
					for (Map.Entry<String, InvoiceLine> entry : items.entrySet()) {
					    String key = entry.getKey();
					    InvoiceLine iLine = entry.getValue();
						unitprice=iLine.getUnitPrice();
						qty=qtyinfo.get(iLine.getProduct().getName());
						
	
						taxrate=Double.parseDouble(iLine.getTax().getRate().toString());		
						description= new StringBuilder();
						description.append(iLine.getProduct().getClModel().getBrand().getName());
						description.append(" <br> ");
						description.append(iLine.getProduct().getClModel().getNatureOfProduct().getName());
						description.append(" <br> ");
						description.append(iLine.getProduct().getClModelname());
						description.append(" - ");
						description.append(iLine.getProduct().getClSize());
						
						taxamount = taxrate*Double.parseDouble((unitprice.multiply(qty).toString()));
						taxamount=taxamount/100;
					
						itemsInfo.append("<tr>");
						itemsInfo.append("<td style='widtht:15%;height:25px;line-height:15px;text-align:left;'>"+iLine.getProduct().getName()+"</td>");
						itemsInfo.append("<td style='width:20%;height:25px;line-height:15px;text-align:left;'>"+ description.toString()+"</td>");
						itemsInfo.append("<td style='width:5%;height:25px;line-height:15px;text-align:left;'>"+qty.toString()+"</td>");
						itemsInfo.append("<td style='width:10%;height:25px;line-height:15px;text-align:left;'>"+unitprice.toString()+"</td>");
						itemsInfo.append("<td style='width:10%;height:25px;line-height:15px;text-align:left;'>"+unitprice.multiply(qty)+"</td>");
						itemsInfo.append("<td style='width:10%;height:25px;line-height:15px;text-align:left;'>"+iLine.getTax().getName()+"</td>");
						itemsInfo.append("<td style='width:15%;height:25px;line-height:15px;text-align:left;'>"+twoDForm.format(taxamount)+"</td>");
						itemsInfo.append("<td style='width:15%;height:25px;line-height:15px;text-align:left;'>"+twoDForm.format((taxamount+(Double.parseDouble(unitprice.multiply(qty).toString()))))+"</td>");
						itemsInfo.append("</tr>");
						
						subTotal = subTotal +((taxamount+(Double.parseDouble(unitprice.multiply(qty).toString()))));
						shipTotal = shipTotal + Double.parseDouble(iLine.getChargeAmount().toString());
						description=null;
	
					}
					
					result.put("logourl", p.getProperty("logoUrl"));
					result.put("invoiceNum", invoice.getDocumentNo());
					result.put("orderDate", invoice.getCreationDate().toString().substring(0, 10));
					result.put("orderId", orderid);
					result.put("dateOfIssue", dateOfIssue);
					result.put("billAddress", billingaddress.toString());
					result.put("shipAddress", billingaddress.toString());
					result.put("itemInfo", itemsInfo.toString());
					result.put("subTotal", Math.round(subTotal));
					result.put("tax14", tax14);
					result.put("tax5", tax5);
					result.put("shipTotal", delivercharge);
					double invtotal = subTotal + tax14 + tax5+delivercharge;
					result.put("invTotal", Math.round(invtotal));
	
				}
			
				items=null;
			} 	
			else{
				
			}
		}	
	      return result;
	    } catch (Exception e) {
	    	e.printStackTrace();
	      throw new OBException(e);
	    }
		finally{
			OBContext.restorePreviousMode();
		}
  	}
  
  	private SOAPMessage findCustomerByEmail(String vendorId, String userID,
		String pwd, String nodeValue) throws Exception {
  		
  		final MessageFactory messageFactory = MessageFactory.newInstance();
		final SOAPMessage soapMessage = messageFactory.createMessage();
		final SOAPPart soapPart = soapMessage.getSOAPPart();
	    // SOAP Envelope
		final SOAPEnvelope envelope = soapPart.getEnvelope();
	    // add namespace
	    envelope.addNamespaceDeclaration("web", "http://webservices.commerce.avetti.com");
	    final SOAPHeader header = envelope.getHeader();
	    try {
	    	// SOAP Headers
	    	SOAPElement el = header.addHeaderElement(envelope.createName("VendorToken", "", "urn:commerce:vendor"));
	    	el = el.addChildElement(envelope.createName("VendorId", "", "urn:commerce:vendor"));
	    	el.setValue(vendorId);
	    	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
	    	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
	    	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
	    	el3.setValue(userID);
	    	el4.setValue(pwd);
	    	
	    	// SOAP Body
	    	final SOAPBody soapBody = envelope.getBody();
	    	SOAPElement soapBodyElem = soapBody.addChildElement("findByEmail", "web");
	    	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
	    	soapBodyElem2.addTextNode(nodeValue);
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	    soapMessage.saveChanges();
		return soapMessage;
  	}

  	private SOAPMessage findOrderItems(String vendorId, String userID, String pwd,
		String nodeValue) throws Exception {
	
	  	final MessageFactory messageFactory = MessageFactory.newInstance();
		final SOAPMessage soapMessage = messageFactory.createMessage();
		final SOAPPart soapPart = soapMessage.getSOAPPart();
	    // SOAP Envelope
		final SOAPEnvelope envelope = soapPart.getEnvelope();
	    // add namespace
	    envelope.addNamespaceDeclaration("web", "http://webservices.commerce.avetti.com");
	    final SOAPHeader header = envelope.getHeader();
	    try {
	    	// SOAP Headers
	    	SOAPElement el = header.addHeaderElement(envelope.createName("VendorToken", "", "urn:commerce:vendor"));
	    	el = el.addChildElement(envelope.createName("VendorId", "", "urn:commerce:vendor"));
	    	el.setValue(vendorId);
	    	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
	    	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
	    	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
	    	el3.setValue(userID);
	    	el4.setValue(pwd);
	    	
	    	// SOAP Body
	    	final SOAPBody soapBody = envelope.getBody();
	    	SOAPElement soapBodyElem = soapBody.addChildElement("findOrderItems", "web");
	    	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
	    	soapBodyElem2.addTextNode(nodeValue);
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	    soapMessage.saveChanges();
		return soapMessage;
	}

	private String getNodeValue(String tagName, NodeList nodes ) {
		for ( int x = 0; x < nodes.getLength(); x++ ) {
			Node node = (Node) nodes.item(x);
			if (node.getNodeName().equalsIgnoreCase(tagName)) {
				NodeList childNodes = node.getChildNodes();
				for (int y = 0; y < childNodes.getLength(); y++ ) {
					Node data = (Node) childNodes.item(y);
					if ( data.getNodeType() == Node.TEXT_NODE )
						return data.getNodeValue();
				}
			}
		}
		return "";
	}

	private SOAPMessage findById(String vendorId, String userID, String pwd,
		String description) throws Exception {
	
		final MessageFactory messageFactory = MessageFactory.newInstance();
		final SOAPMessage soapMessage = messageFactory.createMessage();
		final SOAPPart soapPart = soapMessage.getSOAPPart();
	    // SOAP Envelope
		final SOAPEnvelope envelope = soapPart.getEnvelope();
	    // add namespace
	    envelope.addNamespaceDeclaration("web", "http://webservices.commerce.avetti.com");
	    final SOAPHeader header = envelope.getHeader();
	    try {
	    	// SOAP Headers
	    	SOAPElement el = header.addHeaderElement(envelope.createName("VendorToken", "", "urn:commerce:vendor"));
	    	el = el.addChildElement(envelope.createName("VendorId", "", "urn:commerce:vendor"));
	    	el.setValue(vendorId);
	    	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
	    	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
	    	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
	    	el3.setValue(userID);
	    	el4.setValue(pwd);
	    	
	    	// SOAP Body
	    	final SOAPBody soapBody = envelope.getBody();
	    	SOAPElement soapBodyElem = soapBody.addChildElement("findById", "web");
	    	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
	    	soapBodyElem2.addTextNode(description);
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	    soapMessage.saveChanges();
		return soapMessage;
	}
}
