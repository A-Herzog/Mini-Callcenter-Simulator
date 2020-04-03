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
package ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import language.Language;
import simulator.editmodel.EditModel;
import simulator.statistics.Statistics;
import ui.images.Images;
import ui.statistics.StatisticsPanel;

/**
 * Dieser Dialog ermöglicht es, ein Editor-Modell im read-only Model zu betrachten.
 * Optional kann angeboten werden, dass Modell (per <code>Runnable</code>-Callback) in den Editor zu laden.
 * @author Alexander Herzog
 */
public class ModelViewerFrame extends JDialog {
	private static final long serialVersionUID = 7004585654111284032L;

	private final EditModel model;
	private final Runnable loadModel;

	private final EditorPanel editorPanel;
	private final StatisticsPanel statisticsPanel;

	private final JButton buttonClose, buttonViewEditor, buttonViewStatistics, buttonLoad;

	/**
	 * Konstruktor der Klasse <code>ModelViewerFrame</code>
	 * @param owner	Übergeordnetes Fenster (an dem sich der Dialog ausrichtet)
	 * @param model	Anzuzeigendes Modell
	 * @param statistics Anzuzeigende Statistikinformationen (kann <code>null</code> sein, dann wird nur das Modell angezeigt)
	 * @param loadModel	Callback zum Laden des Modells in den Editor (wird hier <code>null</code> übergeben, so wird die Option zum Laden des Modells in den Editor nicht angeboten)
	 */
	public ModelViewerFrame(final Window owner, final EditModel model, final Statistics statistics, final Runnable loadModel) {
		super(owner,Language.tr("Viewer.Title"));
		this.model=model;
		this.loadModel=loadModel;

		Container c=getContentPane();
		c.setLayout(new BorderLayout());

		JToolBar toolbar=new JToolBar();
		c.add(toolbar,BorderLayout.NORTH);
		toolbar.setFloatable(false);

		toolbar.add(buttonClose=new JButton(Language.tr("Dialog.Button.Close")));
		buttonClose.setToolTipText(Language.tr("Viewer.Close.Hint"));
		buttonClose.addActionListener(new ButtonListener());
		buttonClose.setIcon(Images.GENERAL_EXIT.getIcon());

		if (statistics!=null) {
			toolbar.addSeparator();
			toolbar.add(buttonViewEditor=new JButton(Language.tr("Main.Toolbar.ShowEditor")));
			buttonViewEditor.setToolTipText(Language.tr("Main.Toolbar.ShowEditor.Hint"));
			buttonViewEditor.addActionListener(new ButtonListener());
			buttonViewEditor.setIcon(Images.MODEL.getIcon());
			toolbar.add(buttonViewStatistics=new JButton(Language.tr("Main.Toolbar.ShowStatistics")));
			buttonViewStatistics.setToolTipText(Language.tr("Main.Toolbar.ShowStatistics.Hint"));
			buttonViewStatistics.addActionListener(new ButtonListener());
			buttonViewStatistics.setIcon(Images.STATISTICS.getIcon());
			buttonViewEditor.setSelected(true);
		} else {
			buttonViewEditor=null;
			buttonViewStatistics=null;
		}

		if (loadModel!=null) {
			toolbar.addSeparator();
			toolbar.add(buttonLoad=new JButton(Language.tr("Viewer.LoadModel")));
			buttonLoad.setToolTipText(Language.tr("Viewer.LoadModel.Hint"));
			buttonLoad.addActionListener(new ButtonListener());
			buttonLoad.setIcon(Images.MODEL_LOAD.getIcon());
		} else {
			buttonLoad=null;
		}

		editorPanel=new EditorPanel(model,true);
		c.add(editorPanel,BorderLayout.CENTER);

		if (statistics!=null) {
			statisticsPanel=new StatisticsPanel(1);
			statisticsPanel.setStatistics(statistics);
		} else {
			statisticsPanel=null;
		}

		setMinimumSize(new Dimension(800,600));
		setLocationRelativeTo(owner);

		setModalityType(DEFAULT_MODALITY_TYPE);
	}

	/**
	 * Konstruktor der Klasse <code>ModelViewerFrame</code>
	 * @param owner	Übergeordnetes Fenster (an dem sich der Dialog ausrichtet)
	 * @param model	Anzuzeigendes Modell
	 */
	public ModelViewerFrame(final Window owner, final EditModel model) {
		this(owner,model,null,null);
	}

	/**
	 * Konstruktor der Klasse <code>ModelViewerFrame</code>
	 * @param owner	Übergeordnetes Fenster (an dem sich der Dialog ausrichtet)
	 * @param model	Anzuzeigendes Modell
	 * @param statistics Anzuzeigende Statistikinformationen (kann <code>null</code> sein, dann wird nur das Modell angezeigt)
	 */
	public ModelViewerFrame(final Window owner, final EditModel model, final Statistics statistics) {
		this(owner,model,statistics,null);
	}

	/**
	 * Liefert das aktuell angezeigte Modell zurück.<br>
	 * (Kann z.B. verwendet werden, wenn das Modell in den Editor geladen werden soll.)
	 * @return Aktuell angezeigtes Modell
	 */
	public final EditModel getModel() {
		return model;
	}

	@Override
	protected JRootPane createRootPane() {
		JRootPane rootPane=new JRootPane();
		InputMap inputMap=rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

		KeyStroke stroke=KeyStroke.getKeyStroke("ESCAPE");
		inputMap.put(stroke,"ESCAPE");
		rootPane.getActionMap().put("ESCAPE",new AbstractAction(){
			private static final long serialVersionUID = -6894097779421071249L;
			@Override public void actionPerformed(ActionEvent e) {setVisible(false); dispose();}
		});

		return rootPane;
	}

	private class ButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Object sender=e.getSource();
			if (sender==buttonClose) {setVisible(false); dispose(); return;}
			if (sender==buttonViewEditor) {
				if (buttonViewEditor.isSelected()) return;
				buttonViewEditor.setSelected(true);
				buttonViewStatistics.setSelected(false);
				Container c=getContentPane();
				c.remove(statisticsPanel);
				c.add(editorPanel,BorderLayout.CENTER);
				c.revalidate();
				editorPanel.setVisible(false);
				editorPanel.setVisible(true);
				return;
			}
			if (sender==buttonViewStatistics) {
				if (buttonViewStatistics.isSelected()) return;
				buttonViewEditor.setSelected(false);
				buttonViewStatistics.setSelected(true);
				Container c=getContentPane();
				c.remove(editorPanel);
				c.add(statisticsPanel,BorderLayout.CENTER);
				c.revalidate();
				statisticsPanel.setVisible(false);
				statisticsPanel.setVisible(true);
				return;
			}
			if (sender==buttonLoad) {loadModel.run();setVisible(false); dispose(); return;}
		}
	}
}