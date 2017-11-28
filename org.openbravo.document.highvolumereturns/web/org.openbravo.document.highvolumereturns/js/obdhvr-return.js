/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

OB.OBDHVR = {};

OB.OBDHVR.Returns = {
  open: function (params, view) {
    function get_uuid() {
      function S4() {
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1).toUpperCase();
      }
      return (S4() + S4() + S4() + S4() + S4() + S4() + S4() + S4());
    };

    var contextView = params.button.contextView,
        buttonReplace;

    OB.OBDHVR.currentProcessId = get_uuid(); // make this public so it is accessible, there can be only one at a time
    buttonReplace = isc.OBToolbarActionButton.create({
      id: 'AF4090093D431431E040007F010048A5',
      title: 'Send Materials',
      obManualURL: '/',
      command: 'org.openbravo.document.highvolumereturns.HighVolumeReturnPickEditLines',
      processId: '0C91CC1EEFC0471FB2BE11F5FA5C4085',
      newDefinition: true,
      uiPattern: 'OBUIAPP_PickAndExecute',
      multiRecord: false,
      windowId: 'FF808081330213E60133021822E40007',
      windowTitle: 'Pick/Edit Lines',
      modal: false,
      displayIf: function (form, currentValues, context) {
        return (OB.Utilities.getValue(currentValues, 'processed') === false);
      },
      autosave: true,
      view: view.view,
      contextView: contextView
    });

/* {
      id: 'AF4090093D431431E040007F010048A5',
      title: 'Pick/Edit Lines',
      obManualURL: '/',
      command: 'org.openbravo.common.actionhandler.SRMOPickEditLines',
      property: 'pickFromShipment',
      processId: 'C79A2D191BBD48AC8456DBD1AAF89E04',
      newDefinition: true,
      uiPattern: 'OBUIAPP_PickAndExecute',
      multiRecord: false,
      windowId: 'FF808081330213E60133021822E40007',
      modal: false,
      displayIf: function (form, currentValues, context) {
        return (OB.Utilities.getValue(currentValues, 'processed') === false);
      },
      autosave: true
    }*/

    OB.RemoteCallManager.call('org.openbravo.document.highvolumereturns.SetBPActionHandler', {
      bp: contextView.viewGrid.getSelectedRecord().businessPartner,
      processId: OB.OBDHVR.currentProcessId
    }, {}, function () {
      buttonReplace.runProcess();
    });

  }
};


/**
 * This is a copy of the generated sources of Return P&E process
 */
isc.ClassFactory.defineClass('_0C91CC1EEFC0471FB2BE11F5FA5C4085', isc.OBParameterWindowView).addProperties({
  processId: 'C79A2D191BBD48AC8456DBD1AAF89E04',
  actionHandler: 'org.openbravo.common.actionhandler.SRMOPickEditLines',
  popup: true,
  viewProperties: {
    fields: [{
      isGrid: true,
      viewProperties: {
        tabTitle: 'Pick / Edit Lines',
        entity: 'ReturnMaterialOrderPickEditLines',

        tabId: 'CC35784EF36B4732B40167CD5D2449F0',
        moduleId: '0',

        mapping250: '/RFCOrderPickEditLines/PickEditLines',
        showSelect: true,

        standardProperties: {
          inpTabId: 'CC35784EF36B4732B40167CD5D2449F0',
          inpwindowId: '31496219132644BE8D477517595AE9D3',
          inpTableId: '38A7A1273EA34C93A0EA49CE77B6FB14',
          inpkeyColumnId: 'B04C07EBF2B300E2E040007F01003385',
          keyProperty: 'id',
          inpKeyName: 'inpcRmOrderPickEditLinesId',
          keyColumnName: 'C_Rm_Order_Pick_Edit_Lines_ID',
          keyPropertyType: '_id_13'
        },

        fields: [{
          name: 'obSelected',
          title: 'ob_selected',
          columnName: 'OB_Selected',
          inpColumnName: 'inpobSelected',
          "width": 1,
          "overflow": "visible",
          gridProps: {
            sort: 1,
            showIf: 'false',
            editorProps: {
              showTitle: false,
              showLabel: false
            },
            canGroupBy: false,
            width: '*',
            autoFitWidth: false,
            yesNo: true
          },
          type: '_id_20'
        }, {
          name: 'inOutDocumentNumber',
          title: 'Shipment / Receipt No.',
          required: true,
          disabled: true,
          columnName: 'InOutNo',
          inpColumnName: 'inpinoutno',
          "length": 30,
          gridProps: {
            sort: 2,
            autoExpand: true,
            showHover: true,
            length: 30,
            displaylength: 30
          },
          type: '_id_10'
        }, {
          name: 'movementDate',
          title: 'Date',
          required: true,
          disabled: true,
          columnName: 'Movementdate',
          inpColumnName: 'inpmovementdate',
          "width": "50%",
          "length": 19,
          gridProps: {
            sort: 3,
            cellAlign: 'left',
            filterOnKeypress: false
          },
          type: '_id_15'
        }, {
          name: '',
          personalizable: false,
          type: 'spacer'
        }, {
          name: 'product',
          title: 'Product',
          disabled: true,
          colSpan: 2,
          columnName: 'M_Product_ID',
          inpColumnName: 'inpmProductId',
          refColumnName: 'M_Product_ID',
          targetEntity: 'Product',
          selectorDefinitionId: '2E64F551C7C4470C80C29DBA24B34A5F',
          popupTextMatchStyle: 'startsWith',
          textMatchStyle: 'substring',
          defaultPopupFilterField: '_identifier',
          displayField: '_identifier',
          valueField: 'product$id',
          pickListFields: [{
            title: ' ',
            name: '_identifier',
            type: 'text'
          }],
          showSelectorGrid: true,
          selectorGridFields: [{
            title: 'Search Key',
            name: 'product$searchKey',
            type: '_id_10'
          }, {
            title: 'Name',
            name: 'product$name',
            type: '_id_10'
          }, {
            title: 'Available',
            name: 'available',
            type: '_id_29',
            filterOnKeypress: false
          }, {
            title: 'Warehouse',
            name: 'warehouse',
            type: '_id_19',
            displayField: 'warehouse$_identifier',
            canFilter: true,
            required: false,
            filterEditorType: 'OBSelectorFilterSelectItem',
            filterEditorProperties: {
              entity: 'Warehouse'
            }
          }, {
            title: 'Unit Price',
            name: 'standardPrice',
            type: '_id_800008',
            filterOnKeypress: false
          }, {
            title: 'List Price',
            name: 'netListPrice',
            type: '_id_800008',
            filterOnKeypress: false
          }, {
            title: 'Price List Version',
            name: 'productPrice$priceListVersion',
            type: '_id_19',
            displayField: 'productPrice$priceListVersion$_identifier',
            canFilter: true,
            required: false,
            filterEditorType: 'OBSelectorFilterSelectItem',
            filterEditorProperties: {
              entity: 'PricingPriceListVersion'
            }
          }, {
            title: 'Warehouse Qty.',
            name: 'qtyOnHand',
            type: '_id_29',
            filterOnKeypress: false
          }, {
            title: 'Ordered Qty.',
            name: 'qtyOrdered',
            type: '_id_29',
            filterOnKeypress: false
          }, {
            title: 'Price Limit',
            name: 'priceLimit',
            type: '_id_800008',
            filterOnKeypress: false
          }],
          outFields: {
            'productPrice$priceListVersion$priceList$currency$id': {
              'fieldName': 'productPrice$priceListVersion$priceList$currency$id',
              'suffix': '_CURR',
              'formatType': ''
            },
            'product$uOM$id': {
              'fieldName': 'product$uOM$id',
              'suffix': '_UOM',
              'formatType': ''
            },
            'standardPrice': {
              'fieldName': 'standardPrice',
              'suffix': '_PSTD',
              'formatType': ''
            },
            'netListPrice': {
              'fieldName': 'netListPrice',
              'suffix': '_PLIST',
              'formatType': ''
            },
            'priceLimit': {
              'fieldName': 'priceLimit',
              'suffix': '_PLIM',
              'formatType': ''
            }
          },
          extraSearchFields: ['product$_identifier', 'product$name', 'product$searchKey'],
          init: function () {
            this.optionDataSource = OB.Datasource.create({
              createClassName: '',
              dataURL: OB.Application.contextUrl + 'org.openbravo.service.datasource/ProductByPriceAndWarehouse',
              requestProperties: {
                params: {
                  targetProperty: 'product',
                  adTabId: 'CC35784EF36B4732B40167CD5D2449F0',
                  IsSelectorItem: 'true',
                  Constants_FIELDSEPARATOR: '$',
                  columnName: 'M_Product_ID',
                  Constants_IDENTIFIER: '_identifier',
                  _extraProperties: 'productPrice$priceListVersion$_identifier,available,qtyOnHand,qtyOrdered,netListPrice,priceLimit,standardPrice,productPrice$priceListVersion$priceList$currency$id,product$uOM$id,product$id,product$_identifier,product$name,product$searchKey,warehouse$_identifier'
                }
              },
              fields: [{
                name: 'id',
                type: '_id_13',
                primaryKey: true
              }, {
                name: 'updated',
                type: '_id_16'
              }, {
                name: 'creationDate',
                type: '_id_16'
              }, {
                name: 'productPrice$priceListVersion$_identifier',
                type: '_id_10',
                additional: true
              }, {
                name: 'available',
                type: '_id_29',
                additional: true
              }, {
                name: 'qtyOnHand',
                type: '_id_29',
                additional: true
              }, {
                name: 'qtyOrdered',
                type: '_id_29',
                additional: true
              }, {
                name: 'netListPrice',
                type: '_id_800008',
                additional: true
              }, {
                name: 'priceLimit',
                type: '_id_800008',
                additional: true
              }, {
                name: 'standardPrice',
                type: '_id_800008',
                additional: true
              }, {
                name: 'productPrice$priceListVersion$priceList$currency$id',
                type: '_id_13',
                additional: true,
                primaryKey: true
              }, {
                name: 'product$uOM$id',
                type: '_id_13',
                additional: true,
                primaryKey: true
              }, {
                name: 'product$id',
                type: '_id_13',
                additional: true,
                primaryKey: true
              }, {
                name: 'product$_identifier',
                type: '_id_10',
                additional: true
              }, {
                name: 'product$name',
                type: '_id_10',
                additional: true
              }, {
                name: 'product$searchKey',
                type: '_id_10',
                additional: true
              }, {
                name: 'warehouse$_identifier',
                type: '_id_10',
                additional: true
              }]
            })

            ;
            this.Super('init', arguments);
          },
          whereClause: 'e.active=\'Y\' and (AD_ISORGINCLUDED(e.warehouse.organization.id, @AD_Org_Id@, @AD_Client_Id@)<\>-1 or (AD_ISORGINCLUDED( @AD_Org_Id@, e.warehouse.organization.id, @AD_Client_Id@)<\>-1))',
          outHiddenInputPrefix: 'inpmProductId',
          gridProps: {
            sort: 4,
            autoExpand: true,
            displaylength: 90,
            fkField: true,
            showHover: true
          },
          type: '_id_800060'
        }, {
          name: 'returnReason',
          title: 'Return Reason',
          columnName: 'C_Return_Reason_ID',
          inpColumnName: 'inpcReturnReasonId',
          refColumnName: 'C_Return_Reason_ID',
          targetEntity: 'ReturnReason',
          gridProps: {
            sort: 10,
            autoExpand: true,
            editorProps: {
              displayField: null,
              valueField: null
            },
            displaylength: 60,
            fkField: true,
            showHover: true
          },
          type: '_id_19'
        }, {
          name: 'attributeSetValue',
          title: 'Attribute Set Value',
          disabled: true,
          columnName: 'M_Attributesetinstance_ID',
          inpColumnName: 'inpmAttributesetinstanceId',
          refColumnName: 'M_AttributeSetInstance_ID',
          targetEntity: 'AttributeSetInstance',
          gridProps: {
            sort: 5,
            autoExpand: true,
            displaylength: 32,
            fkField: true,
            showHover: true
          },
          type: '_id_35'
        }, {
          name: 'movementQuantity',
          title: 'Ship/Receipt Qty',
          required: true,
          disabled: true,
          columnName: 'Movementqty',
          inpColumnName: 'inpmovementqty',
          gridProps: {
            sort: 6,
            filterOnKeypress: false
          },
          type: '_id_29'
        }, {
          name: 'uOM',
          title: 'UOM',
          required: true,
          disabled: true,
          columnName: 'C_Uom_ID',
          inpColumnName: 'inpcUomId',
          refColumnName: 'C_UOM_ID',
          targetEntity: 'UOM',
          gridProps: {
            sort: 7,
            autoExpand: true,
            displaylength: 32,
            fkField: true,
            showHover: true
          },
          type: '_id_30'
        }, {
          name: 'returned',
          title: 'Returned',
          required: true,
          columnName: 'Returned',
          inpColumnName: 'inpreturned',
          validationFn: OB.RM.RMOrderQtyValidate,
          gridProps: {
            sort: 8,
            filterOnKeypress: false
          },
          type: '_id_29'
        }, {
          name: 'unitPrice',
          title: 'Unit Price',
          columnName: 'Priceactual',
          inpColumnName: 'inppriceactual',
          gridProps: {
            sort: 9,
            filterOnKeypress: false
          },
          type: '_id_800008'
        }, {
          name: 'orderNo',
          title: 'Order No.',
          disabled: true,
          columnName: 'OrderNo',
          inpColumnName: 'inporderno',
          "length": 30,
          gridProps: {
            sort: 11,
            autoExpand: true,
            showHover: true,
            length: 30,
            displaylength: 30
          },
          type: '_id_10'
        }, {
          name: 'returnQtyOtherRM',
          title: 'Returned Qty other RM',
          disabled: true,
          columnName: 'ReturnedQty',
          inpColumnName: 'inpreturnedqty',
          gridProps: {
            sort: 12,
            filterOnKeypress: false
          },
          type: '_id_29'
        }, {
          name: '1000100001',
          title: 'Audit',
          personalizable: false,
          defaultValue: 'Audit',
          itemIds: ['creationDate', 'createdBy', 'updated', 'updatedBy'],
          type: 'OBAuditSectionItem'
        }, {
          name: 'creationDate',
          title: 'Creation Date',
          disabled: true,
          updatable: false,
          personalizable: false,
          gridProps: {
            sort: 990,
            cellAlign: 'left',
            showIf: 'false'
          },
          type: '_id_16'
        }, {
          name: 'createdBy',
          title: 'Created By',
          disabled: true,
          updatable: false,
          personalizable: false,
          targetEntity: 'User',
          displayField: 'createdBy$_identifier',
          gridProps: {
            sort: 990,
            cellAlign: 'left',
            showIf: 'false'
          },
          type: '_id_30'
        }, {
          name: 'updated',
          title: 'Updated',
          disabled: true,
          updatable: false,
          personalizable: false,
          gridProps: {
            sort: 990,
            cellAlign: 'left',
            showIf: 'false'
          },
          type: '_id_16'
        }, {
          name: 'updatedBy',
          title: 'Updated By',
          disabled: true,
          updatable: false,
          personalizable: false,
          targetEntity: 'User',
          displayField: 'updatedBy$_identifier',
          gridProps: {
            sort: 990,
            cellAlign: 'left',
            showIf: 'false'
          },
          type: '_id_30'
        }, {
          name: '_notes_',
          personalizable: false,
          type: 'OBNoteSectionItem'
        }, {
          name: '_notes_Canvas',
          personalizable: false,
          type: 'OBNoteCanvasItem'
        }, {
          name: '_linkedItems_',
          personalizable: false,
          type: 'OBLinkedItemSectionItem'
        }, {
          name: '_linkedItems_Canvas',
          personalizable: false,
          type: 'OBLinkedItemCanvasItem'
        }, {
          name: '_attachments_',
          personalizable: false,
          type: 'OBAttachmentsSectionItem'
        }, {
          name: '_attachments_Canvas',
          personalizable: false,
          type: 'OBAttachmentCanvasItem'
        }],

        statusBarFields: [],
        selectionFn: OB.RM.RMOrderSelectionChange,

        gridProperties: {
          defaultWhereClause: ' (e.returnOrder.id IS NULL OR e.returnOrder.id = @Order.id@)\nAND CASE WHEN e.salesTransaction = true THEN \'true\' ELSE \'false\' END = @Order.salesTransaction@\nAND e.businessPartner.id = @Order.businessPartner@\nAND (e.orderNo IS NULL OR e.priceIncludesTax = (SELECT priceIncludesTax FROM PricingPriceList WHERE id = @Order.priceList@))',
          orderByClause: 'obSelected desc, salesOrderLine.lineNo, movementDate desc, orderNo, product.name',
          filterClause: 'e.movementDate \>= (now() - 90) OR e.obSelected=true',
          filterName: 'This grid is filtered using an implicit filter.',
          dummy: true
        },

        dataSource: OB.Datasource.create({
          createClassName: 'OBPickAndExecuteDataSource',
          dataURL: OB.Application.contextUrl + 'org.openbravo.service.datasource/OBDHVR_RM_PELines_V',
          requestProperties: {
            params: {
              _className: 'OBPickAndExecuteDataSource',
              Constants_FIELDSEPARATOR: '$',
              Constants_IDENTIFIER: '_identifier'
            }
          },
          fields: [{
            name: 'id',
            type: '_id_13',
            primaryKey: true
          }, {
            name: 'creationDate',
            type: '_id_16'
          }, {
            name: 'updated',
            type: '_id_16'
          }, {
            name: 'movementDate',
            type: '_id_15'
          }]
        })
      }
    }]
  },
  init: function () {
    var gridProps;
    gridProps = this.viewProperties.fields[0].viewProperties.gridProperties;
    gridProps.whereClause = 'e.processRun=\'' + OB.OBDHVR.currentProcessId + '\' and' + gridProps.defaultWhereClause;
    this.Super('init', arguments);
  }
});