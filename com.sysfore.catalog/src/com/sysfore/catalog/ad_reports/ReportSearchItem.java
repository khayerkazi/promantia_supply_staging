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
package com.sysfore.catalog.ad_reports;

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
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

import com.sysfore.catalog.CLModel;

public class ReportSearchItem extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT", "DIRECT")) {

      String strpdtName = vars.getGlobalVariable("inppdtName", "ReportSearchItem|pdtname", "");

      String strModelName = vars.getGlobalVariable("inpModelName", "ReportSearchItem|modelname", "");

      String strModelCode = vars.getGlobalVariable("inpModelCode", "ReportSearchItem|modelcode", "");

      String strSupplier = vars.getGlobalVariable("inpBpname", "ReportSearchItem|supplier", "");

      String strNatofPdt = vars.getStringParameter("inpclNatureOfProductId", "");

      String strDepartment = vars.getGlobalVariable("inpclDepartmentId", "ReportSearchItem|dept", "");

      String strSport = vars.getGlobalVariable("inpclSportId", "ReportSearchItem|sport", "");

      String strSportsCat = vars.getGlobalVariable("inpSportsCategory", "ReportSearchItem|sportscat", "");

      String strMerchandiseCat = vars.getGlobalVariable("inpMerCategory", "ReportSearchItem|merchandisecat", "");

      String strBrand = vars.getGlobalVariable("inpclBrandId", "ReportSearchItem|brand", "");

      String strComBrand = vars.getGlobalVariable("inpclComponentBrandId", "ReportSearchItem|combrand", "");

      String strBluePdt = vars.getGlobalVariable("inpBlueproduct", "ReportSearchItem|bluepdt", "");

      String strLifestage = vars.getGlobalVariable("inpemclLifestage", "ReportSearchItem|lifestage", "");

      String strMerCatCombo = vars.getGlobalVariable("inpMerCategoryCombo", "ReportSearchItem|mercategorycombo", "");

      setHistoryCommand(request, "DIRECT");

      printPageDataSheet(response, vars, strpdtName, strModelName, strModelCode, strSupplier,
          strNatofPdt, strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand,
          strComBrand, strBluePdt, strLifestage, strMerCatCombo, "DEFAULT");
      
    } else if (vars.commandIn("UPDATE")) {
      String strpdtName = vars.getRequestGlobalVariable("inppdtName", "ReportSearchItem|pdtname");

      String strModelName = vars.getRequestGlobalVariable("inpModelName", "ReportSearchItem|modelname");

      String strModelCode = vars.getRequestGlobalVariable("inpModelCode", "ReportSearchItem|modelcode");

      String strSupplier = vars.getRequestGlobalVariable("inpBpname", "ReportSearchItem|supplier");

      String strNatofPdt = vars.getStringParameter("inpclNatureOfProductId", "");

      String strDepartment = vars.getRequestGlobalVariable("inpclDepartmentId", "ReportSearchItem|dept");

      String strSport = vars.getRequestGlobalVariable("inpclSportId", "ReportSearchItem|sport");

      String strSportsCat = vars.getRequestGlobalVariable("inpSportsCategory", "ReportSearchItem|sportscat");

      String strBrand = vars.getRequestGlobalVariable("inpclBrandId", "ReportSearchItem|brand");

      String strComBrand = vars.getRequestGlobalVariable("inpclComponentBrandId", "ReportSearchItem|combrand");

      String strBluePdt = vars.getRequestGlobalVariable("inpBlueproduct", "ReportSearchItem|bluepdt");

      String strLifestage = vars.getRequestGlobalVariable("inpemclLifestage", "ReportSearchItem|lifestage");

      String strMerCatCombo = vars.getRequestGlobalVariable("inpMerCategoryCombo", "ReportSearchItem|mercategorycombo");
      
      // to set Merchandise category
      
      CLModel model = OBDal.getInstance().get(CLModel.class, strMerCatCombo);
      String strMerchandiseCat = "";
      if (model != null) {
        vars.setSessionValue("inpMerCategory", model.getMerchandiseCategory().toString());
        strMerchandiseCat = model.getMerchandiseCategory();
      }

      printPageDataSheet(response, vars, strpdtName, strModelName, strModelCode, strSupplier,
          strNatofPdt, strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand,
          strComBrand, strBluePdt, strLifestage, strMerCatCombo, "DEFAULT");
    }

    else if (vars.commandIn("FIND")) {
      String strName = vars.getRequestGlobalVariable("inppdtName", "ReportSearchItem|pdtname");

      String strModelName = vars.getRequestGlobalVariable("inpModelName", "ReportSearchItem|modelname");

      String strModelCode = vars.getRequestGlobalVariable("inpModelCode", "ReportSearchItem|modelcode");

      String strSupplier = vars.getRequestGlobalVariable("inpBpname", "ReportSearchItem|supplier");

      String strNatofPdt = vars.getStringParameter("inpclNatureOfProductId", "");

      String strDepartment = vars.getRequestGlobalVariable("inpclDepartmentId", "ReportSearchItem|dept");

      String strSport = vars.getRequestGlobalVariable("inpclSportId", "ReportSearchItem|sport");

      String strSportsCat = vars.getRequestGlobalVariable("inpSportsCategory", "ReportSearchItem|sportscat");

      String strBrand = vars.getRequestGlobalVariable("inpclBrandId", "ReportSearchItem|brand");

      String strComBrand = vars.getRequestGlobalVariable("inpclComponentBrandId", "ReportSearchItem|combrand");

      String strBluePdt = vars.getRequestGlobalVariable("inpBlueproduct", "ReportSearchItem|bluepdt");

      String strLifestage = vars.getRequestGlobalVariable("inpemclLifestage", "ReportSearchItem|lifestage");

      String strMerCatCombo = vars.getRequestGlobalVariable("inpMerCategoryCombo", "ReportSearchItem|mercategorycombo");

      CLModel model = OBDal.getInstance().get(CLModel.class, strMerCatCombo);
      String strMerchandiseCat = "";
      if (model != null) {
        vars.setSessionValue("inpMerCategory", model.getMerchandiseCategory().toString());
        strMerchandiseCat = model.getMerchandiseCategory();
      }

      vars.setMessage("ReportSearchItem",processSearchItem(response, vars, strName, strModelName, strModelCode, strSupplier,strNatofPdt, strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage, strMerCatCombo));

      printPageDataSheet(response, vars, strName, strModelName, strModelCode, strSupplier,
          strNatofPdt, strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand,
          strComBrand, strBluePdt, strLifestage, strMerCatCombo, "FIND");
      
    } else if (vars.commandIn("PRINT_XLS")) {

      String strName = vars.getRequestGlobalVariable("inppdtName", "ReportSearchItem|pdtname");

      String strModelName = vars.getRequestGlobalVariable("inpModelName", "ReportSearchItem|modelname");

      String strModelCode = vars.getRequestGlobalVariable("inpModelCode", "SearchReportSearchItemItem|modelcode");

      String strSupplier = vars.getRequestGlobalVariable("inpBpname", "ReportSearchItem|supplier");

      String strNatofPdt = vars.getStringParameter("inpclNatureOfProductId", "");

      String strDepartment = vars.getRequestGlobalVariable("inpclDepartmentId", "ReportSearchItem|dept");

      String strSport = vars.getRequestGlobalVariable("inpclSportId", "ReportSearchItem|sport");

      String strSportsCat = vars.getRequestGlobalVariable("inpSportsCategory", "ReportSearchItem|sportscat");

      String strBrand = vars.getRequestGlobalVariable("inpclBrandId", "ReportSearchItem|brand");

      String strComBrand = vars.getRequestGlobalVariable("inpclComponentBrandId", "ReportSearchItem|combrand");

      String strBluePdt = vars.getRequestGlobalVariable("inpBlueproduct", "ReportSearchItem|bluepdt");

      String strLifestage = vars.getRequestGlobalVariable("inpemclLifestage", "ReportSearchItem|lifestage");

      String strMerCatCombo = vars.getRequestGlobalVariable("inpMerCategoryCombo", "ReportSearchItem|mercategorycombo");

      CLModel model = OBDal.getInstance().get(CLModel.class, strMerCatCombo);
      String strMerchandiseCat = "";
      if (model != null) {
        vars.setSessionValue("inpMerCategory", model.getMerchandiseCategory().toString());
        strMerchandiseCat = model.getMerchandiseCategory();
      }

      setHistoryCommand(request, "DIRECT");
      
      printPageDataXls(response, vars, strName, strModelName, strModelCode, strSupplier,
          strNatofPdt, strDepartment, strSport, strSportsCat, strMerchandiseCat, strBrand,
          strComBrand, strBluePdt, strLifestage, strMerCatCombo);
      
    } else
      pageError(response);
  }

  void printPageDataXls(HttpServletResponse response, VariablesSecureApp vars, String strName,
      String strModelName, String strModelCode, String strSupplier, String strNatofPdt,
      String strDepartment, String strSport, String strSportsCat, String strMerchandiseCat,
      String strBrand, String strComBrand, String strBluePdt, String strLifestage,
      String strMerCatCombo) throws IOException, ServletException {

    String strReportName = null;
    ReportSearchItemData[] data = null;
    System.out.println("strMerCatCombo-> " + strMerCatCombo);
   
    data = ReportSearchItemData.select(this,
        Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"),
        Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"), strName,
        strModelName, strModelCode, strSupplier, strNatofPdt, strDepartment, strSport,
        strSportsCat, strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage);
    String strOutput = "Xls";

    System.out.println("after fetching data" + data.length);
    Connection conn = null;
    try {
      conn = PassiveDB.getInstance().getRetailDBConnection();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    PreparedStatement pst = null;
    ResultSet rs = null;
    for (int i = 0; i <= data.length - 1; i++) {
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1001') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].ssjstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].ssjstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1002') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].bgtstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].bgtstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1003') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].plastore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].plastore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1004') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].thnstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].thnstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1005') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].ahmdstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].ahmdstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1006') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].shmstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].shmstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1007') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].mysstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].mysstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1008') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].zrkstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].zrkstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1009') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].klmstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].klmstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1010') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].cmbstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].cmbstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1011') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].wagstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].wagstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1012') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].ludstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].ludstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1013') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].ndastore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].ndastore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1014') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].mohstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].mohstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1015') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].nskstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].nskstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1016') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;
          data[i].hsrstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].hsrstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
          pst = conn
              .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1017') and  m_product_id='"
                  + data[i].mProductId + "'),0),'0') as msdqty");
          
          rs = pst.executeQuery();
          int flag = 0;
          while (rs.next()) {
            flag++;

            data[i].aurstore = "" + rs.getLong("msdqty");
          }
          if (flag == 0) {
            data[i].aurstore = "0";
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
      try {
          pst = conn
              .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1019') and  m_product_id='"
                  + data[i].mProductId + "'),0),'0') as msdqty");
          
          rs = pst.executeQuery();
          int flag = 0;
          while (rs.next()) {
            flag++;

            data[i].uppstore = "" + rs.getLong("msdqty");
          }
          if (flag == 0) {
            data[i].uppstore = "0";
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
      try {
          pst = conn
              .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1020') and  m_product_id='"
                  + data[i].mProductId + "'),0),'0') as msdqty");
          rs = pst.executeQuery();
          int flag = 0;
          while (rs.next()) {
            flag++;
            data[i].abvstore = "" + rs.getLong("msdqty");
          }
          if (flag == 0) {
            data[i].abvstore = "0";
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
      try {
          pst = conn
              .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1021') and  m_product_id='"
                  + data[i].mProductId + "'),0),'0') as msdqty");
          rs = pst.executeQuery();
          int flag = 0;
          while (rs.next()) {
            flag++;

            data[i].mcystore = "" + rs.getLong("msdqty");
          }
          if (flag == 0) {
            data[i].mcystore = "0";
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
    }
    strReportName = "@basedesign@/com/sysfore/catalog/ad_reports/ReportSearchItem.jrxml";
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    // parameters.put("group", "no");
    renderJR(vars, response, strReportName, "xls", parameters, data, null);
    try {
      if (conn != null) {
        conn.close();
      }
      if (pst != null && rs != null) {
        pst.close();
        rs.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars, String strName,
      String strModelName, String strModelCode, String strSupplier, String strNatofPdt,
      String strDepartment, String strSport, String strSportsCat, String strMerchandiseCat,
      String strBrand, String strComBrand, String strBluePdt, String strLifestage,
      String strMerCatCombo, String command) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    if (log4j.isDebugEnabled()) {
    }
    XmlDocument xmlDocument;

    ReportSearchItemData[] data = null;

    if (command.equals("DEFAULT")) {

    } else {
      if (strBluePdt == null) {

        data = ReportSearchItemData.selectBluproduct(this,
            Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"),
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"), strName,
            strModelName, strModelCode, strSupplier, strNatofPdt, strDepartment, strSport,
            strSportsCat, strMerchandiseCat, strBrand, strComBrand, strLifestage);
      } else {

        data = ReportSearchItemData.select(this,
            Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"),
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"), strName,
            strModelName, strModelCode, strSupplier, strNatofPdt, strDepartment, strSport,
            strSportsCat, strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage);
      }
    }
    if (data == null || data.length == 0) {
      data = ReportSearchItemData.set();

    }
    if (vars.commandIn("DEFAULT")) {

      xmlDocument = xmlEngine.readXmlTemplate("com/sysfore/catalog/ad_reports/ReportSearchItem")
          .createXmlDocument();
      data = ReportSearchItemData.set();
    } else {
      xmlDocument = xmlEngine.readXmlTemplate("com/sysfore/catalog/ad_reports/ReportSearchItem")
          .createXmlDocument();
    }

    System.out.println("after sheet fetching data" + data.length);
    Connection conn = null;
    try {
      conn = PassiveDB.getInstance().getRetailDBConnection();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    PreparedStatement pst = null;
    ResultSet rs = null;
    for (int i = 0; i <= data.length - 1; i++) {
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1001') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].ssjstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].ssjstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1002') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].bgtstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].bgtstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1003') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].plastore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].plastore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1004') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].thnstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].thnstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1005') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].ahmdstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].ahmdstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1006') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].shmstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].shmstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1007') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].mysstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].mysstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1008') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].zrkstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].zrkstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1009') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].klmstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].klmstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1010') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].cmbstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].cmbstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1011') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].wagstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].wagstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1012') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].ludstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].ludstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1013') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].ndastore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].ndastore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1014') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].mohstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].mohstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1015') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;

          data[i].nskstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].nskstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        pst = conn
            .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1016') and  m_product_id='"
                + data[i].mProductId + "'),0),'0') as msdqty");
        rs = pst.executeQuery();
        int flag = 0;
        while (rs.next()) {
          flag++;
          data[i].hsrstore = "" + rs.getLong("msdqty");
        }
        if (flag == 0) {
          data[i].hsrstore = "0";
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
          pst = conn
              .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1017') and  m_product_id='"
                  + data[i].mProductId + "'),0),'0') as msdqty");
          
          rs = pst.executeQuery();
          int flag = 0;
          while (rs.next()) {
            flag++;

            data[i].aurstore = "" + rs.getLong("msdqty");
          }
          if (flag == 0) {
            data[i].aurstore = "0";
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
      try {
          pst = conn
              .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1019') and  m_product_id='"
                  + data[i].mProductId + "'),0),'0') as msdqty");
          
          rs = pst.executeQuery();
          int flag = 0;
          while (rs.next()) {
            flag++;

            data[i].uppstore = "" + rs.getLong("msdqty");
          }
          if (flag == 0) {
            data[i].uppstore = "0";
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
      try {
          pst = conn
              .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1020') and  m_product_id='"
                  + data[i].mProductId + "'),0),'0') as msdqty");
          rs = pst.executeQuery();
          int flag = 0;
          while (rs.next()) {
            flag++;
            data[i].abvstore = "" + rs.getLong("msdqty");
          }
          if (flag == 0) {
            data[i].abvstore = "0";
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
      try {
          pst = conn
              .prepareStatement("select coalesce(round((select sum(qtyonhand) from m_storage_detail where m_locator_id=(select m_locator_id from m_locator where value like 'Saleable IN1021') and  m_product_id='"
                  + data[i].mProductId + "'),0),'0') as msdqty");
          rs = pst.executeQuery();
          int flag = 0;
          while (rs.next()) {
            flag++;

            data[i].mcystore = "" + rs.getLong("msdqty");
          }
          if (flag == 0) {
            data[i].mcystore = "0";
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
          "com.sysfore.catalog.ad_reports.ReportSearchItem");
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
    xmlDocument.setParameter("MerCategoryCombo", strMerCatCombo);

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
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
          "cl_natureofproduct_Id", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportSearchItem"), Utility.getContext(this, vars, "#User_Client",
              "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem", strNatofPdt);
      xmlDocument.setData("reportcl_NatureOfProduct_Id", "liststructure",
          comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
          "cl_department_id", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportSearchItem"), Utility.getContext(this, vars, "#User_Client",
              "ReportSearchItem"), 0);
      Utility
          .fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem", strDepartment);
      xmlDocument.setData("reportcl_Department_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_sport_id", "",
          "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),
          Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem", strSport);
      xmlDocument.setData("reportcl_Sport_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_brand_id", "",
          "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"),
          Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem", strBrand);
      xmlDocument.setData("reportcl_Brand_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
          "cl_component_brand_id", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportSearchItem"), Utility.getContext(this, vars, "#User_Client",
              "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem", strComBrand);
      xmlDocument.setData("reportcl_Component_Brand_Id", "liststructure",
          comboTableData.select(false));
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
    }

    // for Merchandise category strMerCatCombo merchandise_category

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "CL_Model_ID", "",
          "B96DCE37B2B64D9CAB2659BA290FFE10", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportSearchItem"), Utility.getContext(this, vars, "#User_Client",
              "ReportSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportSearchItem",
          strMerCatCombo);
      xmlDocument.setData("reportMerCategoryCombo", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (pst != null && rs != null) {
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

  private OBError processSearchItem(HttpServletResponse response, VariablesSecureApp vars,
      String strName, String strModelName, String strModelCode, String strSupplier,
      String strNatofPdt, String strDepartment, String strSport, String strSportsCat,
      String strMerchandiseCat, String strBrand, String strComBrand, String strBluePdt,
      String strLifestage, String strMerCatCombo) {
    ReportSearchItemData[] data1 = null;

    OBError myMessage = new OBError();
    try {
      data1 = ReportSearchItemData.select(this,
          Utility.getContext(this, vars, "#User_Client", "ReportSearchItem"),
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportSearchItem"), strName,
          strModelName, strModelCode, strSupplier, strNatofPdt, strDepartment, strSport,
          strSportsCat, strMerchandiseCat, strBrand, strComBrand, strBluePdt, strLifestage);

      int datalength = data1.length;

      if (datalength == 0) {
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
