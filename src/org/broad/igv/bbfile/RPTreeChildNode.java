/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.bbfile;

import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * Container class for R+ tree leaf or child node format
 * Note: RPTreeNode interface supports leaf and child node formats
 */
/**
 * File retrieved from BigWig project at Google code on July 26, 2011, revision
 * 36
 * 
 * http://code.google.com/p/bigwig/
 * 
 * This project provides java readers for the UCSC's BigWig and BigBed formats.
 * It was originally developed by Martin Decautis and Jim Robinson for the
 * Integrative Genomics Viewer (http://www.broadinstitute.org/igv). Thanks to
 * Jim Kent and Ann Zweig and from UCSC for their assistance.
 * 
 * 
 * Modification to work with GenomeView by Thomas Abeel.
 * 
 * @author Martin Decautis
 * @author Jim Robinson
 * @author Thomas Abeel
 * 
 */
public class RPTreeChildNode implements RPTreeNode {

	
    private static Logger log = Logger.getLogger(RPTreeChildNode.class.getCanonicalName());

    private RPChromosomeRegion chromosomeBounds;  // chromosome bounds for entire node
    private ArrayList<RPTreeChildNodeItem> childItems; // array for child items

    public RPTreeChildNode() {
    	
        childItems = new ArrayList<RPTreeChildNodeItem>();

        // Note: Chromosome bounds are null until a valid region is specified
    }

    // *** BPTreeNode interface implementation ***


    public RPChromosomeRegion getChromosomeBounds() {
        return chromosomeBounds;
    }

    public int compareRegions(RPChromosomeRegion chromosomeRegion) {

        // test leaf item bounds for hit
        int value = chromosomeBounds.compareRegions(chromosomeRegion);
        return value;
    }

    public boolean isLeaf() {
        return false;
    }

    public int getItemCount() {
        return childItems.size();
    }

    public RPTreeNodeItem getItem(int index) {

        if (index < 0 || index >= childItems.size())
            return null;
        else {
            RPTreeChildNodeItem item = childItems.get(index);
            return (RPTreeNodeItem) item;
        }
    }

    public boolean insertItem(RPTreeNodeItem item) {

        RPTreeChildNodeItem newItem = (RPTreeChildNodeItem) item;

        // Quick implementation: assumes all keys are inserted in rank order
        // todo: or compare key and insert at rank location
        childItems.add(newItem);

        // Update node bounds or start node chromosome bounds with first entry
        if (chromosomeBounds == null)
            chromosomeBounds = new RPChromosomeRegion(newItem.getChromosomeBounds());
        else
            chromosomeBounds = chromosomeBounds.getExtremes(newItem.getChromosomeBounds());

        // success
        return true;
    }

    public boolean deleteItem(int index) {

        int itemCount = getItemCount();

        // unacceptable index  - reject
        if (index < 0 || index >= itemCount)
            return false;

        // delete indexed entry
        childItems.remove(index);

        // successful delete
        return true;
    }

    public void printItems() {

         for (int item = 0; item < childItems.size(); ++item) {
            childItems.get(item).print();
        }
    }


}
