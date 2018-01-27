import org.immutables.value.Value;

// declare style as meta annotation as shown
// or on package/top-level class
// This is just an example, adapt to your taste however you like
@Value.Style(
        // Detect names starting with underscore
        typeAbstract = "_*",
        // Generate without any suffix, just raw detected name
        typeImmutable = "*",
        // Make generated public, leave underscored as package private
        visibility = Value.Style.ImplementationVisibility.PUBLIC,
        // Seems unnecessary to have builder or superfluous copy method
        defaults = @Value.Immutable(builder = false, copy = false))
public @interface Wrapped {}
