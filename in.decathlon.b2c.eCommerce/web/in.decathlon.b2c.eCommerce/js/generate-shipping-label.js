isc.defineClass('ECOMShippingLabelHTML', isc.VLayout);

isc.ECOMShippingLabelHTML.addProperties({
	width: '100%',
	height: '100%',
	overflow: 'auto',
	autoSize: false,
	  
	initWidget: function () {
		
		var stringHTML, pickHTML, processButton, cancelButton, buttonLayout = [],
		me = this;
		
		stringHTML ="<div style='width:100%;height:100%;clear:both;'>" +
						"<div id='shipping_label' style='width:598px;clear:both;border:1px solid #000;'>" +
							"<div style='width:598px;height:49px;line-height:49px;clear:both;border-bottom:1px solid #000;text-align:center;font-weight:bold;font-size:20px;'>SHIPPING LABEL</div>" +
							"<div style='width:598px;height:39px;line-height:39px;clear:both;border-bottom:1px solid #000;text-align:center;font-weight:bold;font-size:20px;'>\"COLLECT CASH ONLY (Rs."+this.pickGridData.invTotal+")\" or \"PRE-PAID\"</div>" +
							"<div style='width:598px;height:19px;line-height:19px;clear:both;border-bottom:1px solid #000;'>" +
								"<div style='width:148px;height:19px;float:left;border-right:1px solid #000;'>&nbsp;Invoice Number&nbsp;</div><div style='width:149px;height:19px;float:left;text-align:center;border-right:1px solid #000;'>&nbsp;<strong>"+this.pickGridData.invoiceNum+"</strong></div>" +
								"<div style='width:149px;height:19px;float:left;border-right:1px solid #000;'>&nbsp;Order Number&nbsp;</div><div style='width:149px;height:19px;float:left;text-align:center;'>&nbsp;<strong>"+this.pickGridData.orderId+"</strong></div>" +
							"</div>" +
							"<div style='width:598px;height:179px;clear:both;border-bottom:1px solid #000;'>" +
								"<div style='width:298px;height:179px;float:left;border-right:1px solid #000;'>" +
									"<div style='width:298px;height:19px;line-height:19px;text-align:center;border-bottom:1px solid #000;'>&nbsp;From:</div>" +
									"<div style='width:298px;height:20px;font-size:11px;line-height:20px;text-align:left;'>&nbsp;Decathlon Sports India Pvt. Limited (eCommerce)</div>" +
									"<div style='width:298px;height:20px;font-size:11px;line-height:20px;text-align:left;'>&nbsp;63 & 64/1,</div>" +
									"<div style='width:298px;height:20px;font-size:11px;line-height:20px;text-align:left;'>&nbsp;Near Koralur Gate,</div>" +
									"<div style='width:298px;height:20px;font-size:11px;line-height:20px;text-align:left;'>&nbsp;Koralur Village,</div>" +
									"<div style='width:298px;height:20px;font-size:11px;line-height:20px;text-align:left;'>&nbsp;Kosaba Hobli, NH 207,</div>" +
									"<div style='width:298px;height:20px;font-size:11px;line-height:20px;text-align:left;'>&nbsp;Bangalore</div>" +
									"<div style='width:298px;height:20px;font-size:11px;line-height:20px;text-align:left;'>&nbsp;560067</div>" +
									"<div style='width:298px;height:20px;font-size:11px;line-height:20px;text-align:left;'>&nbsp;Contact: +91 7676798989</div>" +
								"</div>" +
								"<div style='width:299px;height:179px;float:left;'>" +
									"<div style='width:298px;height:19px;line-height:19px;text-align:center;border-bottom:1px solid #000;'>&nbsp;To:</div>"
									+this.pickGridData.shipAddress+
								"</div>" +
							"</div>" +
							"<div style='width:598px;height:60px;clear:both;'>" +
								"<div style='width:298px;height:60px;float:left;border-right:1px solid #000;'>" +
									"<div style='width:148px;height:19px;line-height:19px;float:left;border-bottom:1px solid #000;border-right:1px solid #000;text-align:center;'>Box Number</div><div style='width:149px;height:19px;float:left;line-height:19px;border-bottom:1px solid #000;text-align:center;'>&nbsp;</div>" +
									"<div style='width:148px;height:19px;line-height:19px;float:left;border-bottom:1px solid #000;border-right:1px solid #000;text-align:center;'>Box Weight</div><div style='width:149px;height:19px;float:left;line-height:19px;border-bottom:1px solid #000;text-align:center;'>&nbsp;</div>" +
									"<div style='width:148px;height:20px;line-height:20px;float:left;border-right:1px solid #000;text-align:center;'>Type of Goods</div><div style='width:149px;height:20px;float:left;line-height:20px;text-align:center;'><strong>Sports Goods</strong></div>" +
								"</div>" +
								"<div style='width:299px;height:60px;float:left;'>" +
									"<div style='width:149px;height:19px;line-height:19px;float:left;border-bottom:1px solid #000;border-right:1px solid #000;text-align:center;'>Transporter</div><div style='width:149px;height:19px;float:left;line-height:19px;border-bottom:1px solid #000;text-align:center;'>&nbsp;</div>" +
									"<div style='width:149px;height:19px;line-height:19px;float:left;border-bottom:1px solid #000;border-right:1px solid #000;text-align:center;'>Mode</div><div style='width:149px;height:19px;float:left;line-height:19px;border-bottom:1px solid #000;text-align:center;'>&nbsp;</div>" +
									"<div style='width:149px;height:20px;line-height:20px;float:left;border-right:1px solid #000;text-align:center;'>Invoice Value</div><div style='width:149px;height:20px;float:left;line-height:20px;text-align:center;'><strong>Rs.&nbsp;"+this.pickGridData.invTotal+"</strong></div>" +
								"</div>" +
							"</div>" +
						"</div>" +
					"</div>";
		
		pickHTML = isc.HTMLFlow.create({
		    width : '100%',
		    styleName:"ShippingLabelBlock",
		    contents: stringHTML
		});
	
		// Print Button
		processButton = isc.OBFormButton.create({
			title: 'Print',
			click: function () {
				var docprint=window.open("about:blank"); 
				var oTable = document.getElementById("shipping_label");
				docprint.document.open(); 
				docprint.document.write('<html><head><title>Decathlon Invoice</title>');
				docprint.document.write('<style type="text/css" media="print,screen">.hideMe{display:none;}.NoPrintClass{display:none;}</style>');
				docprint.document.write('</head><body>');
				docprint.document.write(oTable.parentNode.innerHTML);
				docprint.document.write('</body></html>'); 
				docprint.document.close(); 
				docprint.print();
				docprint.close();
			}
		});

		// Cancel Button
		cancelButton = isc.OBFormButton.create({
			title: 'Cancel',
			click: function () {
				me.closePopup();
			}
	 	});
		
		// Button Layout
		buttonLayout.push(isc.HLayout.create({width: 120}));
		buttonLayout.push(processButton);
		buttonLayout.push(isc.HLayout.create({width: 30}));
		buttonLayout.push(cancelButton);
		
		// Adding all the members to the layout
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

OB.ECOM = OB.ECOM || {};

OB.ECOM.Shipping = {
		generate: function (params, view) {
	 var recordId = params.button.contextView.viewGrid.getSelectedRecords()[0].id,
     processOwnerView = view.getProcessOwnerView(params.processId),
     callback;

    callback = function (rpcResponse, data, rpcRequest) {
    	
    	 var processLayout, popupTitle;
    	 
    	 if (!data) {
    	        view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, null);
    	        return;
    	      }
    	 
    	 processLayout = isc.ECOMShippingLabelHTML.create({
    	        parentWindow: view,
    	        pickGridData: data
    	      });
    	 view.openPopupInTab(processLayout,'Invoice Details','900', '90%', true, true, true, true);
    };

    OB.RemoteCallManager.call('in.decathlon.b2c.eCommerce.ShippingLabelActionHandler', {
    	recordId : recordId,
    	action: params.action
    }, {}, callback);
  },

  shippinglabel: function (params, view) {
    params.action = 'shippingLabel';
    OB.ECOM.Shipping.generate(params, view);
  }
};