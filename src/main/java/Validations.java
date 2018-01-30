import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Try;
import io.vavr.control.Validation;

public class Validations {
    public static Validation<String,String> notBlank(final String content, final String parameterName) {
        return StringUtils.isBlank(content) ?
               Validation.invalid(String.format("%s is blank: must not be blank.",parameterName)) :
               Validation.valid(content);
    }

    public static Validation<String,String> maximumLength(final String content, final String parameterName, final int max) {
        final int length = content.length();
        return null != content && length > max ?
               Validation.invalid(String.format("%d character %s is too long: exceeds maximum name length of %d characters.",
                                                length, parameterName, max)) :
               Validation.valid(content);
    }

    public static Validation<String,String> matches(final String content, final String parameterName, final Pattern pattern) {
        return null != content && pattern.matcher(content).matches() ?
               Validation.valid(content) :
               Validation.invalid(formatBadMatch(content, parameterName, pattern));
    }

    public static String combineErrors(final Seq<String> errors) {
        return errors.mkString(" ");
    }

    private static String formatBadMatch(final String content, final String parameterName, final Pattern pattern) {
        final String str = null == content ? "(null string)" : String.format("string '%s'",content);
        return String.format("'%s' parameter containing %s doesn't match pattern '%s'.",
                             parameterName,str,pattern.toString());
    }


}
