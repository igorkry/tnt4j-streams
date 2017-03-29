package com.jkoolcloud.tnt4j.streams.preparsers;

/**
 * TODO Base class that all activity parsers must extend. It provides some base functionality useful for all activity
 * parsers.
 *
 * @param <O>
 *
 * @version $Revision: 1 $
 */
public interface DataPreParser<O> {
	/**
	 * TODO
	 *
	 * @param data
	 *            activity data package
	 * @return
	 * @throws Exception
	 *
	 * @see com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser#preParse(com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream,
	 *      Object)
	 */
	O preParse(Object data) throws Exception;

	/**
	 * Returns whether this pre-parser supports the given format of the activity data. This is used by activity parsers
	 * to determine if the pre-parser can process activity data in the format that stream provides.
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this pre-parser can process data in the specified format, {@code false} - otherwise
	 */
	boolean isDataClassSupported(Object data);

	/**
	 * Returns type of pre-parsed RAW activity data entries.
	 *
	 * @return type of pre-parsed RAW activity data entries
	 */
	String dataTypeReturned();
}
