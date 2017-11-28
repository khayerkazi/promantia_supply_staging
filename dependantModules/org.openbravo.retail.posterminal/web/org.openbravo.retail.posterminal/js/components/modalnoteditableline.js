/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, $, confirm */

enyo.kind({
  name: 'OB.UI.ModalNotEditableLine',
  kind: 'OB.UI.ModalInfo',
  i18Header: 'OBPOS_modalNoEditableLineHeader',
  bodyContent: {
    i18nContent: 'OBPOS_modalNoEditableLineBody'
  }
});