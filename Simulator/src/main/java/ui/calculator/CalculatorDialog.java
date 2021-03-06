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
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import language.Language;
import mathtools.NumberTools;
import mathtools.distribution.swing.JDistributionEditorPanel;
import mathtools.distribution.swing.JDistributionPanel;
import parser.CalcSystem;
import parser.MathCalcError;
import systemtools.BaseDialog;
import systemtools.SmallColorChooser;
import tools.SetupData;
import ui.calculator.PlotterPanel.Graph;
import ui.help.Help;
import ui.images.Images;

/**
 * Zeigt einen Dialog zur Berechnung von mathematischen Ausdr�cken an.<br>
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
	/** Eingabefelder f�r den Funktionsplotter */
	private final List<JTextField> plotterField;

	/** Wahrscheinlichkeitsverteilungsplotter */
	private final JDistributionPanel distributionPlotter;
	/** Eingabefelder f�r den Wahrscheinlichkeitsverteilungsplotter */
	private final JDistributionEditorPanel distributionEditor;

	/**
	 * Konstruktor der Klasse
	 * @param owner	�bergeordnetes Element
	 */
	public CalculatorDialog(final Component owner) {
		this(owner,null);
	}

	/**
	 * Konstruktor der Klasse
	 * @param owner	�bergeordnetes Element
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

		final JButton button=new JButton("");
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
				field.setBackground((field.getText().trim().isEmpty() || graph.isLastPlotOk())?NumberTools.getTextFieldDefaultBackground():Color.RED);
			}
		});
		plotter.reload();

		/* Tab "Wahrscheinlichkeitsverteilungen" */
		tabs.addTab(Language.tr("CalculatorDialog.Tab.Distributions"),tab=new JPanel(new BorderLayout()));
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
	 * Ver�nderte Einstellungen zu der Wahrscheinlichkeitsverteilung an den Plotter �bertragen.
	 */
	private void updateDistribution() {
		if (distributionEditor!=null) distributionPlotter.setDistribution(distributionEditor.getDistribution());
	}

	/**
	 * Erzeugt eine Eingabezeile mit zugeh�rigem Label
	 * @param labelText	Beschriftungstext
	 * @param value	Initialer Text f�r die Eingabezeile
	 * @param size	L�nge der Eingabezeile; wird hier ein Wert &le;0 angegeben, so wird die maximal m�gliche Breite verwendet
	 * @return	Array aus: Panel das Beschriftung und Eingabezeile enth�lt und Eingabezeile selbst
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
	 * Erzeugt eine Eingabezeile f�r den Funktionsplotter
	 * @param plotter	Zugeh�riger Plotter
	 * @param expression	Initialer Wert f�r den Ausdruck
	 * @param color	Initiale Farbe f�r den entsprechenden Graphen
	 * @return	Array aus: Panel das Beschriftung und Eingabezeile enth�lt und Eingabezeile selbst
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
	 * �bertr�gt die gew�hlte Farbe aus einem {@link Graph}-Objekt
	 * auf eine Farbauswahl-Schaltfl�che.
	 * @param colorButton	Schaltfl�che auf der die Farbe angezeigt werden soll
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
	 * Zeigt ein  Popup-Men� zur Auswahl der Farbe f�r einen Graphen an.
	 * @param colorButton	Aufrufendes Button an dem das Popup-Men� ausgerichtet wird (und dessen Farbeinstellung ggf. automatisch aktualisiert wird)
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
}