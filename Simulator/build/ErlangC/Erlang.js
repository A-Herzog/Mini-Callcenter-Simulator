/*
 * Library for calculating Erlang C formula values
 */

'use strict';

var jStat=require('./jstat.min.js'); /* https://github.com/jstat/jstat */



/*
 * General helper function
 */

/* Calculates x^n/n! */
function PotenzFakultaet(x,n) {
  if (n==0) return 1;
  let prod=1;
  for (let i=1;i<=n;i++) prod*=x/i;
  return prod;
}



/*
 * M/M/c system
 */

/* Calculates p0 for a M/M/c system */
function MMcZustandsP0(a,c) {
  let sum=0;
  for (let K=0;K<c;K++) sum+=PotenzFakultaet(a,K);
  sum+=PotenzFakultaet(a,c)*c/(c-a);

  if (sum>0) return 1/sum;
  return 0
}

/* Calculates pn for a M/M/c system */
function MMcZustandsP(a,c,n) {
  if (n==0) return MMcZustandsP0(a,c);
  if (n<=c) return PotenzFakultaet(a,n)*MMcZustandsP0(a,c);
  return PotenzFakultaet(a,c)*Math.pow(a/c,n-c)*MMcZustandsP0(a,c);
}

/* Calculates P1 for a M/M/c system */
function ErlangC_P1(a,c) {
    return PotenzFakultaet(a,c)*c/(c-a)*MMcZustandsP0(a,c);
}

/* Calculates P(W<=t) for a M/M/c system (this means this is the Erlang C formula) */
function ErlangC(lambda,mu,c,t) {
  const a=lambda/mu;
  if (a>=c) return 0;
  return 1-ErlangC_P1(a,c)*Math.exp(-(c-a)*mu*t);
}

/* Calculates E[NQ] for a M/M/c system */
function ErlangC_ENQ(lambda,mu,c) {
  const a=lambda/mu;
  if (a>=c) return 0;
  return ErlangC_P1(a,c)*a/(c-a);
}

/* Calculates E[N] for a M/M/c system */
function ErlangC_EN(lambda,mu,c) {
  const a=lambda/mu;
  if (a>=c) return 0;
  return ErlangC_P1(a,c)*a/(c-a)+a;
}

/* Calculates E[W] for a M/M/c system */
function ErlangC_EW(lambda,mu,c) {
  const a=lambda/mu;
  if (a>=c) return 0;
  return ErlangC_P1(a,c)/(c*mu-lambda);
}

/* Calculates E[V] for a M/M/c system */
function ErlangC_EV(lambda,mu,c) {
  const a=lambda/mu;
  if (a>=c) return 0;
  return ErlangC_P1(a,c)/(c*mu-lambda)+1/mu;
}



/*
 * M/M/c/c system
 */

/* Calculation of P1 for a M/M/c/c system (this means this is the Erlang B formula) */
function ErlangB(a,c) {
  let sum=0;
  for (let n=0;n<=c;n++) sum+=PotenzFakultaet(a,n);
  return PotenzFakultaet(a,c)/sum;
}



/*
 * M/M/c/K + M system
 */

/* Calculation of Cn for a M/M/c/K+M system */
function MMcKMCn(lambda,mu,nu,c,n) {
  const a=lambda/mu;
  if (n<=c) return PotenzFakultaet(a,n);
  let prod=PotenzFakultaet(a,c);
  for (let i=1;i<=n-c;i++) prod*=lambda/(c*mu+i*nu);
  return prod;
}

/* Calculates pn for a M/M/c/K+M system */
function MMcKMZustandsP(lambda,mu,nu,c,K,n) {
  let p0=0;
  for (let i=0;i<=K;i++) p0+=MMcKMCn(lambda,mu,nu,c,i);
  p0=1/p0;

  if (n==0) return p0;
  if (n>K) return 0;
  return MMcKMCn(lambda,mu,nu,c,n)*p0;
}

/* Calculates P(A) for a M/M/c/K+M system */
function ErwErlangC_PA(lambda,mu,nu,c,K) {
  const p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);
  let sum=0;
  for (let n=c+1;n<=K;n++) sum+=nu/lambda*(n-c)*p0*MMcKMCn(lambda,mu,nu,c,n);
  return sum;
}

/* Calculation of P(W<=t) for a M/M/c/K+M system (this means this is the extended Erlang C formula) */
function ErwErlangC(lambda,mu,nu,c,K,t) {
  const p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);

  let p;
  if (p0==0) p=1; else p=1-p0*MMcKMCn(lambda,mu,nu,c,K);

  for (let n=c;n<K;n++) {
    const a=n-c+1;
    const x=(c*mu+nu)*t;
    const g=1-jStat.lowRegGamma(a,x);

    p-=p0*MMcKMCn(lambda,mu,nu,c,n)*g;
  }

  return p;
}

/* Calculates E[NQ] for a M/M/c/K+M system */
function ErwErlangC_ENQ(lambda,mu,nu,c,K) {
    const p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);
    let sum=0;
    for (let n=c+1;n<=K;n++) sum+=p0*(n-c)*MMcKMCn(lambda,mu,nu,c,n);
    return sum;
}

/* Calculates E[N] for a M/M/c/K+M system */
function ErwErlangC_EN(lambda,mu,nu,c,K) {
  const p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);
  let sum=0;
  for (let n=1;n<=K;n++) sum+=p0*n*MMcKMCn(lambda,mu,nu,c,n);
  return sum;
}

/* Calculates E[W] for a M/M/c/K+M system */
function ErwErlangC_EW(lambda,mu,nu,c,K) {
  return ErwErlangC_ENQ(lambda,mu,nu,c,K)/lambda;
}

/* Calculates E[V] for a M/M/c/K+M system */
function  ErwErlangC_EV(lambda,mu,nu,c,K) {
  return ErwErlangC_EN(lambda,mu,nu,c,K)/lambda;
}