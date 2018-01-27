[immutables.org](http://immutables.org) powers builder-based immutable objects for Java. [vavr.io](http://www.vavr.io/) provides [`Validation`](https://static.javadoc.io/io.vavr/vavr/0.9.2/io/vavr/control/Validation.html), a very useful applicative functor, to let you collect all your validation errors before responding to your caller.

This project shows how you might use these together. It defines an entity class, `Person`, which has various simple value objects as attributes. A couple of those: `SSN` and `ID` are themselves immutable-enabled.

These classes make use of immutables.io `@Value.Check` annotation to define check methods for each class. Immutables.io calls these "normalizing" check methods because these return the target object (as opposed to `void`).

What I was after, was the ability to construct an object of type `T` and get back `Validation<String,T>`. The pattern I established is as follows:

Put the validation code for your immutable, in a (normalizing) check method, pretty much as usual. The wrinkle here, is that this check method is delegating to another method, one that returns a `Validation`. That other method does all the actual validation.

Here is an example from the `SSN` class:

```java
@Value.Check
protected SSN check() {
    return getValidation(ssn())
            .map(x->this)
            // toEither().getOrElseThrow() is required because https://github.com/vavr-io/vavr/issues/2207
            .toEither()
            // can't use method reference here: compiler (or at least IntelliJ) finds it ambiguous
            .getOrElseThrow(errors->new IllegalStateException(errors));
}
```

Here's the method that does the actual validation for `SSN`:

```java

private static Validation<String, String> getValidation(final String content) {
    return Validations.notBlank(content, "SSN")
                      .combine(Validations.matches(content,"SSN", pattern))
                      .ap((ssn1,ssn2)->ssn1)
                      .mapError(Validations::combineErrors);
}
```

Finally, to construct the object and get back a validation object (which might contain one or more errors), you use the builder and pass it to a utility method:

```java
final Validation<String,SSN> ssnv = SSN.toValidation(ImmutableSSN.builder().ssn("111-2p-3333"));
assertThat(ssnv.isInvalid(),is(true));
assertThat(ssnv.getError(),is("SSN string '111-2p-3333' doesn't match pattern '\\d{3}+-\\d{2}+-\\d{4}+'."));
```

Constructing entity objects (with many attributes) and also two-phase construction: constructing simple objects from strings and then constructing entity objects from those simple objects, is demonstrated in the tests.

This approach looks like it could be useful for constructing domain objects from e.g. web requests. Unlike the Rails approach of storing errors directly in your domain (`ActiveRecord`) objects, this approach keeps the errors completely separate. The value of that is that your domain objects can enforce constraints internally. Which was the whole point, wasn't it?

To make this work, I had to define a `toValidation(some-target-class.Builder)` method on each target class. It would have been real nice if Immutables.io provided a way for me to put such a method right on the generated builder (given my validation method). See [immutables.io issue #451](https://github.com/immutables/immutables/issues/451) for the discussion and my proposal. 
