/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jazz.jazzflix.exception;

/**
 * Exception class for Domain Not Found
 *
 * @author Shahzad Sadiq
 * @since 29/09/2023
 */

public class DataNotFoundException extends RuntimeException {

    public DataNotFoundException(String message) {
        super(message);
    }

    public DataNotFoundException() {
    }
}