/*
* Copyright 2014 Patrick Ott <Patrick.Ott@professional-webworkx.de>.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package de.professional_webworkx.blog.contextmenudemo.model;

/**
 *
 * @author Patrick Ott <Patrick.Ott@professional-webworkx.de>
 * @version 1.0
 */
public class Person {
	private String firstName;
	private String lastName;
	private String eMail;

	public Person(final String firstName, final String lastName, final String eMail) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.eMail = eMail;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String geteMail() {
		return eMail;
	}

	public void seteMail(String eMail) {
		this.eMail = eMail;
	}

	@Override
	public String toString() {
		return firstName + " " + lastName + ", E-Mail: " + eMail;
	}
}