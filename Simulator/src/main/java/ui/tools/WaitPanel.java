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
package ui.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.commons.math3.util.FastMath;

import language.Language;
import mathtools.NumberTools;
import simulator.Simulator;
import ui.images.Images;

/**
 * Diese Klasse zeigt ein "Bitte warten"-Panel während der Simulation an.
 * @author Alexander Herzog
 */
public class WaitPanel extends JPanel {
	private static final long serialVersionUID = 3524929788005334671L;

	private boolean simulationSuccessful;
	private boolean abortRun;

	private JLabel info1, info2;
	private JLabel statusbar;
	private JProgressBar progress;
	private JButton cancel;

	private Timer timer;
	private long startTime;
	private long countTimerIntervals;
	private int lastGesamt;

	private Simulator simulator;
	private Runnable simulationDone;

	/**
	 * Konstruktor der Klasse <code>WaitPanel</code>
	 */
	public WaitPanel() {
		super(new BorderLayout());

		JPanel mainarea, p1a, p1b, p2;

		add(statusbar=new JLabel(),BorderLayout.SOUTH);
		statusbar.setPreferredSize(new Dimension(100,20));
		statusbar.setBorder(BorderFactory.createEtchedBorder());

		add(mainarea=new JPanel(),BorderLayout.CENTER);
		mainarea.setLayout(new BoxLayout(mainarea,BoxLayout.Y_AXIS));
		mainarea.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		mainarea.add(Box.createVerticalGlue());
		mainarea.add(p1a=new JPanel()); p1a.setLayout(new BoxLayout(p1a,BoxLayout.X_AXIS));
		mainarea.add(p1b=new JPanel()); p1b.setLayout(new BoxLayout(p1b,BoxLayout.X_AXIS));
		mainarea.add(Box.createVerticalStrut(10));
		mainarea.add(progress=new JProgressBar(0,100));
		mainarea.add(Box.createVerticalStrut(10));
		mainarea.add(p2=new JPanel()); p2.setLayout(new BoxLayout(p2,BoxLayout.X_AXIS));
		mainarea.add(Box.createVerticalGlue());
		mainarea.add(Box.createVerticalGlue());

		progress.setStringPainted(true);

		p1a.add(Box.createHorizontalGlue());
		p1a.add(info1=new JLabel(""));
		p1a.add(Box.createHorizontalGlue());

		p1b.add(Box.createHorizontalGlue());
		p1b.add(info2=new JLabel(""));
		p1b.add(Box.createHorizontalGlue());

		p2.add(Box.createHorizontalGlue());
		p2.add(cancel=new JButton(Language.tr("Dialog.Button.Cancel")));
		p2.add(Box.createHorizontalGlue());
		cancel.addActionListener(e->abortSimulation());
		cancel.setIcon(Images.GENERAL_CANCEL.getIcon());

		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"ESCAPE");
		getActionMap().put("ESCAPE",new AbstractAction() {
			private static final long serialVersionUID = 190237083100271239L;
			@Override public void actionPerformed(ActionEvent e) {abortSimulation();}
		});
	}

	/**
	 * Bricht die Simulation ab.
	 */
	public void abortSimulation() {
		cancel.setEnabled(false);
		abortRun=true;
	}

	/**
	 * Prüft, ob die Simulation erfolgreich beendet wurde.
	 * (Kann z.B. von dem Runnable, welches <code>setSimulator</code> übergeben wird aus aufgerufen werden.)
	 * @return	Gibt <code>true</code> zurück, wenn die Simulation erfolgreich beendet wurde. Andernfalls wurde sie entweder abgebrochen oder läuft noch.
	 */
	public boolean isSimulationSuccessful() {
		return simulationSuccessful;
	}

	/**
	 * Setzt das <code>Simulator</code>-Objekt, dessen Fortschritt in diesem Panel angezeigt werden soll.
	 * @param simulator	Gestarteter Simulator, dessen Daten hier angezeigt werden sollen.
	 * @param simulationDone	Wird aufgerufen, wenn die Simulation beendet wurde (erfolgreich oder per Nutzerabbruch). Wird hier <code>null</code> übergeben, so erfolgt keine Rückmeldung.
	 */
	public final void setSimulator(final Simulator simulator, final Runnable simulationDone) {
		abortRun=false;
		cancel.setEnabled(true);
		simulationSuccessful=false;
		this.simulator=simulator;
		this.simulationDone=simulationDone;

		statusbar.setText(Language.tr("Wait.Status.Start"));
		info1.setText(Language.tr("Wait.Info.Start"));
		info2.setText("");
		progress.setMaximum(1000);
		progress.setValue(0);

		startTime=System.currentTimeMillis();

		countTimerIntervals=0;
		lastGesamt=Integer.MAX_VALUE;

		if (!simulator.isRunning()) {
			finalizeSimulation(true);
		} else {
			timer=new Timer("SimProgressBar",false);
			timer.schedule(new UpdateInfoTask(),50,50);
		}
	}

	private void finalizeSimulation(final boolean successful) {
		if (timer!=null) timer.cancel();
		simulationSuccessful=successful;
		simulator.finalizeRun();
		if (simulationDone!=null) SwingUtilities.invokeLater(simulationDone);
	}

	private class UpdateInfoTask extends TimerTask {
		@Override
		public void run() {
			if (abortRun) simulator.cancel();
			if (abortRun || !simulator.isRunning()) {finalizeSimulation(!abortRun); return;}

			countTimerIntervals++;
			if (countTimerIntervals%10!=0) return;

			final long day=simulator.getSimDayCount();
			final long days=simulator.getSimDaysCount();
			final long time=System.currentTimeMillis();
			final long events=simulator.getEventCount();
			if (time-startTime>3000) {
				double gesamt=(time-startTime)/(((double)day)/days);
				gesamt-=(time-startTime);
				if (gesamt/1000<lastGesamt) lastGesamt=(int)FastMath.round(gesamt/1000);
				info2.setText(String.format(Language.tr("Wait.Info.Day"),NumberTools.formatLong((time-startTime)/1000),NumberTools.formatLong(Math.max(0,lastGesamt))));
			}
			if (events<1_000_000) {
				statusbar.setText(String.format(Language.tr("Wait.Status.DayK"),NumberTools.formatLong(day),NumberTools.formatLong(days),NumberTools.formatLong(events/1000),NumberTools.formatLong(simulator.getEventsPerSecond()/1000)));
			} else {
				statusbar.setText(String.format(Language.tr("Wait.Status.Day"),NumberTools.formatLong(day),NumberTools.formatLong(days),NumberTools.formatLong(events/1000000),NumberTools.formatLong(simulator.getEventsPerSecond()/1000)));
			}
			progress.setValue((int)Math.round(1000.0*day/days));
		}
	}
}