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

import org.apache.commons.math3.util.FastMath;

/**
 * Poisson-Verteilung
 * @author Alexander Herzog
 * @version 1.0
 */
public class CalcSymbolDiscreteDistributionPoisson extends CalcSymbolDiscreteDistribution {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[]{"PoissonDist","PoissonDistribution","PoissonVerteilung"};

	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolDiscreteDistributionPoisson() {
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
		return 1;
	}

	/**
	 * Berechnet lambda^k/k! (und vermeidet dabei Ausl�schungen bei gro�en Werten k)
	 * @param lambda	Wert lambda
	 * @param k	Wert k
	 * @return	lambda^k/k!
	 */
	private double powerFactorial(final double lambda, final int k) {
		/* FastMath.pow(lambda,k)/Functions.getFactorial(k) */
		double d=1;
		for (int i=1;i<=k;i++) d*=lambda/i;
		return d;
	}


	@Override
	protected double calcProbability(double[] parameters, int k) {
		final double lambda=parameters[0];

		if (lambda<=0) return -1;

		return powerFactorial(lambda,k)*FastMath.exp(-lambda);
	}
}
