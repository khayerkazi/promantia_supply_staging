package in.decathlon.ibud.masters.server;

import in.decathlon.supply.dc.data.IDSDOxylaneProdCategory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.TaxCategory;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.plm.Attribute;
import org.openbravo.model.common.plm.AttributeInstance;
import org.openbravo.model.common.plm.AttributeSet;
import org.openbravo.model.common.plm.AttributeUse;
import org.openbravo.model.common.plm.AttributeValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.financialmgmt.tax.TaxRateAccounts;
import org.openbravo.model.financialmgmt.tax.TaxZone;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.web.WebService;

import com.sysfore.catalog.CLBrand;
import com.sysfore.catalog.CLBranddepartment;
import com.sysfore.catalog.CLCOMPONENTBRAND;
import com.sysfore.catalog.CLColor;
import com.sysfore.catalog.CLDepartment;
import com.sysfore.catalog.CLModel;
import com.sysfore.catalog.CLNatureOfProduct;
import com.sysfore.catalog.CLStoreDept;
import com.sysfore.catalog.CLSubdepartment;
import com.sysfore.catalog.CLUniverse;
import com.sysfore.catalog.ClStoreUniverse;

public class MasterWebService implements WebService {

  DataToJsonConverter dataToJsonConverter = new DataToJsonConverter();
  final DataToJsonConverter toJsonConverter = OBProvider.getInstance().get(
      DataToJsonConverter.class);

  GeneralMasterSerializer genMaster;

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPost not implemented");

  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    processRequest(path, request, response);
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPost not Implemented");

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPut not Implemented");

  }

  private void processRequest(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    genMaster = new GeneralMasterSerializer(response);

    String updated = request.getParameter("updated");
    int rowCount = Integer.parseInt(request.getParameter("rowCount"));
    updated = updated.replace("_", " ");

    genMaster.generateJsonWS(org.openbravo.model.financialmgmt.tax.TaxCategory.class, updated,
        rowCount, "", null);
    genMaster.generateJsonWS(TaxCategory.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(TaxRate.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(TaxZone.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(TaxRateAccounts.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(CLBrand.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(CLUniverse.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(CLStoreDept.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(CLDepartment.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(CLCOMPONENTBRAND.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(CLBranddepartment.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(CLSubdepartment.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(CLNatureOfProduct.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(CLColor.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(CLModel.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(ProductCategory.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(UOM.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(Attribute.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(AttributeInstance.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(AttributeSet.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(AttributeUse.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(AttributeValue.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(IDSDOxylaneProdCategory.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(Product.class, updated, rowCount, "", null);
    /*
     * genMaster.generateJsonWS(LedgerMapping.class, updated, rowCount, "", null);
     * genMaster.generateJsonWS(MapDefinitionHeader.class, updated, rowCount, "", null);
     * genMaster.generateJsonWS(DocumentSection.class, updated, rowCount, "", null);
     * genMaster.generateJsonWS(GLCategory.class, updated, rowCount, "", null);
     * genMaster.generateJsonWS(NodeMap.class, updated, rowCount, "", null);
     * genMaster.generateJsonWS(AttributeMap.class, updated, rowCount, "", null);
     */
   // genMaster.generateJsonWS(Sequence.class, updated, rowCount, "DocSequence", null);
   // genMaster.generateJsonWS(DocumentType.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(ClStoreUniverse.class, updated, rowCount, "", null);
  }

}