package org.ontheground.dmmf.ordertaking.common.domain.base

abstract class Entity<T : Any> {

    /**
     * Unique identifier for this entity.
     */
    abstract val id: T

    /**
     * Entities are equal if they are of the same concrete class and have the same ID.
     */
    override fun equals(other: Any?): Boolean {
        return this === other || (
                other != null &&
                        this::class == other::class &&
                        other is Entity<*> &&
                        this.id == other.id
                )
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "${this::class.simpleName}(id=$id)"
}
