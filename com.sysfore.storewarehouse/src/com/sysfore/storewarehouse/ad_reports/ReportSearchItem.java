/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2001-2009 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package com.sysfore.storewarehouse.ad_reports;

import in.decathlon.integration.PassiveDB;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportSearchItem extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
System.out.println(" Harshita is debugging for speed ");
    if (vars.commandIn("DEFAULT", "DIRECT")) {
	System.out.println(" DEFAULT MAI CHECK");
String strpdtName = vars.getGlobalVariable("inppdtName",
          "ReportSearchItem|pdtname", "");
 System.out.println("strpdtName: in default " + strpdtName);
String strModelName = vars.getGlobalVariable("inpModelName",
          "ReportSearchItem|modelname", "");
 System.out.println("strModelName: in default " + strModelName);
String strModelCode = vars.getGlobalVariable("inpModelCode",
          "ReportSearchItem|modelcode", "");
 System.out.println("strModelCode: in default " + strModelCode);
String strSupplier = vars.getGlobalVariable("inpBpname",
          "ReportSearchItem|supplier", "");
 System.out.println("strSupplier: in default " + strSupplier);
//String strNatofPdt = vars.getGlobalVariable("inpclNatureOfProductId",
        //  "SearchItem|natofpdt", "");
 String strNatofPdt = vars.getStringParameter("inpclNatureOfProductId", "");
 System.out.println("strNatofPdt: in default " + strNatofPdt);
String strDepartment = vars.getGlobalVariable("inpclDepartmentId",
          "ReportSearchItem|dept", "");
 System.out.println("strDepartment: in default " + strDepartment);
String strSport = vars.getGlobalVariable("inpclSportId",
          "ReportSearchItem|sport", "");
 System.out.println("strSport: in default " + strSport);
String strSportsCat = vars.getGlobalVariable("inpSportsCategory",
          "ReportSearchItem|sportscat", "");
 System.out.println("strSportsCat: in default " + strSportsCat);
String strMerchandiseCat = vars.getGlobalVariable("inpMerCategory",
          "ReportSearchItem|merchandisecat", "");
 System.out.println("strMerchandiseCat: in default " + strMerchandiseCat);
String strBrand = vars.getGlobalVariable("inpclBrandId",
          "ReportSearchItem|brand", "");
 System.out.println("strBrand: in default " + strBrand);
String strComBrand = vars.getGlobalVariable("inpclComponentBrandId",
          "ReportSearchItem|combrand", "");
 System.out.println("strComBrand: in default " + strComBrand);

 String strBluePdt = vars.getGlobalVariable("inpBlueproduct",
          "ReportSearchItem|bluepdt", "");

 System.out.println("strBluePdt: in default " + strBluePdt);

String strLifestage = vars.getGlobalVariable("inpemclLifestage",
          "ReportSearchItem|lifestage", "");
 System.out.println("strLifestage: in default " + strLifestage);
     // }
      setHistoryCommand(request, "DIRECT");
      printPageDataSheet(response, vars, strpdtName, strModelName, strModelCode, strSupplier, strNatofPdt,
	  strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage, "DEFAULT");
    } else if (vars.commandIn("FIND")) {
     String strName = vars.getRequestGlobalVariable("inppdtName",
          "ReportSearchItem|pdtname");
     System.out.println("strName: in Find " + strName);
      String strModelName = vars.getRequestGlobalVariable("inpModelName",
          "ReportSearchItem|modelname");
      System.out.println("strModelName: in Find " + strModelName);

      String strModelCode = vars.getRequestGlobalVariable("inpModelCode",
          "ReportSearchItem|modelcode");
      System.out.println("strModelCode: in Find " + strModelCode);

      String strSupplier = vars.getRequestGlobalVariable("inpBpname",
          "ReportSearchItem|supplier");
      System.out.println("strSupplier: in Find " + strSupplier);

      //String strNatofPdt = vars.getRequestGlobalVariable("inpclNatureOfProductId",
        //  "SearchItem|natofpdt");
      String strNatofPdt = vars.getStringParameter("inpclNatureOfProductId", "");
      System.out.println("strNatofPdt: in Find " + strNatofPdt);

      String strDepartment = vars.getRequestGlobalVariable("inpclDepartmentId",
          "ReportSearchItem|dept");
      System.out.println("strDepartment: in Find " + strDepartment);

      String strSport = vars.getRequestGlobalVariable("inpclSportId",
          "ReportSearchItem|sport");
      System.out.println("strSport: in Find " + strSport);

      String strSportsCat = vars.getRequestGlobalVariable("inpSportsCategory",
          "ReportSearchItem|sportscat");
      System.out.println("strSportsCat: in Find " + strSportsCat);

      String strMerchandiseCat = vars.getRequestGlobalVariable("inpMerCategory",
          "ReportSearchItem|merchandisecat");
      System.out.println("strMerchandiseCat: in Find " + strMerchandiseCat);

      String strBrand = vars.getRequestGlobalVariable("inpclBrandId",
          "ReportSearchItem|brand");
      System.out.println("strBrand: in Find " + strBrand);

      String strComBrand = vars.getRequestGlobalVariable("inpclComponentBrandId",
          "ReportSearchItem|combrand");
      System.out.println("strComBrand: in Find " + strComBrand);

        String strBluePdt = vars.getRequestGlobalVariable("inpBlueproduct",
          "ReportSearchItem|bluepdt");
 System.out.println("strBluePdt: in default " + strBluePdt);

      System.out.println("strBluePdt: in Find " + strBluePdt);

      String strLifestage = vars.getRequestGlobalVariable("inpemclLifestage",
          "ReportSearchItem|lifestage");
      System.out.println("strLifestage: in Find " + strLifestage);

vars.setMessage("ReportSearchItem", processSearchItem(response, vars, strName, strModelName, strModelCode, strSupplier, strNatofPdt, strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage));

      printPageDataSheet(response, vars, strName, strModelName, strModelCode, strSupplier, strNatofPdt, strDepartment, strSport, strSportsCat,
             strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage, "FIND");
    } else if (vars.commandIn("PRINT_XLS")) {
		System.out.println(" in xls code check here ");	
      String strName = vars.getRequestGlobalVariable("inppdtName",
          "ReportSearchItem|pdtname");
     System.out.println("strName: in XLS " + strName);
      String strModelName = vars.getRequestGlobalVariable("inpModelName",
          "ReportSearchItem|modelname");
      System.out.println("strModelName: in XLS " + strModelName);

      String strModelCode = vars.getRequestGlobalVariable("inpModelCode",
          "SearchReportSearchItemItem|modelcode");
      System.out.println("strModelCode: in XLS " + strModelCode);

      String strSupplier = vars.getRequestGlobalVariable("inpBpname",
          "ReportSearchItem|supplier");
      System.out.println("strSupplier: in XLS " + strSupplier);

     // String strNatofPdt = vars.getRequestGlobalVariable("inpclNatureOfProductId",
      //    "SearchItem|natofpdt");
      String strNatofPdt = vars.getStringParameter("inpclNatureOfProductId", "");
      System.out.println("strNatofPdt: in XLS " + strNatofPdt);

      String strDepartment = vars.getRequestGlobalVariable("inpclDepartmentId",
          "ReportSearchItem|dept");
      System.out.println("strDepartment: in XLS " + strDepartment);

      String strSport = vars.getRequestGlobalVariable("inpclSportId",
          "ReportSearchItem|sport");
      System.out.println("strSport: in XLS " + strSport);

      String strSportsCat = vars.getRequestGlobalVariable("inpSportsCategory",
          "ReportSearchItem|sportscat");
      System.out.println("strSportsCat: in XLS " + strSportsCat);

      String strMerchandiseCat = vars.getRequestGlobalVariable("inpMerCategory",
          "ReportSearchItem|merchandisecat");
      System.out.println("strMerchandiseCat: in XLS " + strMerchandiseCat);

      String strBrand = vars.getRequestGlobalVariable("inpclBrandId",
          "ReportSearchItem|brand");
      System.out.println("strBrand: in XLS " + strBrand);

      String strComBrand = vars.getRequestGlobalVariable("inpclComponentBrandId",
          "ReportSearchItem|combrand");
      System.out.println("strComBrand: in XLS " + strComBrand);

        String strBluePdt = vars.getRequestGlobalVariable("inpBlueproduct",
          "ReportSearchItem|bluepdt");
      System.out.println("strBluePdt: in XLS " + strBluePdt);

      String strLifestage = vars.getRequestGlobalVariable("inpemclLifestage",
          "ReportSearchItem|lifestage");
      System.out.println("strLifestage: in XLS " + strLifestage);
      setHistoryCommand(request, "DIRECT");
      printPageDataXls(response, vars, strName, strModelName, strModelCode, strSupplier, strNatofPdt, strDepartment, strSport, strSportsCat,
             strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage);
    } else
      pageError(response);
  }

  void printPageDataXls(HttpServletResponse response, VariablesSecureApp vars,
      String strName, String strModelName, String strModelCode, String strSupplier, String strNatofPdt, String strDepartment,
      String strSport, String strSportsCat, String strMerchandiseCat, String strBrand, String strComBrand,
      String strBluePdt, String strLifestage ) throws IOException,
      ServletException {


    String strReportName = null;
    ReportSearchItemData[] data = null;

 //  data = SearchItemData.select(this, Utility.getContext(this, vars, "#User_Client", "SearchItem"),
      //      Utility.getContext(this, vars, "#AccessibleOrgTree", "SearchItem"), strModelName, strModelCode,strLifestage,
        //      strName);// strSupplier,
     data = ReportSearchItemData.select(this, Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"),
             Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),strName, strModelName, strModelCode, strSupplier,
             strNatofPdt, strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage);
                 String strOutput = "Xls";
		         System.out.println("after fetching data" + data);
		         Connection conn = null;
		 		try {
		 			conn = PassiveDB.getInstance().getSupplyDBConnection();
		 		} catch (ClassNotFoundException e) {
		 			e.printStackTrace();
		 		} catch (SQLException e) {
		 			e.printStackTrace();
		 		}
		 		PreparedStatement pst = null;
		 		ResultSet rs = null;
		 		for(int i=0;i<=data.length-1;i++) {
		 			try {
		 				pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
		 				System.out.println("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
		 				rs = pst.executeQuery();
		 				int flag = 0;
		 				while (rs.next()) {
		 					flag++;
		 					System.out.println("CAC "+rs.getLong("msdqty"));
		 					data[i].cacstock = ""+rs.getLong("msdqty");
		 				}
		 				if(flag == 0) {
		 						System.out.println("Flag:"+flag);
		 					data[i].cacstock = "0";
		 				}
		 			} catch (SQLException e) {
		 				e.printStackTrace();
		 			}
		 			try {
		 				pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Omega' and em_sw_iscar = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Omega' and em_sw_iscar = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
		 				System.out.println("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Omega' and em_sw_iscar = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Omega' and em_sw_iscar = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
		 				rs = pst.executeQuery();
		 				int flag = 0;
		 				while (rs.next()) {
		 					flag++;
		 					System.out.println("CAR "+rs.getLong("msdqty"));
		 					data[i].carstock = ""+rs.getLong("msdqty");
		 				}
		 				if(flag == 0) {
		 					System.out.println("Flag:"+flag);
		 					data[i].carstock = "0";
		 				}
		 			} catch (SQLException e) {
		 				e.printStackTrace();
		 			}
		 			try {
		 				pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Hub') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Hub') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
		 				System.out.println("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Hub') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Hub') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
		 				rs = pst.executeQuery();
		 				int flag = 0;
		 				while (rs.next()) {
		 					flag++;
		 					System.out.println("HUB "+rs.getLong("msdqty"));
		 					data[i].hubstock = ""+rs.getLong("msdqty");
		 				}
		 				if(flag == 0) {
		 					System.out.println("Flag:"+flag);
		 					data[i].hubstock = "0";
		 				}
		 			} catch (SQLException e) {
		 				e.printStackTrace();
		 			}
		 			try {
		 				pst = conn.prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id in (select m_locator_id from m_locator where value in ('InTransit Warehouse')) and  m_product_id='"+data[i].mProductId+"'),0),'0') as itrstore;");
		 				System.out.println("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id in (select m_locator_id from m_locator where value in ('InTransit Warehouse')) and  m_product_id='"+data[i].mProductId+"'),0),'0') as itrstore;");
		 				rs = pst.executeQuery();
		 				int flag = 0;
		 				while (rs.next()) {
		 					flag++;
		 					System.out.println("ITR "+rs.getLong("itrstore"));
		 					data[i].itrstore = ""+rs.getLong("itrstore");
		 				}
		 				if(flag == 0) {
		 					data[i].itrstore = "0";
		 				}
		 			} catch (SQLException e) {
		 				e.printStackTrace();
		 			}
		 			try {
		 				pst = conn.prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id in (select m_locator_id from m_locator where value in ('InTransit Hub')) and  m_product_id='"+data[i].mProductId+"'),0),'0') as itrhub;");
		 				System.out.println("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id in (select m_locator_id from m_locator where value in ('InTransit Hub')) and  m_product_id='"+data[i].mProductId+"'),0),'0') as itrhub;");
		 				rs = pst.executeQuery();
		 				int flag = 0;
		 				while (rs.next()) {
		 					flag++;
		 					System.out.println("ITR Hub "+rs.getLong("itrhub"));
		 					data[i].itrhub = ""+rs.getLong("itrhub");
		 				}
		 				if(flag == 0) {
		 					data[i].itrhub = "0";
		 				}
		 			} catch (SQLException e) {
		 				e.printStackTrace();
		 			}
		 		}
            strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/ReportSearchItem.jrxml";
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    //parameters.put("group", "no");
    renderJR(vars, response, strReportName, "xls", parameters, data, null);
    try {
		if(conn != null) {
			conn.close();
		}
		if(pst != null && rs != null) {
			pst.close();
			rs.close();
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
  }

  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strName, String strModelName, String strModelCode, String strSupplier, String strNatofPdt, String strDepartment,
      String strSport, String strSportsCat, String strMerchandiseCat, String strBrand, String strComBrand,
      String strBluePdt, String strLifestage, String command) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    if (log4j.isDebugEnabled()){}
    XmlDocument xmlDocument;

    ReportSearchItemData[] data = null;
	System.out.println(" i am in datasheet function ");
	if(command.equals("DEFAULT"))
		{
		
		}
		else{
 if(strBluePdt == null)
    {
        System.out.println("strBluePdt: in if blupdt is null condition printPageDataSheet " + strBluePdt);
        data = ReportSearchItemData.selectBluproduct(this, Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"),
             Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),strName, strModelName, strModelCode, strSupplier,
             strNatofPdt, strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand, strComBrand, strLifestage);
    }
    else{
        System.out.println("strBluePdt: in else blupdt is not null condition printPageDataSheet " + strBluePdt);
      //data = SearchItemData.select(this, Utility.getContext(this, vars, "#User_Client", "SearchItem"),
           //   Utility.getContext(this, vars, "#AccessibleOrgTree", "SearchItem"), strModelName, strModelCode,strLifestage,
           // strName);// strSupplier,
      data = ReportSearchItemData.select(this, Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"),
             Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),strName, strModelName, strModelCode, strSupplier,
             strNatofPdt, strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage);// strSupplier,
    }
		}
    if (data == null || data.length == 0) {
      data = ReportSearchItemData.set();

   }
    if (vars.commandIn("DEFAULT")) {

      xmlDocument = xmlEngine.readXmlTemplate(
          "com/sysfore/storewarehouse/ad_reports/ReportSearchItem").createXmlDocument();
      data = ReportSearchItemData.set();
    } else {
      xmlDocument = xmlEngine.readXmlTemplate(
          "com/sysfore/storewarehouse/ad_reports/ReportSearchItem").createXmlDocument();
    }
    System.out.println("after sheet fetching data" + data.length);
	Connection conn = null;
	try {
		conn = PassiveDB.getInstance().getSupplyDBConnection();
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
	} catch (SQLException e) {
		e.printStackTrace();
	}
	PreparedStatement pst = null;
	ResultSet rs = null;
	for(int i=0;i<=data.length-1;i++) {
		try {
			pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
			System.out.println("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
			rs = pst.executeQuery();
			int flag = 0;
			while (rs.next()) {
				flag++;
				System.out.println("CAC "+rs.getLong("msdqty"));
				data[i].cacstock = ""+rs.getLong("msdqty");
			}
			if(flag == 0) {
					System.out.println("Flag:"+flag);
				data[i].cacstock = "0";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Omega' and em_sw_iscar = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Omega' and em_sw_iscar = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
			System.out.println("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Omega' and em_sw_iscar = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Omega' and em_sw_iscar = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
			rs = pst.executeQuery();
			int flag = 0;
			while (rs.next()) {
				flag++;
				System.out.println("CAR "+rs.getLong("msdqty"));
				data[i].carstock = ""+rs.getLong("msdqty");
			}
			if(flag == 0) {
				System.out.println("Flag:"+flag);
				data[i].carstock = "0";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Hub') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Hub') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
			System.out.println("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Hub') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Hub') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+data[i].mProductId+"';");
			rs = pst.executeQuery();
			int flag = 0;
			while (rs.next()) {
				flag++;
				System.out.println("HUB "+rs.getLong("msdqty"));
				data[i].hubstock = ""+rs.getLong("msdqty");
			}
			if(flag == 0) {
				System.out.println("Flag:"+flag);
				data[i].hubstock = "0";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			pst = conn.prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id in (select m_locator_id from m_locator where value in ('InTransit Warehouse')) and  m_product_id='"+data[i].mProductId+"'),0),'0') as itrstore;");
			System.out.println("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id in (select m_locator_id from m_locator where value in ('InTransit Warehouse')) and  m_product_id='"+data[i].mProductId+"'),0),'0') as itrstore;");
			rs = pst.executeQuery();
			int flag = 0;
			while (rs.next()) {
				flag++;
				System.out.println("ITR "+rs.getLong("itrstore"));
				data[i].itrstore = ""+rs.getLong("itrstore");
			}
			if(flag == 0) {
				data[i].itrstore = "0";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			pst = conn.prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id in (select m_locator_id from m_locator where value in ('InTransit Hub')) and  m_product_id='"+data[i].mProductId+"'),0),'0') as itrhub;");
			System.out.println("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id in (select m_locator_id from m_locator where value in ('InTransit Hub')) and  m_product_id='"+data[i].mProductId+"'),0),'0') as itrhub;");
			rs = pst.executeQuery();
			int flag = 0;
			while (rs.next()) {
				flag++;
				System.out.println("ITR Hub "+rs.getLong("itrhub"));
				data[i].itrhub = ""+rs.getLong("itrhub");
			}
			if(flag == 0) {
				data[i].itrhub = "0";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportSearchItem", false, "", "", "",
        false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sysfore.storewarehouse.ad_reports.ReportSearchItem");		  
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportSearchItem.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportSearchItem.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
   /* {
      OBError myMessage = vars.getMessage("ReportSearchItem");
      vars.removeMessage("ReportSearchItem");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }*/

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("pdtname", strName);
    xmlDocument.setParameter("ModelName", strModelName);
    xmlDocument.setParameter("ModelCode", strModelCode);
    xmlDocument.setParameter("Supplier", strSupplier);
    xmlDocument.setParameter("BlueProduct", strBluePdt);
    xmlDocument.setParameter("NatureofProduct", strNatofPdt);
    xmlDocument.setParameter("Department", strDepartment);
    xmlDocument.setParameter("Sport", strSport);
    xmlDocument.setParameter("SportsCategory", strSportsCat);
    xmlDocument.setParameter("MerCategory", strMerchandiseCat);
    xmlDocument.setParameter("Brand", strBrand);
    xmlDocument.setParameter("ComBrand", strComBrand);
    xmlDocument.setParameter("Lifestage", strLifestage);

 // BEGIN check for any pending message to be shown and clear it from the session
      OBError myMessage = vars.getMessage("ReportSearchItem");
      vars.removeMessage("ReportSearchItem");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    // END check for any pending message to be shown and clear it from the session


     try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_natureofproduct_Id",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),
          Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem",
          strNatofPdt);
      xmlDocument.setData("reportcl_NatureOfProduct_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_department_id",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),
          Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem",
          strDepartment);
      xmlDocument.setData("reportcl_Department_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

     try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_sport_id",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),
          Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem",
          strSport);
      xmlDocument.setData("reportcl_Sport_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_brand_id",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),
          Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem",
          strBrand);
      xmlDocument.setData("reportcl_Brand_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

     try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_component_brand_id",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),
          Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem",
          strComBrand);
      xmlDocument.setData("reportcl_Component_Brand_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    if (log4j.isDebugEnabled())
      log4j.debug("ListData.select lifestage:" + strLifestage);
      try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "17", "EM_Cl_Lifestage",
          "8E405997F12A4DB79252DDF3CAC421D6", "", Utility.getContext(this, vars,
              "#AccessibleOrgTree", "ReportSearchItem"), Utility.getContext(this, vars,
              "#User_Client", "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem", strLifestage);
      xmlDocument.setData("reportEM_cl_LIFESTAGE", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }


if (log4j.isDebugEnabled())
      log4j.debug("ListData.select Blueproduct:" + strBluePdt);
      try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "17", "Blueproduct",
          "C86D244F2916406894C1AC4CC0C05854", "", Utility.getContext(this, vars,
              "#AccessibleOrgTree", "ReportSearchItem"), Utility.getContext(this, vars,
              "#User_Client", "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem", strBluePdt);
      xmlDocument.setData("reportBlueproduct", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    } finally {
		try {
			if(conn != null) {
				conn.close();
			}
			if(pst != null && rs != null) {
				pst.close();
				rs.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

     xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }

 private OBError processSearchItem(HttpServletResponse response, VariablesSecureApp vars,String strName, 
 String strModelName, String strModelCode, String strSupplier, String strNatofPdt, String strDepartment, String strSport, String strSportsCat, String strMerchandiseCat, String strBrand, String strComBrand, String strBluePdt, String strLifestage) {
    ReportSearchItemData[] data1 = null;

    OBError myMessage = new OBError();
    try{
 data1 = ReportSearchItemData.select(this, Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"), Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),strName, strModelName, strModelCode, strSupplier,
 strNatofPdt, strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage);

  int datalength = data1.length;

      System.out.println("datalength==" +datalength);

       if(datalength == 0){
        myMessage.setTitle("Information");
        myMessage.setType("INFO");
       }
        myMessage.setMessage(Utility.messageBD(this, "CL_NoData", vars.getLanguage()));

    } catch (Exception e) {
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "CL_NoData");
    }
    return myMessage;
}

  public String getServletInfo() {
    return "Servlet ReportSearchItem. This Servlet was made by Pablo Sarobe";
  }
}
