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

    private Validation<String, ID> validate() {
        return Validations.notBlank(id(), "ID").map(id->this);
    }

    // FIXME: see https://github.com/immutables/immutables/issues/451
    static Validation<String,ID> toValidation(final ImmutableID.Builder builder) {
        final Either<? extends Throwable,ID> pe = Try.of(()->(ID)builder.build()).toEither();
        return pe.isLeft() ? Validation.invalid(pe.getLeft().getMessage()) : Validation.valid(pe.get());
    }

}
