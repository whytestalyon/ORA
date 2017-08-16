package com.bwc.ora.views.menus;

import com.bwc.ora.collections.ModelsCollection;
import com.bwc.ora.models.DisplaySettings;

import javax.swing.*;

/**
 * Created by Brandon on 1/16/2017.
 */
public class SettingsMenu extends JMenu {

    private JCheckBoxMenuItem displayScaleBarsCheckBox = new JCheckBoxMenuItem("Display Scale Bars", true);
    private JCheckBoxMenuItem displayFileNameCheckBox = new JCheckBoxMenuItem("Display OCT File Name", true);
    private JCheckBoxMenuItem displayLrpPeaksCheckBox = new JCheckBoxMenuItem("Display LRP Peaks", true);
    private JCheckBoxMenuItem showFwhmCheckBox = new JCheckBoxMenuItem("Display FWHM on LRP", false);

    public SettingsMenu() {
        setText("Settings");
        initSettingsMenu();
    }

    public SettingsMenu(String s) {
        super(s);
        initSettingsMenu();
    }

    private void initSettingsMenu() {
        add(displayFileNameCheckBox);
        add(displayScaleBarsCheckBox);
        add(displayLrpPeaksCheckBox);
        add(showFwhmCheckBox);

        connectSettingsToModel();
    }

    private void connectSettingsToModel() {
        DisplaySettings settings = ModelsCollection.getInstance().getDisplaySettings();

        displayFileNameCheckBox.setSelected(settings.isDisplayFileName());
        displayFileNameCheckBox.addItemListener(evt -> settings.setDisplayFileName(displayFileNameCheckBox.isSelected()));
        settings.addPropertyChangeListener(evt -> {
            if (DisplaySettings.PROP_DISPLAY_FILE_NAME.equals(evt.getPropertyName())) {
                displayFileNameCheckBox.setSelected(settings.isDisplayFileName());
            }
        });

        displayScaleBarsCheckBox.setSelected(settings.isDisplayScaleBarsOnOct());
        displayScaleBarsCheckBox.addItemListener(evt -> settings.setDisplayScaleBarsOnOct(displayScaleBarsCheckBox.isSelected()));
        settings.addPropertyChangeListener(evt -> {
            if (DisplaySettings.PROP_DISPLAY_SCALE_BARS_ON_OCT.equals(evt.getPropertyName())) {
                displayScaleBarsCheckBox.setSelected(settings.isDisplayScaleBarsOnOct());
            }
        });

        displayLrpPeaksCheckBox.setSelected(settings.isShowLrpPeaks());
        displayLrpPeaksCheckBox.addItemListener(evt -> settings.setShowLrpPeaks(displayLrpPeaksCheckBox.isSelected()));
        settings.addPropertyChangeListener(evt -> {
            if (DisplaySettings.PROP_SHOW_LRP_PEAKS.equals(evt.getPropertyName())) {
                displayLrpPeaksCheckBox.setSelected(settings.isShowLrpPeaks());
            }
        });

        showFwhmCheckBox.setSelected(settings.isShowFwhmOnLrp());
        showFwhmCheckBox.addItemListener(evt -> settings.setShowFwhmOnLrp(showFwhmCheckBox.isSelected()));
        settings.addPropertyChangeListener(evt -> {
            if (DisplaySettings.PROP_SHOW_FWHM_ON_LRP.equals(evt.getPropertyName())) {
                showFwhmCheckBox.setSelected(settings.isShowFwhmOnLrp());
            }
        });
    }
}