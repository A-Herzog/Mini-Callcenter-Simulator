###################################################
# Library for calculating Erlang C formula values #
###################################################

from math import prod, exp
from scipy.special import gammainc



#
# General helper function
#

# Calculates x^n/n!
def PowerFactorial(x, n):
    if n==0:
        return 1
    return prod([x/i for i in range(1,n+1)])



#
# M/M/c system
#

# Calculates p0 for a M/M/c system
def MMcStateP0(a, c):
    Result=sum([PowerFactorial(a,K) for K in range(0,c)])
    Result+=PowerFactorial(a,c)*c/(c-a)

    if Result>0:
        return 1/Result
    return 0

# Calculates pn for a M/M/c system
def MMcStateP(a, c, n):
    if n==0:
        return MMcStateP0(a,c)
    if n<=c:
        return PowerFactorial(a,n)*MMcStateP0(a,c)
    return PowerFactorial(a,c)*(a/c)^(n-c)*MMcStateP0(a,c)

# Calculates P1 for a M/M/c system
def ErlangC_P1(a, c):
    return PowerFactorial(a,c)*c/(c-a)*MMcStateP0(a,c)

# Calculates P(W<=t) for a M/M/c system (this means this is the Erlang C formula)
def ErlangC(l, mu, c, t):
    a=l/mu
    if a>=c:
        return 0
    return 1-ErlangC_P1(a,c)*exp(-(c-a)*mu*t)

# Calculates E[NQ] for a M/M/c system
def ErlangC_ENQ(l, mu, c):
    a=l/mu
    if a>=c:
        return 0
    return ErlangC_P1(a,c)*a/(c-a)

# Calculates E[N] for a M/M/c system
def ErlangC_EN(l, mu, c):
    a=l/mu
    if a>=c:
        return 0
    return ErlangC_P1(a,c)*a/(c-a)+a

# Calculates E[W] for a M/M/c system
def ErlangC_EW(l, mu, c):
    a=l/mu
    if a>=c:
        return 0
    return ErlangC_P1(a,c)/(c*mu-l)

# Calculates E[V] for a M/M/c system
def ErlangC_EV(l, mu, c):
    a=l/mu
    if a>=c:
        return 0
    return ErlangC_P1(a,c)/(c*mu-l)+1/mu



#
# M/M/c/c system
#

# Calculation of P1 for a M/M/c/c system (this means this is the Erlang B formula)
def ErlangB(a, c):
    partialSum=sum([PowerFactorial(a,n) for n in range(0,c+1)])
    return PowerFactorial(a,c)/partialSum



#
# M/M/c/K + M system
#

# Calculation of Cn for a M/M/c/K+M system
def MMcKMCn(l, mu, nu, c, n):
    if n<=c:
        return PowerFactorial(l/mu,n)

    Result=PowerFactorial(l/mu,c)
    for i in range(1,n-c+1):
        Result=Result*l/(c*mu+i*nu)

    return Result

# Calculates pn for a M/M/c/K+M system
def MMcKMStateP(l, mu, nu, c, K, n):
    p0=sum([MMcKMCn(l,mu,nu,c,i) for i in range(0,K+1)])
    p0=1/p0

    if n==0:
        return p0
    if n>K:
        return 0

    return MMcKMCn(l,mu,nu,c,n)*p0

# Calculates P(A) for a M/M/c/K+M system
def ErwErlangC_PA(l, mu, nu, c, K):
    p0=MMcKMStateP(l,mu,nu,c,K,0)
    return sum([nu/l*(n-c)*p0*MMcKMCn(l,mu,nu,c,n) for n in range(c+1,K+1)])

# Calculation of P(W<=t) for a M/M/c/K+M system (this means this is the extended Erlang C formula)
def ErwErlangC(l, mu, nu, c, K, t):
    p0=MMcKMStateP(l,mu,nu,c,K,0)

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

# Calculates E[NQ] for a M/M/c/K+M system
def ErwErlangC_ENQ(l, mu, nu, c, K):
    p0=MMcKMStateP(l,mu,nu,c,K,0)
    return sum([p0*(n-c)*MMcKMCn(l,mu,nu,c,n) for n in range(c+1,K+1)])

# Calculates E[N] for a M/M/c/K+M system
def ErwErlangC_EN(l, mu, nu, c, K):
    p0=MMcKMStateP(l,mu,nu,c,K,0)
    return sum([p0*n*MMcKMCn(l,mu,nu,c,n) for n in range(1,K+1)])

# Calculates E[W] for a M/M/c/K+M system
def ErwErlangC_EW(l, mu, nu, c, K):
    return ErwErlangC_ENQ(l,mu,nu,c,K)/l

# Calculates E[V] for a M/M/c/K+M system
def ErwErlangC_EV(l, mu, nu, c, K):
    return ErwErlangC_EN(l,mu,nu,c,K)/l