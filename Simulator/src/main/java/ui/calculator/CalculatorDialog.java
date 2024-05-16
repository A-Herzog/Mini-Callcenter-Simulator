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
package ui.calculator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;

import language.Language;
import mathtools.NumberTools;
import mathtools.Table;
import mathtools.distribution.swing.CommonVariables;
import mathtools.distribution.swing.JDistributionEditorPanel;
import mathtools.distribution.swing.JDistributionPanel;
import mathtools.distribution.tools.DistributionRandomNumber;
import parser.CalcSystem;
import parser.MathCalcError;
import statistics.StatisticsDataPerformanceIndicatorWithNegativeValues;
import systemtools.BaseDialog;
import systemtools.MsgBox;
import systemtools.SmallColorChooser;
import systemtools.images.SimToolsImages;
import tools.SetupData;
import ui.calculator.PlotterPanel.Graph;
import ui.help.Help;
import ui.images.Images;

/**
 * Zeigt einen Dialog zur Berechnung von mathematischen Ausdrücken an.<br>
 * Der Konstruktor macht den Dialog direkt sichtbar.
 * @author Alexander Herzog
 * @see CalcSystem
 */
public class CalculatorDialog extends BaseDialog {
	/**
	 * Serialisierungs-ID der Klasse
	 * @see Serializable
	 */
	private static final long serialVersionUID = -3883480454772212675L;

	/** Eingabezeile */
	private final JTextField inputEdit;
	/** Ergebnisausgabezeile */
	private final JTextField outputEdit;

	/** Funktionsplotter */
	private final PlotterPanel plotter;
	/** Eingabefelder für den Funktionsplotter */
	private final List<JTextField> plotterField;

	/**
	 * Anzahl an zu erzeugenden Zufallszahlen
	 * @see #showGenerateRandomNumbersPopup(Component)
	 * @see #randomNumbersIndicators()
	 * @see #randomNumbersCopy()
	 * @see #randomNumbersSave()
	 */
	private long randomNumberCount;
	/** Wahrscheinlichkeitsverteilungsplotter */
	private final JDistributionPanel distributionPlotter;
	/** Eingabefelder für den Wahrscheinlichkeitsverteilungsplotter */
	private final JDistributionEditorPanel distributionEditor;

	/**
	 * Konstruktor der Klasse
	 * @param owner	Übergeordnetes Element
	 */
	public CalculatorDialog(final Component owner) {
		this(owner,null);
	}

	/**
	 * Konstruktor der Klasse
	 * @param owner	Übergeordnetes Element
	 * @param initialExpression	Initial anzuzeigender Ausdruck
	 */
	public CalculatorDialog(final Component owner, final String initialExpression) {
		super(owner,Language.tr("CalculatorDialog.Title"));

		showCloseButton=true;
		final JPanel content=createGUI(()->Help.topicModal(this,"Calculator"));
		content.setLayout(new BorderLayout());

		final JTabbedPane tabs=new JTabbedPane();
		content.add(tabs,BorderLayout.CENTER);

		JPanel tab, line;
		Object[] data;
		Dimension size;
		JButton button;

		/* Tab "Rechner" */
		tabs.addTab(Language.tr("CalculatorDialog.Tab.Calculator"),tab=new JPanel(new BorderLayout()));

		final JPanel lines=new JPanel();
		tab.add(lines,BorderLayout.NORTH);
		lines.setLayout(new BoxLayout(lines,BoxLayout.PAGE_AXIS));

		data=getInputPanel(Language.tr("CalculatorDialog.Expression")+":","",-1);
		lines.add(line=(JPanel)data[0]);
		inputEdit=(JTextField)data[1];
		inputEdit.addKeyListener(new KeyAdapter() {
			@Override public void keyReleased(KeyEvent e) {outputEdit.setText(calc(inputEdit.getText()));}
		});

		data=getInputPanel(Language.tr("CalculatorDialog.Result")+":","",-1);
		lines.add(line=(JPanel)data[0]);
		outputEdit=(JTextField)data[1];
		outputEdit.setEditable(false);

		if (initialExpression!=null) {
			inputEdit.setText(initialExpression);
			SwingUtilities.invokeLater(()->outputEdit.setText(calc(inputEdit.getText())));
		}

		button=new JButton("");
		button.setIcon(Images.COPY.getIcon());
		button.setToolTipText(Language.tr("CalculatorDialog.Result.Copy"));
		size=button.getPreferredSize();
		button.setPreferredSize(new Dimension(size.height,size.height));
		button.addActionListener(e->{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(outputEdit.getText()),null);
		});
		line.add(button,BorderLayout.EAST);

		/* Tab "Funktionsplotter" */
		tabs.addTab(Language.tr("CalculatorDialog.Tab.Plotter"),tab=new JPanel(new BorderLayout()));
		plotterField=new ArrayList<>();
		tab.add(plotter=new PlotterPanel(),BorderLayout.CENTER);
		final JPanel plotterInput=new JPanel();
		tab.add(plotterInput,BorderLayout.SOUTH);
		plotterInput.setLayout(new BoxLayout(plotterInput,BoxLayout.PAGE_AXIS));
		plotterInput.add(getPlotterInputLine(plotter,"10*sin(x)",Color.BLUE));
		plotterInput.add(getPlotterInputLine(plotter,"x^2/5-10",Color.RED));
		plotterInput.add(getPlotterInputLine(plotter,"",Color.GREEN));
		plotter.addRedrawDoneListener(()->{
			final List<PlotterPanel.Graph> graphs=plotter.getGraphs();
			for (int i=0;i<graphs.size();i++) {
				final PlotterPanel.Graph graph=graphs.get(i);
				final JTextField field=plotterField.get(i);
				field.setBackground((field.getText().isBlank() || graph.isLastPlotOk())?NumberTools.getTextFieldDefaultBackground():Color.RED);
			}
		});
		plotter.reload();

		/* Tab "Wahrscheinlichkeitsverteilungen" */
		randomNumberCount=1_000_000;
		tabs.addTab(Language.tr("CalculatorDialog.Tab.Distributions"),tab=new JPanel(new BorderLayout()));
		final JToolBar toolbar=new JToolBar(SwingConstants.HORIZONTAL);
		tab.add(toolbar,BorderLayout.NORTH);
		toolbar.setFloatable(false);
		toolbar.add(button=new JButton(Language.tr("CalculatorDialog.Tab.Distributions.GenerateRandomNumbers"),Images.EXTRAS_CALCULATOR.getIcon()));
		final JButton randomNumbersButton=button;
		button.addActionListener(e->showGenerateRandomNumbersPopup(randomNumbersButton));
		tab.add(distributionPlotter=new JDistributionPanel(new ExponentialDistribution(100),100,false),BorderLayout.CENTER);
		distributionPlotter.setImageSaveSize(SetupData.getSetup().imageSize);
		distributionPlotter.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		distributionPlotter.setPlotType(JDistributionPanel.BOTH);
		tab.add(distributionEditor=new JDistributionEditorPanel(new ExponentialDistribution(100),1000,e->updateDistribution(),true),BorderLayout.SOUTH);

		/* Icons auf den Tabs */
		tabs.setIconAt(0,Images.EXTRAS_CALCULATOR.getIcon());
		tabs.setIconAt(1,Images.EXTRAS_CALCULATOR_PLOTTER.getIcon());
		tabs.setIconAt(2,Images.EXTRAS_CALCULATOR_DISTRIBUTION.getIcon());

		/* Dialog vorbereiten */
		setMinSizeRespectingScreensize(600,400);
		setSizeRespectingScreensize(800,600);
		setResizable(true);
		setLocationRelativeTo(getOwner());
	}

	/**
	 * Veränderte Einstellungen zu der Wahrscheinlichkeitsverteilung an den Plotter übertragen.
	 */
	private void updateDistribution() {
		if (distributionEditor!=null) distributionPlotter.setDistribution(distributionEditor.getDistribution());
	}

	/**
	 * Erzeugt eine Eingabezeile mit zugehörigem Label
	 * @param labelText	Beschriftungstext
	 * @param value	Initialer Text für die Eingabezeile
	 * @param size	Länge der Eingabezeile; wird hier ein Wert &le;0 angegeben, so wird die maximal mögliche Breite verwendet
	 * @return	Array aus: Panel das Beschriftung und Eingabezeile enthält und Eingabezeile selbst
	 */
	private static final Object[] getInputPanel(final String labelText, final String value, final int size) {
		JPanel panel;
		JLabel label;
		JTextField field;

		if (size>0) {
			panel=new JPanel(new FlowLayout(FlowLayout.LEFT));
			label=new JLabel(labelText);
			panel.add(label=new JLabel(labelText));
			panel.add(field=new JTextField(size));
		} else {
			panel=new JPanel(new BorderLayout(5,0));

			Box box;

			box=Box.createVerticalBox();
			box.add(Box.createVerticalGlue());
			final JPanel panelLeft=new JPanel(new FlowLayout());
			panelLeft.add(label=new JLabel(labelText));
			box.add(panelLeft);
			box.add(Box.createVerticalGlue());
			panel.add(box,BorderLayout.WEST);

			field=new JTextField();
			field.setMaximumSize(new Dimension(field.getMaximumSize().width,field.getPreferredSize().height));
			box=Box.createVerticalBox();
			box.add(Box.createVerticalGlue());
			box.add(field);
			box.add(Box.createVerticalGlue());
			panel.add(box,BorderLayout.CENTER);
		}

		label.setLabelFor(field);
		field.setText(value);
		return new Object[]{panel,field};
	}

	/**
	 * Berechnet einen Ausdruck
	 * @param expression	Zu berechnender Ausdruck
	 * @return	Liefert das Ergebnis als Zeichenkette oder eine Fehlermeldung
	 */
	private String calc(final String expression) {
		final CalcSystem calc=new CalcSystem();
		final int error=calc.parse(expression);
		if (error>=0) return String.format(Language.tr("CalculatorDialog.Expression.ParseError"),error+1);
		double d;
		try {
			d=calc.calc();
		} catch (MathCalcError e) {
			return Language.tr("CalculatorDialog.Expression.CalcError");
		}
		return NumberTools.formatNumberMax(d);
	}

	/**
	 * Erzeugt eine Eingabezeile für den Funktionsplotter
	 * @param plotter	Zugehöriger Plotter
	 * @param expression	Initialer Wert für den Ausdruck
	 * @param color	Initiale Farbe für den entsprechenden Graphen
	 * @return	Array aus: Panel das Beschriftung und Eingabezeile enthält und Eingabezeile selbst
	 * @see #getInputPanel(String, String, int)
	 */
	private JPanel getPlotterInputLine(final PlotterPanel plotter, final String expression, final Color color) {
		final PlotterPanel.Graph graph=new PlotterPanel.Graph(expression,color);
		plotter.getGraphs().add(graph);

		final Object[] data=getInputPanel(null,expression==null?"":expression,-1);
		final JPanel panel=(JPanel)data[0];
		final JTextField field=(JTextField)data[1];
		field.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				graph.expression=field.getText();
				plotter.reload();
			}
		});
		final JPanel buttonsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(buttonsPanel,BorderLayout.EAST);
		plotterField.add(field);

		final JButton colorButton=new JButton();
		buttonsPanel.add(colorButton);
		colorButton.setPreferredSize(new Dimension(26,26));
		colorButton.setToolTipText(Language.tr("CalculatorDialog.Plotter.SelectColor"));
		setupColorButton(colorButton,graph);
		colorButton.addActionListener(e->selectColor(colorButton,graph));

		final JButton clearButton=new JButton();
		buttonsPanel.add(clearButton);
		clearButton.setPreferredSize(new Dimension(26,26));
		clearButton.setIcon(Images.EXTRAS_CALCULATOR_PLOTTER_CLEAR.getIcon());
		clearButton.setToolTipText(Language.tr("CalculatorDialog.Plotter.ClearPlot"));
		clearButton.addActionListener(e->{field.setText(graph.expression=""); plotter.reload();});

		return panel;
	}

	/**
	 * Überträgt die gewählte Farbe aus einem {@link Graph}-Objekt
	 * auf eine Farbauswahl-Schaltfläche.
	 * @param colorButton	Schaltfläche auf der die Farbe angezeigt werden soll
	 * @param graph	Graph aus dem die Farbe ausgelesen werden soll
	 */
	private void setupColorButton(final JButton colorButton, final PlotterPanel.Graph graph) {
		final BufferedImage image;
		image=new BufferedImage(16,16,BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics g=image.getGraphics();
		g.setColor(graph.color);
		g.fillRect(0,0,15,15);
		g.setColor(Color.DARK_GRAY);
		g.drawRect(0,0,15,15);
		colorButton.setIcon(new ImageIcon(image));
	}

	/**
	 * Zeigt ein  Popup-Menü zur Auswahl der Farbe für einen Graphen an.
	 * @param colorButton	Aufrufendes Button an dem das Popup-Menü ausgerichtet wird (und dessen Farbeinstellung ggf. automatisch aktualisiert wird)
	 * @param graph	Graph-Objekt aus dem die bisherige Farbe ausgelesen wird und in dem auch ggf. die neue Farbe gespeichert wird
	 */
	private void selectColor(final JButton colorButton, final PlotterPanel.Graph graph) {
		final JPopupMenu popupMenu=new JPopupMenu();

		final SmallColorChooser colorChooser=new SmallColorChooser(graph.color);
		colorChooser.addClickListener(e->{
			graph.color=colorChooser.getColor();
			setupColorButton(colorButton,graph);
			plotter.reload();
			popupMenu.setVisible(false);
		});
		popupMenu.add(colorChooser);
		popupMenu.show(colorButton,0,colorButton.getHeight());
	}

	/**
	 * Zeigt ein Popupmenü mit Befehlen zur Erzeugung von Zufallszahlen gemäß der gewählten Verteilung an.
	 * @param invoker	Aufrufer (zur Ausrichtung des Menüs)
	 */
	private void showGenerateRandomNumbersPopup(final Component invoker) {
		final JPopupMenu popup=new JPopupMenu();

		JMenuItem item;

		final JPanel editorPanel=new JPanel();
		editorPanel.setLayout(new BoxLayout(editorPanel,BoxLayout.PAGE_AXIS));
		final JLabel label=new JLabel(Language.tr("CalculatorDialog.Tab.Distributions.GenerateRandomNumbers.Count")+":");
		editorPanel.add(label);
		final JPanel line=new JPanel(new FlowLayout(FlowLayout.LEFT));
		line.setBorder(BorderFactory.createEmptyBorder(0,25,0,0));
		final JTextField editor=new JTextField(""+randomNumberCount,10);
		line.add(editor);
		editorPanel.add(line);
		label.setLabelFor(editor);
		editor.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {processInput(editor);}
			@Override public void keyReleased(KeyEvent e) {processInput(editor);}
			@Override public void keyPressed(KeyEvent e) {processInput(editor);}
		});
		popup.add(editorPanel);

		popup.addSeparator();

		popup.add(item=new JMenuItem(Language.tr("CalculatorDialog.Tab.Distributions.GenerateRandomNumbers.DetermineCharacteristics"),Images.EXTRAS_CALCULATOR.getIcon()));
		item.addActionListener(e->randomNumbersIndicators());

		popup.add(item=new JMenuItem(Language.tr("CalculatorDialog.Tab.Distributions.GenerateRandomNumbers.Copy"),Images.COPY.getIcon()));
		item.addActionListener(e->randomNumbersCopy());

		popup.add(item=new JMenuItem(Language.tr("CalculatorDialog.Tab.Distributions.GenerateRandomNumbers.Save"),SimToolsImages.SAVE.getIcon()));
		item.addActionListener(e->randomNumbersSave());

		popup.show(invoker,0,invoker.getHeight());
	}

	/**
	 * Verarbeitet die Eingaben in dem Eingabefeld zur Festlegung der Anzahl an zu erzeugenden Zufallszahlen
	 * @param input	Eingabefeld zur Festlegung der Anzahl an zu erzeugenden Zufallszahlen
	 * @see #showGenerateRandomNumbersPopup(Component)
	 */
	private void processInput(final JTextField input) {
		final Long L=NumberTools.getPositiveLong(input,true);
		if (L!=null) randomNumberCount=L.longValue();
	}

	/**
	 * Erzeugt eine Reihe von Zufallszahlen, ermittelt die Kenngrößen der Messreihe und zeigt diese an.
	 */
	private void randomNumbersIndicators() {
		final AbstractRealDistribution distribution=distributionEditor.getDistribution();

		final StatisticsDataPerformanceIndicatorWithNegativeValues indicator=new StatisticsDataPerformanceIndicatorWithNegativeValues(null,-1,-1);
		for (int i=0;i<randomNumberCount;i++) {
			indicator.add(DistributionRandomNumber.random(distribution));
		}

		final StringBuilder info=new StringBuilder();
		info.append(String.format(Language.tr("CalculatorDialog.Tab.Distributions.GenerateRandomNumbers.Generated")+": %s",NumberTools.formatLong(indicator.getCount()))+"\n");
		info.append(String.format(Language.tr("Distribution.Mean")+": E=%s",NumberTools.formatNumber(indicator.getMean(),3))+"\n");
		info.append(String.format(Language.tr("Statistics.Variance")+": Var=%s",NumberTools.formatNumber(indicator.getVar(),3))+"\n");
		info.append(String.format(Language.tr("Distribution.StdDev")+": Std=%s",NumberTools.formatNumber(indicator.getSD(),3))+"\n");
		info.append(String.format(Language.tr("Distribution.CV")+": CV=%s",NumberTools.formatNumber(indicator.getCV(),3))+"\n");
		info.append(String.format(Language.tr("Distribution.Skewness")+": Sk=%s",NumberTools.formatNumber(indicator.getSk(),3))+"\n");

		MsgBox.info(this,Language.tr("CalculatorDialog.Tab.Distributions.GenerateRandomNumbers.DetermineCharacteristics.Title"),info.toString());
	}

	/**
	 * Erzeugt eine Reihe von Zufallszahlen und kopiert diese in die Zwischenablage.
	 */
	private void randomNumbersCopy() {
		final AbstractRealDistribution distribution=distributionEditor.getDistribution();

		final StringBuilder result=new StringBuilder();
		for (int i=0;i<randomNumberCount;i++) {
			result.append(NumberTools.formatNumberMax(DistributionRandomNumber.random(distribution)));
		}

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result.toString()),null);
	}

	/**
	 * Erzeugt eine Reihe von Zufallszahlen und speichert die als Datei.
	 * @return	Liefert <code>true</code>, wenn die Zufallszahlen gespeichert werden konnten
	 */
	private boolean randomNumbersSave() {
		final JFileChooser fc=new JFileChooser();
		CommonVariables.initialDirectoryToJFileChooser(fc);
		fc.setDialogTitle(Language.tr("CalculatorDialog.Tab.Distributions.GenerateRandomNumbers.Save.Title"));
		final FileFilter txt=new FileNameExtensionFilter(Table.FileTypeText+" (*.txt, *.tsv)","txt","tsv");
		fc.addChoosableFileFilter(txt);
		fc.setFileFilter(txt);
		fc.setAcceptAllFileFilterUsed(false);
		if (fc.showSaveDialog(getOwner())!=JFileChooser.APPROVE_OPTION) return false;
		CommonVariables.initialDirectoryFromJFileChooser(fc);
		File file=fc.getSelectedFile();
		if (file.getName().indexOf('.')<0) {
			if (fc.getFileFilter()==txt) file=new File(file.getAbsoluteFile()+".txt");
		}

		final AbstractRealDistribution distribution=distributionEditor.getDistribution();
		final String lineSeparator=System.lineSeparator();

		try(OutputStream stream=new FileOutputStream(file)) {
			try (OutputStreamWriter writer=new OutputStreamWriter(stream,StandardCharsets.UTF_8)) {
				try (BufferedWriter bufferedWriter=new BufferedWriter(writer)) {
					for (int i=0;i<randomNumberCount;i++) {
						bufferedWriter.write(NumberTools.formatNumberMax(DistributionRandomNumber.random(distribution)));
						bufferedWriter.write(lineSeparator);
					}
					return true;
				}
			}
		} catch (IOException e) {
			return false;
		}
	}
}