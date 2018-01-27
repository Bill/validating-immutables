[immutables.org](http://immutables.org) powers builder-based immutable objects for Java. [vavr.io](http://www.vavr.io/) provides [`Validation`](https://static.javadoc.io/io.vavr/vavr/0.9.2/io/vavr/control/Validation.html), a very useful applicative functor, to let you collect all your validation errors before responding to your caller.

This project shows how you might use these together. It defines an entity class, `Person`, which has various simple value objects as attributes. A couple of those: `SSN` and `ID` are themselves immutable-enabled.

These classes make use of immutables.io `@Value.Check` annotation to define check methods for each class. Immutables.io calls these "normalizing" check methods because these return the target object (as opposed to `void`).

What I was after, was the ability to construct an object of type `T` and get back `Validation<String,T>`. In the case of the simple classes like `SSN` and `ID` this took the from of a user-defined factory method e.g.

```java
    public static Validation<String,SSN> create(final String content) {
        return getValidation(content).map(SSN::construct);
    }
```

That method calls a custom per-class method to get the validation:

```java
    private static Validation<String, String> getValidation(final String id) {
        return Validations.notBlank(id, "ID");
    }
```

Note that the hand-crafted check method that the builder will call also leverages that validation:

```java
    @Value.Check
    protected ID check() {
        return getValidation(id())
                .map(id->this)
                // toEither().getOrElseThrow() is required because https://github.com/vavr-io/vavr/issues/2207
                .toEither()
                // can't use method reference here: compiler (or at least IntelliJ) finds it ambiguous
                .getOrElseThrow(errors->new IllegalStateException(errors));
    }
```

And finally, I defined a "back door" construction method to skirt around immutables.io construction+checking:

```java
    private static SSN construct(final String content) {
        return Try.of(()-> {
            final Constructor constructor = ImmutableSSN.class.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return (SSN)constructor.newInstance(content);
        }).get(); // re-raise checked exceptions as unchecked ones yay
    }
```

This wasn't too awful, since simple classes like `SSN` had only one attribute. For complex classes like `Person` you really need to be able to construct through the generated builder. In that case it didn't make sense to define that static factory method since there would be simply too many combinations.
 
 and if it represents an error, I'd like it to throw an exception carrying the `Validation.Error.error` content, somehow.

Hillarity ensues. See [vavr issue #2207](https://github.com/vavr-io/vavr/issues/2207)
