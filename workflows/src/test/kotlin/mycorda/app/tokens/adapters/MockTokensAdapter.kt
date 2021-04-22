package mycorda.app.tokens.adapters

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import mycorda.app.tokens.Parties.NOTARY

class MockTokensAdapter(private val tokens: List<FungibleToken>) : TokensAdapter {

    override fun queryBy(tokenType: TokenType) = tokens.filter {
        it.tokenType == tokenType
    }.map {
        it.wrappedInMockStateAndRef()
    }.asSequence()
}

fun <T : ContractState> T.wrappedInMockStateAndRef(): StateAndRef<T> = StateAndRef(
    state = TransactionState(this, notary = NOTARY.party),
    ref = StateRef(SecureHash.allOnesHash, 0)
)
