package mycorda.app.tokens.services


import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import mycorda.app.tokens.adapters.TokensAdapter
import mycorda.app.tokens.adapters.VaultTokensAdapter
import mycorda.app.tokens.split

/**
 * Matches signature used by the TokensSDK, in case we swap to use that in the future.
 */
fun ServiceHub.generateMove(
    recipients: Map<Party, Amount<TokenType>>,
    changeHolder: Party,
    source: Party,
    tokensAdapter: TokensAdapter = VaultTokensAdapter(vaultService)
): Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> = generateMoveImpl(
    recipients = recipients,
    changeHolder = changeHolder,
    source = source,
    tokensAdapter = tokensAdapter
)


/**
 * Actual implementation of generateMove - in its own method for easy unit testing.
 */
fun generateMoveImpl(
    recipients: Map<Party, Amount<TokenType>>,
    changeHolder: Party,
    source: Party,
    tokensAdapter: TokensAdapter
): Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> {
    val (recipient, amount) = recipients.entries.singleOrNull()
        ?: throw NotImplementedError("This implementation only supports a single recipient")

    // Get _all_ qualifying tokens
    val allInTokens = tokensAdapter.queryBy(amount.token).filter {
        it.state.data.holder == source
    }

    return doGenerateTokenMove(
        recipient,
        changeHolder,
        allInTokens,
        amount
    )
}


private fun doGenerateTokenMove(
    recipient: Party,
    changeHolder: Party,
    allInTokens: Sequence<StateAndRef<FungibleToken>>,
    amount: Amount<TokenType>
): Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> {

    // 1 - remove all tokens that are't spendable , e.g. wrong currency or have some type lock
    val spendableTokens = allInTokens.filter {
        it.state.data.amount.token.tokenType == amount.token
    }

    // 2 - check that total value is >= the requested transfer amount
    if (spendableTokens.sumBy { it.state.data.amount.quantity.toInt() } < amount.quantity.toInt()) {
        throw IllegalArgumentException("There are insufficient Tokens available")
    }

    // 3 - try and find an exact match on a single token
    val exact = spendableTokens.find { it.state.data.amount.quantity == amount.quantity }
    if (exact != null) {
        val tk = exact.state.data
        val movedToken = tk.withNewHolder(recipient)
        return Pair(listOf(exact), listOf(movedToken))
    }

    val descending = spendableTokens.sortedBy { it.state.data.amount.quantity }

    // 4 - try and find a single token > than the requested amount and return a single token with the change
    val single = descending.find { it.state.data.amount.quantity > amount.quantity }
    if (single != null) {
        val (movedToken, changeToken) = single.state.data.split(recipient, amount.quantity)
        return listOf(single) to listOf(movedToken, changeToken.withNewHolder(changeHolder))
    }

    // 5 - go through the list in descending value until enough tokens are found, and if not
    //     an exact match on the requested value then return one token with the change
    val selected = mutableListOf<StateAndRef<FungibleToken>>()
    val moved = mutableListOf<FungibleToken>()
    var totalValue = 0L

    descending.forEach {
        if (totalValue < amount.quantity) {
            selected.add(it)
            totalValue += it.state.data.amount.quantity

            if (totalValue <= amount.quantity) {
                // move the whole token
                val tk = it.state.data
                val movedToken = tk.withNewHolder(recipient)
                moved.add(movedToken)
                if (totalValue == amount.quantity) {
                    // tokens match in value exactly, no need for change
                    return selected to moved
                }
            } else if (totalValue > amount.quantity) {
                // have to split the last token to generate the necessary change
                val changeAmount = Amount(
                    tokenQuantity = kotlin.math.abs(totalValue - amount.quantity - it.state.data.amount.quantity),
                    token = it.state.data.amount.token
                )

                val (movedToken, changeToken) = it.state.data.split(recipient, changeAmount.quantity)
                moved.add(movedToken)
                moved.add(changeToken.withNewHolder(changeHolder))

                return selected to moved
            }
        }
    }

    // safety net - should never get here
    throw IllegalStateException("Cannot generate a token movement")
}

