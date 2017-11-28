package in.decathlon.ibud.shipment.store;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.decathlon.warehouse.truckreception.DTRTruckReception;
import org.decathlon.warehouse.truckreception.DTR_TruckDetails;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.json.JsonToDataConverter;

public class ReceiveDataToTruck {
  public static final Logger log = Logger.getLogger(CreateGRNService.class);
  JsonToDataConverter JsonToData = OBProvider.getInstance().get(JsonToDataConverter.class);
  BusinessEntityMapper getDataforSupply = new BusinessEntityMapper();
  JSONObject responseInout = new JSONObject();
  JSONObject responseShipmentLine = new JSONObject();

  public DTRTruckReception saveTruckHeader(JSONObject shipper, String processid) throws Exception {
    /*
     * This Method receives JSONArray which is sent by supply side , JSONArray will be having
     * shipper header with details with the help of shipper header and shipper details , truck
     * reception header and details can be created
     */

    DTRTruckReception truck = OBProvider.getInstance().get(DTRTruckReception.class);

    try {

      JSONArray shipperArray = shipper.getJSONArray("data");
      for (int i = 0; i < shipperArray.length(); i++) {
        try {
          JSONObject shipperObj = shipperArray.getJSONObject(i);
          JSONObject shipperHeader = (JSONObject) shipperObj.get("Header");
          JSONArray shipperlinedeatils = shipperObj.getJSONArray("Lines");

          String docNo = shipperHeader.getString(SOConstants.jsonDocNo);
          boolean truckAvailable = getTruckDocNo(docNo);
          if (!truckAvailable) {
            Organization org = BusinessEntityMapper.getOrgOfBP(shipperHeader
                .getString(SOConstants.jsonBusinessPartner));
            BusinessPartner bPartner = BusinessEntityMapper.getBPOfOrg(shipperHeader
                .getString(SOConstants.jsonOrganization));

            Location location = bPartner.getBusinessPartnerLocationList().get(0);

            String orgId = org.getId();
            OrgWarehouse orgWarehouse = BusinessEntityMapper.getOrgWarehouse(orgId);
            String warehouseId = orgWarehouse.getWarehouse().getId();
            Locator storageBin = BusinessEntityMapper.getFirstStorageBin(warehouseId);

            truck = createTruckHeader(shipperHeader, org, bPartner, location,
                orgWarehouse.getWarehouse());

            OBDal.getInstance().save(truck);

            addTruckDetails(shipperlinedeatils, org, storageBin, truck);
            OBDal.getInstance().save(truck);
            OBDal.getInstance().flush();
          } else
            continue;
        } catch (Exception e) {
          BusinessEntityMapper.rollBackNlogError(e, processid, null);
          log.error("Exception while creating Copy");
        }

      }

    } catch (Exception e) {
      throw e;
    }
    return truck;
  }

  public DTRTruckReception createTruckHeader(JSONObject shipperJson, Organization org,
      BusinessPartner bPartner, Location location, Warehouse warehouse) throws Exception {
    log.debug(" Enter AddTruck Header method");
    String shipperDocNo = shipperJson.getString("documentNo");
    shipperJson.put("documentStatus", "DR");
    DTRTruckReception bob = null;

    try {
      bob = (DTRTruckReception) JsonToData.toBaseOBObject(shipperJson.put("_entityName",
          "DTR_Truck_Reception"));
      Object docType = getDocType("DTR_TR");
      bob.set("client", OBContext.getOBContext().getCurrentClient());
      bob.set("organization", org);
      bob.set("active", true);
      bob.set("createdBy", OBContext.getOBContext().getUser());
      bob.set("updatedBy", OBContext.getOBContext().getUser());
      bob.set("creationDate", new Date());
      bob.set("updated", new Date());
      bob.set("documentType", docType);
      bob.set("documentNo", shipperDocNo);
      bob.set("receiptDate", new Date());
      bob.set("businessPartner", bPartner);
      bob.set("selectReceiptsBoxes", true);
      bob.set("complete", false);
      bob.set("documentStatus", "DR");
      bob.set("warehouse", warehouse);
      // bob.set("id", null);
      // bob.set("id", SequenceIdData.getUUID());
      bob.setNewOBObject(true);
    } catch (Exception e) {
      throw e;
    }
    return bob;
  }

  /*
   * Following method is used created the truck lines(Truck Details)
   */
  public void addTruckDetails(JSONArray obj, Organization org, Locator sBin, DTRTruckReception truck)
      throws Exception {

    log.debug(" Enter addTruckDetails method");

    for (int i = 0; i < obj.length(); i++) {
      DTR_TruckDetails bobline = null;

      JSONObject jsonobj = obj.getJSONObject(i);
      bobline = (DTR_TruckDetails) JsonToData.toBaseOBObject(jsonobj.put("_entityName",
          "DTR_Truck_Details"));
      String shipDetails = (String) jsonobj.get("goodsShipment$_identifier");
      int indexOfId = shipDetails.indexOf(" -");
      String grnDocNo = shipDetails.substring(0, indexOfId);
      String grnId = getReceipt(grnDocNo);
      ShipmentInOut grn = OBDal.getInstance().get(ShipmentInOut.class, grnId);
      bobline.set("client", OBContext.getOBContext().getCurrentClient());
      bobline.set("organization", org);
      bobline.set("active", true);
      bobline.set("createdBy", OBContext.getOBContext().getUser());
      bobline.set("updatedBy", OBContext.getOBContext().getUser());
      bobline.set("creationDate", new Date());
      bobline.set("updated", new Date());
      bobline.set("goodsShipment", grn);
      bobline.setNewOBObject(true);
      bobline.setDTRTruckReception(truck);
      truck.getDTRTruckDetailsList().add(bobline);
      OBDal.getInstance().save(truck);
    }

    return;
  }

  /*
   * this method is used to get the documentType for the goods receipt
   */
  private Object getDocType(String baseType) throws Exception {
    OBCriteria<DocumentType> docType = OBDal.getInstance().createCriteria(DocumentType.class);
    docType.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, baseType));
    List<DocumentType> shipmentInOutList = docType.list();
    if (shipmentInOutList != null && shipmentInOutList.size() > 0) {
      return docType.list().get(0);
    } else {
      throw new Exception("No GRN found with document Type" + baseType);
    }

  }

  /*
   * this method is used to get the trucks document no and if truck is available with the same
   * documentNo(docNo,which is passed as parameter) it returns the 'TRUE'
   */
  public static boolean getTruckDocNo(String docNo) {
    OBCriteria<DTRTruckReception> truckCrit = OBDal.getInstance().createCriteria(
        DTRTruckReception.class);
    truckCrit.add(Restrictions.eq(DTRTruckReception.PROPERTY_DOCUMENTNO, docNo));
    truckCrit.setMaxResults(1);
    if (truckCrit.count() > 0)
      return true;
    else
      return false;
  }

  /*
   * this method is used to get the Receipt by document No
   */
  private String getReceipt(String grnDocNo) throws Exception {
    String qry = "select shio.id from MaterialMgmtShipmentInOut shio where documentNo ='"
        + grnDocNo + "' and salesTransaction='N'";
    final Query shipInoutQuery = OBDal.getInstance().getSession().createQuery(qry);
    List<String> qryList = shipInoutQuery.list();
    if (qryList != null && qryList.size() > 0) {
      return (String) qryList.get(0);
    } else {
      throw new Exception("No GRN found with document no" + grnDocNo);
    }

  }
}
