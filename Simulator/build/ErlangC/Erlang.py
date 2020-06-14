from math import prod,exp
from scipy.special import gammainc



#
# Allgemeine Hilfsfunktionen
#

# Berechnet x^n/n!
def PotenzFakultaet(x,n):
    if n==0:
        return 1
    return prod([x/i for i in range(1,n+1)])



#
# M/M/c-System
#

# Berechnet p0 für ein M/M/c-System
def MMcZustandsP0(a, c):
    Ergebnis=sum([PotenzFakultaet(a,K) for K in range(0,c)])
    Ergebnis=Ergebnis+PotenzFakultaet(a,c)*c/(c-a)

    if Ergebnis>0:
        return 1/Ergebnis
    return 0

# Berechnet pn für ein M/M/c-System
def MMcZustandsP(a, c, n):
    if n==0:
        return MMcZustandsP0(a,c)
    if n<=c:
        return PotenzFakultaet(a,n)*MMcZustandsP0(a,c)
    return PotenzFakultaet(a,c)*(a/c)^(n-c)*MMcZustandsP0(a,c)

# Berechnet P1 für ein M/M/c-System
def ErlangC_P1(a, c):
    return PotenzFakultaet(a,c)*c/(c-a)*MMcZustandsP0(a,c)

# Berechnet P(W<=t) für ein M/M/c-System (also die Erlang-C-Formel)
def ErlangC(l, mu, c, t):
    a=l/mu
    if a>=c:
        return 0
    return 1-ErlangC_P1(a,c)*exp(-(c-a)*mu*t)

# Berechnet E[NQ] für ein M/M/c/-System
def ErlangC_ENQ(l, mu, c):
    a=l/mu
    if a>=c:
        return 0
    return ErlangC_P1(a,c)*a/(c-a)

# Berechnet E[N] für ein M/M/c/-System
def ErlangC_EN(l, mu, c):
    a=l/mu
    if a>=c:
        return 0
    return ErlangC_P1(a,c)*a/(c-a)+a

# Berechnet E[W] für ein M/M/c/-System
def ErlangC_EW(l, mu, c):
    a=l/mu
    if a>=c:
        return 0
    return ErlangC_P1(a,c)/(c*mu-l)

# Berechnet E[V] für ein M/M/c/-System
def ErlangC_EV(l, mu, c):
    a=l/mu
    if a>=c:
        return 0
    return ErlangC_P1(a,c)/(c*mu-l)+1/mu



#
# M/M/c/c - System
#

# Berechnung von P1 für ein M/M/c/c-System (d.h. Berechnung der Erlang-B-Formel)
def ErlangB(a, c):
    Summe=sum([PotenzFakultaet(a,n) for n in range(0,c+1)])
    return PotenzFakultaet(a,c)/Summe



#
# M/M/c/K + M - System
#

# Berechnung von Cn für ein M/M/c/K+M-System
def MMcKMCn(l, mu, nu, c, n):
    if n<=c:
        return PotenzFakultaet(l/mu,n)

    Ergebnis=PotenzFakultaet(l/mu,c)
    for i in range(1,n-c+1):
        Ergebnis=Ergebnis*l/(c*mu+i*nu)

    return Ergebnis

# Berechnet pn für ein M/M/c/K+M-System
def MMcKMZustandsP(l, mu, nu, c, K, n):
    p0=sum([MMcKMCn(l,mu,nu,c,i) for i in range(0,K+1)])
    p0=1/p0

    if n==0:
        return p0
    if n>K:
        return 0

    return MMcKMCn(l,mu,nu,c,n)*p0

# Berechnet P(A) für ein M/M/c/K+M-System
def ErwErlangC_PA(l, mu, nu, c, K):
    p0=MMcKMZustandsP(l,mu,nu,c,K,0)
    return sum([nu/l*(n-c)*p0*MMcKMCn(l,mu,nu,c,n) for n in range(c+1,K+1)])

# Berechnung von P(W<=t) für ein M/M/c/K+M-System (also die erweiterte Erlang-C-Formel)
def ErwErlangC(l, mu, nu, c, K, t):
    p0=MMcKMZustandsP(l,mu,nu,c,K,0)

    if p0==0:
        p=1
    else:
        p=1-p0*MMcKMCn(l,mu,nu,c,K)

    for n in range(c,K):
        a=n-c+1
        x=(c*mu+nu)*t
        g=1-gammainc(a,x)
        p=p-p0*MMcKMCn(l,mu,nu,c,n)*g

    return p

# Berechnet E[NQ] für ein M/M/c/K+M-System
def ErwErlangC_ENQ(l, mu, nu, c, K):
    p0=MMcKMZustandsP(l,mu,nu,c,K,0)
    return sum([p0*(n-c)*MMcKMCn(l,mu,nu,c,n) for n in range(c+1,K+1)])

# Berechnet E[N] für ein M/M/c/K+M-System
def ErwErlangC_EN(l, mu, nu, c, K):
    p0=MMcKMZustandsP(l,mu,nu,c,K,0)
    return sum([p0*n*MMcKMCn(l,mu,nu,c,n) for n in range(1,K+1)])

# Berechnet E[W] für ein M/M/c/K+M-System
def ErwErlangC_EW(l, mu, nu, c, K):
    return ErwErlangC_ENQ(l,mu,nu,c,K)/l

# Berechnet E[V] für ein M/M/c/K+M-System
def ErwErlangC_EV(l, mu, nu, c, K):
    return ErwErlangC_EN(l,mu,nu,c,K)/l