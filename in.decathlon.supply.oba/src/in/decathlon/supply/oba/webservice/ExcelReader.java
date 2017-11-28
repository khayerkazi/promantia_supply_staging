package in.decathlon.supply.oba.webservice;

import in.decathlon.supply.oba.bean.ItemExistedBean;
import in.decathlon.supply.oba.util.OrdersProjectUtility;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.web.WebService;

import com.sysfore.catalog.CLBrand;
import com.sysfore.catalog.CLDepartment;
import com.sysfore.catalog.CLModel;
import com.sysfore.catalog.CLStoreDept;
import com.sysfore.catalog.CLSubdepartment;
import com.sysfore.catalog.CLUniverse;
import com.sysfore.catalog.ClStoreUniverse;

public class ExcelReader implements WebService {

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
			final OrdersProjectUtility ordersUtility = new OrdersProjectUtility();
		
			// getting all the configurations
			final Map<String, String> configDetails = ordersUtility.getOBAConfigurations();
		
			Workbook workbook = Workbook.getWorkbook(new File(configDetails.get("ModelRefExcelLocation")));
			Sheet sheet = workbook.getSheet(0);
			final List<ItemExistedBean> itemExistedBeanslList = new ArrayList<ItemExistedBean>();
			for(int i=0; i<sheet.getRows()-1;i++) {
				final ItemExistedBean itemExistedBean = new ItemExistedBean();
				itemExistedBean.setModelCode(sheet.getCell(0, i+1).getContents());
				itemExistedBean.setBrandName(sheet.getCell(1, i+1).getContents());
				itemExistedBean.setDmiDept(sheet.getCell(2, i+1).getContents());
				itemExistedBean.setDmiUniverse(sheet.getCell(3, i+1).getContents());
				itemExistedBean.setStoreDept(sheet.getCell(4, i+1).getContents());
				itemExistedBean.setStoreUniverse(sheet.getCell(5, i+1).getContents());
				itemExistedBean.setSubDept(sheet.getCell(6, i+1).getContents());
				
				itemExistedBeanslList.add(itemExistedBean);
			}
			int count = 0;
			System.out.println("list size "+itemExistedBeanslList.size());
			for (ItemExistedBean itemExistedBean : itemExistedBeanslList) {
				
				CLBrand clBrand = null;
				CLStoreDept clStoreDept = null;
				CLDepartment clDepartment = null;
				ClStoreUniverse clStoreUniverse = null;
				CLUniverse clUniverse = null;
				CLSubdepartment clSubdepartment = null;
				
				// fetch the brand
				final OBCriteria<CLBrand> clBrandObCriteria = OBDal.getInstance().createCriteria(CLBrand.class);
				clBrandObCriteria.add(Restrictions.eq(CLBrand.PROPERTY_NAME, itemExistedBean.getBrandName()));
				if(clBrandObCriteria.count() > 0) {
					clBrand = clBrandObCriteria.list().get(0);
				}
				
				// fetches the store department
				final OBCriteria<CLStoreDept> clStoreDeptObCriteria = OBDal.getInstance().createCriteria(CLStoreDept.class);
				clStoreDeptObCriteria.add(Restrictions.eq(CLStoreDept.PROPERTY_NAME, itemExistedBean.getStoreDept()));
				if(clStoreDeptObCriteria.count() > 0) {
					clStoreDept = clStoreDeptObCriteria.list().get(0);
				}
				
				// fetched the DMI department
				final OBCriteria<CLDepartment> clDeptObCriteria = OBDal.getInstance().createCriteria(CLDepartment.class);
				clDeptObCriteria.add(Restrictions.eq(CLDepartment.PROPERTY_NAME, itemExistedBean.getDmiDept()));
				if(clDeptObCriteria.count() > 0) {
					clDepartment = clDeptObCriteria.list().get(0);
				}
				
				
				// fetches the store universe
				final OBCriteria<ClStoreUniverse> clStoreUniverseObCriteria = OBDal.getInstance().createCriteria(ClStoreUniverse.class);
				clStoreUniverseObCriteria.add(Restrictions.eq(ClStoreUniverse.PROPERTY_COMMERCIALNAME, itemExistedBean.getStoreUniverse()));
				if(clStoreUniverseObCriteria.count() > 0) {
					clStoreUniverse = clStoreUniverseObCriteria.list().get(0);
				}
				
				
				// fetches the DMI Universe
				final OBCriteria<CLUniverse> clUniverseObCriteria = OBDal.getInstance().createCriteria(CLUniverse.class);
				clUniverseObCriteria.add(Restrictions.eq(CLUniverse.PROPERTY_NAME, itemExistedBean.getDmiUniverse()));
				if(clUniverseObCriteria.count() > 0) {
					clUniverse = clUniverseObCriteria.list().get(0);
				}
				
				// fetches the sub department
				final OBCriteria<CLSubdepartment> clSubDeptObCriteria = OBDal.getInstance().createCriteria(CLSubdepartment.class);
				clSubDeptObCriteria.add(Restrictions.eq(CLSubdepartment.PROPERTY_COMMERCIALNAME, itemExistedBean.getSubDept()));
				if(clSubDeptObCriteria.count() > 0) {
					clSubdepartment = clSubDeptObCriteria.list().get(0);
				}
				
				if(null != clBrand && null != clStoreDept && null != clDepartment && null != clStoreUniverse && null != clUniverse && null != clSubdepartment) {
					
					final OBCriteria<CLModel> clModelObCriteria = OBDal.getInstance().createCriteria(CLModel.class);
					clModelObCriteria.add(Restrictions.eq(CLModel.PROPERTY_MODELCODE, itemExistedBean.getModelCode()));
					if(clModelObCriteria.count() > 0) {
						System.out.println("updating!"+count);
						final CLModel clModel = clModelObCriteria.list().get(0);
						
						clModel.setBrand(clBrand);
						clModel.setDepartment(clDepartment);
						clModel.setStoreDepartment(clStoreDept);
						clModel.setUniverse(clUniverse);
						clModel.setStoreuniverse(clStoreUniverse);
						clModel.setSubdepartment(clSubdepartment);
						clModel.setUpdated(new Timestamp(new Date().getTime()));
						
						OBDal.getInstance().save(clModel);
						count++;
					}
				}
				if(count % 500 == 0)
					OBDal.getInstance().flush();
				
			}
			OBDal.getInstance().commitAndClose();
			workbook.close();
		
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
