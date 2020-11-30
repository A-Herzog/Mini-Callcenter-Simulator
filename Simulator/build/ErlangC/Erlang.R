###################################################
# Library for calculating Erlang C formula values #
###################################################

#
# General helper function
#

# Calculates x^n/n!
PotenzFakultaet <- function(x, n) {
  if (n==0) return(1);
  return(prod(x/(1:n)));
}



#
# M/M/c system
#

# Calculates p0 for a M/M/c system
MMcZustandsP0 <- function(a, c) {
  Ergebnis=0;
  for (K in 0:(c-1)) {
    Ergebnis=Ergebnis+PotenzFakultaet(a,K);
  }
  Ergebnis=Ergebnis+PotenzFakultaet(a,c)*c/(c-a);
  if (Ergebnis>0) return(1/Ergebnis) else return(0);
}

# Calculates pn for a M/M/c system
MMcZustandsP <- function(a, c, n) {
  if (n==0) return(MMcZustandsP0(a,c));
  if (n<=c) return(PotenzFakultaet(a,n)*MMcZustandsP0(a,c));
  return(PotenzFakultaet(a,c)*(a/c)^(n-c)*MMcZustandsP0(a,c));
}

# Calculates P1 for a M/M/c system
ErlangC_P1 <- function(a, c) {
  return(PotenzFakultaet(a,c)*c/(c-a)*MMcZustandsP0(a,c));
}

# Calculates P(W<=t) for a M/M/c system (this means this is the Erlang C formula)
ErlangC <- function(lambda, mu, c, t) {
  a=lambda/mu;
  if (a>=c) return(0);
  return(1-ErlangC_P1(a,c)*exp(-(c-a)*mu*t));
}

# Calculates E[NQ] for a M/M/c system
ErlangC_ENQ <- function(lambda, mu, c) {
  a=lambda/mu;
  if (a>=c) return(0);
  return(ErlangC_P1(a,c)*a/(c-a));
}

# Calculates E[N] for a M/M/c system
ErlangC_EN <- function(lambda, mu, c) {
  a=lambda/mu;
  if (a>=c) return(0);
  return(ErlangC_P1(a,c)*a/(c-a)+a);
}

# Calculates E[W] for a M/M/c system
ErlangC_EW <- function(lambda, mu, c) {
  a=lambda/mu;
  if (a>=c) return(0);
  return(ErlangC_P1(a,c)/(c*mu-lambda));
}

# Calculates E[V] for a M/M/c system
ErlangC_EV <- function(lambda, mu, c) {
  a=lambda/mu;
  if (a>=c) return(0);
  return(ErlangC_P1(a,c)/(c*mu-lambda)+1/mu);
}



#
# M/M/c/c system
#

# Calculation of P1 for a M/M/c/c system (this means this is the Erlang B formula)
ErlangB <- function(a, c) {
  Summe = 0
  for (n in 0:c) {
    Summe=Summe+PotenzFakultaet(a,n);
  }
  return(PotenzFakultaet(a,c)/Summe);
}



#
# M/M/c/K + M system
#

# Calculation of Cn for a M/M/c/K+M system
MMcKMCn <- function(lambda, mu, nu, c, n) {
  if (n<=c) return(PotenzFakultaet(lambda/mu,n));
  
  Ergebnis=PotenzFakultaet(lambda/mu,c);
  for (i in 1:(n-c)) {
    Ergebnis=Ergebnis*lambda/(c*mu+i*nu);
  }
  return(Ergebnis);
}

# Calculates pn for a M/M/c/K+M system
MMcKMZustandsP <- function (lambda, mu, nu, c, K, n) {
  p0=0
  for (i in 0:K) {
    p0=p0+MMcKMCn(lambda,mu,nu,c,i);
  }
  p0=1/p0;

  if (n==0) return(p0);
  if (n>K) return(0);
  return(MMcKMCn(lambda,mu,nu,c,n)*p0);
}

# Calculates P(A) for a M/M/c/K+M system
ErwErlangC_PA <- function(lambda, mu, nu, c, K) {
  p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);
  Summe=0;
  for (n in (c+1):K) {
    Summe=Summe+nu/lambda*(n-c)*p0*MMcKMCn(lambda,mu,nu,c,n);
  }
  return(Summe);
}

# Calculation of P(W<=t) for a M/M/c/K+M system (this means this is the extended Erlang C formula)
ErwErlangC <- function (lambda, mu, nu, c, K, t) {
  p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);

  if (p0==0) p=1 else p=1-p0*MMcKMCn(lambda,mu,nu,c,K);

  for (n in c:(K-1)) {
    a=n-c+1;
    x=(c*mu+nu)*t;
    g=1-pgamma(x,a,1);
    p=p-p0*MMcKMCn(lambda,mu,nu,c,n)*g;
  }
    
  return(p);
}

# Calculates E[NQ] for a M/M/c/K+M system
ErwErlangC_ENQ <- function(lambda, mu, nu, c, K) {
  p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);
  Summe=0;
  for (n in (c+1):K) {
    Summe=Summe+p0*(n-c)*MMcKMCn(lambda,mu,nu,c,n);
  }
  return(Summe);
}

# Calculates E[N] for a M/M/c/K+M system
ErwErlangC_EN <- function(lambda, mu, nu, c, K) {
  p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);
  Summe=0;
  for (n in 1:K) {
    Summe=Summe+p0*n*MMcKMCn(lambda,mu,nu,c,n);
  }
  return(Summe);
}

# Calculates E[W] for a M/M/c/K+M system
ErwErlangC_EW <- function(lambda, mu, nu, c, K) {
  return(ErwErlangC_ENQ(lambda,mu,nu,c,K)/lambda);
}

# Calculates E[V] for a M/M/c/K+M system
ErwErlangC_EV <- function(lambda, mu, nu, c, K) {
  return(ErwErlangC_EN(lambda,mu,nu,c,K)/lambda);
}



# Beispiel:
# M/M/c, E[I]=100, E[S]=1..99, c=1
# ES=1:99
# plot(ES,sapply(ES,function(x) ErlangC_ENQ(1/100,1/x,1)))