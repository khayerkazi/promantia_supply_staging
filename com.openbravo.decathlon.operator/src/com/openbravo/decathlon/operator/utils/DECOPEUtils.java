/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.openbravo.decathlon.operator.utils;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Utils definition across Operator
 * 
 * @author GIG
 * 
 */

public class DECOPEUtils {
  /**
   * Method that tries to find the organization sent.
   * 
   * @param sellingLocation
   * @return {@link Organization} Organization found
   */
  public static Organization obtainOrganization(String sellingLocation) {
    OBCriteria<Organization> criteria = OBDal.getInstance().createCriteria(Organization.class);
    criteria.setMaxResults(1);
    criteria.add(Restrictions.eq(Organization.PROPERTY_DECIMSELLINGLOCATION, sellingLocation));
    criteria.setFilterOnReadableClients(false);
    Organization organization = (Organization) criteria.uniqueResult();
    return organization;
  }

}