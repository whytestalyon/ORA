/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bwc.ora.views;

import com.bwc.ora.collections.Collections;
import com.bwc.ora.collections.LrpCollection;
import com.bwc.ora.models.DisplaySettings;
import com.bwc.ora.models.Lrp;
import com.bwc.ora.models.LrpSettings;
import com.bwc.ora.models.Models;
import com.bwc.ora.models.Oct;
import com.bwc.ora.models.OctSettings;
import com.bwc.ora.uitil.ChangeSupport;
import ij.ImagePlus;
import ij.process.ImageConverter;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;

/**
 *
 * @author Brandon M. Wilk {@literal <}wilkb777@gmail.com{@literal >}
 */
public class OCTDisplayPanel extends JLabel {

    private final HashMap<String, OCTOverlay> dispLayerMap;
    private final Oct oct = Oct.getInstance();
    private final DisplaySettings dispSettings = Models.getInstance().getDisplaySettings();
    private final OctSettings octSettings = Models.getInstance().getOctSettings();
    private LrpCollection lrps = Collections.getInstance().getLrpCollection();
    private final LrpSettings lrpSettings = Models.getInstance().getLrpSettings();
    private final Collections collections = Collections.getInstance();

    private transient final ChangeSupport changeSupport = new ChangeSupport(this);

    private OCTDisplayPanel() {
        this.dispLayerMap = new HashMap<>();
        setAlignmentX(CENTER_ALIGNMENT);

        //register listener for changes to Oct for auto update on change
        oct.addPropertyChangeListener(evt -> {
            updateDisplay();
        });

        //register listeners for changes to display settings for auto update on change
        dispSettings.addPropertyChangeListener(evt -> {
            switch (evt.getPropertyName()) {
                case DisplaySettings.PROP_DISPLAY_SCALE_BARS_ON_OCT:
                    updateDisplay();
                default:
                    break;
            }
        });

        //register listeners for changes to oct settings for auto update on change
        octSettings.addPropertyChangeListener(evt -> {
            switch (evt.getPropertyName()) {
                case OctSettings.PROP_APPLY_CONTRAST_ADJUSTMENT:
                case OctSettings.PROP_APPLY_NOISE_REDUCTION:
                case OctSettings.PROP_DISPLAY_LOG_OCT:
                case OctSettings.PROP_SHARPEN_KERNEL_RADIUS:
                case OctSettings.PROP_SHARPEN_WEIGHT:
                case OctSettings.PROP_SMOOTHING_FACTOR:
                case OctSettings.PROP_X_SCALE:
                case OctSettings.PROP_Y_SCALE:
                    updateDisplay();
                default:
                    break;
            }
        });

        //add listener to check to see if LRP selection should be displayed 
        lrps.addListSelectionListener((ListSelectionEvent e) -> {
            if (e.getValueIsAdjusting() == false) {
                updateDisplay();
            }
        });

        //add listener to check for updates to lrp settings to change lrp
        lrpSettings.addPropertyChangeListener(e -> {
            if (lrps.getSelectedIndex() > -1) {
                switch (e.getPropertyName()) {
                    case LrpSettings.PROP_LRP_HEIGHT:
                    case LrpSettings.PROP_LRP_WIDTH:
                        updateDisplay();
                    default:
                        break;
                }
            }
        });
    }

    public static OCTDisplayPanel getInstance() {

        return OCTDisplayPanelHolder.INSTANCE;
    }

    private static class OCTDisplayPanelHolder {

        private static final OCTDisplayPanel INSTANCE = new OCTDisplayPanel();
    }

    public void addOverlay(OCTOverlay overlay) {
        dispLayerMap.put(overlay.getName(), overlay);
    }

    public void removeOverlay(OCTOverlay overlay) {
        removeOverlay(overlay.getName());
    }

    public void removeOverlay(String overlayName) {
        if (dispLayerMap.containsKey(overlayName)) {
            dispLayerMap.remove(overlayName);
        }
    }

    public Set<String> listOverlayNames() {
        return dispLayerMap.keySet();
    }

    private void updateDisplay() {
        BufferedImage copyoct = oct.getTransformedOct();
        if (copyoct == null) {
            return;
        }
        //create colorable BI for drawing to
        BufferedImage octBase = new BufferedImage(oct.getImageWidth(), oct.getImageHeight(), BufferedImage.TYPE_INT_ARGB);
        ImagePlus ip = new ImagePlus("", copyoct);
        ImageConverter ic = new ImageConverter(ip);
        ic.convertToRGB();
        copyoct = ip.getBufferedImage();
        copyoct.copyData(octBase.getRaster());

        //order overlay layers and draw to image accordingly
        collections.getOverlaysStream()
                .map(lrp -> {
                    System.out.println("Streaming overlay, draw LRP? " + (lrp.display()));
                    return lrp;
                })
                .filter(OCTOverlay::display)
                .sorted((OCTOverlay o1, OCTOverlay o2) -> {
                    return Integer.compare(o1.getZValue(), o2.getZValue());
                })
                .forEach((OCTOverlay overlay) -> {
                    System.out.println("Drawing overlay...");
                    overlay.drawOverlay(octBase);
                });
        //finally set image to be drawn to the screen
        setIcon(new ImageIcon(octBase));
        //notify listeners that the Panel has updated the image
        changeSupport.fireStateChanged();
    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    /**
     * Determine if the supplied coordinate overlaps with the area of this panel
     * that displays the Oct image
     *
     * @param x
     * @param y
     * @param imageOffsetX
     * @param imageOffsetY
     * @return true if the coordinate is within the bounds of the displayed Oct,
     * false if it isn't or if the Oct image isn't displayed already
     */
    public boolean coordinateOverlapsOCT(int x, int y, int imageOffsetX, int imageOffsetY) {
        if (oct.getLogOctImage() != null) {
            boolean withinX = ((imageOffsetX + oct.getImageWidth()) - x) * (x - imageOffsetX) > -1;
            boolean withinY = ((imageOffsetY + oct.getImageHeight()) - y) * (y - imageOffsetY) > -1;
            return withinX && withinY;
        } else {
            return false;
        }
    }

    /**
     * Utility method used to translate a point (i.e. the location of an event,
     * like a mouse click) in the coordinate space of this panel to the
     * coordinate space of the Oct being displayed in the panel.
     *
     * @param p The point to be translated
     * @return translated point from Panel coordinates -> Oct coordinates, or
     * null if the Oct isn't present or the coordinate is outside of the Oct
     */
    public Point convertPanelPointToOctPoint(Point p) {
        int imageWidth = oct.getImageWidth();
        int panelWidth = this.getWidth();
        int imageOffsetX = 0;
        if (panelWidth > imageWidth) {
            imageOffsetX = (panelWidth - imageWidth) / 2;
        }

        int imageHeight = oct.getImageHeight();
        int panelHeight = this.getHeight();
        int imageOffsetY = 0;
        if (panelHeight > imageHeight) {
            imageOffsetY = (panelHeight - imageHeight) / 2;
        }

        if (!coordinateOverlapsOCT(p.x, p.y, imageOffsetX, imageOffsetY)) {
            return null;
        }

        return new Point(p.x - imageOffsetX, p.y - imageOffsetY);
    }
}