import java.util.Optional;

import org.immutables.builder.Builder;

import io.vavr.Function1;
import io.vavr.control.Validation;

public class Person2 {

    // FIXME !! vavr-encodings doesn't know about factory builders so we can't use vavr Option in this class meh
    private final Optional<ID> id;
    private final String firstName;
    private final String lastName;
    private final int age;
    private final Optional<SSN> ssn;

    // id is present on persisted entities and absent on others
    public Optional<ID> id() {return id;}
    public String firstName() {return firstName;}
    public String lastName() {return lastName;}
    public int age() {return age;}
    public Optional<SSN> ssn() {return ssn;}

    /*
     this is only a getter--can't set it using builder
     also: it's automatically memoized FTW
     also it's lazily computed. if you want it eager use @Value.Derived
     because it's lazy it's also @Value.Auxiliary so it won't affect equality or hashing
     */
    public String name() { return firstName() + " " + lastName();}

    /*
     private most-general constructor
     It performs no validation!
     */
    Person2(final Optional<ID> id,
            final String firstName,
            final String lastName,
            final int age,
            final Optional<SSN> ssn) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.ssn = ssn;
    }

    /*
     This is the way to construct. It constructs an un-validated object and then returns the validation for it.
     */
    @Builder.Factory
    public static Validation<String,Person2>
    person2Validation(final Optional<ID> id,
           final String firstName,
           final String lastName,
           final int age,
           final Optional<SSN> ssn) {
        return new Person2(id,firstName,lastName,age,ssn).validate();
    }

    private Validation<String, Person2> validate() {
        return Validation.combine(
                Person.nameValidation(name(), "name"),
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

    // FIXME: because we have to use Optional in this class we have to duplicate this logic
    private static Validation<String,Optional<SSN>> adultsRequireSSN(
            final Optional<SSN> ssno,
            final String ssnParameterName,
            final int age) {
        return age > 17 && !ssno.isPresent() ?
               Validation.invalid(
                       String.format("%d-year-old has no SSN (in '%s' parameter): violates SSN required if age > 17 years.",
                                     age, ssnParameterName)) :
               Validation.valid(ssno);
    }

}
