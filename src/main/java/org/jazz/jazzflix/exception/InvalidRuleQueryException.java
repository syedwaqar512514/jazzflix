/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jazz.jazzflix.exception;

/**
 * Exception class for data already exist
 *
 * @author Shahzad Sadiq
 * @since 29/09/2023
 */

public class InvalidRuleQueryException extends RuntimeException {
     private final  String runtimeQuery;

    public InvalidRuleQueryException(String message, String runtimeQuery) {
        super(message);
        this.runtimeQuery = runtimeQuery;
    }

    public String getRuntimeQuery() {
        return runtimeQuery;
    }

}