import java.lang.reflect.Constructor;
import java.util.regex.Pattern;

import org.immutables.value.Value;

import io.vavr.control.Try;
import io.vavr.control.Validation;

@Value.Immutable
abstract class SSN {
    private static final Pattern pattern = Pattern.compile("\\d{3}+-\\d{2}+-\\d{4}+");

    @Value.Parameter
    abstract String ssn();

    public String toString() {
        return ssn();
    }

    public static Validation<String,SSN> create(final String content) {
        return getValidation(content).map(SSN::construct);
    }

    @Value.Check
    protected SSN check() {
        return getValidation(ssn())
                .map(x->this)
                // toEither().getOrElseThrow() is required because https://github.com/vavr-io/vavr/issues/2207
                .toEither()
                // can't use method reference here: compiler (or at least IntelliJ) finds it ambiguous
                .getOrElseThrow(errors->new IllegalStateException(errors));
    }

    private static Validation<String, String> getValidation(final String content) {
        return Validations.notBlank(content, "SSN")
                          .combine(Validations.matches(content,"SSN", pattern))
                          .ap((ssn1,ssn2)->ssn1)
                          .mapError(Validations::combineErrors);
    }

    private static SSN construct(final String content) {
        return Try.of(()-> {
            final Constructor constructor = ImmutableSSN.class.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return (SSN)constructor.newInstance(content);
        }).get(); // re-raise checked exceptions as unchecked ones yay
    }

}
