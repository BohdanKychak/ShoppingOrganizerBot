package org.example.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AmountException extends RuntimeException {
    public AmountException(String message) {
        super(message);
    }
}
