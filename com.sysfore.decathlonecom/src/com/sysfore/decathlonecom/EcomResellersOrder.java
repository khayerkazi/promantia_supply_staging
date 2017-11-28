package com.sysfore.decathlonecom;

import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.sysfore.decathlonecom.dao.EcomOrderServiceDAO;
import com.sysfore.decathlonecom.model.EcomOrder;
import com.sysfore.decathlonecom.util.EcomProductSyncUtil;

public class EcomResellersOrder  extends HttpSecureAppServlet implements WebService {

	private static final long serialVersionUID = 1L;

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		boolean flag = false;
		EcomOrderServiceDAO orderserviceDao = new EcomOrderServiceDAO();
		//  final String xmlFile = request.getParameter("ecomOrderXmlfile");
		//	File fXmlFile = new File(xmlFile);
		//	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		//	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		//	Document doc = dBuilder.parse(fXmlFile);
		
		final String xml = request.getParameter("ecomOrder");
		
		// Following code converting the String into XML format

	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(xml));
	    Document doc = builder.parse(is);
		
		
		EcomOrder eComOrder = EcomProductSyncUtil.parseEcomOrderXML(doc);
		
		if (eComOrder.getCustomerId() != null && !eComOrder.getCustomerId().equals("")) {
			String bId = selectBPId(eComOrder.getCustomerId());
			if(bId != null)
				eComOrder.setCustomerId(bId);
			List<com.sysfore.decathlonecom.model.Product> p = eComOrder.getItemOrdered();
			String msg = null;
			for (com.sysfore.decathlonecom.model.Product p1 : p) {
				msg = validateProduct(p1.getProductId());
				if(msg==null){
					flag = true;
					break;
				}
			}
			if(msg != null && !msg.equals("")) {
				 BusinessPartner bp = selectNames(eComOrder.getCustomerId());
				 if(bp!= null){
					  eComOrder.setFirstName(bp.getName());
		              eComOrder.setLastName(bp.getName2());
				 }
			} else {
				// skip that order..
			}
			
			eComOrder.setWarehouseName(selectWareHouse(eComOrder.getWarehouseName()));
			eComOrder.setOrgName(selectOrgId(eComOrder.getOrgName()));
			
			for (com.sysfore.decathlonecom.model.Product p1 : p) {
	              try {
	                TaxRate taxRateObj = OBDal.getInstance().get(TaxRate.class, p1.getTaxId());
	                BigDecimal priceUnitAmt = (new BigDecimal(p1.getUnitPrice()).multiply(new BigDecimal(p1.getUnitQty())));
	                BigDecimal lotPriceAmt = (new BigDecimal(p1.getPcbPrice()).multiply(new BigDecimal(p1.getPcbQty())));
	                BigDecimal boxPriceAmt = (new BigDecimal(p1.getUePrice()).multiply(new BigDecimal(p1.getUeQty())));
	                p1.setLineGrossAmt(((priceUnitAmt).add(lotPriceAmt).add(boxPriceAmt)).toString());
	                System.out.println("........linegrosamt............." + p1.getLineGrossAmt());
	                BigDecimal sumTotal = (new BigDecimal(p1.getUnitQty()).add(new BigDecimal(p1.getUeQty())).add(new BigDecimal(p1.getPcbQty())));
	                p1.setGrossUnitPrice((new BigDecimal(p1.getLineGrossAmt()).divide((sumTotal), 2, RoundingMode.HALF_UP)).toString());
	                System.out.println(".........grossunitprice............" + p1.getUnitPrice());
	                TaxRate ss = EcomOrderServiceDAO.getTax(p1.getTaxId(), eComOrder.getState(),eComOrder.getCustomerId());
	                p1.setLineNetAmt((new BigDecimal(p1.getLineGrossAmt()).multiply(new BigDecimal(100)).divide((new BigDecimal(100)).add(ss.getRate()),2, RoundingMode.HALF_UP)).toString());
	                System.out.println("...........linenetamt.........." + p1.getLineNetAmt());
	                p1.setUnitPrice((new BigDecimal(p1.getLineNetAmt()).divide(new BigDecimal(p1.getQuantityOrdered()), 2, RoundingMode.HALF_UP).toString()));
	              } catch (Exception e) {
	            	  e.printStackTrace();
	              }
			}
			
			msg = orderserviceDao.createEcomOrder(eComOrder);
			System.out.println("Complete It");
			
			if(flag){
				StringBuilder xmlBuilder = new StringBuilder();
			    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
			    xmlBuilder.append("<root>");
			    xmlBuilder.append("<message>").append(msg).append("</message>");
			    xmlBuilder.append("</root>");
			    response.setContentType("text/xml");
			    response.setCharacterEncoding("utf-8");
			    final Writer w = response.getWriter();
			    w.write(xmlBuilder.toString());
			    w.close();
			}else{
			    StringBuilder xmlBuilder = new StringBuilder();
			    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
			    xmlBuilder.append("<root>");
			    xmlBuilder.append("<message>").append(msg).append("</message>");
			    xmlBuilder.append("</root>");
			    response.setContentType("text/xml");
			    response.setCharacterEncoding("utf-8");
			    final Writer w = response.getWriter();
			    w.write(xmlBuilder.toString());
			    w.close();
			}
		}
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		
	}
	
	private String selectBPId(String value) {
		final OBCriteria<BusinessPartner> orderLineList = OBDal.getInstance().createCriteria(BusinessPartner.class);
		orderLineList.add(Restrictions.eq(BusinessPartner.PROPERTY_RCOXYLANE, value));
		final List<BusinessPartner> businessPartners = orderLineList.list();
		if(businessPartners.size() > 0)
			return businessPartners.get(0).getId();
		else
			return null;
	}
	
	private String validateProduct(String productId){
		final Product product = OBDal.getInstance().get(Product.class, productId);
		if(product != null)
			return "product";
		
		return null;
	}
	
	private BusinessPartner selectNames(String bpid){
		final BusinessPartner bId = OBDal.getInstance().get(BusinessPartner.class, bpid);
		return bId;
	}
	
	private String selectWareHouse(String warehouseName){
		final OBCriteria<Warehouse> warehouseList = OBDal.getInstance().createCriteria(Warehouse.class);
		warehouseList.add(Restrictions.eq(BusinessPartner.PROPERTY_NAME, warehouseName));
		final List<Warehouse> warehouse = warehouseList.list();
		if(warehouse.size() > 0)
			return warehouse.get(0).getId();
		else 
			return null;
	}
	
	private String selectOrgId(String orgName){
		final OBCriteria<Organization> organization = OBDal.getInstance().createCriteria(Organization.class);
		organization.add(Restrictions.eq(Organization.PROPERTY_NAME, orgName));
		final List<Organization> org = organization.list();
		if(org.size() > 0)
			return org.get(0).getId();
		else 
			return null;
	}
	
	/*public String createEcomOrder(EcomOrder ecomOrder) {
		 final Order order = OBProvider.getInstance().get(Order.class);
		 order.setClient(OBDal.getInstance().get(Client.class, "187D8FC945A5481CB41B3EE767F80DBB"));
		 Organization org = OBDal.getInstance().get(Organization.class, ecomOrder.getOrgName());
		 if(ecomOrder.getOrgName().equalsIgnoreCase("B2D0E3B212614BA6989ADCA3074FC423")){
			 order.setWarehouse(OBDal.getInstance().get(Warehouse.class, "D490F9B57DE64C3ABAF953FEBB8C5F70"));
		 }else {
			 order.setWarehouse(OBDal.getInstance().get(Warehouse.class, "F55C4BA65ABE4382B65D4549319DC570"));
		 }
		 order.setOrganization(org);
		 order.setCreationDate(new Date());
		 User user = OBDal.getInstance().get(User.class, "53451506CAA9402AA8F89C3F53F509AF");;
		 order.setCreatedBy(user);
		 order.setUpdated(new Date());
		 order.setUpdatedBy(user);
		 order.setTransactionDocument(OBDal.getInstance().get(DocumentType.class, "808F8818F724497D94282AC83493F394"));
		 order.setSalesTransaction(false);
		 order.setDocumentNo("*B2B*" +ecomOrder.getBillNo());
		 order.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, "35586321F375451389832DD198CA1DC7"));
		 Location location = OBDal.getInstance().get(Location.class, "D83677BA1F61434097590985224FD08A");
		 order.setInvoiceAddress(location);
		 order.setPartnerAddress(location);
		 order.setUserContact(user);
		 order.setDescription(ecomOrder.getDescription());
		 order.setPaymentTerms(OBDal.getInstance().get(PaymentTerm.class, "A4B18FE74DF64897B71663B0E57A4EFE"));
		 order.setPriceList(OBDal.getInstance().get(PriceList.class, "2205CDAF5996448484851F4524B25EA2"));
		 order.setShippingCompany(null);
		 order.setSalesRepresentative(user);
		 order.setTrxOrganization(org);
		 order.setActivity(null);
		 order.setDocumentStatus("DR");
		 order.setDocumentAction("CO");
		 order.setDocumentType(OBDal.getInstance().get(DocumentType.class, "808F8818F724497D94282AC83493F394"));
		 order.setOrderDate(new Date());
		 order.setScheduledDeliveryDate(new Date());
		 order.setAccountingDate(new Date());
		 order.setCurrency(OBDal.getInstance().get(Currency.class, "304"));
		 order.setFormOfPayment("B");
		 order.setInvoiceTerms("D");
		 order.setDeliveryTerms("A");
		 order.setFreightCostRule("I");
		 order.setDeliveryMethod("P");
		 order.setPriority("5");
		 order.setPrintDiscount(false);
		 order.setProcessNow(false);
		 order.setDSReceiptno(ecomOrder.getBillNo());
		 order.setDSEMDsRatesatisfaction(ecomOrder.getFeedback());
		 order.setDSTotalpriceadj(0l);
		 order.setDSPosno("0");
		 order.setDSChargeAmt(new BigDecimal(ecomOrder.getChargeAmt()));
		 order.setProcessed(false);
		 order.setGrandTotalAmount(new BigDecimal(0));
		 order.setDsBpartner(OBDal.getInstance().get(BusinessPartner.class, ecomOrder.getCustomerId()));
		 
		 // Adding the lines to c_orderline
		 
		 for (com.sysfore.decathlonecom.model.Product product : ecomOrder.getItemOrdered()) {
		 }
		return null;
	}*/
	
	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

	}
	
}
