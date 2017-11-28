package in.decathlon.ibud.replenishment.bulk;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.replenishment.ReplenishmentDalUtils;
import in.decathlon.ibud.replenishment.data.Replenishment;
import in.decathlon.supply.dc.util.AutoDCMails;
import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.scheduling.ProcessLogger;

import au.com.bytecode.opencsv.CSVWriter;

import com.sysfore.catalog.CLBrand;

public class ReplenishmentService {
	private static final Logger LOG = Logger.getLogger(ReplenishmentService.class);
	private ReplenishmentDAO dao = new ReplenishmentDAO();
	private SupplyWs supplyWs = new SupplyWs();
	private final static Properties p = SuppyDCUtil.getInstance().getProperties();

	//END of Day Replenishment method
	public void executeReplenishment(ProcessLogger logger) {
		// String processid = bundle.getProcessId();
		LOG.info("Starting autoreplenishment process");
		List<Organization> orgList = ReplenishmentDalUtils.getStoreOrganizations();

		if (orgList == null || orgList.size() == 0) {
			return;
		}
		
		String checkForReplType="Y";
		Map<String, Long> qtyPerStores = new HashMap<String, Long>();
		for (Organization org : orgList) {
			try{
				planReplenishmentFor(org, qtyPerStores,checkForReplType,logger);
			}
			catch (Exception e) {
				new OBException(e);
				e.printStackTrace();
			    logger.log(org.getName()+" " +e.toString()+"\n");
			}
		}

		sendFinalMail(qtyPerStores);
	}
	
	//Menu For Replenishment method
	public void executeMenuForReplenishment(Organization org,String replenishmentType, ProcessLogger logger) {
		if (org == null) {
			return;
		}

		Map<String, Long> qtyPerStores = new HashMap<String, Long>();
		planReplenishmentFor(org, qtyPerStores,replenishmentType,logger);
	
	}


	public void planReplenishmentFor(Organization org, Map<String, Long> qtyPerStores, String replenishmentType, ProcessLogger logger) {
		Date now = new Date();
		ReplenishmentTypeEnum type;
		if(replenishmentType.equals("Y")){
			type = findReplenishmentType(org, now);
		}else{
			type = ReplenishmentTypeEnum.valueOf(replenishmentType.toUpperCase());
		}

		LOG.info("The store " + org.getName() + " is replenish with type : " + type);
		logger.log("The store " + org.getName() + " is replenish with type : " + type +"\n");

		if (type == ReplenishmentTypeEnum.NONE) {
			return;
		}

		long startTime = System.currentTimeMillis();

		// Compute replenishment data (which and how much product we need to order)
		Map<String, MinMaxComputed> computed = computeReplenishment(org, type);

		long middleTime = System.currentTimeMillis();
		LOG.info("Computation finished for the store " + org.getName() +
				" in " + ((middleTime - startTime) / 1000) + " s");
		logger.log("Computation finished for the store " + org.getName() +
				" in " + ((middleTime - startTime) / 1000) + " s\n");

		Map<CLBrand, List<Order>> orders = createOrder(org);

		// commit session and reattach object to get data.
		SessionHandler.getInstance().commitAndStart();

		// Reattach object
		for (List<Order> ods : orders.values()) {
			dao.refreshOrder(ods);
		}

		// Send order to supply
		for (List<Order> ods : orders.values()) {
			try {
				/* StatSupplyReturn stat = */supplyWs.processRequest(ods, computed);
			} catch (Exception e) {
				new OBException(e);
				e.printStackTrace();
			    logger.log(org.getName()+" " +e.toString()+"\n");
			}
		}


		long endTime = System.currentTimeMillis();
		LOG.info("Replenishment finished for the store " + org.getName() +
				" [from the start ] in " + ((endTime - startTime) / 1000) + " s");
		logger.log("Replenishment finished for the store " + org.getName() +
				" [from the start ] in " + ((endTime - startTime) / 1000) + " s\n\n");

		sendMailResultWarehouse(org, qtyPerStores, computed,type);
	}



	/**
	 * Create all order from replenishment.
	 * 
	 * @param org
	 */
	private Map<CLBrand, List<Order>> createOrder(Organization org) {
		// Replnishment data OK, now get the data.
		Map<String, Integer> mapCatNbProd = dao.getNumberProductToOrder();

		Map<CLBrand, List<Order>> orders = createOrders(org, mapCatNbProd);
		for (List<Order> ods : orders.values()) {
			dao.saveOrder(ods);
		}

		dao.flush();

		dao.addOrderId(orders);
		dao.associateOrderWithLine();
		dao.flush();

		return orders;
	}

	/**
	 * Create all the order object from the
	 * 
	 * @param org
	 *            organisation
	 * @param mapCatNbProd
	 *            map of storedept / nb of product to order
	 * @return map of store dept / list of order object (not persisted)
	 */
	private Map<CLBrand, List<Order>> createOrders(Organization org, Map<String, Integer> mapCatNbProd) {
		//BusinessPartner bp = BusinessEntityMapper.getBPOfOrg(org.getId());
		// ????? TODO Check if bp is the same has this one
		 String partnerId = BusinessEntityMapper.getSupplyBPartner(org);
		 BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, partnerId);
		if (bp == null) {
			throw new OBException("No business Partner for the supply Organization");
		}

		User openBravoUser = OBDal.getInstance().get(User.class, SOConstants.UserId);
		Warehouse wr = BusinessEntityMapper.getOrgWarehouse(org.getId()).getWarehouse();
		Client dsi = ReplenishmentDalUtils.getClient(SOConstants.Client);
		DocumentType transactionDocTyp = BusinessEntityMapper.getTrasactionDocumentType(org);
		PriceList pricelist = BusinessEntityMapper.getPriceList(SOConstants.POPriceList);
		FIN_PaymentMethod paymentMethod = ReplenishmentDalUtils.getPaymentMethod(SOConstants.PaymentMethod);
		PaymentTerm paymentTerm = ReplenishmentDalUtils.getPaymentTerm(SOConstants.PaymentTerm);

		Map<CLBrand, List<Order>> orders = new LinkedHashMap<CLBrand, List<Order>>();

		Sequence sequence = transactionDocTyp.getDocumentSequence();
		int nextNumSeq = orderNumSeqBulk(mapCatNbProd, sequence);

		for (Entry<String, Integer> catNbProduct : mapCatNbProd.entrySet()) {
			CLBrand storeDept = OBDal.getInstance().get(CLBrand.class, catNbProduct.getKey());
			List<Order> oderForDept = new ArrayList<Order>();
			orders.put(storeDept, oderForDept);
			for (int i = 0; i <= (catNbProduct.getValue() / ReplenishmentUtils.NB_PRODUCT_PER_ORDER); i++) {
				String docNo = sequence.getPrefix() + nextNumSeq;
				nextNumSeq++;

				Order created = createOrder(docNo, openBravoUser, org, bp, wr, dsi, storeDept,
						transactionDocTyp, pricelist, paymentMethod, paymentTerm);

				oderForDept.add(created);
			}
		}

		return orders;
	}

	/**
	 * Create one order
	 * 
	 * @param org
	 *            organisation
	 * @param bp
	 *            business partner
	 * @param wr
	 *            warehouse
	 * @param dsi
	 *            client
	 * @param storeDept
	 *            store department
	 * @return a newly created order object (not persisted in db)
	 */
	private Order createOrder(String docNo, User openBravoUser, Organization org, BusinessPartner bp, Warehouse wr,
			Client dsi, CLBrand storeDept, DocumentType transactionDocTyp, PriceList pricelist,
			FIN_PaymentMethod paymentMethod, PaymentTerm paymentTerm) {

		Order newOrder = OBProvider.getInstance().get(Order.class);
		newOrder.setNewOBObject(true);
		newOrder.setClient(OBContext.getOBContext().getCurrentClient());
		newOrder.setOrganization(org);
		newOrder.setActive(true);
		newOrder.setCreationDate(new Date());
		newOrder.setCreatedBy(openBravoUser);
		newOrder.setUpdatedBy(openBravoUser);
		newOrder.setUpdated(new Date());
		newOrder.setBusinessPartner(bp);
		newOrder.setPartnerAddress(bp.getBusinessPartnerLocationList().get(0));
		newOrder.setPaymentMethod(paymentMethod);
		newOrder.setPaymentTerms(paymentTerm);
		newOrder.setOrderDate(new Date());
		newOrder.setScheduledDeliveryDate(new Date());
		newOrder.setDocumentStatus(SOConstants.DraftDocumentStatus);
		newOrder.setProcessed(false);
//		newOrder.setClStoredept(storeDept);
		newOrder.setClBrand(storeDept);
		newOrder.setTransactionDocument(transactionDocTyp);
		newOrder.setSwIsautoOrder(true);
		newOrder.setPriceList(pricelist);
		newOrder.setDocumentType(transactionDocTyp);
		newOrder.setAccountingDate(new Date());
		newOrder.setSalesTransaction(false);
		newOrder.setCurrency(dsi.getCurrency());
		newOrder.setWarehouse(wr);
		newOrder.setSwIsimplantation(false);
		newOrder.setDocumentNo(docNo);

		return newOrder;
	}

	public int orderNumSeqBulk(Map<String, Integer> mapCatNbProd, Sequence sequence) {
		int nbOfOrder = 0;
		for (Integer nbProd : mapCatNbProd.values()) {
			nbOfOrder += (nbProd / ReplenishmentUtils.NB_PRODUCT_PER_ORDER) + 1;
		}

		SequenceDao seqDao = new SequenceDao();
		return seqDao.getSeqNumAndAddNumberInNewTransaction(sequence, nbOfOrder);
	}

	/**
	 * Build the replenishment data
	 * 
	 * @param org
	 * @param type
	 */
	private Map<String, MinMaxComputed> computeReplenishment(Organization org, ReplenishmentTypeEnum type) {
		dao.createTemporaryTable();
		dao.insertIntoTableProductConfiguration(org);
		dao.computeStock(org);
		dao.computeNeededQuantity(type);
		dao.computInOrderQuantity(org);
		dao.computeToBeOrdered();
		dao.roundQtytoPCB();
		dao.roundQtytoUE();
		dao.ignoreUEEqZeroQty();
		dao.computeRank();
		dao.computePrice(org);

		return dao.getComputed(type == ReplenishmentTypeEnum.SPOON);
	}

	private void sendMailResultWarehouse(Organization org, Map<String, Long> qtyPerStores,
			Map<String, MinMaxComputed> computed, ReplenishmentTypeEnum type) {
		if (!p.getProperty("isMail").equals("true")) {
			return;
		}

		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_hh-mm");
		final String date = format.format(new Date());
		long qtyConfirmed = 0;
		String hubStock="",cwhStock="",rwhStock="";

		File f = new File(IbudConfig.getcsvFile() + org.getName() + "_" + date + ".csv");
		try {
			if (!f.exists()){
				f.createNewFile();
			}
			
			CSVWriter writer = new CSVWriter(new FileWriter(f, true));

			writer.writeNext(new String[] { "Brand", "Item Code", "Model Name", "Model Code", "Life Stage", "Size", "Display Min", "Min", "Max", "UE", "PCB", "Logistic Recharge", "Store Stock", "CWH Stock", "Hub Stock", "Required Quantity", "Open Order", "Order Generated","Confirmed Quantity", "Pcb picking", "Replenishment Type" });

			Set<MinMaxComputed> s = new TreeSet<MinMaxComputed>(new Comparator<MinMaxComputed>() {

				@Override
				public int compare(MinMaxComputed o1, MinMaxComputed o2) {
					int i = o1.getDeptName().compareTo(o2.getDeptName());
					if (i != 0)
						return i;
					i = o1.getModelName().compareTo(o2.getModelName());
					if (i != 0)
						return i;
					return o1.getProductId().compareTo(o2.getProductId());
				}

			});
			s.addAll(computed.values());

			for (MinMaxComputed mmc : s) {
				if(mmc.getToBeOrderedQty() > 0){
					rwhStock="" + mmc.getCacStock();
					cwhStock="" + mmc.getCarStock();
					hubStock="" + mmc.getHubStock();
				}else{
					rwhStock=cwhStock=hubStock="NA";
				}
				writer.writeNext(new String[] { mmc.getDeptName(), mmc.getProductName(), mmc.getModelName(), mmc.getModelCode(), mmc.getLifeStage(), mmc.getsize(),
						"" + mmc.getDisplayMin(), "" + mmc.getMin(), "" + mmc.getMax(), "" + mmc.getUeQty(),
						"" + mmc.getPcbQty(),
						"" + mmc.getLogRec(), "" + mmc.getStoreStock(), rwhStock,hubStock,
						"" + mmc.getRequiredQty(), "" + mmc.getQtyalreadyOrdered(), "" + mmc.getToBeOrderedQty(),
						"" + mmc.getValidatedQty(), "" + mmc.isPcb(), "" + type });
				qtyConfirmed += mmc.getValidatedQty();
			}

			writer.close();
		} catch (IOException e) {
			LOG.warn("Cannot write CSV report...", e);
			return;
		}


		qtyPerStores.put(org.getDescription(), qtyConfirmed);

		AutoDCMails.getInstance().simplySendWarehouseMail(org, f, qtyConfirmed);

	}


	private void sendFinalMail(Map<String, Long> qtyPerStores) {
		if (qtyPerStores.size() == 0 || !p.getProperty("isMail").equals("true")) {
			return;
		}

		AutoDCMails.getInstance().mainMailToAll(qtyPerStores);

	}



	/**
	 * find the replenishment type
	 * 
	 * @param org
	 *            stores
	 * @param now
	 *            the current date
	 * @return replenishment type
	 */
	public ReplenishmentTypeEnum findReplenishmentType(Organization org, Date now) {
		Calendar cal = Calendar.getInstance();
		OBCriteria<Replenishment> replenishCrit = OBDal.getInstance().createCriteria(Replenishment.class);
		replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_ORGANIZATION, org));

		// replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_SPOON, isSpoon ? true : false));
		// replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_REGULAR, isSpoon ? false : true));
		List<Replenishment> replenishList = replenishCrit.list();

		ReplenishmentTypeEnum type = ReplenishmentTypeEnum.NONE;

		for (Replenishment rep : replenishList) {
			if (rep.getDayofweek() != null) {
				String dayOfWeek = (String.valueOf(cal.get(Calendar.DAY_OF_WEEK) - 1));
				if (!dayOfWeek.equals(rep.getDayofweek())) {
					continue;
				}
			}

			if (ReplenishmentUtils.timeCompare(now, rep.getStartingTime(), rep.getEndingTime())) {
				if (rep.isSpoon()) {
					return ReplenishmentTypeEnum.SPOON;
				}
				if (rep.isRegular()) {
					type = ReplenishmentTypeEnum.REGULAR;
				}
			}
		}
		return type;

	}
}
