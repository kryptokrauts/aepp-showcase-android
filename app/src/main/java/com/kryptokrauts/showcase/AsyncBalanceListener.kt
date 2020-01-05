package com.kryptokrauts.showcase

import java.math.BigInteger

interface AsyncBalanceListener {
    fun processBalances(vararg balances : BigInteger?)
}