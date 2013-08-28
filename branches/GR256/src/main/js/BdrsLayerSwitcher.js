/* Copyright (c) 2006-2010 by OpenLayers Contributors (see authors.txt for 
 * full list of contributors). Published under the Clear BSD license.  
 * See http://svn.openlayers.org/trunk/openlayers/license.txt for the
 * full text of the license. */

/**
 * @requires OpenLayers/Strategy.js
 */

// avoid file import ordering issues
if (window.bdrs === undefined) {
	window.bdrs = {};
}
if (bdrs.map === undefined) {
	bdrs.map = {};
}

/**
 * Class: bdrs.map.BdrsCluster
 * Strategy for vector feature clustering with hacks to deselect the currently selected node on reclustering (i.e. on zoom)
 *
 * Inherits from:
 *  - <OpenLayers.Strategy>
 */
bdrs.map.BdrsLayerSwitcher = OpenLayers.Class(OpenLayers.Control.LayerSwitcher, {

    /** 
     * Method: maximizeControl
     * Set up the labels and divs for the control
     * 
     * Parameters:
     * e - {Event} 
     */
    maximizeControl: function(e) {
        // set the div's width and height to empty values, so
        // the div dimensions can be controlled by CSS
        this.div.style.width = "";
        this.div.style.height = "";

        this.showControls(false);

        if (e != null) {
            OpenLayers.Event.stop(e);                                            
        }
		
		// this section is new to this subclass of layer switcher.
		// I don't think openlayers has the ability to call the same method
		// of the superclass from the subclass.
		var layerSwitcherDiv = jQuery(this.layersDiv);
		jQuery('#OpenLayers_Control_MinimizeDiv').css("right", layerSwitcherDiv.HasScrollBar() ? "17px" : "0px");	
    }
});
