package com.kryptokrauts.showcase

import android.content.Context
import android.os.AsyncTask
import com.kryptokrauts.aeternity.sdk.service.aeternity.AeternityServiceConfiguration
import com.kryptokrauts.aeternity.sdk.service.aeternity.AeternityServiceFactory
import java.math.BigInteger
import java.util.*

class GetAccountBalanceTask: AsyncTask<String, BigInteger, Void> {

    private val listener : AsyncBalanceListener

    // normally we wouldn't have to explicitely set the base Urls but in v2.1.0 of the sdk they are wrong
    // see https://github.com/kryptokrauts/aepp-sdk-java/issues/95
    // will be fixed in the upcoming release
    private val aeternityService = AeternityServiceFactory().getService(
        AeternityServiceConfiguration
            .configure()
            .baseUrl("https://sdk-testnet.aepps.com")
            .aeternalBaseUrl("https://testnet.aeternal.io")
            .compile()
    )

    constructor(context: Context) {
        listener = context as AsyncBalanceListener
    }

    override fun doInBackground(vararg params: String?): Void {
        while (true) {
            println("update balances")
            var balanceList: MutableList<BigInteger> = mutableListOf()
            params.forEach { param -> run{
                var acc = aeternityService.accounts.blockingGetAccount(Optional.of(param!!))
                balanceList.add(acc.balance)
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
