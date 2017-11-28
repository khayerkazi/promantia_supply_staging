OB.UI.ModalReceiptPropertiesImpl.extend({
    initComponents: function() {
        var i, customAttributes = [];

        // remove standard receipt description
        /*for (i = 0; i < this.newAttributes.length; i++) {
          if (this.newAttributes[i].name !== 'receiptDescription') {
            customAttributes.push(this.newAttributes[i]);
          }
        }*/

        // add custom receipt properties at the beginning
        customAttributes.unshift({
            kind: 'OB.UI.renderTextProperty',
            name: 'em_voc_voucher1_id',
            modelProperty: 'vocVoucherOne',
            i18nLabel: 'VOC_Voucher1_id_msg'
        }, {
            kind: 'OB.UI.renderTextProperty',
            name: 'voucher1_amt',
            modelProperty: 'vocVoucher1Amount',
            i18nLabel: 'VOC_Voucher1_amount_msg',
        }, {
            kind: 'OB.UI.renderTextProperty',
            name: 'voucher2',
            modelProperty: 'vocVoucherTwo',
            i18nLabel: 'VOC_Voucher2_id_msg'
        }, {
            kind: 'OB.UI.renderTextProperty',
            name: 'voucher2_amt',
            modelProperty: 'vocVoucher2Amount',
            i18nLabel: 'VOC_Voucher2_amount_msg',
        }, {
            kind: 'OB.UI.renderTextProperty',
            name: 'mobile',
            modelProperty: 'vocMobileno',
            i18nLabel: 'VOC_Mobile_msg'
        }, {
            kind: 'OB.UI.renderTextProperty',
            name: 'email',
            modelProperty: 'vocEmail',
            i18nLabel: 'VOC_email_msg',
        }, {
            kind: 'OB.UI.renderTextProperty',
            name: 'landline',
            modelProperty: 'vocLandline',
            i18nLabel: 'VOC_landline_msg',
        });

        this.newAttributes = customAttributes;
        this.inherited(arguments);
    }
});