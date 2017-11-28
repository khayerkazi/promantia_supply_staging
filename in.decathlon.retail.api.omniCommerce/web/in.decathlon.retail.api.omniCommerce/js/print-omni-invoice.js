isc.defineClass('OAPIPickGridProcess', isc.VLayout);

isc.OAPIPickGridProcess.addProperties({
	width: '100%',
	height: '100%',
	overflow: 'auto',
	autoSize: false,
	
	initWidget: function () {
		var stringHTML, pickHTML, processButton, cancelButton, buttonLayout = [], me = this;
		
		stringHTML = "<div id='omni_invoice' style='width:100%;height:100%;clear:both;border:2px solid #000;'>" +
						"<div style='width:100%;height:50px;clear:both;border-bottom:1px solid #000;'>" +
							"<div style='width:30%;height:50px;float:left;text-align:left;line-height:50px;'><img src='"+this.omniGridData.logourl+"' /></div>" +
							"<div style='width:40%;height:50px;float:left;font-weight:bold;text-align:left;line-height:50px;font-size:20px;'>&nbsp;TAX INVOICE</div>" +
							"<div style='width:30%;height:50px;float:left;text-align:left;line-height:20px;font-size:15px;'>&nbsp;Original-Buyers' Copy</div>" +
						"</div>" +
						"<div style='width:100%;height:180px;border-bottom:1px solid #000;clear:both;'>" +
							"<table style='cellspacing:0px;cellpadding:0px;width:49.5%;height:180px;float:left;border-right: 1px solid #000;'>" +
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
							"<table style='cellspacing:0px;cellpadding:0px;width:50%;float:left;height:180px;'>" +
								"<tr><th colspan='2' style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;Invoice</td></tr>" +
								"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Invoice Number <strong>"+this.omniGridData.invoiceNum+"</strong></td></tr>" +
								"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Order ID <strong>"+this.omniGridData.invoiceNum+"</strong></td></tr>" +
								"<tr><td rowspan='3' style='width:100%;height:50px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;<img src='http://www.barcodesinc.com/generator/image.php?code="+this.omniGridData.invoiceNum+"&style=197&type=C128B&width=170&height=50&xres=1&font=6'/></td></tr>" +
								"<tr><td style='width:100%;height:20px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;</td></tr>" +
								"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;</td></tr>" +
								"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Invoice Date <strong>"+this.omniGridData.orderDate+"</strong></td></tr>" +
								"<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;Buyer's Purchase Order Reference</td></tr>" +
							"</table>" +
						"</div>" +
						"<div style='width:100%;height:230px;border-bottom:1px solid #000;clear:both;'>" +
							"<table style='cellspacing:opx;cellpadding:0px;width:49.5%;height:230px;float:left;border-right:1px solid #000'>" +
								"<tr><th colspan='2' style='width:100%;height:15px;line-height:15px;text-align:left;font-size:11px;'>&nbsp;Buyer</td></tr>"
								+this.omniGridData.billAddress+
							"</table>" +
							"<table style='cellspacing:opx;cellpadding:0px;width:50%;height:230px;float:left;'>" +
								"<tr><th colspan='2' style='width:100%;height:15px;line-height:15px;text-align:left;font-size:11px;'>&nbsp;DELIVERY ADDRESS</td></tr>"
								+this.omniGridData.shipAddress+
							"</table>" +
						"</div>" +
						"<div style='width:100%;clear:both;'>" +
							"<table cellspacing='0' cellpadding='0' border='0'>" +
								"<tr>" +
									"<th style='width:15%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>Item Code</th>" +
									"<th style='width:5%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>Part No.</th>" +
									"<th style='width:20%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>Description</th>" +
									"<th style='width:3%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>Qty.</th>" +
									"<th style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>Unit Price</th>" +
									"<th style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>Amount (INR)</th>" +
									"<th style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>Tax Rate</th>" +
									"<th style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>VAT Amount</th>" +
									"<th style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-bottom:1px solid #000;'>Gross Amount (INR)</th>" +
								"</tr>"
								+this.omniGridData.itemInfo+
								"<tr>" +
									"<td style='width:15%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:5%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:20%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'><strong>Sub Total</strong></td>" +
									"<td style='width:3%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+this.omniGridData.totalInvQty+"</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+this.omniGridData.subTotal+"</td>" +
								"</tr>" +
								"<tr>" +
									"<td style='width:15%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:5%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:20%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'><strong>Delivery charges</strong></td>" +
									"<td style='width:3%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+this.omniGridData.shipTotal+"</td>" +
								"</tr>" +
								"<tr>" +
									"<td colspan='4' style='width:43%;height:24px;line-height:15px;text-align:left;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'><strong>Invoice Total</strong></td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>&nbsp;</td>" +
									"<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"+this.omniGridData.invTotal+"</td>" +
								"</tr>" +
							"</table>" +
						"</div>" +
						"<div style='width:100%;height:24px;clear:both;border-bottom:1px solid #000;'>" +
							"<div style='width:49.5%;height:24px;float:left;border-right:1px solid #000;'>&nbsp;</div>" +
							"<div style='width:50%;float:left;height:24px;line-height:24px;text-align:center;font-size:11px;'>&nbsp;Place and date of issue&nbsp;&nbsp;:&nbsp;&nbsp;Bangalore,&nbsp;"+this.omniGridData.dateOfIssue+"</div>" +
						"</div>" +
						"<div style='width:100%;height:49px;clear:both;border-bottom:1px solid #000;'>" +
							"<div style='width:49.5%;height:49px;float:left;border-right:1px solid #000;'></div>" +
							"<div style='width:50%;height:49px;float:left;line-height:70px;'>&nbsp;&nbsp;Signature</div>" +
						"</div>" +
						"<div style='width:100%;height:49px;clear:both;font-size:11px;border-bottom:1px solid #000;'>In respect of the goods covered by this invoice, no credit of the additional duty of customs levied under section 3(5) of the Customs Tariff Act shall be admissible.</div>" +
						"<div style='width:100%;height:49px;clear:both;font-size:11px;border-bottom:1px solid #000;'>*:If the quantity is 0, your product is out of stock and it is being procured from an alternate source. Apologies for the inconvenience caused. Contact 7676798989 in case of further queries.</div>" +
						"<div style='width:100%;height:90px;clear:both;'>" +
							"<div style='width:49.5%;height:90px;font-size:11px;float:left;border-right:1px solid #000;'>Please ensure your goods are received in a good condition. Call us on +91 7676798989 if you find your order incomplete, tampered with, or if you just want to chat :-). Sportingly, Team Decathlon.</div>" +
							"<div style='width:50%;height:90px;float:left;'>" +
								"<div style='width:100%;height:29px;line-height:29px;border-bottom:1px solid #000;'>RECEIVED BY</div>" +
								"<div style='width:100%;height:29px;line-height:29px;border-bottom:1px solid #000;'>" +
									"<div style='width:49.5%;height:29px;float:left;border-right:1px solid #000;'>NAME</div>" +
									"<div style='width:50%;height:29px;float:left;'></div>" +
								"</div>" +
								"<div style='width:100%;height:30px;line-height:30px;'>" +
									"<div style='width:49.5%;height:30px;float:left;border-right:1px solid #000;'>SIGNATURES</div>" +
									"<div style='width:50%;height:30px;float:left;'></div>" +
								"</div>" +
							"</div>" +
						"</div>" +
					"</div>";
		
		alert(this.omniGridData.recordId);
		
		pickHTML = isc.HTMLFlow.create({
		    width : '100%',
		    styleName:"exampleTextBlock",
		    contents: stringHTML
		});
		
		processButton = isc.OBFormButton.create({
	   		title: 'Print',
			      click: function () {
					 var docprint=window.open("about:blank"); 
					 var oTable = document.getElementById("omni_invoice");
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
		
		buttonLayout.push(isc.HLayout.create({width: 120}));
		
		buttonLayout.push(processButton);
		
		buttonLayout.push(isc.HLayout.create({width: 30}));
		
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
		this.parentElement.parentElement.closeClick();
	},
	closeClick: function () {
		return true;
	}
});

OB.OAPI = OB.OAPI || {};

OB.OAPI.Process = {
		
		execute: function (params, view) {
			
			var recordId = params.button.contextView.viewGrid.getSelectedRecords()[0].id,
		     processOwnerView = view.getProcessOwnerView(params.processId),
		     callback;
			
			callback = function (rpcResponse, data, rpcRequest) {
				var processLayout, popupTitle;
				
				if (!data) {
					view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, null);
					return;
				}
				
				processLayout = isc.OAPIPickGridProcess.create({
					parentWindow: view,
					omniGridData: data
				});
				view.openPopupInTab(processLayout,'Omni Commerce Invoice Details','900', '90%', true, true, true, true);
			};
			OB.RemoteCallManager.call('in.decathlon.retail.api.omniCommerce.ad_webservice.OmniPrintInvoiceActionHandler', {recordId : recordId, action: params.action}, {}, callback);
		},

		omniInvoice: function (params, view) {
			params.action = 'omniInvoice';
			OB.OAPI.Process.execute(params, view);
		}
};
