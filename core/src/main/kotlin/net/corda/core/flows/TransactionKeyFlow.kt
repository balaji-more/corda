package net.corda.core.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.identity.AnonymousPartyAndPath
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

/**
 * Very basic flow which exchanges transaction key and certificate paths between two parties in a transaction.
 * This is intended for use as a subflow of another flow.
 */
@StartableByRPC
@InitiatingFlow
class TransactionKeyFlow(val otherSide: Party,
                         val revocationEnabled: Boolean,
                         override val progressTracker: ProgressTracker) : FlowLogic<LinkedHashMap<Party, AnonymousPartyAndPath>>() {
    constructor(otherSide: Party) : this(otherSide, false, tracker())

    companion object {
        object AWAITING_KEY : ProgressTracker.Step("Awaiting key")

        fun tracker() = ProgressTracker(AWAITING_KEY)
        fun validateIdentity(otherSide: Party, anonymousOtherSide: AnonymousPartyAndPath): AnonymousPartyAndPath {
            require(anonymousOtherSide.name == otherSide.name)
            return anonymousOtherSide
        }
    }

    @Suspendable
    override fun call(): LinkedHashMap<Party, AnonymousPartyAndPath> {
        progressTracker.currentStep = AWAITING_KEY
        val legalIdentityAnonymous = serviceHub.keyManagementService.freshKeyAndCert(serviceHub.legalIdentity, revocationEnabled) // TODO legalIdentityAndCert keep it in keyManagementService - not myInfo

        // Special case that if we're both parties, a single identity is generated
        val identities = LinkedHashMap<Party, AnonymousPartyAndPath>()
        if (otherSide == serviceHub.legalIdentity.party) {
            identities.put(otherSide, legalIdentityAnonymous)
        } else {
            val otherSideAnonymous = sendAndReceive<AnonymousPartyAndPath>(otherSide, legalIdentityAnonymous).unwrap { validateIdentity(otherSide, it) }
            serviceHub.identityService.verifyAndRegisterAnonymousIdentity(otherSideAnonymous, otherSide)
            identities.put(serviceHub.legalIdentity.party, legalIdentityAnonymous)
            identities.put(otherSide, otherSideAnonymous)
        }
        return identities
    }

}
