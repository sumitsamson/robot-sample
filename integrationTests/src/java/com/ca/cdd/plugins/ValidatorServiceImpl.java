package com.ca.cdd.plugins;

import com.google.common.base.Strings;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by abrpn01 on 2/7/2016.
 */
public class ValidatorServiceImpl implements ValidatorService {

    private Validator validator;

    public ValidatorServiceImpl() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Override
    public <T> void validate(T entity) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        String errorMessage = getEntityErrorMessages(violations);
        if (!Strings.isNullOrEmpty(errorMessage)) {
            throw new RobotException(errorMessage);
        }
    }

    private <T> String getEntityErrorMessages(Set<ConstraintViolation<T>> violations) {
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            Iterator<ConstraintViolation<T>> iterator = violations.iterator();
            while (iterator.hasNext()) {
                ConstraintViolation<T> violation = iterator.next();
                sb.append("field: ");
                sb.append(violation.getPropertyPath());
                sb.append(", with error: ");
                sb.append(violation.getMessage());
                sb.append('\n');
            }
            return sb.toString();
        }
        return "";
    }
}
