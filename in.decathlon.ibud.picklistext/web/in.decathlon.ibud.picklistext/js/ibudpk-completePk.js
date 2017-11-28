
isc.defineClass('IBUDPK_CompletePopup', isc.OBPopup);
isc.IBUDPK_CompletePopup.addProperties({

  width: 320,
  height: 200,
  title: null,
  showMinimizeButton: false,
  showMaximizeButton: false,

  mainform: null,
  okButton: null,
  cancelButton: null,
  pickings: null,
  organization: null,
  doGroup: null,
  view: null,
  params: null,

  initWidget: function () {

    var pickings = this.pickings,
        originalView = this.view,
        params = this.params;

     
    this.setTitle(OB.I18N.getLabel('IBUDPK_CompletePicklist'));

    this.okButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.OK_BUTTON_TITLE'),
      popup: this,
      action: function () {
        var callback= false;

        callback = function (rpcResponse, data, rpcRequest) {
          var status = rpcResponse.status,
              context = rpcRequest.clientContext,
              view = context.originalView.getProcessOwnerView(context.popup.params.processId);
          if (data.message) {
            view.messageBar.setMessage(data.message.severity, data.message.title, data.message.text);

          }
          rpcRequest.clientContext.popup.closeClick();
          rpcRequest.clientContext.originalView.refresh(false, false);
        };

        OB.RemoteCallManager.call('in.decathlon.ibud.picklistext.CompleteActionHandler', {
          pickings: pickings,
          action: 'complete',
        }, {}, callback, {
          originalView: this.popup.view,
          popup: this.popup
        });
      }
    });

    this.cancelButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.CANCEL_BUTTON_TITLE'),
      popup: this,
      action: function () {
        this.popup.closeClick();
      }
    });


    this.items = [
    isc.VLayout.create({
      defaultLayoutAlign: "center",
      align: "center",
      width: "100%",
      layoutMargin: 10,
      membersMargin: 6,
      members: [
      isc.HLayout.create({
        defaultLayoutAlign: "center",
        align: "center",
        layoutMargin: 30,
        membersMargin: 6,
        members: this.mainform
      }), isc.HLayout.create({
        defaultLayoutAlign: "center",
        align: "center",
        membersMargin: 10,
        members: [this.okButton, this.cancelButton]
      })]
    })];

    this.Super('initWidget', arguments);
  }

});