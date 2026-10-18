package co.sena.iam.domain.model;

/** Kind of actor an account belongs to. Mirrors {@code identity.user.actor_type}. */
public enum ActorType {
    USER,
    INSTRUCTOR,
    LEARNER
}
