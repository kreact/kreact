package org.kreact.core

/**
 * An interface representing a side effect that can occur during the state mutation. This represents
 * this concept: https://en.wikipedia.org/wiki/Side_effect_(computer_science).
 *
 * Side effects are helpful as optional operations during state mutation when additional outcomes
 * are desired. This can be logging, database operations, caching to disk or memory etc.
 */
interface SideEffect