import org.junit.Test;

import io.vavr.control.Try;
import io.vavr.control.Validation;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

// test the generated ID (String alias) class
public class IDTest {
    @Test
    public void testGood() {
        final Try<ID> idt = Try.of(()->ImmutableID.of("hi"));
        assertThat(idt.isSuccess(),is(true));
    }
    @Test
    public void testNull() {
        final Try<ID> idt = Try.of(()->ImmutableID.of(null));
        assertThat(idt.isFailure(),is(true));
        assertThat(idt.getCause(),is(instanceOf(NullPointerException.class)));
        assertThat(idt.getCause().getMessage(),is("id"));
    }
    @Test
    public void testEmpty() {
        final Try<ID> idt = Try.of(()->ImmutableID.of(""));
        assertThat(idt.isFailure(),is(true));
        assertThat(idt.getCause(),is(instanceOf(IllegalStateException.class)));
        assertThat(idt.getCause().getMessage(),is("ID is blank: must not be blank."));
    }
    @Test
    public void testFactoryConstructionGood() {
        final Validation<String,ID> idv = ID.buildValidation(ImmutableID.builder().id("hi"));
        assertThat(idv.isValid(),is(true));
        assertThat(idv.get().toString(),is("hi"));
    }
    @Test
    public void testFactoryConstructionBad() {
        final Validation<String,ID> idv = ID.buildValidation(ImmutableID.builder().id(""));
        assertThat(idv.isInvalid(),is(true));
        assertThat(idv.getError(),is("ID is blank: must not be blank."));
    }

}
