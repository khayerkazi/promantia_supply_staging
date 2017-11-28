//OB.Utilities = OB.Utilities || {}


OB.Utilities.Action.set('refreshTheGrid', function (paramObj) {
var grid = OB.MainView.TabSet.getSelectedTab().pane.grid.viewGrid;
grid.invalidateCache(); //force refresh
});
