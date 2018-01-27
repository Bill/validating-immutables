import org.junit.Test;

import io.vavr.control.Try;
import io.vavr.control.Validation;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

public class SSNTest {
    @Test
    public void testGood() {
        final Try<SSN> ssnt = Try.of(()->ImmutableSSN.of("111-22-3333"));
        assertThat(ssnt.isSuccess(),is(true));
        assertThat(ssnt.get().toString(),is("111-22-3333"));
    }

    @Test
    public void testBad() {
        final Try<SSN> ssnt = Try.of(()->ImmutableSSN.of("111-2p-3333"));
        assertThat(ssnt.isFailure(),is(true));
        assertThat(ssnt.getCause(),is(instanceOf(IllegalStateException.class)));
        assertThat(ssnt.getCause().getMessage(),is("SSN string '111-2p-3333' doesn't match pattern '\\d{3}+-\\d{2}+-\\d{4}+'."));
    }

    @Test
    public void testFactoryConstructionGood() {
        final Validation<String,SSN> ssnv = SSN.toValidation(ImmutableSSN.builder().ssn("111-22-3333"));
        assertThat(ssnv.isValid(),is(true));
        assertThat(ssnv.get().toString(),is("111-22-3333"));
    }
    @Test
    public void testFactoryConstructionBad() {
        final Validation<String,SSN> ssnv = SSN.toValidation(ImmutableSSN.builder().ssn("111-2p-3333"));
        assertThat(ssnv.isInvalid(),is(true));
        assertThat(ssnv.getError(),is("SSN string '111-2p-3333' doesn't match pattern '\\d{3}+-\\d{2}+-\\d{4}+'."));
    }

}
