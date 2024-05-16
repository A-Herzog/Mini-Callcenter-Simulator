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

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import language.Language;
import mathtools.NumberTools;
import simulator.editmodel.EditModel;
import simulator.statistics.Statistics;
import systemtools.commandline.AbstractCommand;
import systemtools.commandline.BaseCommandLineSystem;

/**
 * Führt einenen Benchmark-Test der Simulatorleistung durch.
 * @author Alexander Herzog
 * @see AbstractCommand
 * @see CommandLineSystem
 */
public final class CommandBenchmark extends AbstractSimulationCommand {
	/** Maximalanzahl an Threads (wird durch die Hardware weiter limitiert) */
	private int maxThreads=Integer.MAX_VALUE;
	/** Benchmark-Modus */
	private int speedTestMode=0;
	/** Zu simulierende Modelldatei */
	private File modelFile;

	/**
	 * Konstruktor der Klasse
	 * @param system	Referenz auf das Kommandozeilensystem
	 */
	public CommandBenchmark(final BaseCommandLineSystem system) {
		super(system);
	}

	@Override
	public String[] getKeys() {

		List<String> list=new ArrayList<>();

		list.add(Language.tr("CommandLine.Benchmark.Name1"));
		for (String s: Language.trOther("CommandLine.Benchmark.Name1")) if (!list.contains(s)) list.add(s);

		if (!list.contains(Language.tr("CommandLine.Benchmark.Name2"))) list.add(Language.tr("CommandLine.Benchmark.Name2"));
		for (String s: Language.trOther("CommandLine.Benchmark.Name2")) if (!list.contains(s)) list.add(s);

		return list.toArray(String[]::new);
	}

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public String getShortDescription() {
		return Language.tr("CommandLine.Benchmark.Description.Short");
	}

	@Override
	public String[] getLongDescription() {
		return Language.tr("CommandLine.Benchmark.Description.Long").split("\n");
	}

	@Override
	public String prepare(String[] additionalArguments, InputStream in, PrintStream out) {
		String s=parameterCountCheck(0,1,additionalArguments); if (s!=null) return s;

		if (additionalArguments.length==0) return null;
		String arg=additionalArguments[0];

		Integer I=NumberTools.getNotNegativeInteger(additionalArguments[0]);
		if (I!=null && I!=0) {maxThreads=I; return null;}

		speedTestMode=1;
		modelFile=new File(arg);
		if (!modelFile.isFile()) return String.format(Language.tr("CommandLine.Error.File.InputDoesNotExist"),modelFile.toString());
		if (!isModelFile(modelFile)) return String.format(Language.tr("CommandLine.Error.File.InputNoValidModelFile"),modelFile.toString());
		return null;
	}

	@Override
	public void run(AbstractCommand[] allCommands, InputStream in, PrintStream out) {
		EditModel editModel=null;

		switch (speedTestMode) {
		case 0:
			editModel=new EditModel();
			break;
		case 1:
			editModel=new EditModel();
			String s=editModel.loadFromFile(modelFile);
			if (s!=null) {out.println(BaseCommandLineSystem.errorBig+": "+Language.tr("CommandLine.Error.LoadingModel")+": "+s); return;}
			out.println(Language.tr("CommandLine.Benchmark.UsedModel")+": "+modelFile.getName());
			break;
		}

		if (editModel==null) return;

		for (int i=0;i<5;i++) {
			if (isCanceled()) break;
			if (i>0) out.println(Language.tr("CommandLine.Benchmark.SimulaionRun")+" "+(i+1));
			Statistics statistics=singleSimulation(editModel,true,maxThreads,out);
			if (statistics==null) {out.println(BaseCommandLineSystem.errorBig+": "+Language.tr("CommandLine.Benchmark.SimulaionFailed")); return;}
			if (i==0) out.println(Language.tr("CommandLine.Benchmark.Threads")+": "+NumberTools.formatLong(statistics.simulationData.runThreads));
			if (i==0) out.println(Language.tr("CommandLine.Benchmark.SimulaionRun")+" "+(i+1));
			out.println("  "+Language.tr("CommandLine.Benchmark.NeededCalculationTime")+": "+NumberTools.formatLong(statistics.simulationData.runTime)+" ms");
			out.println("  "+Language.tr("CommandLine.Benchmark.EventsPerSecond")+": "+NumberTools.formatLong(1000*statistics.simulationData.runEvents/statistics.simulationData.runTime));
		}
	}
}