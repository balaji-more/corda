package net.corda.core.node.services

import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.Party

/**
 * An identity service maintains an bidirectional map of [Party]s to their associated public keys and thus supports
 * lookup of a party given its key. This is obviously very incomplete and does not reflect everything a real identity
 * service would provide.
 */
interface IdentityService {
    fun registerIdentity(party: Party)

    // There is no method for removing identities, as once we are made aware of a Party we want to keep track of them
    // indefinitely. It may be that in the long term we need to drop or archive very old Party information for space,
    // but for now this is not supported.

    fun partyFromKey(key: CompositeKey): Party?
    fun partyFromName(name: String): Party?
}
