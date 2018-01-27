import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

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

    public static Validation<String,ID> create(final String content) {
        return getValidation(content).map(ID::construct);
    }

    @Value.Check
    protected ID check() {
        return getValidation(id())
                .map(id->this)
                // toEither().getOrElseThrow() is required because https://github.com/vavr-io/vavr/issues/2207
                .toEither()
                // can't use method reference here: compiler (or at least IntelliJ) finds it ambiguous
                .getOrElseThrow(errors->new IllegalStateException(errors));
    }

    private static Validation<String, String> getValidation(final String id) {
        return Validations.notBlank(id, "ID");
    }

    private static ID construct(final String content) {
        return Try.of(()-> {
                final Constructor constructor = ImmutableID.class.getDeclaredConstructor(String.class);
                constructor.setAccessible(true);
                return (ID)constructor.newInstance(content);
            }).get(); // re-raise checked exceptions as unchecked ones yay
    }

    // FIXME
    static Validation<String,ID> toValidation(final ImmutableID.Builder builder) {
        final Either<? extends Throwable,ID> pe = Try.of(()->(ID)builder.build()).toEither();
        return pe.isLeft() ? Validation.invalid(pe.getLeft().getMessage()) : Validation.valid(pe.get());
    }

}
