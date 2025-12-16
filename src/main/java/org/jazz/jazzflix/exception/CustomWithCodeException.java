/*Author Name:muhammad.anas
Project Name: zconnect_backoffice
Package Name:com.mfs.zconnect_backoffice.utils
Class Name: CustomDataNotFoundException
Date and Time:11/11/2023 11:10 AM
Version:1.0*/
package org.jazz.jazzflix.exception;

public class CustomWithCodeException extends RuntimeException {

    private String responseCode;

    public CustomWithCodeException(String message, String responseCode) {
        super(message);
        this.responseCode = responseCode;
    }

    public String getResponseCode() {
        return responseCode;
    }
}
