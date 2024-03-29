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
package ui.commandline;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

import simulator.editmodel.EditModel;
import systemtools.commandline.AbstractCommand;
import systemtools.commandline.BaseCommandLineSystem;
import ui.MainFrame;
import ui.MainPanel;

/**
 * System zur Ausf�hrung von Kommandozeilen-Befehlen
 * @author Alexander Herzog
 */
public class CommandLineSystem extends BaseCommandLineSystem {

	/**
	 * Konstruktor der Klasse <code>CommandLineSystem</code>
	 * @param in	Ein {@link InputStream}-Objekt oder <code>null</code>, �ber das Zeichen von der Konsole gelesen werden k�nnen (<code>null</code>, wenn keine Konsole verf�gbar ist)
	 * @param out	Ein {@link PrintStream}-Objekt, �ber das Texte ausgegeben werden k�nnen.
	 */
	public CommandLineSystem(InputStream in, PrintStream out) {
		super(MainFrame.PROGRAM_NAME,EditModel.systemVersion,MainPanel.AUTHOR,in,out);
	}

	/**
	 * Konstruktor der Klasse <code>CommandLineSystem</code><br>
	 * Die Ausgabe der Befehle erfolgt auf <code>System.out</code>
	 */
	public CommandLineSystem() {
		this(System.in,System.out);
	}

	@Override
	protected List<AbstractCommand> getCommands() {
		List<AbstractCommand> list=super.getCommands();

		list.add(new CommandSimulation(this));
		list.add(new CommandBenchmark(this));
		list.add(new CommandReport(this));
		list.add(new CommandReset(this));

		return list;
	}
}