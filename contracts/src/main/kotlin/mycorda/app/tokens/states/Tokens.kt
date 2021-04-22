package mycorda.app.tokens.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import java.util.Currency

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
sealed class MyCorDappTokenType(
    override val tokenIdentifier: String,
    override val fractionDigits: Int,
    open val issuer: String
) : TokenType(tokenIdentifier, fractionDigits) {
    class CashTokenType(override val issuer: String, val currency: Currency) :
        MyCorDappTokenType(currency.currencyCode, currency.defaultFractionDigits, issuer)

    class SecurityTokenType(override val issuer: String, val identifier: String) :
        MyCorDappTokenType(identifier, 0, issuer)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MyCorDappTokenType

        if (tokenIdentifier != other.tokenIdentifier) return false
        if (fractionDigits != other.fractionDigits) return false
        if (issuer != other.issuer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tokenIdentifier.hashCode() xor issuer.hashCode()
        result = 31 * result + fractionDigits
        return result
    }
}
