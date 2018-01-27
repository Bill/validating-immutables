import java.lang.reflect.Constructor;
import java.util.regex.Pattern;

import org.immutables.value.Value;

import io.vavr.control.Either;
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

    // FIXME: see https://github.com/immutables/immutables/issues/451
    static Validation<String,SSN> toValidation(final ImmutableSSN.Builder builder) {
        final Either<? extends Throwable,SSN> pe = Try.of(()->(SSN)builder.build()).toEither();
        return pe.isLeft() ? Validation.invalid(pe.getLeft().getMessage()) : Validation.valid(pe.get());
    }

}
