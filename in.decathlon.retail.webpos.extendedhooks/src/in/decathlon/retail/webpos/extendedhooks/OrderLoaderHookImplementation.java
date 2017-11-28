/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package in.decathlon.retail.webpos.extendedhooks;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.retail.posterminal.OrderLoaderHook;

@ApplicationScoped
public class OrderLoaderHookImplementation implements OrderLoaderHook {
  private static final Logger log = Logger.getLogger(OrderLoaderHookImplementation.class);

  @Override
  public void exec(JSONObject js, Order order, ShipmentInOut shipment, Invoice invoice)
      throws Exception {
    /* Margin Calculation */
    int stdPrecision = order.getCurrency().getStandardPrecision().intValue();
    JSONArray orderlines = js.getJSONArray("lines");
    /* save values */
    TriggerHandler.getInstance().disable();
    try {
      for (int i = 0; i < orderlines.length(); i++) {
        JSONObject orderLine = (JSONObject) orderlines.get(i);
        OrderLine savedOrderLine = order.getOrderLineList().get(i);
        JSONObject product = orderLine.getJSONObject("product");
        BigDecimal unitQty = BigDecimal.ZERO;
        BigDecimal lotQty = BigDecimal.ZERO, boxQty = BigDecimal.ZERO;
        BigDecimal cessionPrice = BigDecimal.ZERO;
        BigDecimal lbtRate = BigDecimal.ZERO;
        BigDecimal totalTaxAmt = BigDecimal.ZERO;
        BigDecimal lineQty = new BigDecimal(orderLine.getLong("qty"));
        BigDecimal marginCalculation = BigDecimal.ZERO;
        if (product.has("SLP_rangeIdentifier")) {
          String usedRange = product.getString("SLP_rangeIdentifier");
          if (usedRange.equals("LOT")) {
            lotQty = new BigDecimal(orderLine.getLong("qty"));
          } else if (usedRange.equals("BOX")) {
            boxQty = new BigDecimal(orderLine.getLong("qty"));
          } else {
            unitQty = new BigDecimal(orderLine.getDouble("qty"));
          }
        } else {
          unitQty = new BigDecimal(orderLine.getDouble("qty"));
        }

        try {
          cessionPrice = BigDecimal.valueOf(product.getDouble("dsitite_clCessionprice")).setScale(
              stdPrecision, RoundingMode.HALF_UP);
        } catch (Exception e) {
          throw new Exception("Margin Calculation: Error getting cession price. [order: '"
                + order.getId() + "', lineno: '" + i + "'");
        }

        try {
          lbtRate = BigDecimal.valueOf(product.getDouble("dsitite_clLbtrate")).setScale(
              stdPrecision, RoundingMode.HALF_UP);
        } catch (Exception e) {
          log.warn("Margin Calculation: Error getting Lbtrate. [order: '" + order.getId()
                + "', lineno: '" + i + "'");
            lbtRate = BigDecimal.ZERO;
        }

        if (!(unitQty.compareTo(BigDecimal.ZERO) > 0 || lotQty.compareTo(BigDecimal.ZERO) > 0 || boxQty
            .compareTo(BigDecimal.ZERO) > 0)) {
          throw new Exception(
                "Margin Calculation: At least one of these values should be greater than 0: unitQty, lotQty, boxQty. [order: '"
                    + order.getId() + "', lineno: '" + i + "'");
        }

        try {
          totalTaxAmt = BigDecimal.valueOf(orderLine.getDouble("taxAmount")).setScale(stdPrecision,
              RoundingMode.HALF_UP);
        } catch (Exception e) {
          throw new Exception("Margin Calculation: Error getting tax amount. [order: '"
                + order.getId() + "', lineno: '" + i + "'");
        }

        try {
          if (order.getObposApplications().getObposTerminaltype().getOrganization()
              .getOrganizationInformationList().get(0).getLocationAddress().getRegion().getName()
              .equals("Maharashtra")) {
            marginCalculation = savedOrderLine
                .getLineGrossAmount()
                .subtract(totalTaxAmt)
                .subtract(cessionPrice.multiply(lineQty))
                .subtract(
                    (((lbtRate.divide(new BigDecimal(100))).multiply(lineQty))
                        .multiply(cessionPrice)));
            // provided by shane pereira
            // v_margin=round(v_lineamt-sum(coalesce(NEW.em_ds_taxamount,0))-
            // (sum(coalesce(NEW.em_ds_cessionprice*((coalesce((NEW.em_ds_unitqty),0))+(coalesce((NEW.em_ds_lotqty),0))+
            // (coalesce((NEW.em_ds_boxqty),0))),0)))-
            // ((coalesce(v_lbtrate,0)/100)*(sum(coalesce((NEW.em_ds_unitqty),0))
            // +sum(coalesce((NEW.em_ds_lotqty),0))+ sum(coalesce((NEW.em_ds_boxqty),0)))
            // *(to_number(coalesce(NEW.em_ds_cessionprice,0)))
            // ),2);
          } else {
            marginCalculation = savedOrderLine.getLineGrossAmount().subtract(totalTaxAmt)
                .subtract(cessionPrice.multiply(lineQty));
            // provided by shane pereira
            // v_margin=round(v_lineamt-sum(coalesce(NEW.em_ds_taxamount,0))-
            // (sum(coalesce(NEW.em_ds_cessionprice*((coalesce((NEW.em_ds_unitqty),0))
            // +(coalesce((NEW.em_ds_lotqty),0))+(coalesce((NEW.em_ds_boxqty),0))),0))),2);
          }
          marginCalculation.setScale(stdPrecision, RoundingMode.HALF_UP);
        } catch (Exception e) {
          throw new Exception("Margin Calculation: Error getting margin calculation. [order: '"
                + order.getId() + "', lineno: '" + i + "'");
        }

        savedOrderLine.setDSEMDSUnitQty(unitQty);
        savedOrderLine.setDSLotqty(lotQty);
        savedOrderLine.setDSBoxqty(boxQty);
        savedOrderLine.setDSCessionPrice(cessionPrice);
        savedOrderLine.setDSEMDsCcunitprice(savedOrderLine.getGrossListPrice().setScale(
            stdPrecision, RoundingMode.HALF_UP));
        savedOrderLine.setDSTaxamount(totalTaxAmt);
        savedOrderLine.setDsMarginamt(marginCalculation);
        savedOrderLine.setDsLinenetamt(savedOrderLine.getLineGrossAmount().setScale(stdPrecision,
            RoundingMode.HALF_UP));
      }

      order.setDSPosno(order.getObposApplications().getSearchKey());
      order.setDSChargeAmt(BigDecimal.ZERO);
      order.setOrderDate(order.getCreationDate());
      order.setUserContact(order.getCreatedBy());
    } finally {
      TriggerHandler.getInstance().enable();
    }

    /* End Margin Calculation */

    List<ShipmentInOutLine> lines = shipment.getMaterialMgmtShipmentInOutLineList();
    for (ShipmentInOutLine shipLine : lines) {
      OBCriteria<MaterialTransaction> transactionCrit = OBDal.getInstance().createCriteria(
          MaterialTransaction.class);
      transactionCrit.add(Restrictions.eq("goodsShipmentLine", shipLine));
      List<MaterialTransaction> lstTransaction = transactionCrit.list();
      if (lstTransaction.size() == 1) {
        MaterialTransaction transaction = lstTransaction.get(0);
        transaction.setSwDocumentno(order.getDocumentNo());
      }
    }
  }
}
