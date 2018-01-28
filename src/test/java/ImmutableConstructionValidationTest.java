import java.util.Optional;

import org.junit.Test;

import io.vavr.Function2;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

public class ImmutableConstructionValidationTest {
    @Test
    public void testBuiltInImmutablesRequiredAttributeMissing() {

        final ImmutablePerson.Builder builder =
                ImmutablePerson.builder().firstName("Fred").lastName("Mertz");

        final Try<Person> pt = Try.of(() -> builder.build());
        assertThat(pt.isFailure(),is(true));
        assertThat(pt.getCause(),is(instanceOf(IllegalStateException.class)));
        assertThat(pt.getCause().getMessage(),is("Cannot build Person, some of required attributes are not set [age]"));
    }

    @Test
    public void testCheckMethodFindsInvalidField() {

        final ImmutablePerson.Builder builder =
                ImmutablePerson.builder().firstName("John").lastName("Jacob Jingleheimerschmidt").age(5);

        final Try<Person> pt = Try.of(() -> builder.build());
        assertThat(pt.isFailure(),is(true));
        assertThat(pt.getCause(),is(instanceOf(IllegalStateException.class)));
        assertThat(pt.getCause().getMessage(),is("30 character name is too long: violates maximum name length of 16 characters."));
    }

    @Test
    public void testCheckMethodFindsMultiFieldValidationProblem() {

        final ImmutablePerson.Builder builder =
                ImmutablePerson.builder().firstName("Lucy").lastName("Ricardo").age(20);

        final Try<Person> pt = Try.of(() -> builder.build());
        assertThat(pt.getCause(),is(instanceOf(IllegalStateException.class)));
        assertThat(pt.getCause().getMessage(),is("20-year-old has no SSN (in 'ssn' parameter): violates SSN required if age > 17 years."));
    }

    @Test
    public void testCopyConstructionEnforcesChecks() {

        final ImmutablePerson.Builder builder =
                ImmutablePerson.builder().firstName("Lucy").lastName("Ricardo").age(20)
                               .ssn(Option.some(ImmutableSSN.of("100-10-1000")));

        final Try<Person> pt = Try.of(() -> builder.build());
        assertThat(pt.isSuccess(),is(true));
        pt.map(person->{

            final Try<Person> pt2 = Try.of(() -> ImmutablePerson.copyOf(person).withSsn(Option.none()));

            assertThat(pt2.isFailure(),is(true));
            assertThat(pt2.getCause(),is(instanceOf(IllegalStateException.class)));
            assertThat(pt2.getCause().getMessage(),is("person aged 20 years (over 17 years) must have ssn."));
            return person;
        });
    }

    @Test
    public void testMultipleErrorsAtOnce() {

        final ImmutablePerson.Builder builder =
                ImmutablePerson.builder().firstName("John").lastName("Jacob Jingleheimerschmidt").age(20);

        final Try<Person> pt = Try.of(() -> builder.build());
        assertThat(pt.isFailure(),is(true));
        assertThat(pt.getCause(),is(instanceOf(IllegalStateException.class)));
        assertThat(pt.getCause().getMessage(),is("30 character name is too long: violates maximum name length of 16 characters. 20-year-old has no SSN (in 'ssn' parameter): violates SSN required if age > 17 years."));

    }

    @Test
    public void testFromStringlyTypedToTypedErrors() {
        final Validation<String, Person> allValidation =
                constructPerson("111-2x-3333", "",
                                ImmutableConstructionValidationTest::createPersonValidation);

        assertThat("validation errors were missed",allValidation.isInvalid(),is(true));
        assertThat("errors content is incomplete",allValidation.getError(),
                   is("SSN string '111-2x-3333' doesn't match pattern '\\d{3}+-\\d{2}+-\\d{4}+'. ID is blank: must not be blank. 20-year-old has no SSN (in 'ssn' parameter): violates SSN required if age > 17 years."));
    }

    @Test
    public void testFromStringlyTypedToTypedGood() {
        final Validation<String, Person> allValidation =
                constructPerson("111-22-3333", "anything-16",
                                ImmutableConstructionValidationTest::createPersonValidation);

        assertThat("valid args resulted in errors", allValidation.isValid(),is(true));
        assertThat("failed to construct entity from valid args",allValidation.get(),is(notNullValue()));
    }

    @Test
    public void testFromStringlyTypedToTypedErrors2() {
        final Validation<String, Person2> allValidation =
                constructPerson("111-2x-3333", "",
                                ImmutableConstructionValidationTest::createPerson2Validation);

        assertThat("validation errors were missed",allValidation.isInvalid(),is(true));
        assertThat("errors content is incomplete",allValidation.getError(),
                   is("SSN string '111-2x-3333' doesn't match pattern '\\d{3}+-\\d{2}+-\\d{4}+'. ID is blank: must not be blank. 20-year-old has no SSN (in 'ssn' parameter): violates SSN required if age > 17 years."));
    }

    @Test
    public void testFromStringlyTypedToTypedGood2() {
        final Validation<String, Person2> allValidation =
                constructPerson("111-22-3333", "anything-16",
                                ImmutableConstructionValidationTest::createPerson2Validation);

        assertThat("valid args resulted in errors", allValidation.isValid(),is(true));
        assertThat("failed to construct entity from valid args",allValidation.get(),is(notNullValue()));
    }

    /*
      When processing e.g. an HTTP request, we'll have strings and things in hand and we'll want to convert them to
      the primitive (value) types used to construct entities like Person. Validation is two-steps:
       1. validate primitive construction
       2. validate entity construction
      While construction proceeds in two steps, we'll want to continue as far as we can. For instance, we'll want to
      proceed into step (2) even if some of step (1) failed.
      Continuing like this is only ok if this method has no side-effects.
     */
     private static <PersonType> Validation<String, PersonType> constructPerson(
             final String ssnString, final String idString,
             final Function2<Validation<String, SSN>, Validation<String, ID>, Validation<String, PersonType>>
                     createPersonValidation) {
        /*
         Build the primitives
         */

         final Validation<String, SSN> ssnv = SSN.buildValidation(ImmutableSSN.builder().ssn(ssnString));
         final Validation<String, ID> idv = ID.buildValidation(ImmutableID.builder().id(idString));

        /*
         Build the entity
         */

         final Validation<String, PersonType> pv = createPersonValidation.apply(ssnv, idv);

        /*
         Capture all errors
         */

         return Validation.combine(ssnv, idv, pv).ap((_ssn, _id, p) -> p).mapError(Validations::combineErrors);
     }

    private static Validation<String, Person> createPersonValidation(
            final Validation<String, SSN> ssnv,
            final Validation<String, ID> idv) {
        final ImmutablePerson.Builder builder =
                ImmutablePerson.builder().firstName("Lucy").lastName("Ricardo").age(20);
        ssnv.peek(ssn -> builder.setValueSsn(ssn));
        idv.peek(id -> builder.setValueId(id));
        return Person.buildValidation(builder);
    }

    // Here we build a Person2 just to see what things might look like using a factory builder instead of an ImmutablePerson
    private static Validation<String, Person2> createPerson2Validation(
            final Validation<String, SSN> ssnv,
            final Validation<String, ID> idv) {
        final Person2ValidationBuilder builder =
                new Person2ValidationBuilder().firstName("Lucy").lastName("Ricardo").age(20);
        // hmm… apparently vavr-encodings doesn't know how to doctor up factory builders to work with validations
        ssnv.peek(ssn -> builder.ssn(Optional.of(ssn)));
        idv.peek(id -> builder.id(Optional.of(id)));
        return builder.build();
    }

}
