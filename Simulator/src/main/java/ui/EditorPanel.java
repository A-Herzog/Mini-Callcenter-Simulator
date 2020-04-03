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

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import language.Language;
import mathtools.NumberTools;
import mathtools.distribution.NeverDistributionImpl;
import mathtools.distribution.swing.JDistributionPanel;
import simulator.editmodel.EditModel;
import ui.images.Images;

/**
 * Diese Klasse kapselt einen vollständigen Editor für ein <code>EditModel</code>-Objekt
 * @see EditorPanelBase
 * @author Alexander Herzog
 */
public class EditorPanel extends EditorPanelBase {
	private static final long serialVersionUID = 871808238984135272L;

	/* Allgemeines */
	private JTextField name;
	private JTextArea description;
	private JTextField callsToSimulate;

	/* Ankünfte */
	private JTextField batchArrival;
	private JDistributionPanel interArrivalTimeDist;

	/* Warteraum und Wartezeittoleranz */
	private JRadioButton[] waitingRoomSizeSelect;
	private JTextField waitingRoomSize;
	private JRadioButton[] waitingTimeSelect;
	private JDistributionPanel waitingTimeDist;

	/* Bedienungen */
	private JTextField agents;
	private JTextField batchWorking;
	private JDistributionPanel workingTimeDist;
	private JTextField callContinueProbability;

	/* Nachbearbeitungszeiten */
	private JDistributionPanel postProcessingTimeDist;

	/* Wiederholungen */
	private JTextField retryProbability;
	private JDistributionPanel retryTimeDist;

	/**
	 * Konstruktor der Klasse
	 * @param model	Anzuzeigendes Modell
	 * @param readOnly	Wird hier <code>true</code> angegeben, so kann das Modell nicht verändert werden.
	 */
	public EditorPanel(final EditModel model, final boolean readOnly) {
		super(model,readOnly);
	}

	/**
	 * Konstruktor der Klasse<br>
	 * Erstellt ein leeres {@link EditorPanel}.
	 */
	public EditorPanel() {
		super();
	}

	@Override
	protected void buildGUI() {
		JPanel p;

		p=addTab(Language.tr("Editor.Model"),Images.MODEL_EDITOR_MODEL.getIcon());
		p.setLayout(new BorderLayout());
		p.add(new CallcenterModellPanel(EditModel.systemVersion),BorderLayout.CENTER);

		p=addTab(Language.tr("Editor.General"),Images.MODEL_EDITOR_GENERAL.getIcon());
		name=addInputLine(p,Language.tr("Editor.General.Name"),readOnly);
		description=addInputArea(p,Language.tr("Editor.General.Description"),readOnly);
		callsToSimulate=addInputLine(p,Language.tr("Editor.General.CallsToSimulate"),readOnly);
		addCheckInput(callsToSimulate,new Runnable() {@Override public void run() {NumberTools.getPositiveLong(callsToSimulate,true);}});
		p.add(Box.createVerticalStrut(5));

		p=addTab(Language.tr("Editor.Arrivals"),Images.MODEL_EDITOR_ARRIVALS.getIcon());
		batchArrival=addInputLine(p,Language.tr("Editor.Arrivals.ClientsPerArrival"),readOnly);
		addCheckInput(batchArrival,new Runnable() {@Override public void run() {NumberTools.getPositiveLong(batchArrival,true);}});
		interArrivalTimeDist=addDistribution(p,Language.tr("Editor.Arrivals.InterArrivalTimes"),3600,readOnly);
		p.add(Box.createVerticalStrut(5));

		p=addTab(Language.tr("Editor.WaitingRoomAndWaitingTimeTolerance"),Images.MODEL_EDITOR_WAITING.getIcon());
		waitingRoomSizeSelect=addOptions(p,null,new String[]{Language.tr("Editor.WaitingRoomAndWaitingTimeTolerance.WaitingRoom.NoLimit"),Language.tr("Editor.WaitingRoomAndWaitingTimeTolerance.WaitingRoom.Limit")+":"});
		waitingRoomSize=addInputLine(p,null,readOnly);
		addCheckInput(waitingRoomSize,new Runnable() {@Override public void run() {NumberTools.getPositiveLong(waitingRoomSize,true);}});
		waitingTimeSelect=addOptions(p,null,new String[]{Language.tr("Editor.WaitingRoomAndWaitingTimeTolerance.WaitingTimeTolerance.NoLimit"),Language.tr("Editor.WaitingRoomAndWaitingTimeTolerance.WaitingTimeTolerance.Limit")+":"});
		waitingTimeDist=addDistribution(p,null,3600,readOnly);
		p.add(Box.createVerticalStrut(5));

		p=addTab(Language.tr("Editor.Service"),Images.MODEL_EDITOR_SERVICE.getIcon());
		agents=addInputLine(p,Language.tr("Editor.Service.NumberOfAgents"),readOnly);
		addCheckInput(agents,new Runnable() {@Override public void run() {NumberTools.getPositiveLong(agents,true);}});
		batchWorking=addInputLine(p,Language.tr("Editor.Service.ClientsPerServiceBatch"),readOnly);
		addCheckInput(batchWorking,new Runnable() {@Override public void run() {NumberTools.getPositiveLong(batchWorking,true);}});
		workingTimeDist=addDistribution(p,Language.tr("Editor.Service.ServiceTimes"),3600,readOnly);
		callContinueProbability=addInputLine(p,Language.tr("Editor.Service.ForwardingProbability"),readOnly);
		addCheckInput(callContinueProbability,new Runnable() {@Override public void run() {NumberTools.getProbability(callContinueProbability,true);}});
		p.add(Box.createVerticalStrut(5));

		p=addTab(Language.tr("Editor.PostProcessing"),Images.MODEL_EDITOR_POST_PROCESSING.getIcon());
		postProcessingTimeDist=addDistribution(p,null,3600,readOnly);

		p=addTab(Language.tr("Editor.Retry"),Images.MODEL_EDITOR_RETRY.getIcon());
		retryProbability=addInputLine(p,Language.tr("Editor.Retry.RetryProbability"),readOnly);
		addCheckInput(retryProbability,new Runnable() {@Override public void run() {NumberTools.getProbability(retryProbability,true);}});
		retryTimeDist=addDistribution(p,Language.tr("Editor.Retry.RetryDistances"),3600,readOnly);
		p.add(Box.createVerticalStrut(5));
	}

	@Override
	protected void writeGUIDataToModel() {
		Long L;
		Double D;

		/* Allgemeines */
		model.name=name.getText();
		model.description=description.getText();
		L=NumberTools.getPositiveLong(callsToSimulate,true); if (L!=null) model.callsToSimulate=(int)((long)L);

		/* Ankünfte */
		L=NumberTools.getPositiveLong(batchArrival,true); if (L!=null) model.batchArrival=(int)((long)L);
		model.interArrivalTimeDist=interArrivalTimeDist.getDistribution();

		/* Warteraum und Wartezeittoleranz */
		if (waitingRoomSizeSelect[0].isSelected()) model.waitingRoomSize=-1; else {
			model.waitingRoomSize=0;
			L=NumberTools.getPositiveLong(waitingRoomSize,true); if (L!=null) model.waitingRoomSize=(int)((long)L);
		}
		if (waitingTimeSelect[0].isSelected()) model.waitingTimeDist=new NeverDistributionImpl(); else model.waitingTimeDist=waitingTimeDist.getDistribution();

		/* Bedienungen */
		L=NumberTools.getPositiveLong(agents,true); if (L!=null) model.agents=(int)((long)L);
		L=NumberTools.getPositiveLong(batchWorking,true); if (L!=null) model.batchWorking=(int)((long)L);
		model.workingTimeDist=workingTimeDist.getDistribution();
		D=NumberTools.getProbability(callContinueProbability,true); if (D!=null) model.callContinueProbability=D;

		/* Nachbearbeitungszeiten */
		model.postProcessingTimeDist=postProcessingTimeDist.getDistribution();

		/* Wiederholungen */
		D=NumberTools.getProbability(retryProbability,true); if (D!=null) model.retryProbability=D;
		model.retryTimeDist=retryTimeDist.getDistribution();
	}

	@Override
	protected void loadGUIDataFromModel() {
		/* Allgemeines */
		name.setText(model.name);
		description.setText(model.description);
		callsToSimulate.setText(""+model.callsToSimulate);

		/* Ankünfte */
		batchArrival.setText(""+model.batchArrival);
		interArrivalTimeDist.setDistribution(model.interArrivalTimeDist);

		/* Warteraum und Wartezeittoleranz */
		waitingRoomSizeSelect[0].setSelected(model.waitingRoomSize<0);
		waitingRoomSizeSelect[1].setSelected(model.waitingRoomSize>=0);
		waitingRoomSize.setText(""+Math.max(0,model.waitingRoomSize));
		if (model.waitingTimeDist instanceof NeverDistributionImpl) {
			waitingTimeSelect[0].setSelected(true);
			waitingTimeDist.setDistribution(120);
		} else {
			waitingTimeSelect[1].setSelected(true);
			waitingTimeDist.setDistribution(model.waitingTimeDist);
		}

		/* Bedienungen */
		agents.setText(""+model.agents);
		batchWorking.setText(""+model.batchWorking);
		workingTimeDist.setDistribution(model.workingTimeDist);
		callContinueProbability.setText(NumberTools.formatPercent(model.callContinueProbability));

		/* Nachbearbeitungszeiten */
		postProcessingTimeDist.setDistribution(model.postProcessingTimeDist);

		/* Wiederholungen */
		retryProbability.setText(NumberTools.formatPercent(model.retryProbability));
		retryTimeDist.setDistribution(model.retryTimeDist);
	}
}