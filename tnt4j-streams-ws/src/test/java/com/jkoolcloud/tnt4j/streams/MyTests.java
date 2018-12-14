/*
 * Copyright 2014-2018 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * The type My tests.
 *
 * @author akausinis
 * @version 1.0
 */
public class MyTests {

	/**
	 * Test curl call.
	 */
	@Test
	public void testCurlCall() {
		String username = "myusername"; // NON-NLS
		String password = "mypassword"; // NON-NLS
		String url = "https://www.example.com/xyz/abc"; // NON-NLS

		String[] command = { "curl", "-H", "Accept:application/json", "-u", username + ":" + password, url }; // NON-NLS
		ProcessBuilder process = new ProcessBuilder(command);

		Process p;
		try {
			p = process.start();
			// p = Runtime.getRuntime().exec("C:\\SysUtils\\curl\\curl -i
			// https://api.github.com/users/octocat/orgs"); //NON-NLS

			String result = Utils.readInput(p.getInputStream(), true);
			System.out.print("IN:\n" + result); // NON-NLS

			result = Utils.readInput(p.getErrorStream(), true);
			System.out.print("ERR:\n" + result); // NON-NLS
		} catch (Exception e) {
			System.out.print("error"); // NON-NLS
			e.printStackTrace();
		}
	}
}
