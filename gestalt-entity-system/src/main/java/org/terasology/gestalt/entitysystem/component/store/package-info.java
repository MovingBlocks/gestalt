/**
 * This package provides component stores - each component store handles storage of a single type
 * of component across entities, by entity id. Different implementations are offered for different
 * performance use cases, although if in doubt {@link org.terasology.gestalt.entitysystem.component.store.ArrayComponentStore}
 * is probably the reliable go-to. {@link org.terasology.gestalt.entitysystem.component.store.ConcurrentComponentStore}
 * can be used to make any component store thread safe, at least in so far as making single actions
 * atomic - it doesn't protect against broader concerns such as lost update scenarios.
 */
package org.terasology.gestalt.entitysystem.component.store;