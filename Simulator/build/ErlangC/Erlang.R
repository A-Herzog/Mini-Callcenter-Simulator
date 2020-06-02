#
# Allgemeine Hilfsfunktionen
#

# Berechnet x^n/n!
PotenzFakultaet <- function(x, n) {
  if (n==0) return(1);
  return(prod(x/(1:n)));
}



#
# M/M/c-System
#

# Berechnet p0 für ein M/M/c-System
MMcZustandsP0 <- function(a, c) {
  Ergebnis=0;
  for (K in 0:(c-1)) {
    Ergebnis=Ergebnis+PotenzFakultaet(a,K);
  }
  Ergebnis=Ergebnis+PotenzFakultaet(a,c)*c/(c-a);
  if (Ergebnis>0) return(1/Ergebnis) else return(0);
}

# Berechnet pn für ein M/M/c-System
MMcZustandsP <- function(a, c, n) {
  if (n==0) return(MMcZustandsP0(a,c));
  if (n<=c) return(PotenzFakultaet(a,n)*MMcZustandsP0(a,c));
  return(PotenzFakultaet(a,c)*(a/c)^(n-c)*MMcZustandsP0(a,c));
}

# Berechnet P1 für ein M/M/c-System
ErlangC_P1 <- function(a, c) {
  return(PotenzFakultaet(a,c)*c/(c-a)*MMcZustandsP0(a,c));
}

# Berechnet P(W<=t) für ein M/M/c-System (also die Erlang-C-Formel)
ErlangC <- function(lambda, mu, c, t) {
  a=lambda/mu;
  if (a>=c) return(0);
  return(1-ErlangC_P1(a,c)*exp(-(c-a)*mu*t));
}

# Berechnet E[NQ] für ein M/M/c/-System
ErlangC_ENQ <- function(lambda, mu, c) {
  a=lambda/mu;
  if (a>=c) return(0);
  return(ErlangC_P1(a,c)*a/(c-a));
}

# Berechnet E[N] für ein M/M/c/-System
ErlangC_EN <- function(lambda, mu, c) {
  a=lambda/mu;
  if (a>=c) return(0);
  return(ErlangC_P1(a,c)*a/(c-a)+a);
}

# Berechnet E[W] für ein M/M/c/-System
ErlangC_EW <- function(lambda, mu, c) {
  a=lambda/mu;
  if (a>=c) return(0);
  return(ErlangC_P1(a,c)/(c*mu-lambda));
}

# Berechnet E[V] für ein M/M/c/-System
ErlangC_EV <- function(lambda, mu, c) {
  a=lambda/mu;
  if (a>=c) return(0);
  return(ErlangC_P1(a,c)/(c*mu-lambda)+1/mu);
}



#
# M/M/c/c - System
#

# Berechnung von P1 für ein M/M/c/c-System (d.h. Berechnung der Erlang-B-Formel)
ErlangB <- function(a, c) {
  Summe = 0
  for (n in 0:c) {
    Summe=Summe+PotenzFakultaet(a,n);
  }
  return(PotenzFakultaet(a,c)/Summe);
}



#
# M/M/c/K + M - System
#

# Berechnung von Cn für ein M/M/c/K+M-System
MMcKMCn <- function(lambda, mu, nu, c, n) {
  if (n<=c) return(PotenzFakultaet(lambda/mu,n));
  
  Ergebnis=PotenzFakultaet(lambda/mu,c);
  for (i in 1:(n-c)) {
    Ergebnis=Ergebnis*lambda/(c*mu+i*nu);
  }
  return(Ergebnis);
}

# Berechnet pn für ein M/M/c/K+M-System
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

# Berechnet P(A) für ein M/M/c/K+M-System
ErwErlangC_PA <- function(lambda, mu, nu, c, K) {
  p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);
  Summe=0;
  for (n in (c+1):K) {
    Summe=Summe+nu/lambda*(n-c)*p0*MMcKMCn(lambda,mu,nu,c,n);
  }
  return(Summe);
}

# Berechnung von P(W<=t) für ein M/M/c/K+M-System (also die erweiterte Erlang-C-Formel)
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

# Berechnet E[NQ] für ein M/M/c/K+M-System
ErwErlangC_ENQ <- function(lambda, mu, nu, c, K) {
  p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);
  Summe=0;
  for (n in (c+1):K) {
    Summe=Summe+p0*(n-c)*MMcKMCn(lambda,mu,nu,c,n);
  }
  return(Summe);
}

# Berechnet E[N] für ein M/M/c/K+M-System
ErwErlangC_EN <- function(lambda, mu, nu, c, K) {
  p0=MMcKMZustandsP(lambda,mu,nu,c,K,0);
  Summe=0;
  for (n in 1:K) {
    Summe=Summe+p0*n*MMcKMCn(lambda,mu,nu,c,n);
  }
  return(Summe);
}

# Berechnet E[W] für ein M/M/c/K+M-System
ErwErlangC_EW <- function(lambda, mu, nu, c, K) {
  return(ErwErlangC_ENQ(lambda,mu,nu,c,K)/lambda);
}

# Berechnet E[V] für ein M/M/c/K+M-System
ErwErlangC_EV <- function(lambda, mu, nu, c, K) {
  return(ErwErlangC_EN(lambda,mu,nu,c,K)/lambda);
}



# Beispiel:
# M/M/c, E[I]=100, E[S]=1..99, c=1
# ES=1:99
# plot(ES,sapply(ES,function(x) ErlangC_ENQ(1/100,1/x,1)))