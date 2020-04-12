package com.kryptokrauts.showcase

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kryptokrauts.aeternity.sdk.domain.secret.impl.BaseKeyPair
import com.kryptokrauts.aeternity.sdk.domain.secret.impl.MnemonicKeyPair
import com.kryptokrauts.aeternity.sdk.service.aeternity.AeternityServiceFactory
import com.kryptokrauts.aeternity.sdk.service.aeternity.impl.AeternityService
import com.kryptokrauts.aeternity.sdk.service.keypair.KeyPairService
import com.kryptokrauts.aeternity.sdk.service.keypair.KeyPairServiceFactory
import com.kryptokrauts.aeternity.sdk.service.transaction.type.model.SpendTransactionModel
import com.kryptokrauts.aeternity.sdk.service.unit.UnitConversionService
import com.kryptokrauts.aeternity.sdk.service.unit.impl.DefaultUnitConversionServiceImpl
import com.kryptokrauts.aeternity.sdk.util.EncodingUtils
import com.kryptokrauts.aeternity.sdk.util.UnitConversionUtil
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class MainActivity : AppCompatActivity(), AsyncBalanceListener {

    lateinit var aeternityService: AeternityService
    lateinit var keyPairService: KeyPairService
    lateinit var unitConversionService: UnitConversionService

    lateinit var mnemonicMaster: MnemonicKeyPair
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
        // the default configuration points to the testnet
        // to change that you need to provide a custom configuration when initializing the aeternityService
        aeternityService = AeternityServiceFactory().service
        keyPairService = KeyPairServiceFactory().service
        unitConversionService = DefaultUnitConversionServiceImpl()
        mnemonicMaster = keyPairService.recoverMasterMnemonicKeyPair(listOf("acquire", "useful", "napkin", "ranch", "witness", "scare", "lunch", "smart", "sibling", "situate", "potato", "inspire"), null)
        keyPairAlice = EncodingUtils.createBaseKeyPair(keyPairService.generateDerivedKey(mnemonicMaster, true))
        keyPairBob = EncodingUtils.createBaseKeyPair(keyPairService.generateDerivedKey(mnemonicMaster, true))

        println("PK Alice: ${keyPairAlice.publicKey}")
        println("PK Bob: ${keyPairBob.publicKey}")

        setContentView(R.layout.activity_main)

        balanceAlice = findViewById(R.id.valueBalanceAlice)
        balanceBob = findViewById(R.id.valueBalanceBob)

        GetAccountBalanceTask(this).execute(keyPairAlice.publicKey, keyPairBob.publicKey)
    }

    fun aliceToBob(view: View) {
        sendAE(keyPairAlice, keyPairBob, unitConversionService.toSmallestUnit("0.1"))
    }

    fun bobToAlice(view: View) {
        sendAE(keyPairBob, keyPairAlice, unitConversionService.toSmallestUnit("0.1"))
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
