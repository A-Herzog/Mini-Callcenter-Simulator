/**
 * Copyright 2021 Alexander Herzog
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
package parser.symbols.distributions;

import org.apache.commons.math3.distribution.AbstractRealDistribution;

import mathtools.distribution.SawtoothLeftDistribution;
import mathtools.distribution.tools.WrapperBetaDistribution;
import mathtools.distribution.tools.WrapperSawtoothLeftDistribution;

/**
 * Linke S�gezahn-Verteilung - die Parameter sind hier Erwartungswert und Standardabweichung
 * @author Alexander Herzog
 * @see SawtoothLeftDistribution
 */
public class CalcSymbolDistributionSawtoothLeftDirect extends CalcSymbolDistribution {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[]{"LeftSawtoothDistributionDirect","LeftSawtoothDistDirect","LinkeS�gezahnVerteilungDirekt"};

	@Override
	public String[] getNames() {
		return names;
	}

	@Override
	protected int getParameterCount() {
		return 2;
	}

	/**
	 * Factory-Objekt f�r die Verteilungen<br>
	 * Die Methode {@link WrapperBetaDistribution#getDistributionForFit(double, double, double, double)}
	 * hat keine Seiteneffekte, kann also problemlos von mehreren Threads parallel aufgerufen werden.
	 */
	private static final WrapperSawtoothLeftDistribution wrapper=new WrapperSawtoothLeftDistribution();

	@Override
	protected AbstractRealDistribution getDistribution(double[] parameters) {
		return wrapper.getDistribution(parameters[0],parameters[1]);
	}
}
