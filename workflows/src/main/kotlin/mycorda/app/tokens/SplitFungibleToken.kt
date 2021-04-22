package mycorda.app.tokens

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.Amount
import net.corda.core.identity.Party

fun FungibleToken.split(recipient: Party, quantity: Long): Pair<FungibleToken, FungibleToken> {
    if (quantity >= amount.quantity)
        throw IllegalArgumentException("Quantity to split ($quantity) exceeds token quantity (${amount.quantity}).")

    val transferredAmount = Amount(quantity, amount.token)
    val remainingAmount = Amount(amount.quantity - quantity, amount.token)

    val transferredToken = FungibleToken(transferredAmount, recipient)
    val remainingToken = FungibleToken(remainingAmount, holder)

    return transferredToken to remainingToken
}
