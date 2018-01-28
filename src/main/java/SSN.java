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
        return validate()
                // toEither().getOrElseThrow() is required because https://github.com/vavr-io/vavr/issues/2207
                .toEither()
                // can't use method reference here: compiler (or at least IntelliJ) finds it ambiguous
                .getOrElseThrow(errors->new IllegalStateException(errors));
    }

    /*
     In https://github.com/immutables/immutables/issues/451 I propose
     @Value.Validate to generate a buildValidation() on the generated Builder class
     */
    private Validation<String,SSN> validate() {
        final String content = ssn();
        return Validations.notBlank(content, "SSN")
                          .combine(Validations.matches(content,"SSN", pattern))
                          .ap((ssn1,ssn2)->this)
                          .mapError(Validations::combineErrors);
    }

    /*
     This method would get generated on the Builder class under my proposal.
     In that case it wouldn't need to unpack the exception like this because check()
     would be avoided
     */
    static Validation<String,SSN> buildValidation(final ImmutableSSN.Builder builder) {
        final Either<? extends Throwable,SSN> pe = Try.of(()->(SSN)builder.build()).toEither();
        return pe.isLeft() ? Validation.invalid(pe.getLeft().getMessage()) : Validation.valid(pe.get());
    }

}
