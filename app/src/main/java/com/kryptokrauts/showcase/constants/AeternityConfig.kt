package com.kryptokrauts.showcase.constants

object AeternityConfig {

    enum class Network(
        val baseUrl: String,
        val compilerUrl: String,
        val mdwUrl: String
    ) {

        TESTNET(
            "https://testnet.aeternity.io",
            "https://compiler.aeternity.io",
            "https://testnet.aeternity.io/mdw"
        ),

        MAINNET(
            "https://mainnet.aeternity.io",
            "https://compiler.aeternity.io",
            "https://mainnet.aeternity.io/mdw"
        )
    }
}