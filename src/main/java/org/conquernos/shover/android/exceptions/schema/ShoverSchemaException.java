package org.conquernos.shover.android.exceptions.schema;

import org.conquernos.shover.android.exceptions.ShoverException;

public class ShoverSchemaException extends ShoverException {

	public ShoverSchemaException() {
	}

	public ShoverSchemaException(String message) {
		super(message);
	}

	public ShoverSchemaException(Throwable cause) {
		super(cause);
	}

	public ShoverSchemaException(String message, Throwable cause) {
		super(message, cause);
	}

}
