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
package parser.symbols;

import parser.MathCalcError;
import parser.coresymbols.CalcSymbolPreOperator;

/**
 * Maximum der Werte der Datenreihe, die aus den �bergebenen Parametern gebildet wird
 * @author Alexander Herzog
 * @see CalcSymbolPreOperatorMin
 */
public final class CalcSymbolPreOperatorMax extends CalcSymbolPreOperator {
	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolPreOperatorMax() {
		/*
		 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
		 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
		 */
	}

	@Override
	protected double calc(double[] parameters) throws MathCalcError {
		if (parameters.length==0) throw error();
		double max=parameters[0];
		for (double d:parameters) if (d>max) max=d;
		return max;
	}

	@Override
	protected double calcOrDefault(final double[] parameters, final double fallbackValue) {
		if (parameters.length==0) return fallbackValue;
		double max=parameters[0];
		for (double d:parameters) if (d>max) max=d;
		return max;
	}

	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[]{"Maximum","Max","Mx"};

	@Override
	public String[] getNames() {
		return names;
	}
}
