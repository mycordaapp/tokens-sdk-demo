package mycorda.app.tokens

import mycorda.app.tokens.states.MyCorDappTokenType
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import java.util.*


// Currencies
val EUR = MyCorDappTokenType.CashTokenType("European Central Bank", Currency.getInstance("EUR"))
val GBP = MyCorDappTokenType.CashTokenType("Bank of England", Currency.getInstance("GBP"))
val USD = MyCorDappTokenType.CashTokenType("Federal Reserve", Currency.getInstance("USD"))

// Securities
val ACME = MyCorDappTokenType.SecurityTokenType("Acme Ltd.", "ACM")
val NORTHWIND = MyCorDappTokenType.SecurityTokenType("Northwind", "NTH")

// Parties
object Parties {
    val NOTARY = TestIdentity(CordaX500Name.parse("O=Notary,L=London,C=GB"))

    val PARTICIPANT_1 = TestIdentity(CordaX500Name.parse("O=Alice SARL, L=Paris, C=FR"))
    val PARTICIPANT_2 = TestIdentity(CordaX500Name.parse("O=Bob Ltd, L=Milton Keynes, C=GB"))
    val PARTICIPANT_3 = TestIdentity(CordaX500Name.parse("O=Charlie Pty. Ltd., L=Sydney, S=New South Wales, C=AU"))

    val ISSUER = TestIdentity(CordaX500Name.parse("O=R3 Ltd,L=London,C=GB"))

}

