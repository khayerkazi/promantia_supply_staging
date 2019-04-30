/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.openbravo.decathlon.operator.utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;

import com.decathlon.oagis942.SyncPersonnelType;

/**
 * Singleton definition of the JAXBContext to be used to marshall and unmarshall
 * 
 * @author GIG
 * 
 */

public class JAXBContextSingleton {

  private static JAXBContext jaxbContext;

  public synchronized static JAXBContext getContext() {
    if (jaxbContext == null) {
      init();
    }
    return jaxbContext;
  }

  private static void init() {
    try {
      jaxbContext = JAXBContext.newInstance(SyncPersonnelType.class);
    } catch (JAXBException e) {
      e.printStackTrace();
    }
  }

  public static SyncPersonnelType getIntrospector(Object syncPersonnelType) {
    return (SyncPersonnelType) JAXBIntrospector.getValue(syncPersonnelType);
  }

}