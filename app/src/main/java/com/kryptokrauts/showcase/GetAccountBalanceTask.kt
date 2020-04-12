package com.kryptokrauts.showcase

import android.content.Context
import android.os.AsyncTask
import com.kryptokrauts.aeternity.sdk.service.aeternity.AeternityServiceFactory
import java.math.BigInteger
import java.util.*

class GetAccountBalanceTask: AsyncTask<String, BigInteger, Void> {

    private val listener : AsyncBalanceListener
    // the default configuration points to the testnet
    // to change that you need to provide a custom configuration when initializing the aeternityService
    private val aeternityService = AeternityServiceFactory().service

    constructor(context: Context) {
        listener = context as AsyncBalanceListener
    }

    override fun doInBackground(vararg params: String?): Void {
        while (true) {
            println("update balances")
            var balanceList: MutableList<BigInteger> = mutableListOf()
            params.forEach { param -> run{
                var acc = aeternityService.accounts.blockingGetAccount(Optional.of(param!!))
                val balance = acc.balance ?: BigInteger.ZERO
                balanceList.add(balance)
            } }
            publishProgress(*balanceList.toTypedArray())
            println("wait 5 seconds ...")
            Thread.sleep(5000)
        }
    }

    override fun onProgressUpdate(vararg values: BigInteger?) {
        listener.processBalances(*values)
    }
}
