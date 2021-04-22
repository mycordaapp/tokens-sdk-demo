package mycorda.app.tokens.services

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import mycorda.app.tokens.ACME
import mycorda.app.tokens.GBP
import mycorda.app.tokens.NORTHWIND
import mycorda.app.tokens.Parties.ISSUER
import mycorda.app.tokens.Parties.PARTICIPANT_1
import mycorda.app.tokens.Parties.PARTICIPANT_2
import mycorda.app.tokens.Parties.PARTICIPANT_3
import mycorda.app.tokens.USD
import mycorda.app.tokens.adapters.MockTokensAdapter
import mycorda.app.tokens.states.MyCorDappTokenType
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.identity.Party
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import java.util.*

/**
 * Some example testcases using the Adapter
 */
class GenerateMoveTests {
    private val party1 = PARTICIPANT_1.party
    private val party2 = PARTICIPANT_2.party
    private val party3 = PARTICIPANT_3.party
    private val issuer = ISSUER.party

    @Test
    fun `single cash token for exact amount`() {
        // setup
        val availableToken = tokenOf(100, GBP, party1)

        // execute
        val (spent, output) = generateMoveImpl(
            recipients = mapOf(party2 to (100 of GBP)),
            changeHolder = party1,
            source = party1,
            tokensAdapter = MockTokensAdapter(listOf(availableToken))
        )

        // validate
        assertThat(spent.size, equalTo(1))
        assertThat(spent, containsTokenState(availableToken))
        val expected = availableToken.withNewHolder(party2)
        assertThat(output.size, equalTo(1))
        assertThat(output, containsToken(expected))
    }


    @Test
    fun `mix of cash and security tokens in the vault`() {
        // setup
        val oneHundredGPB = listOf(
            tokenOf(10, GBP, party1),
            tokenOf(20, GBP, party1),
            tokenOf(30, GBP, party1),
            tokenOf(40, GBP, party1)
        )

        val someDollars = listOf(
            tokenOf(17, USD, party1),
            tokenOf(3, USD, party1),
            tokenOf(1234, USD, party1)
        )
        val someSecurities = listOf(
            tokenOf(10, ACME, party1),
            tokenOf(35, ACME, party1)
        )

        val available =
            (oneHundredGPB + someDollars + someSecurities).shuffled(Random(0))

        // execute
        val (input, output) = generateMoveImpl(
            mapOf(party2 to 100.of(GBP)),
            party1,
            party1,
            MockTokensAdapter(available)
        )

        // validate
        assertThat(input.size, equalTo(4))
        assertThat(input, containsTokenState(tokenOf(10, GBP, party1)))
        assertThat(input, containsTokenState(tokenOf(20, GBP, party1)))
        assertThat(input, containsTokenState(tokenOf(30, GBP, party1)))
        assertThat(input, containsTokenState(tokenOf(40, GBP, party1)))

        assertThat(input, containsTokenState(tokenOf(10, GBP, party1)))
        assertThat(input, containsTokenState(tokenOf(20, GBP, party1)))
        assertThat(input, containsTokenState(tokenOf(30, GBP, party1)))
        assertThat(input, containsTokenState(tokenOf(40, GBP, party1)))

        assertThat(output, containsToken(tokenOf(10, GBP, party2)))
        assertThat(output, containsToken(tokenOf(20, GBP, party2)))
        assertThat(output, containsToken(tokenOf(30, GBP, party2)))
        assertThat(output, containsToken(tokenOf(40, GBP, party2)))

    }


    @Test
    fun `single security token for exact amount`() {
        // setup
        val available = tokenOf(100, ACME, party1)

        // execute
        val (input, output) = generateMoveImpl(
            mapOf(party2 to 100.of(ACME)),
            party1,
            party1,
            MockTokensAdapter(listOf(available))
        )

        // validate
        assertThat(input.size, equalTo(1))
        assertThat(input, containsTokenState(available))

        val expected = available.withNewHolder(party2)

        assertThat(output.size, equalTo(1))
        assertThat(output, containsToken(expected))
    }

    @Test
    fun `single token with change`() {
        // setup
        val available = tokenOf(100, ACME, party1)

        // execute
        val (input, output) = generateMoveImpl(
            mapOf(party2 to 10.of(ACME)),
            party1,
            party1,
            MockTokensAdapter(listOf(available))
        )

        // validate
        assertThat(input.size, equalTo(1))
        assertThat(input, containsTokenState(available))

        val change = tokenOf(90, ACME, party1)
        val sent = tokenOf(10, ACME, party2)

        assertThat(output.size, equalTo(2))
        assertThat(output, containsToken(change))
        assertThat(output, containsToken(sent))
    }


    @Test
    fun `multiple selected tokens for exact amount`() {
        // setup
        val requested = 100.of(ACME)
        val available = listOf(
            tokenOf(10, ACME, party1),
            tokenOf(90, ACME, party1)
        )

        // execute
        val (input, output) = generateMoveImpl(
            mapOf(party2 to 100.of(ACME)),
            party1,
            party1,
            MockTokensAdapter(available)
        )

        // validate
        assertThat(input.size, equalTo(2))
        assertThat(input, containsTokenState(tokenOf(10, ACME, party1)))
        assertThat(input, containsTokenState(tokenOf(90, ACME, party1)))

        assertThat(output.size, equalTo(2))
        assertThat(output, containsToken(tokenOf(90, ACME, party2)))
        assertThat(output, containsToken(tokenOf(10, ACME, party2)))
    }

    @Test
    fun `multiple selected tokens with change`() {
        // setup
        val available = listOf(
            tokenOf(10, ACME, party1),
            tokenOf(20, ACME, party1),
            tokenOf(30, ACME, party1)
        ).shuffled()

        // execute
        val (input, output) = generateMoveImpl(
            mapOf(party2 to 33.of(ACME)),
            party1,
            party1,
            MockTokensAdapter(available)
        )

        // validate
        assertThat(input.size, equalTo(3))
        assertThat(input, containsTokenState(tokenOf(10, ACME, party1)))
        assertThat(input, containsTokenState(tokenOf(20, ACME, party1)))
        assertThat(input, containsTokenState(tokenOf(30, ACME, party1)))

        assertThat(output.size, equalTo(4))
        assertThat(output, containsToken(tokenOf(10, ACME, party2)))
        assertThat(output, containsToken(tokenOf(20, ACME, party2)))
        assertThat(output, containsToken(tokenOf(3, ACME, party2)))
        assertThat(output, containsToken(tokenOf(27, ACME, party1)))
    }


    @Test
    fun `select correct security token by token type`() {
        // setup
        val available = listOf(
            tokenOf(33, NORTHWIND, party1),
            tokenOf(67, NORTHWIND, party1),
            tokenOf(33, ACME, party1),
            tokenOf(67, ACME, party1)
        ).shuffled()  // randomised the order

        // execute
        val (input, output) = generateMoveImpl(
            mapOf(party2 to 100.of(ACME)),
            party1,
            party1,
            MockTokensAdapter(available)
        )

        // validate
        assertThat(input.size, equalTo(2))
        assertThat(input, containsTokenState(tokenOf(33, ACME, party1)))
        assertThat(input, containsTokenState(tokenOf(67, ACME, party1)))

        assertThat(output.size, equalTo(2))
        assertThat(output, containsToken(tokenOf(33, ACME, party2)))
        assertThat(output, containsToken(tokenOf(67, ACME, party2)))
    }

    @Test
    fun `throws exception if insufficient balance`() {
        // setup
        val requested = 11.of(ACME)
        val tenForACME = listOf(
            tokenOf(1, ACME, party1),
            tokenOf(2, ACME, party1),
            tokenOf(3, ACME, party1),
            tokenOf(4, ACME, party1)
        )
        val available = (tenForACME + tokenOf(1, NORTHWIND, party1)).shuffled(Random(0))

        // execute
        val expectedMsg = "There are insufficient Tokens available"
        assertThat(
            {
                generateMoveImpl(
                    recipients = mapOf(party2 to requested),
                    changeHolder = party1,
                    source = party1,
                    tokensAdapter = MockTokensAdapter(available)
                )
            },
            throws<IllegalArgumentException>(has(Exception::message, present(equalTo(expectedMsg))))
        )
    }


    @Test
    fun `change recipient can be specified`() {
        // setup
        val available = tokenOf(1000, GBP, party1)

        // execute
        val (spent, output) = generateMoveImpl(
            recipients = mapOf(party2 to 200.of(GBP)),
            changeHolder = party3,
            source = party1,
            tokensAdapter = MockTokensAdapter(listOf(available))
        )

        // validate
        assertThat(spent.size, equalTo(1))
        assertThat(spent, containsTokenState(available))

        val out = tokenOf(200, GBP, party2)
        val change = tokenOf(800, GBP, party3)

        assertThat(output.size, equalTo(2))
        assertThat(output, containsToken(out))
        assertThat(output, containsToken(change))
    }

    @Test
    fun `does not select on identifier alone`() {
        val nasdaq = MyCorDappTokenType.SecurityTokenType("NASDAQ", "R3")
        val footsie = MyCorDappTokenType.SecurityTokenType("Footsie", "R3")

        // setup
        val mixedR3 = listOf(
            tokenOf(1, nasdaq, party1),
            tokenOf(
                1, footsie, party1
            )
        )

        assertThat(
            {
                generateMoveImpl(
                    recipients = mapOf(party2 to 2.of(footsie)),
                    changeHolder = party1,
                    source = party1,
                    tokensAdapter = MockTokensAdapter(mixedR3)
                )
            },
            throws<IllegalArgumentException>(
                has(
                    Exception::message,
                    present(equalTo("There are insufficient Tokens available"))
                )
            )
        )
    }

    @Suppress("RemoveExplicitTypeArguments")
    private fun containsTokenState(available: FungibleToken): Matcher.Primitive<List<StateAndRef<FungibleToken>>> =
        anyElement(
            isA<StateAndRef<FungibleToken>>(
                has(
                    StateAndRef<FungibleToken>::state,
                    isA<TransactionState<FungibleToken>>(
                        has(
                            TransactionState<FungibleToken>::data,
                            isA<FungibleToken>(equalTo(available))
                        )
                    )
                )
            )
        )

    @Suppress("RemoveExplicitTypeArguments")
    private fun containsToken(available: FungibleToken): Matcher.Primitive<List<FungibleToken>> = anyElement(
        isA<FungibleToken>(
            equalTo(available)
        )
    )

    private fun tokenOf(quantity: Long, type: MyCorDappTokenType, holder: Party): FungibleToken {
        return FungibleToken(quantity of type issuedBy issuer, holder)
    }

}
