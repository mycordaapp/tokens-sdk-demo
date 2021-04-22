package mycorda.app.tokens.adapters

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.StateAndRef

/**
 * Make the query logic injectable via the adapter pattern, so that we can test the raw
 * token selection logic independent to the contract and workflow logic.
 */
interface TokensAdapter {
    fun queryBy(tokenType: TokenType): Sequence<StateAndRef<FungibleToken>>
}