package mycorda.app.tokens.adapters


import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.utilities.tokenAmountsByToken
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.VaultService


/**
 * Returns a list of unspent state references for a given token type
 */
class VaultTokensAdapter(private val vaultService: VaultService) : TokensAdapter {
    private val limit = 1_000

    override fun queryBy(tokenType: TokenType): Sequence<StateAndRef<FungibleToken>> {
        val result = vaultService.tokenAmountsByToken(tokenType)

        // TODO - a production quality implementation
        //        would need to manage large result sets
        if (result.totalStatesAvailable > limit)
            throw NotImplementedError("Found ${result.totalStatesAvailable} unspent tokens, but result sets larger than $limit are not supported.")

        @Suppress("UNCHECKED_CAST")
        return result.states.asSequence()
    }
}