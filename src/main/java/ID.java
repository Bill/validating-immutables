import org.immutables.value.Value;

import io.vavr.control.Either;
import io.vavr.control.Try;
import io.vavr.control.Validation;

@Value.Immutable
abstract class ID {

    @Value.Parameter
    abstract String id();

    public String toString() {
        return id();
    }

    @Value.Check
    protected ID check() {
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
    private Validation<String, ID> validate() {
        return Validations.notBlank(id(), "ID").map(id->this);
    }

    /*
     This method would get generated on the Builder class under my proposal.
     In that case it wouldn't need to unpack the exception like this because check()
     would be avoided
     */
    static Validation<String,ID> buildValidation(final ImmutableID.Builder builder) {
        final Either<? extends Throwable,ID> pe = Try.of(()->(ID)builder.build()).toEither();
        return pe.isLeft() ? Validation.invalid(pe.getLeft().getMessage()) : Validation.valid(pe.get());
    }

}
