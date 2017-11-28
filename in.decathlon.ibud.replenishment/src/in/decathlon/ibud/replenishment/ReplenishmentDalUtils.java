package in.decathlon.ibud.replenishment;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.replenishment.data.Replenishment;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;

import com.sysfore.catalog.CLModel;


/**
 * @author promantia
 * 
 */
public class ReplenishmentDalUtils {
  private static final Logger log = Logger.getLogger(ReplenishmentGenerator.class);
	private static SimpleDateFormat inputParser = new SimpleDateFormat("HH:mm");

  /**
   * This function returns the list of active Organizations of type isstore
   * 
   * @return - returns a list of organizations
   */
	public static List<Organization> getStoreOrganizations() {
    OBCriteria<Organization> orgCrit = OBDal.getInstance().createCriteria(Organization.class);
    orgCrit.add(Restrictions.eq(Organization.PROPERTY_SWISSTORE, true));
    orgCrit.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
    orgCrit.add(Restrictions.eq(Organization.PROPERTY_DSIDEFISAUTODC, true));
    orgCrit.addOrder(org.hibernate.criterion.Order.asc(Organization.PROPERTY_IBDREPORGREPPRIORITY));
    List<Organization> orgList = orgCrit.list();
    return orgList;

  }

  public void createOrderLines(Product prd, BigDecimal qtyToBeOrdered, Order orderHeader, Long line)
      throws Exception {
    try {
      User openBravoUser = OBDal.getInstance().get(User.class, SOConstants.UserId);
      OrderLine newOrderLine = OBProvider.getInstance().get(OrderLine.class);
      //line = line + 10;
      newOrderLine.setLineNo(line);

      BigDecimal price = getPrice(prd);

      // skipping creation of line for no price list
      if (price == null) {
        return;
      }
      newOrderLine.setNewOBObject(true);

      newOrderLine.setClient(orderHeader.getClient());
      newOrderLine.setOrganization(orderHeader.getOrganization());
      newOrderLine.setActive(true);
      newOrderLine.setCreationDate(new Date());
      newOrderLine.setCreatedBy(openBravoUser);
      newOrderLine.setUpdatedBy(openBravoUser);
      newOrderLine.setUpdated(new Date());
      newOrderLine.setOrderDate(new Date());
      newOrderLine.setSalesOrder(orderHeader);
      newOrderLine.setProduct(prd);
      newOrderLine.setUOM(prd.getUOM());
      newOrderLine.setCurrency(prd.getClient().getCurrency());
      newOrderLine.setOrderedQuantity(qtyToBeOrdered);
      newOrderLine.setDeliveredQuantity(BigDecimal.ZERO);
      newOrderLine.setReservedQuantity(BigDecimal.ZERO);
      newOrderLine.setInvoicedQuantity(BigDecimal.ZERO);
      newOrderLine.setListPrice(BigDecimal.ZERO);
      newOrderLine.setUnitPrice(BigDecimal.ZERO);
      newOrderLine.setTax(getTaxRate(prd));
      newOrderLine.setDirectShipment(false);
      newOrderLine.setFreightAmount(BigDecimal.ZERO);
      newOrderLine.setLineNetAmount(BigDecimal.ZERO);
      newOrderLine.setLineGrossAmount(BigDecimal.ZERO);
      newOrderLine.setPriceLimit(BigDecimal.ZERO);
      newOrderLine.setStandardPrice(BigDecimal.ZERO);
      newOrderLine.setGrossListPrice(BigDecimal.ZERO);
      newOrderLine.setDescriptionOnly(false);
      newOrderLine.setGrossUnitPrice(BigDecimal.ZERO);
      newOrderLine.setDSCessionPrice(BigDecimal.ZERO);
      newOrderLine.setWarehouse(orderHeader.getWarehouse());
      orderHeader.getOrderLineList().add(newOrderLine);
    } catch (Exception e) {
      throw new Exception(e.toString());
    }

  }

  public Order createPurchaseOrderHeader(Organization org, Warehouse wr, CLModel model,
			Sequence docSeq, boolean isImplanted) throws Exception {
    try {
      User openBravoUser = OBDal.getInstance().get(User.class, SOConstants.UserId);
      log.debug("inside createPurchaseOrderHeader");
      Order newOrder = OBProvider.getInstance().get(Order.class);
      String partnerId = BusinessEntityMapper.getSupplyBPartner(org);
      BusinessPartner partner = OBDal.getInstance().get(BusinessPartner.class, partnerId);
      newOrder.setNewOBObject(true);
      newOrder.setClient(OBContext.getOBContext().getCurrentClient());
      newOrder.setOrganization(org);
      newOrder.setActive(true);
      newOrder.setCreationDate(new Date());
      newOrder.setCreatedBy(openBravoUser);
      newOrder.setUpdatedBy(openBravoUser);
      newOrder.setUpdated(new Date());
      newOrder.setBusinessPartner(partner);
      if (partner != null)
        newOrder.setPartnerAddress(partner.getBusinessPartnerLocationList().get(0));
      else
        throw new OBException("No business Partner for the supply Organization");
      FIN_PaymentMethod paymentMethod = getPaymentMethod(SOConstants.PaymentMethod);
      newOrder.setPaymentMethod(paymentMethod);
      PaymentTerm paymentTerm = getPaymentTerm(SOConstants.PaymentTerm);
      newOrder.setPaymentTerms(paymentTerm);
      newOrder.setOrderDate(new Date());
      newOrder.setScheduledDeliveryDate(new Date());
      newOrder.setDocumentStatus(SOConstants.DraftDocumentStatus);
      newOrder.setProcessed(false);
      newOrder.setSwDept(model.getDepartment());
//      newOrder.setClStoredept(model.getStoreDepartment());
      newOrder.setClBrand(model.getBrand());
      DocumentType transactionDocumentType = BusinessEntityMapper.getTrasactionDocumentType(org);
      newOrder.setTransactionDocument(transactionDocumentType);
      newOrder.setSwIsautoOrder(true);
      PriceList pricelist = BusinessEntityMapper.getPriceList(SOConstants.POPriceList);
      newOrder.setPriceList(pricelist);
      newOrder.setDocumentType(transactionDocumentType);
      newOrder.setAccountingDate(new Date());
      newOrder.setSalesTransaction(false);
      newOrder.setCurrency(getClient(SOConstants.Client).getCurrency());
      newOrder.setWarehouse(wr);
      newOrder.setSwIsimplantation(isImplanted);
      newOrder.setOrderLineList(new ArrayList<OrderLine>());

      return newOrder;
    } catch (Exception e) {
      throw e;
    }

  }

  public void bookPO(Set<String> pOsToBeBooked) throws Exception {

    if (pOsToBeBooked != null && pOsToBeBooked.size() > 0) {
      OBDal.getInstance().flush();
      for (String ordId : pOsToBeBooked) {
        try {
          BusinessEntityMapper.executeProcess(ordId, "104", "SELECT * FROM c_order_post(?)");
          log.info("Booked PO " + ordId);

        } catch (Exception e) {
          log.error("Error while executing stored procedure" + e.getMessage(), e);
          throw e;
        }
      }
      OBDal.getInstance().flush();
    } else {
      log.info("There are no PO's to be booked");
    }

  }

	public static PaymentTerm getPaymentTerm(String name) {
    OBCriteria<PaymentTerm> paymentTermCrit = OBDal.getInstance().createCriteria(PaymentTerm.class);
    paymentTermCrit.add(Restrictions.eq(PaymentTerm.PROPERTY_NAME, name));
    List<PaymentTerm> finPayTermCritList = paymentTermCrit.list();
    if (finPayTermCritList != null && finPayTermCritList.size() > 0) {
      return finPayTermCritList.get(0);
    } else {
      throw new OBException("payment term not found");
    }
  }

	public static FIN_PaymentMethod getPaymentMethod(String name) {
    OBCriteria<FIN_PaymentMethod> paymentCrit = OBDal.getInstance().createCriteria(
        FIN_PaymentMethod.class);
    paymentCrit.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_NAME, name));
    List<FIN_PaymentMethod> finPayCritList = paymentCrit.list();
    if (finPayCritList != null && finPayCritList.size() > 0) {
      return finPayCritList.get(0);
    } else {
      throw new OBException("payment method not found");
    }
  }

  public TaxRate getTaxRate(Product product) {

    List<TaxRate> taxRateList = product.getTaxCategory().getFinancialMgmtTaxRateList();
    if (taxRateList == null || taxRateList.size() <= 0) {
      throw new OBException("specify tax rate to product " + product);
    } else
      return taxRateList.get(0);
  }


	public static Client getClient(String name) {
    OBCriteria<Client> clientCrit = OBDal.getInstance().createCriteria(Client.class);
    clientCrit.add(Restrictions.eq(PaymentTerm.PROPERTY_NAME, name));
    List<Client> clientCritList = clientCrit.list();
    if (clientCritList != null && clientCritList.size() > 0) {
      return clientCritList.get(0);
    } else {
      throw new OBException("client not found");
    }
  }

  public BigDecimal getPrice(Product pr) {
    String prId = pr.getId();
    String qry = "id in ( from PricingProductPrice pp where pp.priceListVersion.priceList.salesPriceList=true and pp.product.id= :prId )";
    OBQuery<ProductPrice> prodPriceQry = OBDal.getInstance().createQuery(ProductPrice.class, qry);
    prodPriceQry.setNamedParameter("prId", prId);
    List<ProductPrice> prodPriceList = prodPriceQry.list();
    if (prodPriceList != null && prodPriceList.size() > 0) {
      return prodPriceList.get(0).getClCessionprice();
    } else {
      log.info("No cession price for product " + pr.getName());
      return null;
    }
  }

  	/**
	 * Compare only the hour/minute portion of date.
	 * 
	 * @param dateToBeCompared
	 * @param startDate
	 *            taken from db for that row
	 * @param endDate
	 *            from db for that row
	 * @return
	 */
	private boolean timeCompare(Date dateToBeCompared, Date startDate, Date endDate) {
		if (startDate == null) {
			throw new OBException("Start date cannot be null");
		}
		if (endDate == null) {
			throw new OBException("End date cannot be null");
		}

		try {
			// Compare only the time portion(HH:MM) of start and end date with the date being compared
			Calendar cal = Calendar.getInstance();

			Date dateComparedHHMM, startDateHHMM, endDateHHMM;

			cal.setTime(dateToBeCompared);
			String dateAsStr = cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
			dateComparedHHMM = parseDate(dateAsStr);

			cal.setTime(startDate);
			dateAsStr = cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
			startDateHHMM = parseDate(dateAsStr);

			cal.setTime(endDate);
			dateAsStr = cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
			endDateHHMM = parseDate(dateAsStr);

			// standard choice : start and end on the same day
			if (startDateHHMM.before(endDateHHMM)) {
				return (dateComparedHHMM.after(startDateHHMM) && dateComparedHHMM.before(endDateHHMM));
			}

			// we also can use the from 23h to 6h format, so...
			return ((dateComparedHHMM.after(startDateHHMM) && dateComparedHHMM.before(getEndTime("after"))) || (dateComparedHHMM
					.after(getEndTime("before")) && dateComparedHHMM.before(endDateHHMM)));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new OBException(e.getMessage());
		}
	}

	private Date getEndTime(String afterBefore) {
		Calendar cal = Calendar.getInstance();

		if (afterBefore.equals("before")) {
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
		}
		if (afterBefore.equals("after")) {
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
		}

		String dateAsStr = cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
		Date dateComparedHHMM = parseDate(dateAsStr);
		return dateComparedHHMM;
	}

	/**
	 * NOTE USE OF A STATIC simpledateformat which is NOT THREAD SAFE. So method is synchronized.
	 * 
	 * @param date
	 * @return
	 */
	private static synchronized Date parseDate(String date) {

		try {
			return inputParser.parse(date);
		} catch (java.text.ParseException e) {
			return new Date(0);
		}
	}

	public boolean timeCheck(Organization org, boolean isSpoon, Date dateToBeCompared) throws ParseException {
		Calendar cal = Calendar.getInstance();
		try {
			OBCriteria<Replenishment> replenishCrit = OBDal.getInstance().createCriteria(Replenishment.class);
			replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_ORGANIZATION, org));

			replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_SPOON, isSpoon ? true : false));
			replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_REGULAR, isSpoon ? false : true));
			List<Replenishment> replenishList = replenishCrit.list();

			for (Replenishment rep : replenishList) {
				if (rep.getDayofweek() != null) {
					String dayOfWeek = (String.valueOf(cal.get(Calendar.DAY_OF_WEEK) - 1));
					if (!dayOfWeek.equals(rep.getDayofweek())) {
						continue;
					}
				}
				log.debug("current time is:" + new Date());
				log.debug("start time:" + rep.getStartingTime());
				log.debug("End time: " + rep.getEndingTime());

				if (timeCompare(dateToBeCompared, rep.getStartingTime(), rep.getEndingTime())) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			log.error("Cannot check if replenishment has to run", e);
			throw new OBException(e.getMessage());
		}
	}

}
