package services;

import java.util.Objects;

/**
 * A single user-configurable planning constraint.
 *
 * Each constraint has:
 * <ul>
 *   <li>an identifier (one of the {@link ConstraintIds} keys)</li>
 *   <li>a human-readable French label</li>
 *   <li>a value stored as String (parsed as needed by the consumer)</li>
 *   <li>a {@link ConstraintPriority} (HARD = blocking, SOFT = warning)</li>
 * </ul>
 */
public class Constraint {

    private final String id;
    private String label;
    private String value;
    private ConstraintPriority priority;
    private final boolean priorityLocked;

    public Constraint(String id, String label, String value, ConstraintPriority priority) {
        this(id, label, value, priority, false);
    }

    public Constraint(String id, String label, String value, ConstraintPriority priority, boolean priorityLocked) {
        this.id = Objects.requireNonNull(id);
        this.label = label;
        this.value = value;
        this.priority = priority == null ? ConstraintPriority.SOFT : priority;
        this.priorityLocked = priorityLocked;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public ConstraintPriority getPriority() { return priority; }
    public void setPriority(ConstraintPriority priority) {
        if (priorityLocked) {
            return; // some constraints are intrinsically HARD and cannot be downgraded
        }
        this.priority = priority == null ? ConstraintPriority.SOFT : priority;
    }

    public boolean isPriorityLocked() { return priorityLocked; }

    public boolean isHard() { return priority == ConstraintPriority.HARD; }

    // ─── Typed accessors ────────────────────────────────────────────────────

    public int asInt(int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean asBool(boolean defaultValue) {
        if (value == null) return defaultValue;
        String v = value.trim().toLowerCase();
        if ("true".equals(v) || "1".equals(v) || "yes".equals(v) || "oui".equals(v)) return true;
        if ("false".equals(v) || "0".equals(v) || "no".equals(v) || "non".equals(v)) return false;
        return defaultValue;
    }

    public String asString(String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }
}
