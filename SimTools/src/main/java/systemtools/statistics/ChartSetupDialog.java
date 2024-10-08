/**
 * Copyright 2020 Alexander Herzog
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package systemtools.statistics;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.Serializable;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import systemtools.BaseDialog;
import systemtools.LabeledColorChooserButton;
import systemtools.OptionalColorChooserButton;
import systemtools.images.SimToolsImages;

/**
 * Erm�glicht die Einstellung der Schriftarten und
 * Farben der Statistikdiagramme.
 * @author Alexander Herzog
 * @see ChartSetup
 */
public class ChartSetupDialog extends BaseDialog {
	/**
	 * Serialisierungs-ID der Klasse
	 * @see Serializable
	 */
	private static final long serialVersionUID=-9001423367503679988L;

	/**
	 * Tabs in dem Dialog
	 */
	private final JTabbedPane tabs;

	/**
	 * Einstellungen f�r die Titelschriftart
	 */
	private final Object[] setupTitleFont;

	/**
	 * Einstellungen f�r die Schriftart f�r die Namen der Achsen
	 */
	private final Object[] setupAxisLabelFont;

	/**
	 * Einstellungen f�r die Schriftart f�r die Wertangaben an den Achsen
	 */
	private final Object[] setupAxisValueFont;

	/**
	 * Einstellungen f�r die Schriftart f�r die Legende
	 */
	private final Object[] setupLegendFont;

	/**
	 * Einstellungen f�r die Hintergrundfarbe 1
	 */
	private final LabeledColorChooserButton setupBackgroundColor1;

	/**
	 * Einstellungen f�r die Hintergrundfarbe 2
	 */
	private final OptionalColorChooserButton setupBackgroundColor2;

	/**
	 * Einstellungen f�r die Rahmenfarbe
	 */
	private final LabeledColorChooserButton setupOutlineColor;

	/**
	 * Einstellungen f�r die Rahmenbreite
	 */
	private final SpinnerModel setupOutlineStroke;

	/**
	 * Einstellung f�r die Gr��e zum Speichern von Diagrammen
	 */
	private final SpinnerModel setupSize;

	/**
	 * Konstruktor der Klasse
	 * @param owner	�bergeordnetes Element
	 * @param saveSize	Gr��e beim Speichern von Bildern
	 * @param chartSetup	Einstellungen f�r die Anzeige von Diagrammen
	 */
	public ChartSetupDialog(final Component owner, final int saveSize, final ChartSetup chartSetup) {
		super(owner,StatisticsBasePanel.viewersChartSetupTitle);

		addUserButton(StatisticsBasePanel.viewersChartSetupDefaults,StatisticsBasePanel.viewersChartSetupDefaultsHint,SimToolsImages.UNDO.getIcon());
		final JPanel content=createGUI(null);
		content.setLayout(new BorderLayout());

		content.add(tabs=new JTabbedPane(),BorderLayout.CENTER);

		JPanel tabOuter;
		JPanel tab;
		JPanel line;

		/* Tab "Diagrammtitel" */
		tabs.addTab(StatisticsBasePanel.viewersChartSetupTitleFont,tabOuter=new JPanel(new BorderLayout()));
		tabOuter.add(tab=new JPanel(),BorderLayout.NORTH);
		tab.setLayout(new BoxLayout(tab,BoxLayout.PAGE_AXIS));
		setupTitleFont=buildFontInput(tab,chartSetup.titleFont);

		/* Tab "Achsenbeschriftung" */
		tabs.addTab(StatisticsBasePanel.viewersChartSetupAxisFont,tabOuter=new JPanel(new BorderLayout()));
		tabOuter.add(tab=new JPanel(),BorderLayout.NORTH);
		tab.setLayout(new BoxLayout(tab,BoxLayout.PAGE_AXIS));
		buildInfoLine(tab,StatisticsBasePanel.viewersChartSetupAxisLabelsFont);
		setupAxisLabelFont=buildFontInput(tab,chartSetup.axisLabelFont);
		buildInfoLine(tab,StatisticsBasePanel.viewersChartSetupAxisValuesFont);
		setupAxisValueFont=buildFontInput(tab,chartSetup.axisValueFont);

		/* Tab "Legende" */
		tabs.addTab(StatisticsBasePanel.viewersChartSetupLegendFont,tabOuter=new JPanel(new BorderLayout()));
		tabOuter.add(tab=new JPanel(),BorderLayout.NORTH);
		tab.setLayout(new BoxLayout(tab,BoxLayout.PAGE_AXIS));
		setupLegendFont=buildFontInput(tab,chartSetup.legendFont);

		/* Tab "Zeichenfl�che" */
		tabs.addTab(StatisticsBasePanel.viewersChartSetupSurface,tabOuter=new JPanel(new BorderLayout()));
		tabOuter.add(tab=new JPanel(),BorderLayout.NORTH);
		tab.setLayout(new BoxLayout(tab,BoxLayout.PAGE_AXIS));

		tab.add(line=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		line.add(setupBackgroundColor1=new LabeledColorChooserButton(StatisticsBasePanel.viewersChartSetupSurfaceBackgroundColor+":",chartSetup.backgroundColor1));

		tab.add(line=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		line.add(new JLabel(StatisticsBasePanel.viewersChartSetupSurfaceBackgroundGradient+":"));
		line.add(setupBackgroundColor2=new OptionalColorChooserButton(StatisticsBasePanel.viewersChartSetupSurfaceBackgroundGradientActive,chartSetup.backgroundColor2,Color.WHITE));

		tab.add(line=new JPanel(new FlowLayout(FlowLayout.LEFT)));
		line.add(setupOutlineColor=new LabeledColorChooserButton(StatisticsBasePanel.viewersChartSetupSurfaceOutlineColor+":",chartSetup.outlineColor));

		setupOutlineStroke=buildSizeInput(tab,StatisticsBasePanel.viewersChartSetupSurfaceOutlineWidth,((BasicStroke)chartSetup.outlineStroke).getLineWidth(),0,20,0.5);

		/* Tab "Aufl�sung beim Speichern" */
		tabs.addTab(StatisticsBasePanel.viewersSaveImageSizePrompt,tabOuter=new JPanel(new BorderLayout()));
		tabOuter.add(tab=new JPanel(),BorderLayout.NORTH);
		tab.setLayout(new BoxLayout(tab,BoxLayout.PAGE_AXIS));
		setupSize=buildSizeInput(tab,StatisticsBasePanel.viewersSaveImageSizePrompt,saveSize,50,5000);

		/* Icons auf den Tabs */
		tabs.setIconAt(0,SimToolsImages.STATISTICS_DIAGRAM_FONT_SIZE.getIcon());
		tabs.setIconAt(1,SimToolsImages.STATISTICS_DIAGRAM_FONT_SIZE.getIcon());
		tabs.setIconAt(2,SimToolsImages.STATISTICS_DIAGRAM_FONT_SIZE.getIcon());
		tabs.setIconAt(3,SimToolsImages.STATISTICS_DIAGRAM_LINE.getIcon());
		tabs.setIconAt(4,SimToolsImages.STATISTICS_DIAGRAM_PICTURE.getIcon());

		/* Dialog starten */
		setMinSizeRespectingScreensize(800,0);
		pack();
		setLocationRelativeTo(getOwner());
		setVisible(true);
	}

	/**
	 * F�gt eine fett dargestellte Infozeile ein.
	 * @param parent	�bergeordnetes Element
	 * @param text	Auszugebende Infozeile
	 */
	private void buildInfoLine(final JComponent parent, final String text) {
		final JPanel line=new JPanel(new FlowLayout(FlowLayout.LEFT));
		parent.add(line);

		line.add(new JLabel("<html><body><b>"+text+"</b></body></html>"));
	}

	/**
	 * F�gt Eingabefelder zur Konfiguration einer Schrift ein.
	 * @param parent	�bergeordnetes Element
	 * @param font	Anf�nglich anzuzeigende Schriftartdaten
	 * @return	Array aus Eingabefeld und Checkboxen
	 */
	private Object[] buildFontInput(final JComponent parent, final Font font) {
		JLabel label;

		final JPanel line=new JPanel(new FlowLayout(FlowLayout.LEFT));
		parent.add(line);

		line.add(label=new JLabel(StatisticsBasePanel.viewersChartSetupFontSize+":"));

		final SpinnerModel spinnerModel=new SpinnerNumberModel(font.getSize(),1,72,1);
		final JSpinner spinner=new JSpinner(spinnerModel);
		final JSpinner.NumberEditor editor=new JSpinner.NumberEditor(spinner);
		editor.getFormat().setGroupingUsed(false);
		editor.getTextField().setColumns(4);
		spinner.setEditor(editor);
		line.add(spinner);
		label.setLabelFor(spinner);

		final JCheckBox styleBold=new JCheckBox("<html><body><b>"+StatisticsBasePanel.viewersChartSetupFontBold+"</b></body></html>",font.isBold());
		line.add(styleBold);

		final JCheckBox styleItalic=new JCheckBox("<html><body><i>"+StatisticsBasePanel.viewersChartSetupFontItalic+"</i></body></html>",font.isItalic());
		line.add(styleItalic);

		return new Object[] {spinnerModel,styleBold,styleItalic};
	}

	/**
	 * Erzeugt ein Spinner-Eingabefeld
	 * @param parent	�bergeordnetes Element
	 * @param text	Beschriftung
	 * @param value	Initial anzuzeigender Wert
	 * @param min	Minimum
	 * @param max	Maximum
	 * @return	Spinner-Modell
	 */
	public SpinnerModel buildSizeInput(final JComponent parent, final String text, final int value, final int min, final int max) {
		JLabel label;

		final JPanel line=new JPanel(new FlowLayout(FlowLayout.LEFT));
		parent.add(line);

		line.add(label=new JLabel(text+":"));

		final SpinnerModel spinnerModel=new SpinnerNumberModel(value,min,max,1);
		final JSpinner spinner=new JSpinner(spinnerModel);
		final JSpinner.NumberEditor editor=new JSpinner.NumberEditor(spinner,"###0.###");
		editor.getFormat().setGroupingUsed(false);
		editor.getTextField().setColumns(4);
		spinner.setEditor(editor);
		line.add(spinner);
		label.setLabelFor(spinner);

		return spinnerModel;
	}

	/**
	 * Erzeugt ein Spinner-Eingabefeld
	 * @param parent	�bergeordnetes Element
	 * @param text	Beschriftung
	 * @param value	Initial anzuzeigender Wert
	 * @param min	Minimum
	 * @param max	Maximum
	 * @param step	Schrittweite f�r den Spinner
	 * @return	Spinner-Modell
	 */
	public SpinnerModel buildSizeInput(final JComponent parent, final String text, final double value, final double min, final double max, final double step) {
		JLabel label;

		final JPanel line=new JPanel(new FlowLayout(FlowLayout.LEFT));
		parent.add(line);

		line.add(label=new JLabel(text+":"));

		final SpinnerModel spinnerModel=new SpinnerNumberModel(value,min,max,step);
		final JSpinner spinner=new JSpinner(spinnerModel);
		final JSpinner.NumberEditor editor=new JSpinner.NumberEditor(spinner);
		editor.getFormat().setGroupingUsed(false);
		editor.getTextField().setColumns(4);
		spinner.setEditor(editor);
		line.add(spinner);
		label.setLabelFor(spinner);

		return spinnerModel;
	}

	/**
	 * Liefert die Schriftarteneinstellungen aus dem Dialog
	 * @param setup	Array aus Schriftgr��eneingabefeld und Checkboxen f�r den Stil
	 * @param template	Bisherige Schriftart
	 * @return	Neue Schriftart
	 */
	private Font getFont(final Object[] setup, final Font template) {
		final int size=((Integer)(((SpinnerModel)setup[0]).getValue())).intValue();

		int style=Font.PLAIN;
		if (((JCheckBox)setup[1]).isSelected()) style+=Font.BOLD;
		if (((JCheckBox)setup[2]).isSelected()) style+=Font.ITALIC;

		return new Font(template.getName(),style,size);
	}

	/**
	 * Aktualisiert die Schriftartneinstellungen im Dialog
	 * @param setup	Array aus Schriftgr��eneingabefeld und Checkboxen f�r den Stil
	 * @param data	Schriftartendaten, die in die Eingabefeld eingetragen werden sollen
	 */
	private void updateFonts(final Object[] setup, final Font data) {
		((SpinnerModel)setup[0]).setValue(data.getSize());

		((JCheckBox)setup[1]).setSelected(data.isBold());
		((JCheckBox)setup[2]).setSelected(data.isItalic());
	}

	/**
	 * Liefert die eingestellte Gr��e beim Speichern von Bildern.
	 * @return	Gr��e beim Speichern von Bildern
	 */
	public int getSaveSize() {
		return Math.max(50,Math.min(5000,((Integer)setupSize.getValue()).intValue()));
	}

	/**
	 * Liefert die eingestellten Einstellungen f�r die Anzeige von Diagrammen.
	 * @return	Einstellungen f�r die Anzeige von Diagrammen
	 */
	public ChartSetup getChartSetup() {
		final ChartSetup chartSetup=new ChartSetup();

		chartSetup.titleFont=getFont(setupTitleFont,chartSetup.titleFont);
		chartSetup.axisLabelFont=getFont(setupAxisLabelFont,chartSetup.axisLabelFont);
		chartSetup.axisValueFont=getFont(setupAxisValueFont,chartSetup.axisValueFont);
		chartSetup.legendFont=getFont(setupLegendFont,chartSetup.legendFont);

		chartSetup.backgroundColor1=setupBackgroundColor1.getColor();
		chartSetup.backgroundColor2=setupBackgroundColor2.getColor();
		chartSetup.outlineStroke=new BasicStroke(((Double)setupOutlineStroke.getValue()).floatValue());

		return chartSetup;
	}

	/**
	 * Setzt die Einstellungen auf der aktuellen oder allen Dialogseiten zur�ck.
	 * @param allPages	Alle Seiten (<code>true</code>) oder nur die aktuelle Seite (<code>false</code>)
	 */
	private void resetData(final boolean allPages) {
		final ChartSetup defaults=new ChartSetup();

		for (int i=0;i<5;i++) if (tabs.getSelectedIndex()==i || allPages) switch (i) {
		case 0:
			/* Diagrammtitel */
			updateFonts(setupTitleFont,defaults.titleFont);
			break;
		case 1:
			/* Achsenbeschriftung */
			updateFonts(setupAxisLabelFont,defaults.axisLabelFont);
			updateFonts(setupAxisValueFont,defaults.axisValueFont);
			break;
		case 2:
			/* Legende */
			updateFonts(setupLegendFont,defaults.legendFont);
			break;
		case 3:
			/* Zeichenfl�che */
			setupBackgroundColor1.setColor(defaults.backgroundColor1);
			setupBackgroundColor2.setColor(defaults.backgroundColor2);
			setupOutlineColor.setColor(defaults.outlineColor);
			setupOutlineStroke.setValue(0.5);
			break;
		case 4:
			/* Aufl�sung beim Speichern */
			setupSize.setValue(2000);
			break;
		}
	}

	@Override
	protected void userButtonClick(final int nr, final JButton button) {
		final JPopupMenu popup=new JPopupMenu();
		JMenuItem item;

		popup.add(item=new JMenuItem(StatisticsBasePanel.viewersChartSetupDefaultsThis,SimToolsImages.UNDO.getIcon()));
		item.addActionListener(e->resetData(false));

		popup.add(item=new JMenuItem(StatisticsBasePanel.viewersChartSetupDefaultsAll,SimToolsImages.UNDO.getIcon()));
		item.addActionListener(e->resetData(false));

		popup.show(button,0,button.getHeight());
	}
}
