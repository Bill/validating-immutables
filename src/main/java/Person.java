import org.immutables.value.Value;
import org.immutables.vavr.encodings.VavrEncodingEnabled;

import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;

@Value.Immutable
@VavrEncodingEnabled
public abstract class Person {

    // id is present on persisted entities and absent on others
    public abstract Option<ID> id();

    public abstract String firstName();
    public abstract String lastName();
    public abstract int age();
    public abstract Option<SSN> ssn();

    @Value.Lazy
    /*
     this is only a getter--can't set it using builder
     also: it's automatically memoized FTW
     also it's lazily computed. if you want it eager use @Value.Derived
     because it's lazy it's also @Value.Auxiliary so it won't affect equality or hashing
     */
    public String name() { return firstName() + " " + lastName();}

    @Value.Check
    protected Person check() {
        return validate()
                // toEither().getOrElseThrow() is required because https://github.com/vavr-io/vavr/issues/2207
                .toEither()
                .getOrElseThrow(errors->new IllegalStateException(errors));
    }

    private Validation<String, Person> validate() {
        return Validation.combine(
                nameValidation(name(), "name"),
                adultsRequireSSN(ssn(), "ssn", age()))
                         .ap((nameIgnored, ssnoIgnored) ->
                             /*
                              name is a derived property so we don't actually need to set it (it's already set)
                              return ImmutablePerson.copyOf(this).withName(name);
                              adultsRequireSSN() won't change SSN
                              */
                             this
                         )
                         .mapError(Validations::combineErrors);
    }

    /*
     FIXME: builder should have a method that returns Validation<String,Person>, similar to the
            create() method we hand-crafted on SSN and ID. In the case of SSN and ID it was ok
            to put the method on the target class instead of the builder because those classes
            were "simple" (only one attribute each). No builder is ever used on those. Entity
            classes really need their builders, and it'd be really nice if those builders could
            produce validations.

            See Immutables.io ticket: https://github.com/immutables/immutables/issues/451
     */
    static Validation<String,Person> toValidation(final ImmutablePerson.Builder builder) {
        // go through whole construction and check() process and raise any exception
        final Either<? extends Throwable,Person> pe = Try.of(()->(Person)builder.build()).toEither();
        /*
         now reverse that exception message back into a String so we can put it into our Validation
         and let the caller manipulate it
         */
        return pe.isLeft() ? Validation.invalid(pe.getLeft().getMessage()) : Validation.valid(pe.get());
    }

    private static Validation<String,String> nameValidation(final String name, final String parameterName) {
        return Validations.notBlank(name, "name").combine(
                Validations.maximumLength(name, "name", 16))
                          .ap((n1,n2)->n1)
                          .mapError(Validations::combineErrors);
    }

    private static Validation<String,Option<SSN>> adultsRequireSSN(
            final Option<SSN> ssno,
            final String ssnParameterName,
            final int age) {
        return age > 17 && ssno.isEmpty() ?
               Validation.invalid(
                       String.format("%d-year-old has no SSN (in '%s' parameter): violates SSN required if age > 17 years.",
                                     age, ssnParameterName)) :
               Validation.valid(ssno);
    }

}
