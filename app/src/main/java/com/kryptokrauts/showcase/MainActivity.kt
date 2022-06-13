package com.kryptokrauts.showcase

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kryptokrauts.aeternity.sdk.constants.Network
import com.kryptokrauts.aeternity.sdk.domain.secret.HdWallet
import com.kryptokrauts.aeternity.sdk.domain.secret.KeyPair
import com.kryptokrauts.aeternity.sdk.service.aeternity.AeternityServiceConfiguration
import com.kryptokrauts.aeternity.sdk.service.aeternity.AeternityServiceFactory
import com.kryptokrauts.aeternity.sdk.service.aeternity.impl.AeternityService
import com.kryptokrauts.aeternity.sdk.service.keypair.KeyPairService
import com.kryptokrauts.aeternity.sdk.service.keypair.KeyPairServiceFactory
import com.kryptokrauts.aeternity.sdk.service.transaction.type.model.SpendTransactionModel
import com.kryptokrauts.aeternity.sdk.service.unit.UnitConversionService
import com.kryptokrauts.aeternity.sdk.service.unit.impl.DefaultUnitConversionServiceImpl
import com.kryptokrauts.aeternity.sdk.util.UnitConversionUtil
import com.kryptokrauts.showcase.constants.AeternityConfig
import com.kryptokrauts.showcase.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.math.BigInteger

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var aeternityService: AeternityService
    private lateinit var keyPairService: KeyPairService
    private lateinit var unitConversionService: UnitConversionService

    private lateinit var firstHdWallet: HdWallet
    private lateinit var alice: KeyPair
    private lateinit var bob: KeyPair

    private val exampleWalletSeedPhrase = listOf(
        "express",
        "outside",
        "alter",
        "diamond",
        "comic",
        "bulk",
        "kidney",
        "cancel",
        "cushion",
        "can",
        "bridge",
        "foil"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // use the DNS resolver embedded in Java
        System.setProperty("vertx.disableDnsResolver", "true")

        val config = AeternityServiceConfiguration.configure()
            .baseUrl(AeternityConfig.Network.TESTNET.baseUrl)
            .compilerBaseUrl(AeternityConfig.Network.TESTNET.compilerUrl)
            .mdwBaseUrl(AeternityConfig.Network.TESTNET.mdwUrl)
            .network(Network.TESTNET)
            .compile()
        aeternityService = AeternityServiceFactory().getService(config)
        keyPairService = KeyPairServiceFactory().service
        unitConversionService = DefaultUnitConversionServiceImpl()

        firstHdWallet = keyPairService.recoverHdWallet(exampleWalletSeedPhrase, null)
        alice = keyPairService.getNextKeyPair(firstHdWallet)
        bob = keyPairService.getNextKeyPair(firstHdWallet)

        println("Alice: ${alice.address}")
        println("Bob: ${bob.address}")

        binding.buttonAliceToBob.setOnClickListener { aliceToBob() }
        binding.buttonBobToAlice.setOnClickListener { bobToAlice() }

        launch {
            while (true) {
                println("Updating balances for addresses:\n${alice.address}\n${bob.address}")
                getAccountBalance(alice.address, bob.address)
                println("Wait for 5 seconds ...")
                delay(5000)
            }
        }
    }

    private suspend fun getAccountBalance(vararg params: String?) {
        val job = coroutineScope {
            async {
                val balanceList: MutableList<BigInteger> = mutableListOf()
                params.forEach { param ->
                    run {
                        val acc = aeternityService.accounts?.blockingGetAccount(param)
                        val balance = acc?.balance ?: BigInteger.ZERO
                        balanceList.add(balance)
                    }
                }

                balanceList.toTypedArray()
            }
        }
        val result = job.await()
        processBalances(result)
    }

    private fun processBalances(balances: Array<BigInteger>?) {
        val aliceBalance = UnitConversionUtil.fromAettos(
            BigDecimal(balances?.get(0) ?: BigInteger("0")),
            UnitConversionUtil.Unit.AE
        )
        val aliceBalanceText = "$aliceBalance AE"
        binding.valueBalanceAlice.text = aliceBalanceText

        val bobBalance = UnitConversionUtil.fromAettos(
            BigDecimal(balances?.get(1) ?: BigInteger("0")),
            UnitConversionUtil.Unit.AE
        )
        val bobBalanceText = "$bobBalance AE"
        binding.valueBalanceBob.text = bobBalanceText

        binding.progressCircular.visibility = View.GONE
    }

    private fun aliceToBob() {
        sendAE(alice, bob, unitConversionService.toSmallestUnit("0.1"))
    }

    private fun bobToAlice() {
        sendAE(bob, alice, unitConversionService.toSmallestUnit("0.1"))
    }

    private fun sendAE(from: KeyPair, to: KeyPair, amount: BigInteger) {
        val account = aeternityService.accounts.blockingGetAccount(from.address)
        val balance =
            UnitConversionUtil.fromAettos(BigDecimal(account.balance), UnitConversionUtil.Unit.AE)
        println(
            "Sending ${
                UnitConversionUtil.fromAettos(
                    BigDecimal(amount),
                    UnitConversionUtil.Unit.AE
                )
            } AE from account with balance: $balance AE"
        )

        if (amount < account.balance) {
            val spendTx = SpendTransactionModel.builder()
                .amount(amount)
                .nonce(account.nonce.add(BigInteger.ONE))
                .recipient(to.address)
                .sender(from.address)
                .payload("https://github.com/kryptokrauts/aepp-showcase-android")
                .build()
            binding.progressCircular.visibility = View.VISIBLE
            async {
                val result = aeternityService.transactions.blockingPostTransaction(
                    spendTx,
                    from.encodedPrivateKey
                )
                println("tx-hash: ${result.txHash}")
            }
        } else {
            println("Not enough funds.")
        }
    }
}