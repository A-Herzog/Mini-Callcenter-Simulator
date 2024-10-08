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
package parser.symbols.distributions;

import org.apache.commons.math3.distribution.AbstractRealDistribution;

import mathtools.distribution.FatigueLifeDistributionImpl;

/**
 * Fatigue-Life-Verteilung
 * @author Alexander Herzog
 * @see FatigueLifeDistributionImpl
 */
public final class CalcSymbolDistributionFatigueLife extends CalcSymbolDistribution {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[]{"FatigueLifeDist","FatigueLifeDistribution","FatigueLifeVerteilung"};

	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolDistributionFatigueLife() {
		/*
		 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
		 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
		 */
	}

	@Override
	public String[] getNames() {
		return names;
	}

	@Override
	protected int getParameterCount() {
		return 3;
	}

	@Override
	protected AbstractRealDistribution getDistribution(double[] parameters) {
		return new FatigueLifeDistributionImpl(parameters[0],parameters[1],parameters[2]);
	}
}
