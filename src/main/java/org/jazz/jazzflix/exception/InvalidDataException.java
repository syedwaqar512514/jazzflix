/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jazz.jazzflix.exception;

/**
 * Exception class for Invalid Data
 *
 * @author Shahzad Sadiq
 * @since 29/09/2023
 */

public class InvalidDataException extends RuntimeException {

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException() {
    }
}