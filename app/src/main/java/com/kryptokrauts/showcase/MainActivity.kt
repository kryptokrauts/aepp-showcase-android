package com.kryptokrauts.showcase

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kryptokrauts.aeternity.sdk.domain.secret.impl.BaseKeyPair
import com.kryptokrauts.aeternity.sdk.domain.secret.impl.MnemonicKeyPair
import com.kryptokrauts.aeternity.sdk.service.aeternity.AeternityServiceConfiguration
import com.kryptokrauts.aeternity.sdk.service.aeternity.AeternityServiceFactory
import com.kryptokrauts.aeternity.sdk.service.aeternity.impl.AeternityService
import com.kryptokrauts.aeternity.sdk.service.keypair.KeyPairService
import com.kryptokrauts.aeternity.sdk.service.keypair.KeyPairServiceFactory
import com.kryptokrauts.aeternity.sdk.service.transaction.type.model.SpendTransactionModel
import com.kryptokrauts.aeternity.sdk.util.EncodingUtils
import com.kryptokrauts.aeternity.sdk.util.UnitConversionUtil
import org.bitcoinj.crypto.ChildNumber
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class MainActivity : AppCompatActivity(), AsyncBalanceListener {

    lateinit var aeternityService: AeternityService
    lateinit var keyPairService: KeyPairService

    lateinit var mnemonicKeyPair: MnemonicKeyPair
    lateinit var keyPairAlice: BaseKeyPair
    lateinit var keyPairBob: BaseKeyPair

    lateinit var balanceAlice: TextView
    lateinit var balanceBob: TextView

    init {
        // this needs to be set to avoid vert.x creating a cache folder (which it does by default)
        // without setting the property an exception is thrown when using an sdk-service that initializes a vert.x client
        System.setProperty("vertx.disableFileCPResolving", "true")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // normally we wouldn't have to explicitely set the base Urls but in v2.1.0 of the sdk they are wrong
        // see https://github.com/kryptokrauts/aepp-sdk-java/issues/95
        // will be fixed in the upcoming release
        aeternityService = AeternityServiceFactory().getService(
            AeternityServiceConfiguration
                .configure()
                .baseUrl("https://sdk-testnet.aepps.com")
                .aeternalBaseUrl("https://testnet.aeternal.io")
                .compile()
        )

        keyPairService = KeyPairServiceFactory().service
        mnemonicKeyPair = keyPairService.recoverMasterMnemonicKeyPair(listOf("acquire", "useful", "napkin", "ranch", "witness", "scare", "lunch", "smart", "sibling", "situate", "potato", "inspire"), null)
        keyPairAlice = EncodingUtils.createBaseKeyPair(keyPairService.generateDerivedKey(mnemonicKeyPair, true, ChildNumber.ZERO_HARDENED, ChildNumber(0, true)))
        keyPairBob = EncodingUtils.createBaseKeyPair(keyPairService.generateDerivedKey(mnemonicKeyPair, true, ChildNumber.ZERO_HARDENED, ChildNumber(1, true)))

        println("PK Alice: ${keyPairAlice.publicKey}")
        println("PK Bob: ${keyPairBob.publicKey}")

        setContentView(R.layout.activity_main)

        balanceAlice = findViewById(R.id.valueBalanceAlice)
        balanceBob = findViewById(R.id.valueBalanceBob)

        GetAccountBalanceTask(this).execute(keyPairAlice.publicKey, keyPairBob.publicKey)
    }

    fun aliceToBob(view: View) {
        sendAE(keyPairAlice, keyPairBob, UnitConversionUtil.toAettos("0.1", UnitConversionUtil.Unit.AE).toBigInteger())
    }

    fun bobToAlice(view: View) {
        sendAE(keyPairBob, keyPairAlice, UnitConversionUtil.toAettos("0.1", UnitConversionUtil.Unit.AE).toBigInteger())
    }

    private fun sendAE(from: BaseKeyPair, to: BaseKeyPair, amount: BigInteger) {
        var account = aeternityService.accounts.blockingGetAccount(Optional.of(from.publicKey))
        println("Balance: ${UnitConversionUtil.fromAettos(BigDecimal(account.balance), UnitConversionUtil.Unit.AE)}")
        if (amount < account.balance) {
            var spendTx = SpendTransactionModel.builder().amount(amount).nonce(account.nonce.add(
                BigInteger.ONE)).recipient(
                to.publicKey).sender(from.publicKey).ttl(BigInteger.ZERO).build()
            var result = aeternityService.transactions.blockingPostTransaction(spendTx, from.privateKey)
            println("tx-hash: ${result.txHash}")
        } else {
            println("not enough funds")
        }
    }

    override fun processBalances(vararg balances: BigInteger?) {
        balanceAlice.setText("${UnitConversionUtil.fromAettos(BigDecimal(balances[0]), UnitConversionUtil.Unit.AE)} AE")
        balanceBob.setText("${UnitConversionUtil.fromAettos(BigDecimal(balances[1]), UnitConversionUtil.Unit.AE)} AE")
    }
}
