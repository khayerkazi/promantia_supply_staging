isc.defineClass('ECOMPickValidateProcess', isc.VLayout);

isc.ECOMPickValidateProcess.addProperties({
	width: '100%',
	  height: '100%',
	  overflow: 'auto',
	  autoSize: false,
	  
	  initWidget: function () {
	var stringHTML, pickHTML, processButton, cancelButton, buttonLayout = [],
	me = this;
	/*var baseUrl = window.location.href;
	var urls = baseUrl.split("/");alert(urls);
	baseUrl = urls[0]+urls[1]+"//"+urls[2]+"/"+urls[3];alert(baseUrl);<img border='0' name='logo' id='logo' align='middle' src='"+baseUrl+"/utility/ShowImageLogo?logo=yourcompanymenu'/>*/
	
	stringHTML = "<div id='invoice_anvesh' style='width:100%;height:100%;clear:both;'>" +
					"<div style='width:30%;height:40px;float:left;text-align:left;line-height:40px;'>&nbsp;</div><div style='font-weight:bold;width:40%;height:40px;float:left;text-align:left;line-height:40px;font-size:15px;'>&nbsp;TAX INVOICE</div><div style='width:30%;height:40px;float:left;text-align:left;line-height:20px;font-size:11px;'>&nbsp;Original-Buyers' Copy</div>" +
					"<div style='width:100%;height:15px;clear:both;'>&nbsp;</div>" +
					"<div style='width:100%;border-bottom:2px solid #000000;clear:both;'>" +
						"<table style='cellspacing:opx;cellpadding:0px;width:50%;float:left;'>" +
							"<tr><th colspan='2' style='width:100%;height:15px;line-height:15px;text-align:left;'>&nbsp;Seller:</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Decathlon Sports India Pvt. Ltd.</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Tata Housing Xylem, Plot no 4 and 4(A),</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Dyavasandra Industrial Area,</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;1st Phase Krishnarajapuram Hobli,</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Mahadevpura Post,</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;ITPB Main Road, Whitefield,</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Bangalore - 560 048.</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;TIN: 29640215582</td></tr>" +
						"</table>" +
						"<table style='cellspacing:opx;cellpadding:0px;width:50%;float:left;'>" +
							"<tr><th colspan='2' style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;Invoice</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Invoice No. : "+this.pickGridData.invoiceNum+"</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Order No. : "+this.pickGridData.orderId+"</td></tr>" +
							"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Invoice Date : "+this.pickGridData.invoiceDate+"</td></tr>" +
						"</table>" +
					"</div>" +
					"<div style='width:100%;border-bottom:2px solid #000000;clear:both;'>&nbsp;</div>" +
					"<div style='width:100%;height:15px;clear:both;'>&nbsp;</div>" +
					"<div style='width:100%;border-bottom:2px solid #000000;clear:both;'>" +
						"<table style='cellspacing:opx;cellpadding:0px;width:50%;float:left;'>" +
							"<tr><th colspan='2' style='width:100%;height:15px;line-height:15px;text-align:left;'>&nbsp;BILLING ADDRESS</td></tr>"
							+this.pickGridData.billAddress+
						"</table>" +
						"<table style='cellspacing:opx;cellpadding:0px;width:50%;float:left;'>" +
							"<tr><th colspan='2' style='width:100%;height:15px;line-height:15px;text-align:left;'>&nbsp;SHIPPING ADDRESS</td></tr>"
							+this.pickGridData.shipAddress+
						"</table>" +
					"</div>" +
					"<div style='width:100%;clear:both;'>" +
						"<table cellspacing='0' cellpadding='0' border='1'>" +
							"<tr>" +
								"<th style='width:15%;height:25px;line-height:15px;text-align:center;font-size:12px;'>Item Code</th>" +
								"<th style='width:20%;height:25px;line-height:15px;text-align:center;font-size:12px;'>Description</th>" +
								"<th style='width:5%;height:25px;line-height:15px;text-align:center;font-size:12px;'>Qty.</th>" +
								"<th style='width:10%;height:25px;line-height:15px;text-align:center;font-size:12px;'>Unit Price</th>" +
								"<th style='width:10%;height:25px;line-height:15px;text-align:center;font-size:12px;'>Amount</th>" +
								"<th style='width:10%;height:25px;line-height:15px;text-align:center;font-size:12px;'>Tax Rate</th>" +
								"<th style='width:15%;height:25px;line-height:15px;text-align:center;font-size:12px;'>Tax Amount</th>" +
								"<th style='width:15%;height:25px;line-height:15px;text-align:center;font-size:12px;'>Total Amount</th>" +
							"</tr>"
							+this.pickGridData.itemInfo+
						"</table>" +
					"</div>" +
					"<div style='width:100%;height:15px;clear:both;'>&nbsp;</div>" +
					"<div style='width:70%;height:25px;line-height:15px;text-align:right;float:left;font-weight:bold;'>Sub Total&nbsp;&nbsp;</div><div style='width:30%;height:25px;line-height:15px;text-align:center;float:left;'>Rs.&nbsp;"+this.pickGridData.subTotal+"</div>" +
					"<div style='width:70%;height:25px;line-height:15px;text-align:right;float:left;font-weight:bold;'>Delivery charges for items @ 5.5 %&nbsp;&nbsp;</div><div style='width:30%;height:25px;line-height:15px;text-align:center;float:left;'>Rs.&nbsp;"+this.pickGridData.tax14+"</div>" +
					"<div style='width:70%;height:25px;line-height:15px;text-align:right;float:left;font-weight:bold;'>Delivery charges for items @ 14.5 %&nbsp;&nbsp;</div><div style='width:30%;height:25px;line-height:15px;text-align:center;float:left;'>Rs.&nbsp;"+this.pickGridData.tax5+"</div>" +
					"<div style='width:70%;height:25px;line-height:15px;text-align:right;float:left;font-weight:bold;'>Delivery Charges Paid&nbsp;&nbsp;</div><div style='width:30%;height:25px;line-height:15px;text-align:center;float:left;'>Rs.&nbsp;"+this.pickGridData.shipTotal+"</div>" +
					"<div style='width:70%;height:25px;line-height:15px;text-align:right;float:left;font-weight:bold;'>Invoice Total&nbsp;&nbsp;</div><div style='width:30%;height:25px;line-height:15px;text-align:center;float:left;'>Rs.&nbsp;"+this.pickGridData.invTotal+"</div>" +
					"<div style='width:50%;height:15px;float:left;'>&nbsp;</div><div style='width:50%;float:left;height:15px;text-align:center;'>&nbsp;Place and date of issue&nbsp;&nbsp;:&nbsp;&nbsp;Bangalore,&nbsp;"+this.pickGridData.dateOfIssue+"</div>" +
					"<div style='width:49.5%;height:45px;float:left;border-left:1px solid #000;border-bottom:1px solid #000;border-top:1px solid #000;'>&nbsp;</div><div style='width:49.4%;height:45px;border:1px solid #000;float:left;text-align:center;font-weight:bold;line-height:70px;'>Signature</div>" +
					"<div style='width:99%;height:30px;line-height:15px;border-left:1px solid #000;border-right:1px solid #000;clear:both;line-height:15px;text-align:center;font-size:11px;'>In respect of the goods covered by this invoice, no credit of the additional duty of customs levied under section 3(5) of the Customs Tariff Act shall be admissible.</div>" +
					"<div style='width:69%;height:45px;float:left;line-height:15px;font-size:11px;border:1px solid #000;'>Please ensure your goods are received in a good condition. Call us on +91 80950 34577 / +91 80955 00075 if you find your order incomplete, tampered with, or if you just want to chat :-). Sportingly, Team Decathlon.</div><div style='width:30%;height:14px;font-size:11px;float:left;text-align:left;font-weight:bold;border-right:1px solid #000;border-bottom:1px solid #000;border-top:1px solid #000;'>RECEIVED BY&nbsp;&nbsp;:</div><div style='width:30%;height:14px;font-size:11px;float:left;text-align:left;font-weight:bold;border-bottom:1px solid #000;border-right:1px solid #000;'>NAME&nbsp;&nbsp;:</div><div style='width:30%;height:15px;font-size:11px;float:left;text-align:left;font-weight:bold;border-bottom:1px solid #000;border-right:1px solid #000;'>SIGNATURES&nbsp;&nbsp;:</div>" +
				"</div>";
	
	pickHTML = isc.HTMLFlow.create({
	    width : '100%',
	    styleName:"exampleTextBlock",
	    contents: stringHTML
	});
	
	 processButton = isc.OBFormButton.create({
   		title: 'Print',
		      click: function () {
				 var docprint=window.open("about:blank"); 
				 var oTable = document.getElementById("invoice_anvesh");
				 docprint.document.open(); 
				 docprint.document.write('<html><head><title>Decathlon Invoice</title>');
				 docprint.document.write('<style type="text/css" media="print,screen">.hideMe{display:none;}.NoPrintClass{display:none;}</style>');
				 docprint.document.write('</head><body>');
				 docprint.document.write(oTable.parentNode.innerHTML);
				 docprint.document.write('</body></html>'); 
				 docprint.document.close(); 
				 docprint.print();
				 docprint.close();
		      },
	 });

	cancelButton = isc.OBFormButton.create({
		      title: 'Cancel',
		      click: function () {
			me.closePopup();
		      },
 	});
	buttonLayout.push(isc.HLayout.create({
	      width: 120
	    }));
	buttonLayout.push(processButton);
	buttonLayout.push(isc.HLayout.create({
	      width: 30
	    }));
	 buttonLayout.push(cancelButton);
	 
	 this.members = [pickHTML, isc.HLayout.create({
	      align: 'center',
	      width: '100%',
	      height: 15,
	      members: [isc.HLayout.create({
		width: 1,
		layoutTopMargin:12,
		overflow: 'visible',
		styleName: this.buttonBarStyleName,
		height: 40,
		defaultLayoutAlign: 'center',
		members: buttonLayout
	      })]
	    })];

	    this.Super('initWidget', arguments);
	
},
destroy: function () {
    clearInterval(this.focusCheckInterval);
    return this.Super('destroy', arguments);
  },
  closePopup: function () {
    this.parentElement.parentElement.closeClick(); // Super call
  },
  closeClick: function () {
    return true;
  }
});

OB.IDSD = OB.IDSD || {};

OB.IDSD.Process = {
	create: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(), 
        orders = [],
        callback;
    callback = function (rpcResponse, data, rpcRequest) {
    	if(data == null) {
    		isc.say("Please click onother record!");
    		alert("null");
    	} else {
    		var message = data.message;
    	      view.activeView.messageBar.setMessage(isc.OBMessageBar[message.severity], message.title, message.text);
    	      isc.say("Please click 'OK'");
    	      // close process to refresh current view
    	      params.button.closeProcessPopup();
    	}
      
    };
    for (i = 0; i < selection.length; i++) {
      orders.push(selection[i].id);
    }
    isc.confirm(OB.I18N.getLabel('OBWPL_CreateConfirm'), function (clickedOK) {
        if (clickedOK) {
        	 this.disabled=true;
             isc.warn("processing ... Please wait");
             OB.RemoteCallManager.call('in.decathlon.supply.dc.CreatePickListActionHandler', {
             orders: orders,
             action: 'create'
          }, {}, callback);
        }
        this.disabled=false;
      });
  },

  generateSinglePicklist: function (params, view) {
    params.action = 'printInvoice';
    OB.IDSD.Process.create(params, view);
  }
};